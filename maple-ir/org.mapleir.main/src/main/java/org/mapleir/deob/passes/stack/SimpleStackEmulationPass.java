package org.mapleir.deob.passes.stack;

import org.mapleir.X;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class SimpleStackEmulationPass implements IPass {
    private final Arithmetic arithmetic = new Arithmetic();

    @Override
    public PassResult accept(PassContext pcxt) {
        final AnalysisContext cxt = pcxt.getAnalysis();

        for (Map.Entry<MethodNode, ControlFlowGraph> pair : cxt.getIRCache().entrySet()) {
            run(pair.getKey(), pair.getValue());
        }

        return null;
    }

    public void run(final MethodNode methodNode, final ControlFlowGraph cfg) {
        final Map<Local, Map<BasicBlock, LocalValue>> localValueMap = new HashMap<>();
        final BasicBlock entry = cfg.getEntries().iterator().next();

        for (Stmt stmt : entry) {
            if (!(stmt instanceof CopyVarStmt)) {
                continue;
            }

            final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;

            if (copyVarStmt.getExpression() instanceof ConstantExpr) {

            }
        }
    }

    public static class Arithmetic {
        private final Map<Type, ArithmeticProcessor<? extends Number>> processors = new HashMap<>();

        public Arithmetic() {
            init();
        }

        public Number doArithmetic(final Number a, final Number b, final ArithmeticExpr.Operator operator) {
            final boolean isLeftInteger = a instanceof Byte || a instanceof Short || a instanceof Integer;
            final boolean isRightInteger = b instanceof Byte || b instanceof Short || b instanceof Integer;

            if (isLeftInteger && isRightInteger) {
                return process(processors.get(Type.INT_TYPE), a, b, operator);
            } else if (isLeftInteger || isRightInteger) {
                throw new IllegalStateException("Incompatible types: " + a.getClass() + " / " + b.getClass());
            }

            final boolean isLeftLong = a instanceof Long;
            final boolean isRightLong = b instanceof Long;

            if (isLeftLong && isRightLong) {
                return process(processors.get(Type.INT_TYPE), a, b, operator);
            } else if (isLeftLong || isRightLong) {
                throw new IllegalStateException("Incompatible types: " + a.getClass() + " / " + b.getClass());
            }

            final boolean isLeftFloat = a instanceof Float;
            final boolean isRightFloat = b instanceof Float;

            if (isLeftFloat && isRightFloat) {
                return process(processors.get(Type.INT_TYPE), a, b, operator);
            } else if (isLeftFloat || isRightFloat) {
                throw new IllegalStateException("Incompatible types: " + a.getClass() + " / " + b.getClass());
            }

            final boolean isLeftDouble = a instanceof Double;
            final boolean isRightDouble = b instanceof Double;

            if (isLeftDouble && isRightDouble) {
                return process(processors.get(Type.INT_TYPE), a, b, operator);
            } else if (isLeftDouble || isRightDouble) {
                throw new IllegalStateException("Incompatible types: " + a.getClass() + " / " + b.getClass());
            }

            throw new IllegalStateException("Numbers of type " + a.getClass() + " / " + b.getClass() + " unrecognised!");
        }

        private <T extends Number> T process(final ArithmeticProcessor<T> processor, final Number a, final Number b, final ArithmeticExpr.Operator operator) {
            switch (operator) {
                case ADD:
                    return processor.add((T) a, (T) b);
                case SUB:
                    return processor.sub((T) a, (T) b);
                case MUL:
                    return processor.mul((T) a, (T) b);
                case DIV:
                    return processor.div((T) a, (T) b);
                case REM:
                    return processor.mod((T) a, (T) b);
                case SHL:
                    return processor.shl((T) a, (T) b);
                case SHR:
                    return processor.shr((T) a, (T) b);
                case USHR:
                    return processor.ushr((T) a, (T) b);
                case XOR:
                    return processor.xor((T) a, (T) b);
                case OR:
                    return processor.or((T) a, (T) b);
                case AND:
                    return processor.and((T) a, (T) b);
                default:
                    throw new IllegalStateException("Operator " + operator.getSign() + " is not currently supported!");
            }
        }

        private void init() {
            final ArithmeticProcessor<Integer> intProcessor = new ArithmeticProcessor<Integer>() {
                @Override
                public Integer add(Integer a, Integer b) {
                    return a + b;
                }

                @Override
                public Integer sub(Integer a, Integer b) {
                    return a - b;
                }

                @Override
                public Integer mul(Integer a, Integer b) {
                    return a * b;
                }

                @Override
                public Integer div(Integer a, Integer b) {
                    return a / b;
                }

                @Override
                public Integer mod(Integer a, Integer b) {
                    return a % b;
                }

                @Override
                public Integer shl(Integer a, Integer b) {
                    return a << b;
                }

                @Override
                public Integer shr(Integer a, Integer b) {
                    return a >> b;
                }

                @Override
                public Integer ushr(Integer a, Integer b) {
                    return a >>> b;
                }

                @Override
                public Integer xor(Integer a, Integer b) {
                    return a ^ b;
                }

                @Override
                public Integer or(Integer a, Integer b) {
                    return a | b;
                }

                @Override
                public Integer and(Integer a, Integer b) {
                    return a & b;
                }
            };
            processors.put(Type.BYTE_TYPE, intProcessor);
            processors.put(Type.SHORT_TYPE, intProcessor);
            processors.put(Type.INT_TYPE, intProcessor);

            processors.put(Type.LONG_TYPE, new ArithmeticProcessor<Long>() {
                @Override
                public Long add(Long a, Long b) {
                    return a + b;
                }

                @Override
                public Long sub(Long a, Long b) {
                    return a - b;
                }

                @Override
                public Long mul(Long a, Long b) {
                    return a * b;
                }

                @Override
                public Long div(Long a, Long b) {
                    return a / b;
                }

                @Override
                public Long mod(Long a, Long b) {
                    return a % b;
                }

                @Override
                public Long shl(Long a, Long b) {
                    return a << b;
                }

                @Override
                public Long shr(Long a, Long b) {
                    return a >> b;
                }

                @Override
                public Long ushr(Long a, Long b) {
                    return a >>> b;
                }

                @Override
                public Long xor(Long a, Long b) {
                    return a ^ b;
                }

                @Override
                public Long or(Long a, Long b) {
                    return a | b;
                }

                @Override
                public Long and(Long a, Long b) {
                    return a & b;
                }
            });

            processors.put(Type.FLOAT_TYPE, new ArithmeticProcessor<Float>() {
                @Override
                public Float add(Float a, Float b) {
                    return a + b;
                }

                @Override
                public Float sub(Float a, Float b) {
                    return a - b;
                }

                @Override
                public Float mul(Float a, Float b) {
                    return a * b;
                }

                @Override
                public Float div(Float a, Float b) {
                    return a / b;
                }

                @Override
                public Float mod(Float a, Float b) {
                    return a % b;
                }
            });

            processors.put(Type.DOUBLE_TYPE, new ArithmeticProcessor<Double>() {
                @Override
                public Double add(Double a, Double b) {
                    return a + b;
                }

                @Override
                public Double sub(Double a, Double b) {
                    return a - b;
                }

                @Override
                public Double mul(Double a, Double b) {
                    return a * b;
                }

                @Override
                public Double div(Double a, Double b) {
                    return a / b;
                }

                @Override
                public Double mod(Double a, Double b) {
                    return a % b;
                }
            });
        }
    }

    public interface ArithmeticProcessor<T> {
        T add(final T a, T b);

        T sub(final T a, T b);

        T mul(final T a, T b);

        T div(final T a, T b);

        T mod(final T a, T b);

        default T shl(final T a, T b) {
            throw new IllegalStateException("Number of type " + a.getClass() + " is not compatible with bitwise operations.");
        }

        default T shr(final T a, T b) {
            throw new IllegalStateException("Number of type " + a.getClass() + " is not compatible with bitwise operations.");
        }

        default T ushr(final T a, T b) {
            throw new IllegalStateException("Number of type " + a.getClass() + " is not compatible with bitwise operations.");
        }

        default T xor(final T a, T b) {
            throw new IllegalStateException("Number of type " + a.getClass() + " is not compatible with bitwise operations.");
        }

        default T or(final T a, T b) {
            throw new IllegalStateException("Number of type " + a.getClass() + " is not compatible with bitwise operations.");
        }

        default T and(final T a, T b) {
            throw new IllegalStateException("Number of type " + a.getClass() + " is not compatible with bitwise operations.");
        }
    }

    public static class LocalValue {
        private final LocalType type;
        private final Object value;

        public LocalValue(LocalType type, Object value) {
            this.type = type;
            this.value = value;
        }

        public LocalType getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }

    public enum LocalType {
        UNKNOWN,
        COMPUTED
    }
}
