package dev.skidfuscator.obfuscator.creator;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;
import org.topdank.byteengineer.commons.data.LocateableJarContents;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SkidApplicationClassSource extends ApplicationClassSource {
    //private final LocateableJarContents<ClassNode> nodes;

    public SkidApplicationClassSource(String name, boolean phantom, JarContents nodes) {
        super(name, phantom, nodes.getClassContents().stream().map(JarClassData::getClassNode).collect(Collectors.toList()));
    }

    public SkidApplicationClassSource(String name, boolean phantom, Map<String, ClassNode> nodeMap) {
        super(name, phantom, nodeMap);
    }

    @Override
    public void add(ClassNode node) {
        super.add(node);
    }
}
