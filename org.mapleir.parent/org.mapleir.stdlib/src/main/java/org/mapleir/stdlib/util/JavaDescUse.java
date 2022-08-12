package org.mapleir.stdlib.util;

import java.util.Objects;

/**
 * Represents a specific usage of something with a JavaDesc.
 * For example, loading a field from a field load statement is such a use.
 * Note that in that case, the field load statement would be an IUsesJavaDesc, whose
 * JavaDescUse describes the specific relationship it has with the IJavaDesc it uses.
 * Holds information about the source of the use and the destination.
 * Useful for tracking data flow.
 */
public class JavaDescUse {
    public final JavaDesc target;
    public final IUsesJavaDesc flowElement;
    public final UseType flowType;

    public JavaDescUse(JavaDesc target, IUsesJavaDesc flowElement, UseType flowType) {
        this.target = target;
        this.flowElement = flowElement;
        this.flowType = flowType;
    }

    public enum UseType {
        READ,
        WRITE,
        CALL,
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JavaDescUse that = (JavaDescUse) o;
        return Objects.equals(target, that.target) &&
                Objects.equals(flowElement, that.flowElement) &&
                flowType == that.flowType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, flowElement, flowType);
    }

    @Override
    public String toString() {
        return target + " " + flowType + " " + flowElement;
    }
}
