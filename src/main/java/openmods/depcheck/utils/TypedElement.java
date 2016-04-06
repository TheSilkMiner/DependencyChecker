package openmods.depcheck.utils;

import java.io.Serializable;

public class TypedElement implements Serializable {

    private static final long serialVersionUID = 8206186136037881452L;

    public final ElementType type;

    public final String name;

    public final String desc;

    public TypedElement(ElementType type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TypedElement) {
            TypedElement other = (TypedElement)o;
            return this.type.equals(other.type)
                    && this.name.equals(other.name)
                    && this.desc.equals(other.desc);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode() ^ name.hashCode() ^ desc.hashCode();
    }

    @Override
    public String toString() {
        return type.name() + " " + desc + " " + name;
    }
}
