package openmods.depcheck;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import openmods.depcheck.DependencyResolveResult.MissingDependencies;
import openmods.depcheck.TargetParser.TargetClassVisitor;
import openmods.depcheck.TargetParser.TargetModContentsVisitor;
import openmods.depcheck.TargetParser.TargetModVisitor;
import openmods.depcheck.utils.ClassElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DependencyCollector implements TargetModVisitor {

    private static final Logger logger = LoggerFactory.getLogger(DependencyCollector.class);

    private class ClassDependencyVisitor implements TargetClassVisitor {
        private final DependencyResolveResult result;

        private final String targetClsName;

        public ClassDependencyVisitor(DependencyResolveResult result, String targetClsName) {
            this.result = result;
            this.targetClsName = targetClsName;
        }

        @Override
        public void visitRequiredClass(String requiredClsName) {
            final Optional<ModInfo> maybeMod = availableDependencies.identifyMod(requiredClsName);
            if (maybeMod.isPresent()) {
                final ModInfo mod = maybeMod.get();
                logger.trace("Adding class dependency {} from {}", requiredClsName, mod.modId);

                final Set<String> matchingVersions = mod.matchClass(requiredClsName);
                final Set<String> missingVersions = Sets.difference(mod.allVersions(), matchingVersions);

                for (String version : missingVersions) {
                    final MissingDependencies missingDeps = result.getOrCreate(mod.modId, version);
                    missingDeps.missingClasses.put(targetClsName, requiredClsName);
                }
            } else {
                logger.trace("Class dependency {} does not belong to any known mod, discarding", requiredClsName);
            }
        }

        @Override
        public void visitRequiredField(String requiredCls, String fieldName, String fieldDesc) {
            final Optional<ModInfo> maybeMod = availableDependencies.identifyMod(requiredCls);
            if (maybeMod.isPresent()) {
                final ModInfo mod = maybeMod.get();
                logger.trace("Adding field dependency {} {} from {}", fieldName, fieldDesc, mod.modId);

                final Set<String> matchingVersions = mod.matchField(requiredCls, fieldName, fieldDesc);
                final Set<String> missingVersions = Sets.difference(mod.allVersions(), matchingVersions);

                for (String version : missingVersions) {
                    final MissingDependencies missingDeps = result.getOrCreate(mod.modId, version);
                    missingDeps.missingFields.put(targetClsName, new ClassElement(requiredCls, fieldName, fieldDesc));
                }
            } else {
                logger.trace("Field dependency {} {} does not belong to any known mod, discarding", fieldName, fieldDesc);
            }
        }

        @Override
        public void visitRequiredMethod(String requiredCls, String methodName, String methodDesc) {
            final Optional<ModInfo> maybeMod = availableDependencies.identifyMod(requiredCls);
            if (maybeMod.isPresent()) {
                final ModInfo mod = maybeMod.get();
                logger.trace("Adding method dependency {} {} from {}", methodName, methodDesc, mod.modId);

                final Set<String> matchingVersions = mod.matchMethod(requiredCls, methodName, methodDesc);
                final Set<String> missingVersions = Sets.difference(mod.allVersions(), matchingVersions);

                for (String version : missingVersions) {
                    final MissingDependencies missingDeps = result.getOrCreate(mod.modId, version);
                    missingDeps.missingMethods.put(targetClsName, new ClassElement(requiredCls, methodName, methodDesc));
                }
            } else {
                logger.trace("Method dependency {} {} does not belong to any known mod, discarding", methodName, methodDesc);
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
            public TargetClassVisitor visitClass(String cls) {
                logger.trace("Visiting mod class {}", cls);
                return new ClassDependencyVisitor(result, cls);
            }
        };
    }

    public List<DependencyResolveResult> getResults() {
        return results;
    }

}
