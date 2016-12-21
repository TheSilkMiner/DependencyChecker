package openmods.depcheck;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyChecker {

    private static final Logger logger = LoggerFactory.getLogger(DependencyChecker.class);

    public static void main(String... args) {
        if (args.length == 0)
            args = new String[] { "data" };

        for (String dir : args) {
            final File topDir = new File(dir);
            logger.info("Processing dir: {}", topDir.getAbsolutePath());
            final SourceParser depWalker = new SourceParser(topDir);
            final SourceDependencies availableDependencies = depWalker.collectAvailableDependencies();

            DependencyCollector collector = new DependencyCollector(availableDependencies);
            new TargetParser(topDir).accept(collector);

            final List<DependencyResolveResult> results = collector.getResults();
            new ResultPrinter().print(new File(topDir, "output.html"), availableDependencies, results);
        }
    }
}
