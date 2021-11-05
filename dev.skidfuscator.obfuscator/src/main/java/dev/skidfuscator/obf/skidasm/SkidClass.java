package dev.skidfuscator.obf.skidasm;

import lombok.Data;
import org.mapleir.asm.ClassNode;

import java.util.List;

@Data
public class SkidClass {
    private final ClassNode node;
    private final List<SkidGraph> graphs;

    public SkidClass(ClassNode node, List<SkidGraph> graphs) {
        this.node = node;
        this.graphs = graphs;
    }
}
