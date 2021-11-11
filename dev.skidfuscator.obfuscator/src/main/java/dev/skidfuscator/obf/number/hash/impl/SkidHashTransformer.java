package dev.skidfuscator.obf.number.hash.impl;

import dev.skidfuscator.obf.maple.FakeArithmeticExpr;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.utils.RandomUtil;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkidHashTransformer implements HashTransformer {
    private final Map<Integer, MutatingOperation> mutatingOperationMap = new HashMap<>();

    @Override
    public SkiddedHash hash(int starting, Local caller) {
        final int hashed = hash(starting);
        final Expr hash = hash(caller);
        return new SkiddedHash(hash, hashed);
    }

    @Override
    public int hash(int starting) {
        final MutatingOperation mutatingOperation = mutatingOperationMap.get(starting);

        if (mutatingOperation == null) {

        }

        return (starting ^ (starting >>> 16));
    }

    @Override
    public Expr hash(Local caller) {
        // (starting >>> 16)
        final Expr var_calla = new VarExpr(caller, Type.INT_TYPE);
        final Expr const_29 = new ConstantExpr(29, Type.INT_TYPE);
        final Expr shifted7to29 = new FakeArithmeticExpr(var_calla, const_29, ArithmeticExpr.Operator.USHR);

        // (starting ^ (starting >>> 16))
        final Expr shiftedvartoshift = new FakeArithmeticExpr(new VarExpr(caller, Type.INT_TYPE), shifted7to29, ArithmeticExpr.Operator.XOR);
        return shiftedvartoshift;
    }

    private void createMutating() {
        for (int i = 0; i < RandomUtil.nextInt(5); i++) {
            // Todo: WIP
        }
    }

    static class MutatingOperation implements Operation {
        private List<BitwiseOperation> bitwiseOperationList = new ArrayList<>();

        public MutatingOperation(List<BitwiseOperation> bitwiseOperationList) {
            this.bitwiseOperationList = bitwiseOperationList;
        }

        @Override
        public int op(int var) {
            for (BitwiseOperation bitwiseOperation : bitwiseOperationList) {
                var = bitwiseOperation.op(var);
            }
            return var;
        }

        @Override
        public Expr op(Expr var) {
            for (BitwiseOperation bitwiseOperation : bitwiseOperationList) {
                var = bitwiseOperation.op(var);
            }
            return var;
        }
    }

    static class LocalOperation implements Operation {
        private final Local local;
        private final int value;

        public LocalOperation(Local local, int value) {
            this.local = local;
            this.value = value;
        }

        @Override
        public int op(int var) {
            return 0;
        }

        @Override
        public Expr op(Expr var) {
            return null;
        }
    }

    static class BitwiseOperation implements Operation {
        private final ArithmeticExpr.Operator operator;
        private final int seed;

        public BitwiseOperation(ArithmeticExpr.Operator operator) {
            this.operator = operator;
            this.seed = RandomUtil.nextInt(32);
        }

        @Override
        public int op(int var) {
            switch (operator) {
                case ADD: return var + seed;
                case SUB: return var - seed;
                case MUL: return var * seed;
                case DIV: return var / seed;
                case REM: return var % seed;
                case OR: return var | seed;
                case AND: return var & seed;
                case XOR: return var ^ seed;
                case SHL: return var << seed;
                case SHR: return var >> seed;
                case USHR: return var >>> seed;
                default: throw new IllegalStateException("How did this happen!");
            }
        }

        @Override
        public Expr op(Expr var) {
            return new ArithmeticExpr(var, new ConstantExpr(seed), operator);
        }
    }

    interface Operation {
        int op(final int var);

        Expr op(final Expr var);
    }
}
