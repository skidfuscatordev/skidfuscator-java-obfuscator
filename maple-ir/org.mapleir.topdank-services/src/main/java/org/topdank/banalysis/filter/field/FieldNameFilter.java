package org.topdank.banalysis.filter.field;

import org.mapleir.asm.FieldNode;
import org.topdank.banalysis.filter.FieldFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author sc4re
 */
public class FieldNameFilter implements FieldFilter {
    protected final List<String> names;

    public FieldNameFilter(String...names) {
        this.names = new ArrayList<>();
        this.names.addAll(Arrays.asList(names));
    }

    @Override
    public boolean accept(FieldNode fieldNode) {
        return names.contains(fieldNode.getName());
    }
}
