package openmods.depcheck.utils;

public class ClassElement extends TypedElement {

    private static final long serialVersionUID = 8678295110957226253L;

    public final String cls;

    public ClassElement(String cls, String name, String desc) {
        super(name, desc);
        this.cls = cls;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof ClassElement) {
            final ClassElement other = (ClassElement)o;
            return this.cls.equals(other.cls) && super.equals(other);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return cls.hashCode() ^ super.hashCode();
    }

    @Override
    public String toString() {
        return cls + ": " + desc + " " + name;
    }
}
