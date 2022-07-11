package org.mapleir.ir.locals.dynamic;

import org.mapleir.ir.locals.Local;

public class DynamicLocal extends Local {
    private static int localIndex = 0;

    public DynamicLocal() {
        super(localIndex++);
    }

    public DynamicLocal(boolean stack) {
        super(localIndex++, stack);
    }

    public DynamicLocal(int index) {
        super(index);
    }

    public DynamicLocal(int index, boolean stack) {
        super(index, stack);
    }

    public static void resetCounter() {
        localIndex = 0;
    }
}
