package dev.skidfuscator.obfuscator.verifier;

import org.mapleir.asm.MethodNode;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.CheckMethodAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
            Textifier textifier = new Textifier();
            TraceMethodVisitor textifierVisitor = new TraceMethodVisitor(textifier);
            methodNode.node.accept(textifierVisitor);

            System.out.println(
                    textifier.text.stream().map(Object::toString).collect(Collectors.joining(""))
            );

            Analyzer<BasicValue> valueAnalyzer = new CheckFrameAnalyzer<>(
                    new BasicVerifier()
            );
            valueAnalyzer.analyze(methodNode.getOwner(), methodNode.node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
