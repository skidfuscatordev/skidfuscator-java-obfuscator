package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.ProgressUtil;
import dev.skidfuscator.obfuscator.util.progress.ProgressWrapper;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassNode;

public class SkidClassTree extends ClassTree {
    private final Skidfuscator skidfuscator;

    public SkidClassTree(ApplicationClassSource source, Skidfuscator skidfuscator) {
        super(source);
        this.skidfuscator = skidfuscator;
    }

    public SkidClassTree(ApplicationClassSource source, boolean allowPhantomClasses, Skidfuscator skidfuscator) {
        super(source, allowPhantomClasses);
        this.skidfuscator = skidfuscator;
    }

    @Override
    public void init() {
        for (ClassNode node : source.iterate()) {
            if (skidfuscator.getExemptAnalysis().isExempt(node))
                continue;

            addVertex(node);
        }
    }

    @Override
    public void verify() {
        try (final ProgressWrapper wrapper = ProgressUtil.progressCheck(
                source.size(),
                "Verified classpath for " + source.size() + " classes"
        )){
            for (ClassNode node : source.iterate()) {
                wrapper.tick();
                verifyVertex(node);
            }
        }
    }

    @Override
    public void verifyVertex(ClassNode cn) {
        if (skidfuscator.getExemptAnalysis().isExempt(cn))
            return;

        if(cn == null) {
            throw new IllegalStateException("Vertex is null!");
        }

        if (!containsVertex(cn)) {
            addVertex(cn);
        }

        if(cn != rootNode) {
            ClassNode sup;

            try {
                sup = requestClass0(cn.node.superName, cn.getName());
            } catch (Exception e) {
                throw new IllegalStateException(String.format("No superclass %s for %s", cn.node.superName, cn.getName()));
            }
            if(sup == null) {
                throw new IllegalStateException(String.format("No superclass %s for %s", cn.node.superName, cn.getName()));
            }

            for (String s : cn.node.interfaces) {
                ClassNode iface = requestClass0(s, cn.getName());
                if(iface == null) {
                    throw new IllegalStateException(String.format("No superinterface %s for %s", s, cn.getName()));
                }
            }
        }
    }
}
