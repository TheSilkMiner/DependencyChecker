package openmods.depcheck;

import static j2html.TagCreator.*;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import openmods.depcheck.DependencyResolveResult.MissingDependencySink;
import openmods.depcheck.utils.TypedElement;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.*;

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

    private static class MissingSourceDependencies {
        public final Set<String> missingClasses = Sets.newHashSet();
        public final SetMultimap<String, TypedElement> missingElements = HashMultimap.create();
    }

    private static class MissingTargetDependencies {
        private final Map<String, MissingSourceDependencies> targetClass = Maps.newHashMap();

        private MissingSourceDependencies get(String targetCls) {
            return targetClass.computeIfAbsent(targetCls, k -> new MissingSourceDependencies());
        }
    }

    private static class SourceModCompatibilityTable {
        // (target, version) -> missing stuff
        public final Table<File, ArtifactVersion, MissingTargetDependencies> missingDependencies = HashBasedTable.create();

        public MissingTargetDependencies getOrCreate(File target, ArtifactVersion sourceVersion) {
            MissingTargetDependencies result = missingDependencies.get(target, sourceVersion);
            if (result == null) {
                result = new MissingTargetDependencies();
                missingDependencies.put(target, sourceVersion, result);
            }

            return result;
        }
    }

    private static class CompatibilityData {
        private final Set<File> allTargets = Sets.newHashSet();
        private final Map<String, SourceModCompatibilityTable> modCompatibilityTable = Maps.newHashMap();

        private SourceModCompatibilityTable get(String sourceMod) {
            return modCompatibilityTable.computeIfAbsent(sourceMod, k -> new SourceModCompatibilityTable());
        }

        private MissingDependencySink createForTarget(File target) {
            return new MissingDependencySink() {
                @Override
                public void acceptMissingClass(String targetCls, String sourceMod, String sourceCls, Set<String> versions) {
                    final SourceModCompatibilityTable modDeps = get(sourceMod);

                    for (String version : versions) {
                        final ArtifactVersion v = new DefaultArtifactVersion(version);
                        modDeps.getOrCreate(target, v).get(targetCls).missingClasses.add(sourceCls);
                    }
                }

                @Override
                public void acceptMissingElement(String targetCls, String sourceMod, String sourceCls, TypedElement sourceElement, Set<String> versions) {
                    final SourceModCompatibilityTable modDeps = get(sourceMod);
                    for (String version : versions) {
                        final ArtifactVersion v = new DefaultArtifactVersion(version);
                        modDeps.getOrCreate(target, v).get(targetCls).missingElements.put(sourceCls, sourceElement);
                    }
                }
            };
        }

        public void load(DependencyResolveResult deps) {
            allTargets.add(deps.jarFile);
            deps.visit(createForTarget(deps.jarFile));
        }
    }

    private static CompatibilityData convertData(List<DependencyResolveResult> results) {
        final CompatibilityData result = new CompatibilityData();
        results.forEach(result::load);
        return result;
    }

    private static String createAnchor(String target, String source, ArtifactVersion version) {
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
        final CompatibilityData data = convertData(results);
        createSourceEntries(tags, availableDependencies, data);
        createMissingDependenciesEntries(tags, data);
        return tags;

    }

    private static void createSourceEntries(List<Tag> output, SourceDependencies availableDependencies, CompatibilityData data) {
        final List<File> allTargets = Lists.newArrayList(data.allTargets);
        allTargets.sort(Comparator.comparing(File::getName));
        availableDependencies.getAllModIds().stream().sorted().forEach(source -> {
            final SourceModCompatibilityTable compatibilityTable = data.get(source);

            final List<ArtifactVersion> allVersions = availableDependencies.getMod(source).allVersions().stream().map(DefaultArtifactVersion::new).sorted().collect(Collectors.toList());
            output.add(h2(source));
            output.add(table()
                    .with(
                            thead().with(
                                    tr()
                                            .with(th())
                                            .with(
                                                    allVersions.stream().map(v -> th().withText(v.toString())).collect(Collectors.toList())
                                            )
                                    )
                    )
                    .with(
                            tbody().with(
                                    createCompatibilityTableRows(source, allTargets, allVersions, compatibilityTable)
                                    )
                    )
                    );
        });
    }

    private static List<Tag> createCompatibilityTableRows(String source, List<File> allTargets, List<ArtifactVersion> allVersions,
            SourceModCompatibilityTable table) {
        return allTargets.stream().map(target -> {
            final ContainerTag rowTag = tr();
            final String targetName = target.getName();
            rowTag.with(td(targetName));

            for (ArtifactVersion version : allVersions) {
                rowTag.with(table.missingDependencies.contains(target, version)
                        ? td().withClass("r").with(a().withHref("#" + createAnchor(targetName, source, version)).withText("\u2612"))
                        : td("\u2611").withClass("g"));
            }

            return rowTag;
        }).collect(Collectors.toList());
    }

    private static void createMissingDependenciesEntries(List<Tag> output, CompatibilityData data) {
        data.modCompatibilityTable.forEach((source, compatiblityTable) -> {
            compatiblityTable.missingDependencies.cellSet().forEach(e -> {
                final String target = e.getRowKey().getName();

                final ArtifactVersion version = e.getColumnKey();

                final List<Tag> tags = Lists.newArrayList();
                {
                    tags.add(h3(target + ":" + source + ":" + version));

                    e.getValue().targetClass.forEach((targetCls, missingSourceElements) -> {
                        tags.add(h4(targetCls));
                        final List<String> missing = Lists.newArrayList();

                        missing.addAll(missingSourceElements.missingClasses);
                        missingSourceElements.missingElements.entries().forEach(el -> missing.add(el.getKey() + " " + el.getValue()));

                        missing.sort(Comparator.naturalOrder());
                        tags.add(pre().withText(Joiner.on('\n').join(missing)));
                    });
                }

                output.add(div().withClass("missing").withId(createAnchor(target, source, version)).with(tags));
            });
        });
    }

}
