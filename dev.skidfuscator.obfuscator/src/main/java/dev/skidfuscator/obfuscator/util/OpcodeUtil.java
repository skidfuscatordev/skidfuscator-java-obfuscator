package dev.skidfuscator.obfuscator.util;

import lombok.experimental.UtilityClass;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.Opcodes;

@UtilityClass
public class OpcodeUtil {

    /**
     * Skidded from ASM with slight modifications
     * @param methodDescriptor Method description, eg: (III)V; would return 3 since three integers
     *                         all occupy only 1 stack space
     * @return Stack size of all the arguments
     */
    public int getArgumentsSizes(String methodDescriptor) {
        int argumentsSize = 1;
        int currentOffset = 1;

        char currentChar;
        int semiColumnOffset;
        for (currentChar = methodDescriptor.charAt(currentOffset); currentChar != ')'; currentChar = methodDescriptor.charAt(currentOffset)) {
            if (currentChar != 'J' && currentChar != 'D') {
                while (methodDescriptor.charAt(currentOffset) == '[') {
                    ++currentOffset;
                }

                if (methodDescriptor.charAt(currentOffset++) == 'L') {
                    semiColumnOffset = methodDescriptor.indexOf(59, currentOffset);
                    currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
                }

                ++argumentsSize;
            } else {
                ++currentOffset;
                argumentsSize += 2;
            }
        }

        return argumentsSize;
    }

    public boolean isStatic(final MethodNode methodNode) {
        return (methodNode.node.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isSynthetic(final MethodNode methodNode) {
        return (methodNode.node.access & Opcodes.ACC_SYNTHETIC) != 0;
    }

}
