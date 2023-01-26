package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;
import org.topdank.byteengineer.commons.data.LocateableJarContents;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SkidApplicationClassSource extends ApplicationClassSource {
    //private final LocateableJarContents<ClassNode> nodes;
    private final Skidfuscator skidfuscator;

    public SkidApplicationClassSource(String name, boolean phantom, JarContents nodes, Skidfuscator skidfuscator) {
        super(name, phantom, nodes.getClassContents().stream().map(JarClassData::getClassNode).collect(Collectors.toList()));
        this.skidfuscator = skidfuscator;
    }

    public SkidApplicationClassSource(String name, boolean phantom, Map<String, ClassNode> nodeMap, Skidfuscator skidfuscator) {
        super(name, phantom, nodeMap);
        this.skidfuscator = skidfuscator;
    }

    @Override
    public void add(ClassNode node) {
        super.add(node);
    }

    @Override
    public ClassTree getClassTree() {
        if (classTree == null) {
            classTree = new SkidClassTree(this, phantom, skidfuscator);
            classTree.init();
        }
        return classTree;
    }
}
