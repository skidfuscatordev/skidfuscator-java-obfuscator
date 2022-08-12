package dev.skidfuscator.obfuscator.order.priority;

import org.mapleir.asm.MethodNode;
import org.objectweb.asm.Opcodes;

import java.util.Comparator;

public class MethodPriority {
    public static final Comparator<MethodNode> COMPARATOR = Comparator.comparingInt(o -> MethodFlag.from(o).getAmount());

    private enum MethodFlag {
        CLINIT(0),
        INIT(1),
        STATIC(10),
        SYNTHETIC(50),
        ELSE(10000);

        private final int amount;

        MethodFlag(int amount) {
            this.amount = amount;
        }

        public int getAmount() {
            return amount;
        }

        static MethodFlag from(final MethodNode methodNode) {
            if (methodNode.isClinit())
                return CLINIT;
            else if (methodNode.isInit())
                return INIT;
            else if (methodNode.isStatic())
                return STATIC;
            else if (((methodNode.node.access) & Opcodes.ACC_SYNTHETIC) != 0)
                return SYNTHETIC;
            else
                return ELSE;
        }
    }
}
