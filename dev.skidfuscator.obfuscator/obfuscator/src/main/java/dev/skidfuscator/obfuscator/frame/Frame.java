package dev.skidfuscator.obfuscator.frame;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidBogusStmt;
import lombok.Setter;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

@Deprecated
public class Frame {
    private final Skidfuscator skidfuscator;
    private final Map<Frame, Set<Stmt>> parents;
    private final Set<Frame> children;
    private final BasicBlock block;
    private final Type[] frame;
    private final Type[] inputTypes;
    private final Type[] outputTypes;
    private final Map<Integer, Type> used;
    private final Map<Integer, Set<CopyVarStmt>> defs;
    private final Map<Integer, Set<VarExpr>> uses;

    @Setter
    private Type[] staticFrame;

    public Frame(Skidfuscator skidfuscator, BasicBlock block) {
        this.skidfuscator = skidfuscator;
        this.block = block;
        this.parents = new HashMap<>();
        this.children = new HashSet<>();

        this.frame = new Type[block.cfg.getLocals().getMaxLocals() + 2];
        this.inputTypes = new Type[block.cfg.getLocals().getMaxLocals() + 2];
        this.outputTypes = new Type[block.cfg.getLocals().getMaxLocals() + 2];

        this.used = new HashMap<>();
        this.defs = new HashMap<>();
        this.uses = new HashMap<>();
    }

    public void preprocess() {
        for (Stmt stmt : block) {
            if (stmt instanceof CopyVarStmt) {
                final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;
                /*
                 * Since we now have assigned it a value, it
                 * is no longer something computed from the
                 * frame, so we exempt it.
                 */
                defs.computeIfAbsent(copyVarStmt.getIndex(), b -> new HashSet<>()).add(copyVarStmt);

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
                        this.uses.computeIfAbsent(varExpr.getIndex(), b -> new HashSet<>())
                                .add(varExpr);

                        /*
                         * Since the value has been assigned in the block,
                         * then it is not dependent on the stack frame.
                         *
                         * We don't need to check the var expression index
                         * since we are iterating linearly.
                         */
                        if (defs.containsKey(varExpr.getIndex()))
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

    public void hackyMess() {
        for (int index = 0; index < inputTypes.length; index++) {
            if (outputTypes[index] != null)
                continue;

            outputTypes[index] = inputTypes[index];
        }
    }


    /**
     * Compute the frame for a specific block.
     */
    public void compute() {
        for (int index = 0; index < inputTypes.length; index++) {
            Type computedType = inputTypes[index];

            for (Frame parent : parents.keySet()) {
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

    private Type getOutput(final int index) {
        return this.getOutput(index, new HashSet<>());
    }

    /**
     * Get the output at a specific index
     *
     * @param index     int for index
     * @param visited   visitation set to prevent circular dependencies
     * @return      output type for said index
     */
    private Type getOutput(final int index, final Set<Frame> visited) {
        Type outputType = outputTypes[index];

        /* Fuck circular loops */
        if (visited.contains(this))
            return outputType;
        visited.add(this);

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
            for (Frame parent : parents.keySet()) {
                final Type parentType = parent.getOutput(index, visited);

                proposedType = this.mergeTypes(proposedType, parentType);
            }

            outputType = proposedType;
        }

        return outputType;
    }

    public Frame getUseDefine(final int index, final Set<Frame> visited) {
        /* Fuck circular loops */
        if (visited.contains(this))
            return null;

        visited.add(this);

        if (defs.containsKey(index)) {
            return this;
        }

        for (Frame parent : parents.keySet()) {
            final Frame parentFrame = parent.getUseDefine(index, visited);

            if (parentFrame != null)
                return parentFrame;
        }

        return null;
    }

    public Set<Integer> getUsesNoDefined() {
        final Set<Integer> undefined = new HashSet<>();
        for (Map.Entry<Integer, Set<VarExpr>> integerSetEntry : uses.entrySet()) {
            final Integer use = integerSetEntry.getKey();

            final Set<VarExpr> exprs = integerSetEntry.getValue();
            final Set<CopyVarStmt> defsStmts = defs.get(use);

            /*
             * If the variable has no definitions, it means it's
             * inherited from a frame.
             */
            if (defsStmts == null) {
                undefined.add(use);
                continue;
            }

            /*
             * If the defs statement exists but is empty, it means
             * that it's been defined by a previous frame.
             */
            if (defsStmts.isEmpty())
                continue;

            final int lowestHeight = exprs
                    .stream()
                    .map(Expr::getRootParent)
                    .mapToInt(block::indexOf)
                    .min()
                    .orElse(block.size());

            final boolean previouslyInitiated = defsStmts
                    .stream()
                    .anyMatch(e -> block.indexOf(e) < lowestHeight);

            /*
             * If the variable has been initiated before a
             * statement, it means its defined. So we skip.
             */
            if (previouslyInitiated)
                continue;

            undefined.add(use);
        }

        return undefined;
    }

    public boolean isDefinedBeforeIndex(final int index, final Stmt stmt) {
        final Set<CopyVarStmt> defsStmts = defs.get(index);

        if (defsStmts == null)
            return false;

        final int lowestHeight = block.indexOf(stmt);
        return defsStmts
                .stream()
                .anyMatch(e -> block.indexOf(e) < lowestHeight);
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

    public void setInput(final int index, final Type type) {
        this.inputTypes[index] = type;
    }

    public void addParent(final Frame parent, final Stmt caller) {
        parents.computeIfAbsent(parent, e -> new HashSet<>()).add(caller);
        parent.addChildren(this);
    }

    public void addChildren(final Frame child) {
        children.add(child);
    }

    public boolean isTerminating() {
        return children.isEmpty();
    }

    public Set<Frame> getParents() {
        return parents.keySet();
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

    public Map<Integer, Set<CopyVarStmt>> getDefs() {
        return defs;
    }

    public Set<Integer> getUses() {
        return uses.keySet();
    }

    @Override
    public String toString() {
        return "Frame " + block.getDisplayName() + " {" +
                "\n  parents: " + parents.keySet().stream().map(e -> e.getBlock().getDisplayName()).collect(Collectors.joining(", ")) +
                "\n  children: " + children.stream().map(e -> e.getBlock().getDisplayName()).collect(Collectors.joining(", ")) +
                "\n  frame: " + Arrays.toString(frame) +
                "\n  inputTypes: " + Arrays.toString(inputTypes) +
                "\n  outputTypes: " + Arrays.toString(outputTypes) +
                "\n  assigns: \n    " + defs.entrySet().stream().map(e -> e.getKey() + " --> " + Arrays.toString(e.getValue().toArray())).collect(Collectors.joining("\n    ")) +
                "\n  uses: " + Arrays.toString(uses.keySet().toArray()) +
                "\n}";
    }
}
