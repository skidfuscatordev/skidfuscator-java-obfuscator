package org.mapleir.stdlib.util;

public class IntegerRange extends Range<Integer> {
    public IntegerRange(Integer min, Integer max) {
        super(min, max);
    }

    public void increase(final int i) {
        increaseMin(i);
        increaseMax(i);
    }

    public void increaseMin(final int i) {
        this.setMin(this.getMin() + i);
    }

    public void increaseMax(final int i) {
        this.setMax(this.getMax() + i);
    }
}
