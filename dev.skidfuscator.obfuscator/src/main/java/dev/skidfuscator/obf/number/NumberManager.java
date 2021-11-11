package dev.skidfuscator.obf.number;

import dev.skidfuscator.obf.number.encrypt.NumberTransformer;
import dev.skidfuscator.obf.number.encrypt.impl.DebugNumberTransformer;
import dev.skidfuscator.obf.number.encrypt.impl.RandomShiftNumberTransformer;
import dev.skidfuscator.obf.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obf.number.hash.impl.IntelliJHashTransformer;
import dev.skidfuscator.obf.number.hash.impl.LegacyHashTransformer;
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
            //new DebugNumberTransformer(),
            new XorNumberTransformer(),
            new RandomShiftNumberTransformer()
    };

    private static final HashTransformer[] HASHER = {
            new BitwiseHashTransformer(),
            new IntelliJHashTransformer(),
            new LegacyHashTransformer()
    };

    public static Expr encrypt(final int outcome, final int starting, final Local startingExpr) {
        // Todo add more transformers + randomization
        return TRANSFORMERS[RandomUtil.nextInt(TRANSFORMERS.length)].getNumber(outcome, starting, startingExpr);
    }

    public static SkiddedHash hash(final int starting, final Local local) {
        // Todo add more transformers + randomization
        return HASHER[RandomUtil.nextInt(HASHER.length)].hash(starting, local);
    }
}
