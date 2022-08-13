package dev.skidfuscator.obfuscator.verifier;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.util.HashSet;
import java.util.Set;

public class Verifier {
    public static void verify(final ClassNode classNode) {
        final Set<String> fields = new HashSet<>();
        for (FieldNode field : classNode.fields) {
            final String name = field.name;

            if (fields.contains(name))
                throw new IllegalStateException("Cannot have a duplicate field");

            fields.add(name);
        }
    }
    public static void verify(final MethodNode methodNode) {
        try {
            Analyzer<?> analyzer = new Analyzer<>(new BasicVerifier());
            analyzer.analyzeAndComputeMaxs(methodNode.name, methodNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
