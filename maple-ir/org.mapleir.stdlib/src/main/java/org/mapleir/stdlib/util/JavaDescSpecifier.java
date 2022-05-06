package org.mapleir.stdlib.util;

public class JavaDescSpecifier {
    public final String ownerRegex, nameRegex, descRegex;
    public final JavaDesc.DescType descType;

    public JavaDescSpecifier(String ownerRegex, String nameRegex, String descRegex, JavaDesc.DescType descType) {
        this.ownerRegex = ownerRegex;
        this.nameRegex = nameRegex;
        this.descRegex = descRegex;
        this.descType = descType;

        if (descType == JavaDesc.DescType.CLASS)
            assert(nameRegex.isEmpty() && descRegex.isEmpty());
    }

    @Override
    public String toString() {
        return "(" + descType + ")" + ownerRegex + "#" + nameRegex + descRegex;
    }

    public boolean matches(JavaDesc desc) {
        if (descType == JavaDesc.DescType.CLASS)
            return desc.owner.matches(ownerRegex);
        return desc.owner.matches(ownerRegex) && desc.name.matches(nameRegex) && desc.desc.matches(descRegex) && (descType == null || desc.descType == descType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaDescSpecifier javaDesc = (JavaDescSpecifier) o;

        if (ownerRegex != null ? !ownerRegex.equals(javaDesc.ownerRegex) : javaDesc.ownerRegex != null) return false;
        if (nameRegex != null ? !nameRegex.equals(javaDesc.nameRegex) : javaDesc.nameRegex != null) return false;
        if (descRegex != null ? !descRegex.equals(javaDesc.descRegex) : javaDesc.descRegex != null) return false;
        if (descType != null ? !descType.equals(javaDesc.descType) : javaDesc.descType != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerRegex != null ? ownerRegex.hashCode() : 0;
        result = 31 * result + (nameRegex != null ? nameRegex.hashCode() : 0);
        result = 31 * result + (descRegex != null ? descRegex.hashCode() : 0);
        result = 31 * result + (descType != null ? descType.hashCode() : 0);
        return result;
    }
}
