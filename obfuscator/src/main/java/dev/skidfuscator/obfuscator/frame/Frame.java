package dev.skidfuscator.obfuscator.frame;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.objectweb.asm.Type;

import java.util.*;

public class Frame {
    private final Skidfuscator skidfuscator;
    private final Set<Frame> parents;
    private final BasicBlock block;

    private final Type[] frame;
    private final Type[] inputTypes;
    private final Type[] outputTypes;
    private final Map<Integer, Type> used;
    private final Map<Integer, Set<CopyVarStmt>> assigns;
    private final Set<Integer> uses;

    public Frame(Skidfuscator skidfuscator, BasicBlock block, Set<Frame> parents) {
        this.skidfuscator = skidfuscator;
        this.block = block;
        this.parents = parents;

        this.frame = new Type[block.cfg.getLocals().getMaxLocals() + 2];
        this.inputTypes = new Type[block.cfg.getLocals().getMaxLocals() + 2];
        this.outputTypes = new Type[block.cfg.getLocals().getMaxLocals() + 2];

        this.used = new HashMap<>();
        this.assigns = new HashMap<>();
        this.uses = new HashSet<>();
    }

    protected void preprocess() {
        for (Stmt stmt : block) {
            if (stmt instanceof CopyVarStmt) {
                final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;
                /*
                 * Since we now have assigned it a value, it
                 * is no longer something computed from the
                 * frame, so we exempt it.
                 */
                assigns.computeIfAbsent(copyVarStmt.getIndex(), b -> new HashSet<>()).add(copyVarStmt);

                /*
                 * Since this is assigned in here, we can set
                 * the frame output to correspond. If it is
                 * in fact overriden, then it'll be overriden
                 * here too.
                 */
                outputTypes[copyVarStmt.getIndex()] = this._fix(copyVarStmt.getType());
            } else {
                for (Expr expr : stmt.enumerateOnlyChildren()) {
                    if (expr instanceof VarExpr) {
                        final VarExpr varExpr = (VarExpr) expr;

                        /*
                         * We want to make sure we know that we're using
                         * this index *somewhere* in this block
                         */
                        this.uses.add(varExpr.getIndex());

                        /*
                         * Since the value has been assigned in the block,
                         * then it is not dependent on the stack frame.
                         *
                         * We don't need to check the var expression index
                         * since we are iterating linearly.
                         */
                        if (assigns.containsKey(varExpr.getIndex()))
                            continue;

                        /*
                         * We're loading a value we didn't define in the
                         * block hence it must be defined in the stack frame,
                         * or else the type won't compute.
                         */
                        inputTypes[varExpr.getIndex()] = this._fix(varExpr.getType());
                    }
                }
            }
        }
    }


    /**
     * Compute the frame for a specific block.
     */
    public void compute() {
        for (int index = 0; index < inputTypes.length; index++) {
            Type computedType = inputTypes[index];

            for (Frame parent : parents) {
                final Type parentType = parent.getOutput(index);

                computedType = mergeTypes(parentType, computedType);

                if (computedType == Type.VOID_TYPE)
                    break;
            }

            frame[index] = computedType;

            /* Doubles and longs take double spots */
            if (computedType == Type.DOUBLE_TYPE || computedType == Type.LONG_TYPE) {
                index++;
                frame[index] = Type.VOID_TYPE;
            }
        }
    }

    /**
     * Get the output at a specific index
     *
     * @param index int for index
     * @return      output type for said index
     */
    public Type getOutput(final int index) {
        Type outputType = outputTypes[index];

        compute: {
            /*
             * Output type is not null. This means the
             * variable is set to a type in this block.
             * This means we can just return the output
             * type.
             */
            if (outputType != null)
                break compute;

            /*
             * In the compute section we'll be trying to
             * compute precisely what the output is based
             * on the parent's output type.
             */
            Type proposedType = null;
            for (Frame parent : parents) {
                final Type parentType = parent.getOutput(index);

                proposedType = this.mergeTypes(proposedType, parentType);
            }

            outputType = proposedType;
        }

        return outputType;
    }

    private Type mergeTypes(final Type head, final Type newest) {
        /*
         * If the parent type is null (undefined),
         * then just skip. We don't need this.
         */
        if (head == null)
            return newest;

        /*
         * If the newest type is null (undefined),
         * then just return the head.
         */
        if (newest == null) {
            return head;
        }

        /*
         * If the proposed type is identical, we
         * can move onto the next parent as the
         * local is coherent.
         */
        if (newest == head)
            return newest;

        /*
         * Here's a conflict. The proposed type
         * and parent type are different. Yikes.
         *
         * 1) If    they are both references
         *    Then  get common supertype
         *
         * 2) If    they are anything else
         *    Then  push exception
         *
         * todo: array support
         */
        if (head.getSort() == Type.OBJECT && newest.getSort() == Type.OBJECT) {
            final ClassNode selfClassNode = skidfuscator
                    .getClassSource()
                    .findClassNode(head.getInternalName());
            final ClassNode otherClassNode = skidfuscator
                    .getClassSource()
                    .findClassNode(newest.getInternalName());

            final ClassNode commonClassNode = skidfuscator.getClassSource()
                    .getClassTree()
                    .getCommonAncestor(Arrays.asList(selfClassNode, otherClassNode))
                    .iterator()
                    .next();

            return Type.getType("L" + commonClassNode.getName() + ";");
        } else if (true /* debug */) {
            return Type.VOID_TYPE;
        }

        /* kekw this suckz */
        throw new IllegalStateException(
                "Incompatible merge types: \n" +
                        "Head: " + head + "\n" +
                        "Newest: "+ newest + "\n"
        );
    }


    private Type _fix(final Type type) {
        if (type.equals(Type.BOOLEAN_TYPE)
                || type.equals(Type.BYTE_TYPE)
                || type.equals(Type.CHAR_TYPE)
                || type.equals(Type.SHORT_TYPE))
            return Type.INT_TYPE;

        return type;
    }

    /**
     * In retrospect, this is so fucking useless since we're already
     * establishing the context in the input types, which has an
     * override on the output types... dumb dumb me
     */
    @Deprecated
    protected void updateScope(final int index, final Type type) {
        /*
         * The variable defined is used by the block with a
         * specific type. If the previous type is undefined
         * (eg it has been defined but not transmitted), then
         * we redefine it.
         *
         * However, if the type is anything but a TOP type,
         * we override it with TOP to limit conflicts
         */
        final Type currentType = outputTypes[index];
        if (currentType == null) {
            outputTypes[index] = type;
        } else if (!currentType.equals(Type.VOID_TYPE)) {
            outputTypes[index] = Type.VOID_TYPE;
        }

        /*
         * Here the local is defined, meaning we no longer have
         * to update the output scope provided by this frame.
         */
        if (used.containsKey(index))
            return;

        for (Frame parent : parents) {
            parent.updateScope(index, type);
        }
    }

    public Set<Frame> getParents() {
        return parents;
    }

    public BasicBlock getBlock() {
        return block;
    }

    public Type[] getFrame() {
        return frame;
    }

    public Type[] getInputTypes() {
        return inputTypes;
    }

    public Type[] getOutputTypes() {
        return outputTypes;
    }

    public Map<Integer, Type> getUsed() {
        return used;
    }

    public Map<Integer, Set<CopyVarStmt>> getAssigns() {
        return assigns;
    }

    public Set<Integer> getUses() {
        return uses;
    }
}
