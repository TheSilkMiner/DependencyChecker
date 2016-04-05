package openmods.depcheck;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import openmods.depcheck.utils.TypedElement;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ModInfo implements Serializable {

    private static final long serialVersionUID = -8439947254870180769L;

    public class ModRegistrationContext {
        private final String modVersion;

        private ModRegistrationContext(String modVersion) {
            this.modVersion = modVersion;
        }

        private ClassVersions getOrCreateClass(String clsName) {
            ClassVersions cls = classes.get(clsName);

            if (cls == null) {
                cls = new ClassVersions();
                classes.put(clsName, cls);
            }
            return cls;
        }

        public void registerClass(String clsName, String superClass, Set<String> interfaces) {
            final ClassVersions cls = getOrCreateClass(clsName);
            cls.createForVersion(modVersion, superClass, interfaces);
        }

        public void registerMethod(String clsName, String name, String desc) {
            final ClassVersions cls = getOrCreateClass(clsName);
            final ClassVersion cv = cls.getForVersion(modVersion);
            cv.methods.add(new TypedElement(name, desc));
        }

        public void registerField(String clsName, String name, String desc) {
            final ClassVersions cls = getOrCreateClass(clsName);
            final ClassVersion cv = cls.getForVersion(modVersion);
            cv.fields.add(new TypedElement(name, desc));
        }
    }

    public static class ClassVersion implements Serializable {
        private static final long serialVersionUID = 3023800591787115776L;

        public final String superClass;

        public final Set<String> interfaces;

        private final Set<TypedElement> methods = Sets.newHashSet();
        private final Set<TypedElement> fields = Sets.newHashSet();

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
        return new ModRegistrationContext(version);
    }

    public boolean hasVersion(String version) {
        return allVersions.contains(version);
    }

    public Set<String> allVersions() {
        return allVersions;
    }

    public boolean matchPackage(String pkg) {
        return pkg.startsWith(pkgPrefix);
    }

    public Set<String> matchClass(String cls) {
        final ClassVersions classVersions = classes.get(cls);
        return classVersions != null ? classVersions.versions.keySet() : Sets.newHashSet();
    }

    private boolean isElementInVersion(String cls, Predicate<ClassVersion> predicate, String version) {
        if (cls.equals("java.lang.Object"))
            return false;

        final ClassVersions classVersions = classes.get(cls);
        if (classVersions == null)
            return false;

        final ClassVersion classVersion = classVersions.getForVersion(version);
        if (classVersion == null)
            return false;

        if (predicate.test(classVersion))
            return true;

        if (isElementInVersion(classVersion.superClass, predicate, version))
            return true;

        for (String intf : classVersion.interfaces)
            if (isElementInVersion(intf, predicate, version))
                return true;

        return false;
    }

    private Set<String> selectClassVersions(String cls, Predicate<String> predicate) {
        final ClassVersions classVersions = classes.get(cls);
        if (classVersions == null)
            return Sets.newHashSet();

        Set<String> result = Sets.newHashSet();
        for (Map.Entry<String, ClassVersion> e : classVersions.versions.entrySet()) {
            final String version = e.getKey();

            if (predicate.test(version))
                result.add(version);
        }

        return result;
    }

    public Set<String> matchMethod(String cls, String name, String desc) {
        final TypedElement e = new TypedElement(name, desc);
        return selectClassVersions(cls, version -> isElementInVersion(cls, cv -> cv.methods.contains(e), version));
    }

    public Set<String> matchField(String cls, String name, String desc) {
        final TypedElement e = new TypedElement(name, desc);
        return selectClassVersions(cls, version -> isElementInVersion(cls, cv -> cv.fields.contains(e), version));
    }

}
