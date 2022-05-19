package dev.skidfuscator.obfuscator.creator;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.LocateableJarContents;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SkidApplicationClassSource extends ApplicationClassSource {
    //private final LocateableJarContents<ClassNode> nodes;

    public SkidApplicationClassSource(String name, LocateableJarContents nodes) {
        super(name, nodes.getClassContents().stream().map(JarClassData::getClassNode).collect(Collectors.toList()));
    }

    public SkidApplicationClassSource(String name, Map<String, ClassNode> nodeMap, LocateableJarContents nodes) {
        super(name, nodeMap);
    }

    @Override
    public void add(ClassNode node) {
        super.add(node);
    }
}
