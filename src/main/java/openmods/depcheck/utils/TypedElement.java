package openmods.depcheck.utils;

import java.io.Serializable;

public class TypedElement implements Serializable {

    private static final long serialVersionUID = 8206186136037881452L;

    public final String name;

    public final String desc;

    public TypedElement(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TypedElement) {
            TypedElement other = (TypedElement)o;
            return name.equals(other.name) && desc.equals(other.desc);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ desc.hashCode();
    }

    @Override
    public String toString() {
        return desc + " " + name;
    }
}
