package openmods.depcheck;

import java.io.File;
import java.util.List;

public class DependencyChecker {

    public static void main(String[] args) {
        final File topDir = new File("data");
        final SourceParser depWalker = new SourceParser(topDir);
        final SourceDependencies availableDependencies = depWalker.collectAvailableDependencies();

        DependencyCollector collector = new DependencyCollector(availableDependencies);
        new TargetParser(topDir).accept(collector);

        final List<DependencyResolveResult> results = collector.getResults();
        new ResultPrinter().print(new File(topDir, "output.html"), availableDependencies, results);
    }
}
