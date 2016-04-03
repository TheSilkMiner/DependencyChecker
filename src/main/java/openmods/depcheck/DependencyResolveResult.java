package openmods.depcheck;

import java.io.File;

import openmods.depcheck.utils.Field;

import org.objectweb.asm.commons.Method;

import com.google.common.collect.*;

public class DependencyResolveResult {

    public static class ClassElement<T> {
        public final String cls;
        public final T element;

        public ClassElement(String cls, T element) {
            this.cls = cls;
            this.element = element;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof ClassElement) {
                ClassElement<?> other = (ClassElement<?>)o;
                return cls.equals(other.cls) && element.equals(other.element);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return cls.hashCode() ^ element.hashCode();
        }

        @Override
        public String toString() {
            return cls + ": " + element;
        }
    }

    public static class MissingDependencies {
        public final Multimap<String, String> missingClasses = HashMultimap.create();

        public final Multimap<String, ClassElement<Method>> missingMethods = HashMultimap.create();

        public final Multimap<String, ClassElement<Field>> missingFields = HashMultimap.create();
    }

    public final File jarFile;

    public final Table<String, String, MissingDependencies> missingDependencies = HashBasedTable.create();

    public DependencyResolveResult(File jarFile) {
        this.jarFile = jarFile;
    }

    public MissingDependencies getOrCreate(String modId, String version) {
        MissingDependencies result = missingDependencies.get(modId, version);
        if (result == null) {
            result = new MissingDependencies();
            missingDependencies.put(modId, version, result);
        }

        return result;
    }

}
