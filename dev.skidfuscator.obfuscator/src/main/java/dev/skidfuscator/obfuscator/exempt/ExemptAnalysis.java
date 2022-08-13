package dev.skidfuscator.obfuscator.exempt;

import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public interface ExemptAnalysis {
    /**
     * @param methodNode  Method node being checked
     * @return Boolean value of whether the method is exempted or not
     */
    boolean isExempt(final MethodNode methodNode);

    /**
     * @param classNode  Class node being checked
     * @return Boolean value of whether the class is exempted or not
     */
    boolean isExempt(final ClassNode classNode);


    /**
     * @param exclusion Exclusion string
     */
    void add(final String exclusion);

    /**
     * @param exclusion Exclusion string
     */
    void add(final ClassNode exclusion);

    /**
     * @param exclusion Exclusion string
     */
    void add(final MethodNode exclusion);


    /**
     * Strictly for debugging purposes
     * @return String debug output
     */
    String toString();
}
