package openmods.depcheck.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.objectweb.asm.Type;

public enum ElementType {
    FIELD {
        @Override
        public boolean isInClass(Class<?> cls, String name, String desc) {
            try {
                final Field f = cls.getDeclaredField(name);
                final String actualDesc = Type.getDescriptor(f.getType());
                return Objects.equals(desc, actualDesc);
            } catch (NoSuchFieldException e) {
                return false;
            }
        }
    },
    METHOD {
        @Override
        public boolean isInClass(Class<?> cls, String name, String desc) {
            for (Method m : cls.getDeclaredMethods())
                if (Objects.equals(m.getName(), name) &&
                        Objects.equals(Type.getMethodDescriptor(m), desc))
                    return true;

            return false;
        }
    };

    public abstract boolean isInClass(Class<?> cls, String name, String desc);
}
