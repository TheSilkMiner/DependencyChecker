package openmods.depcheck;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import openmods.depcheck.utils.ElementType;
import openmods.depcheck.utils.LibClassChecker;
import openmods.depcheck.utils.TypedElement;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ModInfo implements Serializable {

    private static final long serialVersionUID = -8439947254870180769L;

    public class ModRegistrationContext {
        private final String modVersion;
        private boolean notEmpty;

        private ModRegistrationContext(String modVersion) {
            this.modVersion = modVersion;
        }

        private ClassVersions getOrCreateClass(String clsName) {
            return classes.computeIfAbsent(clsName, k -> new ClassVersions());
        }

        public void registerClass(String clsName, String superClass, Set<String> interfaces) {
            final ClassVersions cls = getOrCreateClass(clsName);
            cls.createForVersion(modVersion, superClass, interfaces);
            notEmpty = true;
        }

        public void registerElement(String clsName, ElementType type, String name, String desc) {
            final ClassVersions cls = getOrCreateClass(clsName);
            final ClassVersion cv = cls.getForVersion(modVersion);
            cv.elements.add(new TypedElement(type, name, desc));
            notEmpty = true;
        }

        public boolean isEmpty() {
            return !notEmpty;
        }
    }

    public static class ClassVersion implements Serializable {
        private static final long serialVersionUID = 3023800591787115776L;

        public final String superClass;
        public final Set<String> interfaces;

        private final Set<TypedElement> elements = Sets.newHashSet();

        public ClassVersion(String superClass, Set<String> interfaces) {
            this.superClass = superClass;
            this.interfaces = ImmutableSet.copyOf(interfaces);
        }
    }

    public static class ClassVersions implements Serializable {
        private static final long serialVersionUID = 2659734086399983238L;

        public final Map<String, ClassVersion> versions = Maps.newHashMap();

        public void createForVersion(String version, String superClass, Set<String> interfaces) {
            versions.put(version, new ClassVersion(superClass, interfaces));
        }

        public ClassVersion getForVersion(String version) {
            return versions.get(version);
        }
    }

    private transient boolean isUpdated;

    public final String pkgPrefix;
    public final String modId;

    private final Set<String> allVersions = Sets.newHashSet();

    private final Map<String, ClassVersions> classes = Maps.newHashMap();

    public ModInfo(String pkgPrefix, String modId) {
        this.pkgPrefix = pkgPrefix;
        this.modId = modId;
    }

    public ModRegistrationContext registerVersion(String version) {
        final boolean isNew = allVersions.add(version);
        Preconditions.checkState(isNew, "Duplicate version '%s' in mod %s", version, modId);
        isUpdated = true;
        return new ModRegistrationContext(version);
    }

    public boolean hasVersion(String version) {
        return allVersions.contains(version);
    }

    public Set<String> allVersions() {
        return allVersions;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public boolean matchPackage(String pkg) {
        return pkg.startsWith(pkgPrefix);
    }

    public Set<String> findMatchingVersions(String cls) {
        final ClassVersions classVersions = classes.get(cls);
        return classVersions != null ? classVersions.versions.keySet() : Sets.newHashSet();
    }

    private boolean isElementInVersion(String cls, TypedElement element, String version) {
        if (cls.startsWith("java."))
            return LibClassChecker.isElementInClass(cls, element);

        final ClassVersions classVersions = classes.get(cls);
        if (classVersions == null)
            return false;

        final ClassVersion classVersion = classVersions.getForVersion(version);
        if (classVersion == null)
            return false;

        if (classVersion.elements.contains(element))
            return true;

        if (isElementInVersion(classVersion.superClass, element, version))
            return true;

        for (String intf : classVersion.interfaces)
            if (isElementInVersion(intf, element, version))
                return true;

        return false;
    }

    private Set<String> selectClassVersions(String cls, TypedElement element) {
        final ClassVersions classVersions = classes.get(cls);
        if (classVersions == null)
            return Sets.newHashSet();

        return classVersions.versions.keySet().stream()
                .filter(version -> isElementInVersion(cls, element, version))
                .collect(Collectors.toSet());
    }

    public Set<String> findMatchingVersions(String cls, ElementType type, String name, String desc) {
        return selectClassVersions(cls, new TypedElement(type, name, desc));
    }
}
