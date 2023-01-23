package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
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
    public void verifyVertex(ClassNode cn) {
        if (skidfuscator.getExemptAnalysis().isExempt(cn))
            return;
        super.verifyVertex(cn);
    }
}
