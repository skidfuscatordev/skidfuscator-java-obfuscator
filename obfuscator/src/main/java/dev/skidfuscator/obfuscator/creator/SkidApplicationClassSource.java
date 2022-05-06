package dev.skidfuscator.obfuscator.creator;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.LocateableJarContents;

import java.util.Collection;
import java.util.Map;

public class SkidApplicationClassSource extends ApplicationClassSource {
    private final LocateableJarContents<ClassNode> nodes;

    public SkidApplicationClassSource(String name, LocateableJarContents<ClassNode> nodes) {
        super(name, nodes.getClassContents());
        this.nodes = nodes;
    }

    public SkidApplicationClassSource(String name, Map<String, ClassNode> nodeMap, LocateableJarContents<ClassNode> nodes) {
        super(name, nodeMap);
        this.nodes = nodes;
    }

    @Override
    public void add(ClassNode node) {
        nodes.getClassContents().add(node);
        super.add(node);
    }
}
