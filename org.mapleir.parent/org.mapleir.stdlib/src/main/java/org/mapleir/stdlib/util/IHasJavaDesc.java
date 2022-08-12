package org.mapleir.stdlib.util;

public interface IHasJavaDesc {
    String getOwner();
    String getName();
    String getDesc();
    JavaDesc.DescType getDescType();

    //todo: cache
    default JavaDesc getJavaDesc() {
        return new JavaDesc(getOwner(), getName(), getDesc(), getDescType());
    }
}
