package openmods.depcheck.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.objectweb.asm.Type;

public enum ElementType {
    FIELD {
        @Override
        public boolean isInClass(Class<?> cls, String name, String desc) {
            try {
                final Field f = cls.getDeclaredField(name);
                final String actualDesc = Type.getDescriptor(f.getType());
                return desc.equals(actualDesc);
            } catch (NoSuchFieldException e) {
                return false;
            }
        }
    },
    METHOD {
        @Override
        public boolean isInClass(Class<?> cls, String name, String desc) {
            for (Method m : cls.getDeclaredMethods())
                if (m.getName().equals(name) &&
                        Type.getMethodDescriptor(m).equals(desc))
                    return true;

            return false;
        }
    };

    public abstract boolean isInClass(Class<?> cls, String name, String desc);
}
