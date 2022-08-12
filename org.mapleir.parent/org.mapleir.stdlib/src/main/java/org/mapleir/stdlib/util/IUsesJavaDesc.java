package org.mapleir.stdlib.util;

/**
 * Represents something that references an IJavaDesc, such as a field load/store, method call, etc.
 * This is different from JavaDescUse in the sense that this is the actual
 * flow element that actually uses the IHasJavaDesc in question.
 * Holds information about the source of the use and the destination.
 * Useful for tracking data flow.
 */
public interface IUsesJavaDesc extends IHasJavaDesc {
    /**
     * The data use type, e.g. read, write, call.
     * @return The data use type
     */
    JavaDescUse.UseType getDataUseType();

    /**
     * The source of the data use.
     * For example, if a field is referenced in a method, that method would be the use location.
     * @return The source of the data use
     */
    JavaDesc getDataUseLocation();

    default JavaDescUse getDataUse() {
        return new JavaDescUse(getJavaDesc(), this, getDataUseType());
    }
}
