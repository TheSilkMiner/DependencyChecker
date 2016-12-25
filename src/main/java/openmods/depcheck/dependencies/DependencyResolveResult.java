package openmods.depcheck.dependencies;

import java.io.File;
import java.util.*;

import openmods.depcheck.utils.TypedElement;

import com.google.common.collect.*;

public class DependencyResolveResult {

    public interface MissingDependencySink {
        void acceptMissingClass(String targetCls, String sourceMod, String sourceCls, Set<String> versions);

        void acceptMissingElement(String targetCls, String sourceMod, String sourceCls, TypedElement sourceElement, Set<String> versions);
    }

    private static class MissingSourceClass {
        public final Set<String> missingClassVersions = Sets.newHashSet();
        public final SetMultimap<TypedElement, String> missingElementVersions = HashMultimap.create();
    }

    public static class MissingSourceDependencies {
        private final Map<String, MissingSourceClass> missingClasses = Maps.newHashMap();

        private MissingSourceClass getForClass(String cls) {
            return missingClasses.computeIfAbsent(cls, k -> new MissingSourceClass());
        }

        public void addMissingClass(String sourceClass, Collection<String> versions) {
            getForClass(sourceClass).missingClassVersions.addAll(versions);
        }

        public void addMissingElement(String sourceClass, TypedElement element, Collection<String> versions) {
            getForClass(sourceClass).missingElementVersions.putAll(element, versions);
        }
    }

    public static class MissingClassDependencies {
        private final Map<String, MissingSourceDependencies> modToMissingDependencies = Maps.newHashMap();

        public MissingSourceDependencies getOrCreate(String sourceMod) {
            return modToMissingDependencies.computeIfAbsent(sourceMod, k -> new MissingSourceDependencies());
        }
    }

    public final File jarFile;
    private final Map<String, MissingClassDependencies> missingTargetClassDependencies = Maps.newHashMap();

    public DependencyResolveResult(File jarFile) {
        this.jarFile = jarFile;
    }

    public MissingClassDependencies getOrCreate(String targetClass) {
        return missingTargetClassDependencies.computeIfAbsent(targetClass, k -> new MissingClassDependencies());
    }

    public Optional<MissingClassDependencies> get(String targetClass) {
        return Optional.ofNullable(missingTargetClassDependencies.get(targetClass));
    }

    @SuppressWarnings("CodeBlock2Expr")
    public void visit(MissingDependencySink sink) {
        missingTargetClassDependencies.forEach((targetCls, missingTargetDeps) -> {
            missingTargetDeps.modToMissingDependencies.forEach((sourceMod, missingSourceDeps) -> {
                missingSourceDeps.missingClasses.forEach((sourceCls, missingSourceClass) -> {
                    if (!missingSourceClass.missingClassVersions.isEmpty())
                        sink.acceptMissingClass(targetCls, sourceMod, sourceCls, missingSourceClass.missingClassVersions);

                    Multimaps.asMap(missingSourceClass.missingElementVersions)
                            .forEach((sourceElement, versions) -> sink.acceptMissingElement(targetCls, sourceMod, sourceCls, sourceElement, versions));
                });
            });
        });
    }
}
