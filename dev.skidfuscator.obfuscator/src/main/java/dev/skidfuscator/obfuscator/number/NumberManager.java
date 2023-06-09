package dev.skidfuscator.obfuscator.number;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.number.encrypt.NumberTransformer;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class NumberManager {
    private static final NumberTransformer[] TRANSFORMERS = {
            //new DebugNumberTransformer(),
            new XorNumberTransformer(),
            //new RandomShiftNumberTransformer()
    };

    private static final HashTransformer[] HASHER = {
            //new BitwiseHashTransformer(),
            //new IntelliJHashTransformer(),
            //new LegacyHashTransformer()
    };

    public static Expr encrypt(final int outcome, final int starting, final BasicBlock vertex, final PredicateFlowGetter startingExpr) {
        // Todo add more transformers + randomization
        return TRANSFORMERS[RandomUtil.nextInt(TRANSFORMERS.length)]
                .getNumber(outcome, starting, vertex, startingExpr);
    }

    public static SkiddedHash hash(final Skidfuscator skidfuscator, final int starting, final BasicBlock vertex, final PredicateFlowGetter local) {
        // Todo add more transformers + randomization
        return HASHER[RandomUtil.nextInt(HASHER.length)].hash(starting, vertex, local);
    }

    public static HashTransformer randomHasher(final Skidfuscator skidfuscator) {
        return skidfuscator.getLegacyHasher();//HASHER[RandomUtil.nextInt(HASHER.length)];
    }
}
