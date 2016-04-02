package openmods.depcheck;

import static j2html.TagCreator.*;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import openmods.depcheck.DependencyResolveResult.MissingDependencies;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ResultPrinter {

    public void print(File file, SourceDependencies availableDependencies, List<DependencyResolveResult> results) {
        try (OutputStream os = new FileOutputStream(file);
                Writer w = new OutputStreamWriter(os, Charsets.UTF_8)) {

            w.write(document().render());
            final ContainerTag contents = html().with(
                    head().with(
                            meta().attr("charser", "UTF-8"),
                            title("Dependencies")
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
        for (DependencyResolveResult result : results) {
            tags.add(h1(result.jarFile.getName()));
            tags.add(ul().with(
                    result.missingDependencies
                            .rowMap()
                            .entrySet()
                            .stream()
                            .map(e -> createModEntry(e.getKey(), availableDependencies, e.getValue()))
                            .collect(Collectors.toList())
                    )
                    );

        }
        return tags;

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
}
