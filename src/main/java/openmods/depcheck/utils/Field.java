package openmods.depcheck.utils;

public class Field {

    public final String name;

    public final String desc;

    public Field(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Field) {
            Field other = (Field)o;
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
