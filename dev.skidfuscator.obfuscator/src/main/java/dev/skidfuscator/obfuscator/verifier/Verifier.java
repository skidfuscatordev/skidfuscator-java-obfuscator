package dev.skidfuscator.obfuscator.verifier;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

public class Verifier {
    public static void verify(final MethodNode methodNode) {
        try {
            Analyzer<?> analyzer = new Analyzer<>(new BasicVerifier());
            analyzer.analyzeAndComputeMaxs(methodNode.name, methodNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
