package openmods.depcheck;

import static j2html.TagCreator.*;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import openmods.depcheck.DependencyResolveResult.MissingDependencies;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.collect.Table.Cell;

public class ResultPrinter {

    private static final String STYLE =
            "table {" +
                    "     border-collapse: collapse;" +
                    "}" +
                    "th, td {" +
                    "    border: 1px solid black;" +
                    "    text-align: center;" +
                    "}" +
                    ".r {" +
                    "    color: red;" +
                    "}" +
                    ".g {" +
                    "    color: green;" +
                    "}" +
                    "div.missing {" +
                    "    display: none;" +
                    "}" +
                    "div.missing:target {" +
                    "    display: block;" +
                    "}";

    private static String createAnchor(String target, String source, String version) {
        return target + "__" + source + "__" + version;
    }

    public void print(File file, SourceDependencies availableDependencies, List<DependencyResolveResult> results) {
        try (OutputStream os = new FileOutputStream(file);
                Writer w = new OutputStreamWriter(os, Charsets.UTF_8)) {

            w.write(document().render());
            final ContainerTag contents = html().with(
                    head().with(
                            meta().attr("charset", "UTF-8"),
                            title("Dependencies"),
                            style().attr("type", "text/css").with(unsafeHtml(STYLE))
                            ),
                    body().with(
                            createEntries(availableDependencies, results)
                            )
                    );
            w.write(contents.render());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static List<Tag> createEntries(SourceDependencies availableDependencies, List<DependencyResolveResult> results) {
        List<Tag> tags = Lists.newArrayList();
        createSourceEntries(tags, availableDependencies, results);
        createTargetEntries(tags, availableDependencies, results);
        createMissingDependenciesEntries(tags, results);
        return tags;

    }

    private static class SourceCompatibilityTable {
        public final Table<File, String, MissingDependencies> missingDependencies = HashBasedTable.create();
        public final Set<String> allVersions = Sets.newTreeSet();
    }

    private static void createSourceEntries(List<Tag> output, SourceDependencies availableDependencies, List<DependencyResolveResult> results) {
        final Set<File> allTargets = Sets.newHashSet();
        final Map<String, SourceCompatibilityTable> compatibilityTable = Maps.newHashMap();

        for (DependencyResolveResult result : results) {
            final File target = result.jarFile;
            allTargets.add(target);

            for (Table.Cell<String, String, MissingDependencies> cell : result.missingDependencies.cellSet()) {
                final String source = cell.getRowKey();
                final String sourceVersion = cell.getColumnKey();
                final MissingDependencies value = cell.getValue();

                SourceCompatibilityTable tmp = compatibilityTable.get(source);

                if (tmp == null) {
                    tmp = new SourceCompatibilityTable();
                    tmp.allVersions.addAll(Sets.newTreeSet(availableDependencies.getMod(source).allVersions()));
                    compatibilityTable.put(source, tmp);
                }

                tmp.missingDependencies.put(target, sourceVersion, value);
            }
        }

        for (Map.Entry<String, SourceCompatibilityTable> e : compatibilityTable.entrySet()) {
            final String source = e.getKey();
            output.add(h2(source));
            output.add(table()
                    .with(
                            thead().with(
                                    tr()
                                            .with(th())
                                            .with(
                                                    e.getValue().allVersions.stream().sorted().map(v -> th().withText(v)).collect(Collectors.toList())
                                            )
                                    )
                    )
                    .with(
                            tbody().with(
                                    createCompatibilityTableRows(source, e.getValue())
                                    )
                    )
                    );
        }
    }

    private static List<Tag> createCompatibilityTableRows(String source, SourceCompatibilityTable value) {
        return value.missingDependencies.rowMap().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> {
            final ContainerTag rowTag = tr();
            final String target = e.getKey().getName();
            rowTag.with(td(target));

            for (String version : value.allVersions) {
                final MissingDependencies missingDeps = e.getValue().get(version);
                rowTag.with(missingDeps == null
                        ? td("\u2611").withClass("g")
                        : td().withClass("r").with(a().withHref("#" + createAnchor(target, source, version)).withText("\u2612")));
            }

            return rowTag;
        }).collect(Collectors.toList());
    }

    private static void createTargetEntries(List<Tag> output, SourceDependencies availableDependencies, List<DependencyResolveResult> requiredDependencies) {
        for (DependencyResolveResult result : requiredDependencies) {
            output.add(h2(result.jarFile.getName()));
            output.add(ul().with(
                    result.missingDependencies
                            .rowMap()
                            .entrySet()
                            .stream()
                            .map(e -> createModEntry(e.getKey(), availableDependencies, e.getValue()))
                            .collect(Collectors.toList())
                    )
                    );

        }
    }

    private static Tag createModEntry(String mod, SourceDependencies availableDependencies, Map<String, MissingDependencies> versions) {
        Set<String> missing = versions.keySet();
        Set<String> all = availableDependencies.getMod(mod).allVersions();
        Set<String> compatible = Sets.difference(all, missing);
        return li()
                .withText(mod)
                .with(
                        ul().with(
                                li().withText("Compatible: " + compatible.stream().sorted().collect(Collectors.joining(", "))),
                                li().withText("Not compatible: " + missing.stream().sorted().collect(Collectors.joining(", ")))
                                )
                );
    }

    private static void createMissingDependenciesEntries(List<Tag> output, List<DependencyResolveResult> results) {
        for (DependencyResolveResult dep : results) {
            final String target = dep.jarFile.getName();
            for (Cell<String, String, MissingDependencies> c : dep.missingDependencies.cellSet()) {
                final String source = c.getRowKey();
                final String version = c.getColumnKey();

                final List<Tag> tags = Lists.newArrayList();
                tags.add(h3(target + ":" + source + ":" + version));
                addMissingElementEntry(tags, "Classes", c.getValue().missingClasses);
                addMissingElementEntry(tags, "Fields", c.getValue().missingFields);
                addMissingElementEntry(tags, "Methods", c.getValue().missingMethods);

                output.add(div().withClass("missing").withId(createAnchor(target, source, version)).with(tags));
            }
        }
    }

    private static <T> void addMissingElementEntry(List<Tag> output, String name, Multimap<String, T> missingElements) {
        if (missingElements.isEmpty())
            return;

        output.add(h4(name));

        for (Map.Entry<String, Collection<T>> e : missingElements.asMap().entrySet()) {
            output.add(h5(e.getKey()));
            final ContainerTag p = pre();
            output.add(p);
            for (T missingElement : e.getValue())
                p.withText(missingElement.toString() + "\n");
        }
    }

}
