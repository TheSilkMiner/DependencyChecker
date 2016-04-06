package openmods.depcheck;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import openmods.depcheck.DependencyResolveResult.MissingClassDependencies;
import openmods.depcheck.TargetParser.TargetClassVisitor;
import openmods.depcheck.TargetParser.TargetModContentsVisitor;
import openmods.depcheck.TargetParser.TargetModVisitor;
import openmods.depcheck.utils.ElementType;
import openmods.depcheck.utils.TypedElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DependencyCollector implements TargetModVisitor {

    private static final Logger logger = LoggerFactory.getLogger(DependencyCollector.class);

    private static class ClassDependencyVisitor implements TargetClassVisitor {
        private final MissingClassDependencies missingDependencies;
        private final SourceDependencies availableDependencies;

        public ClassDependencyVisitor(SourceDependencies availableDependencies, MissingClassDependencies missingDependencies) {
            this.availableDependencies = availableDependencies;
            this.missingDependencies = missingDependencies;
        }

        @Override
        public void visitRequiredClass(String requiredClsName) {
            final Optional<ModInfo> maybeMod = availableDependencies.identifyMod(requiredClsName);
            if (maybeMod.isPresent()) {
                final ModInfo mod = maybeMod.get();
                logger.trace("Adding class dependency {} from {}", requiredClsName, mod.modId);

                final Set<String> matchingVersions = mod.findMatchingVersions(requiredClsName);
                final Set<String> missingVersions = Sets.difference(mod.allVersions(), matchingVersions);

                if (!missingVersions.isEmpty())
                    missingDependencies.getOrCreate(mod.modId).addMissingClass(requiredClsName, missingVersions);
            } else {
                logger.trace("Class dependency {} does not belong to any known mod, discarding", requiredClsName);
            }
        }

        @Override
        public void visitRequiredElement(String requiredCls, ElementType type, String fieldName, String fieldDesc) {
            final Optional<ModInfo> maybeMod = availableDependencies.identifyMod(requiredCls);
            if (maybeMod.isPresent()) {
                final ModInfo mod = maybeMod.get();
                logger.trace("Adding {} dependency to {} {} from {}", type, fieldName, fieldDesc, mod.modId);

                final Set<String> matchingVersions = mod.findMatchingVersions(requiredCls, type, fieldName, fieldDesc);
                final Set<String> missingVersions = Sets.difference(mod.allVersions(), matchingVersions);

                if (!missingVersions.isEmpty())
                    missingDependencies.getOrCreate(mod.modId).addMissingElement(requiredCls, new TypedElement(type, fieldName, fieldDesc), missingVersions);
            } else {
                logger.trace("{} dependency {} {} does not belong to any known mod, discarding", type, fieldName, fieldDesc);
            }
        }

    }

    private final SourceDependencies availableDependencies;

    private final List<DependencyResolveResult> results = Lists.newArrayList();

    public DependencyCollector(SourceDependencies availableDependencies) {
        this.availableDependencies = availableDependencies;
    }

    @Override
    public TargetModContentsVisitor visitFile(File file) {
        logger.trace("Visiting mod file {}", file.getAbsolutePath());
        DependencyResolveResult result = new DependencyResolveResult(file);
        results.add(result);

        return new TargetModContentsVisitor() {
            @Override
            public TargetClassVisitor visitClass(String targetClass) {
                logger.trace("Visiting mod class {}", targetClass);
                return new ClassDependencyVisitor(availableDependencies, result.getOrCreate(targetClass));
            }
        };
    }

    public List<DependencyResolveResult> getResults() {
        return results;
    }

}
