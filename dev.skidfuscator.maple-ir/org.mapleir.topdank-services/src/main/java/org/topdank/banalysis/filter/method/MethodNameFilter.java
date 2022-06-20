package org.topdank.banalysis.filter.method;

import org.mapleir.asm.MethodNode;
import org.topdank.banalysis.filter.MethodFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author sc4re
 */
public class MethodNameFilter implements MethodFilter {

    protected final List<String> names;

    public MethodNameFilter(String...names) {
        this.names = new ArrayList<>();
        this.names.addAll(Arrays.asList(names));
    }

    @Override
    public boolean accept(MethodNode methodNode) {
       return names.contains(methodNode.getName());
    }
}
