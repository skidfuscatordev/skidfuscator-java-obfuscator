package org.mapleir.ir.printer;

import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Expr.Precedence;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.*;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.mapleir.ir.printer.Util.isNonEmpty;

@SuppressWarnings("unused")
public abstract class MethodNodePrinter extends ASMPrinter<MethodNode> {

    private static final String[] METHOD_ATTR_FLAG_NAMES = Util
            .asOpcodesAccessFieldFormat(new String[] { "public", "private", "protected", "static",
                    "final", "synchronized", "bridge", "varargs", "native", "abstract", "strict",
                    "synthetic", "deprecated" });

    public MethodNodePrinter(TabbedStringWriter sw, IPropertyDictionary settingsDict) {
        super(sw, settingsDict, METHOD_ATTR_FLAG_NAMES);
    }

    @Override
    public void print(MethodNode mn) {
        this.sw.newline().print(".method ").print(mn.desc).print(" ").print(mn.name);

        this.sw.print(" {").tab();
        this.emitMethodAttributes(mn);
        this.emitCode(mn);
        this.sw.untab().newline().print("}");
    }

    protected abstract ControlFlowGraph getCfg(MethodNode mn);

    public void emitCode(MethodNode mn) {
        ControlFlowGraph cfg = this.getCfg(mn);

        this.emitHandlers(cfg);
        this.sw.newline().print(".code {").tab();
        this.emitCfg(cfg);
        this.sw.untab().newline().print("}");
    }

    public void emitCfg(ControlFlowGraph cfg) {
        for (BasicBlock b : cfg.verticesInOrder()) {
            this.emitBlock(b);
        }
    }

    public void emitBlock(BasicBlock b) {
        this.sw.newline().print(b.getDisplayName()).print(": {").tab();
        for (Stmt stmt : b) {
            this.sw.newline();
            this.emitStmt(stmt);
        }
        this.sw.untab().newline().print("}");
    }

