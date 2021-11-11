package dev.skidfuscator.obf.skidasm;

import lombok.Data;
import org.mapleir.asm.ClassNode;

import java.util.List;

@Data
public class SkidGroup {
    private final List<SkidClass> graphs;
    private boolean isInherited;

    public SkidGroup(List<SkidClass> graphs, boolean isInherited) {
        this.graphs = graphs;
        this.isInherited = isInherited;
    }
}
