package dev.skidfuscator.pureanalysis.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.PurityContext;
import dev.skidfuscator.pureanalysis.PurityReport;
import org.objectweb.asm.tree.TypeInsnNode;

public class TypeInstructionAnalyzer extends InstructionAnalyzer {
    public TypeInstructionAnalyzer(PurityContext context, PurityAnalyzer analyzer) {
        super("Type", context, analyzer, TypeInsnNode.class);
    }

    @Override
    public PurityReport analyze(Context ctx) {
        // All type instructions are pure by default
        return pure();
    }
}