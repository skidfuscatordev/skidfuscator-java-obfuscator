package dev.skidfuscator.obf.number;

import dev.skidfuscator.obf.number.encrypt.NumberTransformer;
import dev.skidfuscator.obf.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.number.hash.impl.BitwiseHashTransformer;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.locals.Local;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class NumberManager {
    private static final NumberTransformer[] TRANSFORMERS = {
            new XorNumberTransformer()
    };

    private static final HashTransformer[] HASHER = {
            new BitwiseHashTransformer()
    };

    public static Expr encrypt(final Number outcome, final Number starting, final Expr startingExpr) {
        // Todo add more transformers + randomization
        return TRANSFORMERS[0].getNumber(outcome, starting, startingExpr);
    }

    public static SkiddedHash hash(final int starting, final Local local) {
        // Todo add more transformers + randomization
        return HASHER[0].hash(starting, local);
    }
}
