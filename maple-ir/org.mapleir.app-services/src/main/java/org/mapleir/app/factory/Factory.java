package org.mapleir.app.factory;

public interface Factory<T> {
    Builder<T> block();
}
