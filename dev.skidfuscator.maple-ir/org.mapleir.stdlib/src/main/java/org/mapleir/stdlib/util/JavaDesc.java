package org.mapleir.stdlib.util;

public class JavaDesc  {
    public final String owner, name, desc;
    public final DescType descType; // FIELD or METHOD -- METHOD=argument flow or return value flow

    public JavaDesc(String owner, String name, String desc, DescType descType) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.descType = descType;

        if (descType == DescType.CLASS)
            assert(name.isEmpty() && desc.isEmpty());
    }

    @Override
    public String toString() {
        return "(" + descType + ")" + owner + "#" + name + desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaDesc javaDesc = (JavaDesc) o;

        if (owner != null ? !owner.equals(javaDesc.owner) : javaDesc.owner != null) return false;
        if (name != null ? !name.equals(javaDesc.name) : javaDesc.name != null) return false;
        if (desc != null ? !desc.equals(javaDesc.desc) : javaDesc.desc != null) return false;
        if (descType != null ? !descType.equals(javaDesc.descType) : javaDesc.descType != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        result = 31 * result + (descType != null ? descType.hashCode() : 0);
        return result;
    }

    public enum DescType {
        FIELD,
        METHOD, // flow into method call args or out of method via return value
        CLASS,
    }
}
