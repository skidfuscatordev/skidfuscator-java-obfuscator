package dev.skidfuscator.obf.transform_legacy.number;

import dev.skidfuscator.obf.transform_legacy.number.impl.XorNumberTransformer;
import org.mapleir.ir.code.Expr;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class NumberManager {
    private static final NumberTransformer[] TRANSFORMERS = {
            new XorNumberTransformer()
    };

    public static Expr transform(final Number outcome, final Number starting, final Expr startingExpr) {
        // Todo add more transformers + randomization
        return TRANSFORMERS[0].getNumber(outcome, starting, startingExpr);
    }
}
