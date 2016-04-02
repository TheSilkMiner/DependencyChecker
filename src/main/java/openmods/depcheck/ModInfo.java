package openmods.depcheck;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import openmods.depcheck.utils.Field;

import org.objectweb.asm.commons.Method;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ModInfo {
    public final String pkgPrefix;
    public final String modId;

    private final Map<String, File> allVersions = Maps.newHashMap();

    public static class ClassVersion {
        public final String superClass;

        public final Set<String> interfaces;

        private final Set<Method> methods = Sets.newHashSet();
        private final Set<Field> fields = Sets.newHashSet();

        public ClassVersion(String superClass, Set<String> interfaces) {
            this.superClass = superClass;
            this.interfaces = ImmutableSet.copyOf(interfaces);
        }
    }

    public static class ClassVersions {
        public final Map<String, ClassVersion> versions = Maps.newHashMap();

        public void createForVersion(String version, String superClass, Set<String> interfaces) {
            ClassVersion cv = new ClassVersion(superClass, interfaces);
            versions.put(version, cv);
        }

        public ClassVersion getForVersion(String version) {
            return versions.get(version);
        }
    }

    private final Map<String, ClassVersions> classes = Maps.newHashMap();

    public ModInfo(String pkgPrefix, String modId) {
        this.pkgPrefix = pkgPrefix;
        this.modId = modId;
    }

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

        public void registerMethod(String clsName, Method method) {
            final ClassVersions cls = getOrCreateClass(clsName);
            final ClassVersion cv = cls.getForVersion(modVersion);
            cv.methods.add(method);
        }

        public void registerField(String clsName, Field field) {
            final ClassVersions cls = getOrCreateClass(clsName);
            final ClassVersion cv = cls.getForVersion(modVersion);
            cv.fields.add(field);
        }
    }

    public ModRegistrationContext registerVersion(String version, File source) {
        final File prev = allVersions.put(version, source);
        Preconditions.checkState(prev == null, "Duplicate version '%s': %s -> %s", version, prev, source);
        return new ModRegistrationContext(version);
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

    private Set<String> matchElement(String cls, Predicate<String> predicate) {
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

    public Set<String> matchMethod(String cls, Method method) {
        return matchElement(cls, version -> isElementInVersion(cls, cv -> cv.methods.contains(method), version));
    }

    public Set<String> matchField(String cls, Field field) {
        return matchElement(cls, version -> isElementInVersion(cls, cv -> cv.fields.contains(field), version));
    }

    public Set<String> allVersions() {
        return allVersions.keySet();
    }
}
