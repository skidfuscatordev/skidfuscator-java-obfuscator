package org.topdank.banalysis.filter.field;

import org.mapleir.asm.FieldNode;
import org.topdank.banalysis.filter.FieldFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author sc4re
 */
public class FieldDescFilter implements FieldFilter {
    protected final List<String> descs;

    public FieldDescFilter(String...names) {
        this.descs = new ArrayList<>();
        this.descs.addAll(Arrays.asList(names));
    }

    @Override
    public boolean accept(FieldNode fieldNode) {
        return descs.contains(fieldNode.getDesc());
    }
}
