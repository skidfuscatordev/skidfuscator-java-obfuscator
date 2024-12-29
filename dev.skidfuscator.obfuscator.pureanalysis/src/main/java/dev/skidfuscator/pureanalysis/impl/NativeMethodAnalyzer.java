package dev.skidfuscator.pureanalysis.impl;

import dev.skidfuscator.pureanalysis.Analyzer;
import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.PurityContext;
import dev.skidfuscator.pureanalysis.PurityReport;
import org.objectweb.asm.Opcodes;

public class NativeMethodAnalyzer extends Analyzer {
    public NativeMethodAnalyzer(PurityContext context, PurityAnalyzer analyzer) {
        super("Native", context, analyzer);
    }

    @Override
    public PurityReport analyze(Context ctx) {
        if ((ctx.method().access & Opcodes.ACC_NATIVE) == 0) {
            return pure();
        }

        return impure("Method is native");
    }
}
