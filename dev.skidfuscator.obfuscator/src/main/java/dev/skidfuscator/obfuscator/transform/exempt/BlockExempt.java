package dev.skidfuscator.obfuscator.transform.exempt;

import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;

import java.util.function.Function;

public enum BlockExempt {
    EMPTY(block -> block.size() == 0),
    NO_OPAQUE(block -> block.isFlagSet(SkidBlock.FLAG_NO_OPAQUE)),
    NO_EXCEPT(block -> block.isFlagSet(SkidBlock.FLAG_NO_EXCEPTION))
    ;
    private final Function<SkidBlock, Boolean> function;

    BlockExempt(Function<SkidBlock, Boolean> function) {
        this.function = function;
    }

    public boolean isExempt(final SkidBlock block) {
        return function.apply(block);
    }

    public static boolean isExempt(final SkidBlock block, final BlockExempt... exemption) {
        for (BlockExempt blockExemption : exemption) {
            if (blockExemption.isExempt(block)) {
                return true;
            }
        }

        return false;
    }
}
