package dev.skidfuscator.dependanalysis.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A ClassVisitor used to extract the superclass and interface names from a given class.
 * It normalizes slashes in class names to dot notation.
 */
public class HierarchyVisitor extends ClassVisitor {
    public String superName;
    public String[] interfaces = new String[0];

    public HierarchyVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (superName != null) {
            this.superName = superName.replace('/', '.');
        }
        if (interfaces != null) {
            String[] replaced = new String[interfaces.length];
            for (int i = 0; i < interfaces.length; i++) {
                replaced[i] = interfaces[i].replace('/', '.');
            }
            this.interfaces = replaced;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }
}