    public void emitStmt(Stmt stmt) {
        int opcode = stmt.getOpcode();

        switch (opcode) {
            case Opcode.LOCAL_STORE:
            case Opcode.PHI_STORE: {
                AbstractCopyStmt cvs = (AbstractCopyStmt) stmt;

                if (cvs.isSynthetic()) {
                    this.sw.print(".synth ");
                }

                this.emitExpr(cvs.getVariable());
                this.sw.print(" = ");
                this.emitExpr(cvs.getExpression());
                break;
            }

            case Opcode.ARRAY_STORE: {
                ArrayStoreStmt ars = (ArrayStoreStmt) stmt;

                Expr arrayExpr = ars.getArrayExpression();
                Expr indexExpr = ars.getIndexExpression();
                Expr valexpr = ars.getValueExpression();

                int accessPriority = Precedence.ARRAY_ACCESS.ordinal();
                int basePriority = arrayExpr.getPrecedence();
                if (basePriority > accessPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(arrayExpr);
                if (basePriority > accessPriority) {
                    this.sw.print(')');
                }
                this.sw.print('[');
                this.emitExpr(indexExpr);
                this.sw.print(']');
                this.sw.print(" = ");
                this.emitExpr(valexpr);
                break;
            }
            case Opcode.FIELD_STORE: {
                FieldStoreStmt fss = (FieldStoreStmt) stmt;

                Expr valExpr = fss.getValueExpression();
                if (!fss.isStatic()) {
                    int selfPriority = Precedence.MEMBER_ACCESS.ordinal();
                    Expr instanceExpr = fss.getInstanceExpression();
                    int basePriority = instanceExpr.getPrecedence();
                    if (basePriority > selfPriority) {
                        this.sw.print('(');
                    }
                    this.emitExpr(instanceExpr);
                    if (basePriority > selfPriority) {
                        this.sw.print(')');
                    }
                } else {
                    this.sw.print(fss.getOwner());
                }
                this.sw.print('.');
                this.sw.print(fss.getName());
                this.sw.print(" = ");
                this.emitExpr(valExpr);
                break;
            }
            case Opcode.COND_JUMP: {
                ConditionalJumpStmt cjs = (ConditionalJumpStmt) stmt;

                this.sw.print(".if (");
                this.emitExpr(cjs.getLeft());
                this.sw.print(" ").print(cjs.getComparisonType().getSign()).print(" ");
                this.emitExpr(cjs.getRight());
                this.sw.print(")");
                this.sw.tab().newline().print(".goto ")
                        .print(cjs.getTrueSuccessor().getDisplayName()).untab();
                break;
            }
            case Opcode.UNCOND_JUMP: {
                UnconditionalJumpStmt ujs = (UnconditionalJumpStmt) stmt;
                this.sw.print(".goto ").print(ujs.getTarget().getDisplayName());
                break;
            }
            case Opcode.THROW: {
                ThrowStmt ts = (ThrowStmt) stmt;
                this.sw.print(".throw ");
                this.emitExpr(ts.getExpression());
                break;
            }
            case Opcode.MONITOR: {
                MonitorStmt ms = (MonitorStmt) stmt;

                switch (ms.getMode()) {
                    case ENTER: {
                        this.sw.print(".monitor_enter ");
                        break;
                    }
                    case EXIT: {
                        this.sw.print(".monitor_exit ");
                        break;
                    }
                }

                this.emitExpr(ms.getExpression());
                break;
            }
            case Opcode.POP: {
                PopStmt ps = (PopStmt) stmt;
                this.sw.print(".consume ");
                this.emitExpr(ps.getExpression());
                break;
            }
            case Opcode.RETURN: {
                ReturnStmt rs = (ReturnStmt) stmt;
                this.sw.print(".return");
                if (rs.getExpression() != null) {
                    this.sw.print(" ");
                    this.emitExpr(rs.getExpression());
                }
                break;
            }
            case Opcode.SWITCH_JUMP: {
                SwitchStmt ss = (SwitchStmt) stmt;
                this.sw.print(".switch(");
                this.emitExpr(ss.getExpression());
                this.sw.print(") {");
                this.sw.tab();
                for (Entry<Integer, BasicBlock> e : ss.getTargets().entrySet()) {
                    this.sw.newline().print(".case ").print(String.valueOf(e.getKey()))
                            .print(": .goto ").print(e.getValue().getDisplayName());
                }
                this.sw.newline().print(".default: .goto ")
                        .print(ss.getDefaultTarget().getDisplayName());
                this.sw.untab().newline().print("}");
                break;
            }
            default: {
                throw new UnsupportedOperationException("Got: " + Opcode.opname(opcode));
            }
        }
    }

    private void emitExpr(Expr e) {
        int opcode = e.getOpcode();

        switch (opcode) {
            case Opcode.CONST_LOAD: {
                ConstantExpr ce = (ConstantExpr) e;
                this.emitLiteral(ce.getConstant());
                break;
            }
            case Opcode.LOCAL_LOAD: {
                VarExpr ve = (VarExpr) e;
                // TODO: display name
                this.sw.print(ve.getLocal().toString());
                break;
            }
            case Opcode.FIELD_LOAD: {
                FieldLoadExpr fle = (FieldLoadExpr) e;

                if (fle.isStatic()) {
                    this.sw.print(fle.getOwner());
                } else {
                    Expr instanceExpr = fle.getInstanceExpression();
                    int selfPriority = fle.getPrecedence();
                    int basePriority = instanceExpr.getPrecedence();
                    if (basePriority > selfPriority) {
                        this.sw.print('(');
                    }
                    this.emitExpr(instanceExpr);
                    if (basePriority > selfPriority) {
                        this.sw.print(')');
                    }
                }

                this.sw.print(".").print(fle.getName());
                break;
            }
            case Opcode.ARRAY_LOAD: {
                ArrayLoadExpr ale = (ArrayLoadExpr) e;

                Expr arrayExpr = ale.getArrayExpression();
                Expr indexExpr = ale.getIndexExpression();

                int selfPriority = ale.getPrecedence();
                int expressionPriority = arrayExpr.getPrecedence();
                if (expressionPriority > selfPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(arrayExpr);
                if (expressionPriority > selfPriority) {
                    this.sw.print(')');
                }
                this.sw.print('[');
                this.emitExpr(indexExpr);
                this.sw.print(']');
                break;
            }
            case Opcode.INVOKE: {
                InvocationExpr ie = (InvocationExpr) e;

                if (ie.isDynamic()) {
                    this.sw.print("dynamic_invoke<");
                    this.sw.print(((DynamicInvocationExpr) ie).getProvidedFuncType().getClassName());
                    this.sw.print(">(");
                }
                
                if (ie.isStatic()) {
                    this.sw.print(ie.getOwner());
                } else {
                    int memberAccessPriority = Precedence.MEMBER_ACCESS.ordinal();
                    Expr instanceExpression = ie.getPhysicalReceiver();
                    int instancePriority = instanceExpression.getPrecedence();
                    if (instancePriority > memberAccessPriority) {
                        this.sw.print('(');
                    }
                    this.emitExpr(instanceExpression);
                    if (instancePriority > memberAccessPriority) {
                        this.sw.print(')');
                    }
                }

                this.sw.print('.').print(ie.getName()).print('(');

                Expr[] args = ie.getPrintedArgs();
                for (int i = 0; i < args.length; i++) {
                    this.emitExpr(args[i]);
                    if ((i + 1) < args.length) {
                        this.sw.print(", ");
                    }
                }

                this.sw.print(')');
                if (ie.isDynamic()) {
                    this.sw.print(')');
                }
                break;
            }
            case Opcode.ARITHMETIC: {
                ArithmeticExpr ae = (ArithmeticExpr) e;

                Expr left = ae.getLeft();
                Expr right = ae.getRight();

                int selfPriority = ae.getPrecedence();
                int leftPriority = left.getPrecedence();
                int rightPriority = right.getPrecedence();
                if (leftPriority > selfPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(left);
                if (leftPriority > selfPriority) {
                    this.sw.print(')');
                }
                this.sw.print(" " + ae.getOperator().getSign() + " ");
                if (rightPriority > selfPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(right);
                if (rightPriority > selfPriority) {
                    this.sw.print(')');
                }

                break;
            }
            case Opcode.NEGATE: {
                NegationExpr ne = (NegationExpr) e;

                Expr expr = ne.getExpression();
                int selfPriority = ne.getPrecedence();
                int exprPriority = expr.getPrecedence();
                this.sw.print('-');
                if (exprPriority > selfPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(expr);
                if (exprPriority > selfPriority) {
                    this.sw.print(')');
                }

                break;
            }
            case Opcode.ALLOC_OBJ: {
                AllocObjectExpr aoe = (AllocObjectExpr) e;
                this.sw.print("new ").print(aoe.getType().getClassName().replace(".", "/"));
                break;
            }
            case Opcode.INIT_OBJ: {
                InitialisedObjectExpr ioe = (InitialisedObjectExpr) e;

                this.sw.print("new ");
                this.sw.print(ioe.getOwner());
                this.sw.print('(');

                Expr[] args = ioe.getParameterExprs();
                for (int i = 0; i < args.length; i++) {
                    boolean needsComma = (i + 1) < args.length;
                    this.emitExpr(args[i]);
                    if (needsComma) {
                        this.sw.print(", ");
                    }
                }
                this.sw.print(')');

                break;
            }
            case Opcode.NEW_ARRAY: {
                NewArrayExpr nae = (NewArrayExpr) e;

                Type type = nae.getType();
                this.sw.print("new " + type.getElementType().getClassName());

                Expr[] bounds = nae.getBounds();
                for (int dim = 0; dim < type.getDimensions(); dim++) {
                    this.sw.print('[');
                    if (dim < bounds.length) {
                        this.emitExpr(bounds[dim]);
                    }
                    this.sw.print(']');
                }
                break;
            }
            case Opcode.ARRAY_LEN: {
                ArrayLengthExpr ale = (ArrayLengthExpr) e;

                Expr expr = ale.getExpression();
                int selfPriority = ale.getPrecedence();
                int expressionPriority = expr.getPrecedence();
                if (expressionPriority > selfPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(expr);
                if (expressionPriority > selfPriority) {
                    this.sw.print(')');
                }
                this.sw.print(".length");
                break;
            }
            case Opcode.CAST: {
                CastExpr ce = (CastExpr) e;

                Expr expr = ce.getExpression();
                int selfPriority = ce.getPrecedence();
                int exprPriority = expr.getPrecedence();
                this.sw.print('(');
                Type type = ce.getType();
                this.sw.print(type.getClassName());
                this.sw.print(')');
                if (exprPriority > selfPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(expr);
                if (exprPriority > selfPriority) {
                    this.sw.print(')');
                }
                break;
            }
            case Opcode.INSTANCEOF: {
                InstanceofExpr ioe = (InstanceofExpr) e;

                Expr expr = ioe.getExpression();

                int selfPriority = ioe.getPrecedence();
                int expressionPriority = expr.getPrecedence();
                if (expressionPriority > selfPriority) {
                    this.sw.print('(');
                }
                this.emitExpr(expr);
                if (expressionPriority > selfPriority) {
                    this.sw.print(')');
                }
                this.sw.print(" .instanceof ");
                this.sw.print(ioe.getType().getClassName());
                break;
            }
            case Opcode.COMPARE: {
                ComparisonExpr ce = (ComparisonExpr) e;
                this.sw.print(".compare(");
                this.emitExpr(ce.getLeft());
                switch (ce.getComparisonType()) {
                    case CMP: {
                        this.sw.print("==");
                        break;
                    }
                    case LT: {
                        this.sw.print("<");
                        break;
                    }
                    case GT: {
                        this.sw.print(">");
                        break;
                    }
                }
                this.emitExpr(ce.getRight());
                this.sw.print(")");
                break;
            }
            case Opcode.CATCH: {
                this.sw.print(".catch");
                break;
            }
            default: {
                throw new UnsupportedOperationException("Got: " + Opcode.opname(opcode));
            }
        }
    }

    private void emitHandlers(ControlFlowGraph cfg) {
        List<ExceptionRange<BasicBlock>> ranges = cfg.getRanges();
        if (ranges.size() > 0) {

            List<BasicBlock> allNodes = cfg.verticesInOrder();

            List<Map<String, String>> handlerEntries = new ArrayList<>();
            for (ExceptionRange<BasicBlock> range : ranges) {
                List<BasicBlock> nodes = range.getNodes();
                // key order for nice print
                Map<String, String> entry = new LinkedHashMap<>();

                entry.put("start", nodes.get(0).getDisplayName());
                BasicBlock end = nodes.get(nodes.size() - 1);
                // exclusive
                BasicBlock endNext = allNodes.get(allNodes.indexOf(end) + 1);
                entry.put("end", endNext.getDisplayName());
                entry.put("handler", range.getHandler().getDisplayName());

                handlerEntries.add(entry);
            }

            this.emitDirective("handlers", handlerEntries);
        }
    }

    public void emitMethodAttributes(MethodNode mn) {
        if (mn.access != 0) {
            this.emitAccessDirective(mn.access);
        }

        if (mn.signature != null) {
            this.emitDirective("signature", mn.signature);
        }

        if (isNonEmpty(mn.exceptions)) {
            this.emitDirective("exceptions", mn.exceptions);
        }

        if (isNonEmpty(mn.parameters)) {
            throw new UnsupportedOperationException();
        }

        if (isNonEmpty(mn.visibleAnnotations)) {
            this.emitDirective("visibleAnnotations", mn.visibleAnnotations);
        }
        if (isNonEmpty(mn.invisibleAnnotations)) {
            this.emitDirective("invisibleAnnotations", mn.invisibleAnnotations);
        }
        if (isNonEmpty(mn.visibleTypeAnnotations)) {
            this.emitDirective("visibleTypeAnnotations", mn.visibleTypeAnnotations);
        }
        if (isNonEmpty(mn.invisibleTypeAnnotations)) {
            this.emitDirective("invisibleTypeAnnotations", mn.invisibleTypeAnnotations);
        }
        if (isNonEmpty(mn.visibleParameterAnnotations)) {
            this.emitDirective("visibleParameterAnnotations", mn.visibleParameterAnnotations);
        }
        if (isNonEmpty(mn.invisibleParameterAnnotations)) {
            this.emitDirective("invisibleParameterAnnotations", mn.invisibleParameterAnnotations);
        }
        if (isNonEmpty(mn.visibleLocalVariableAnnotations)) {
            throw new UnsupportedOperationException("visibleLocalVariableAnnotations");
        }
        if (isNonEmpty(mn.invisibleLocalVariableAnnotations)) {
            throw new UnsupportedOperationException("invisibleLocalVariableAnnotations");
        }
        if (isNonEmpty(mn.attrs)) {
            this.emitNodeAttributes(mn.attrs);
        }
        if (mn.annotationDefault != null) {
            this.emitDirective("annotationDefault", mn.annotationDefault);
        }
    }
}
