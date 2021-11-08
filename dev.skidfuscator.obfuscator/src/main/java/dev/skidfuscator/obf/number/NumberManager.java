package dev.skidfuscator.obf.number;

import dev.skidfuscator.obf.number.encrypt.NumberTransformer;
import dev.skidfuscator.obf.number.encrypt.impl.RandomShiftNumberTransformer;
import dev.skidfuscator.obf.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obf.utils.RandomUtil;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.locals.Local;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class NumberManager {
    private static final NumberTransformer[] TRANSFORMERS = {
            new XorNumberTransformer(),
            new RandomShiftNumberTransformer()
    };

    private static final HashTransformer[] HASHER = {
            new BitwiseHashTransformer()
    };

    public static Expr encrypt(final Number outcome, final Number starting, final Local startingExpr) {
        // Todo add more transformers + randomization
        return TRANSFORMERS[RandomUtil.nextInt(TRANSFORMERS.length)].getNumber(outcome, starting, startingExpr);
    }

    public static SkiddedHash hash(final int starting, final Local local) {
        // Todo add more transformers + randomization
        return HASHER[0].hash(starting, local);
    }
}
