package dev.skidfuscator.obfuscator.exempt;

import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public interface ExemptAnalysis {
    /**
     * @param transformer Transformer which is being audited (or any subsidiary of this
     *                    transformer (yes there can be child transformers)
     * @param methodNode  Method node being checked
     * @return Boolean value of whether the method is exempted or not
     */
    boolean isExempt(final Transformer transformer, final MethodNode methodNode);

    /**
     * @param transformer Transformer which is being audited (or any subsidiary of this
     *                    transformer (yes there can be child transformers)
     * @param classNode  Class node being checked
     * @return Boolean value of whether the class is exempted or not
     */
    boolean isExempt(final Transformer transformer, final ClassNode classNode);
}
