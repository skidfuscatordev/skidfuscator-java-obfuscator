package dev.skidfuscator.obf.transform.impl.kappa;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform.impl.ProjectPass;
import dev.skidfuscator.obf.utils.RandomUtil;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.expr.NewArrayExpr;
import org.mapleir.ir.code.stmt.ArrayStoreStmt;
import org.mapleir.ir.code.stmt.FieldStoreStmt;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * Do not question this. This is a stupid idea; Stupid ideas are bets. Bets are funny.
 * End of discussion.
 */
public class AhegaoPass implements ProjectPass {

    @Override
    public void pass(SkidSession session) {
        for (ClassNode classNode : session.getClassSource().iterate()) {
            final org.objectweb.asm.tree.FieldNode fieldNode = new org.objectweb.asm.tree.FieldNode(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    "nothing_to_see_here",
                    "[Ljava/lang/String;",
                    null,
                    null
            );
            final FieldNode mapleNode = new FieldNode(fieldNode, classNode);
            classNode.getFields().add(mapleNode);
            classNode.node.fields.add(fieldNode);

            MethodNode clinit = classNode.getMethods().stream()
                    .filter(e -> e.getName().equals("<clinit>") && e.getDesc().equals("()V"))
                    .findFirst()
                    .orElse(null);

            if (clinit == null) {
                final org.objectweb.asm.tree.MethodNode asmclinit = new org.objectweb.asm.tree.MethodNode(
                        Opcodes.ACC_STATIC,
                        "<clinit>",
                        "()V",
                        null,
                        new String[0]
                );

                clinit = new MethodNode(asmclinit, classNode);

                classNode.getMethods().add(clinit);
                classNode.node.methods.add(asmclinit);
            }

            final String[] array = ahegaos.get(RandomUtil.nextInt(ahegaos.size()));

            final NewArrayExpr array_expr = new NewArrayExpr(
                    new Expr[] { new ConstantExpr(array.length, Type.INT_TYPE)},
                    Type.getType(String[].class)
            );


            final ControlFlowGraph cfg = session.getCxt().getIRCache().getFor(clinit);

            final Stack<Stmt> stack = new Stack<>();

            stack.add(new FieldStoreStmt(
                    null,
                    array_expr,
                    mapleNode.getOwner(),
                    mapleNode.getName(),
                    mapleNode.getDesc(),
                    true
            ));

            for (int i = 0; i < array.length; i++) {
                stack.add(new ArrayStoreStmt(
                        new FieldLoadExpr(
                                null,
                                mapleNode.getOwner(),
                                mapleNode.getName(),
                                mapleNode.getDesc(),
                                true
                        ),
                        new ConstantExpr(i, Type.INT_TYPE),
                        new ConstantExpr(array[i]),
                        TypeUtils.ArrayType.OBJECT
                ));
            }

            Stmt stmt = stack.pop();

            if (cfg.getEntries().isEmpty()) {
                cfg.addVertex(new BasicBlock(cfg));
            }

            while (stmt != null) {
                cfg.getEntries().iterator().next().add(0, stmt);

                if (stack.isEmpty())
                    break;
                stmt = stack.pop();
            }
        }
    }

    @Override
    public String getName() {
        return "Ahegao Pass";
    }

    private static final List<String[]> ahegaos = Arrays.asList(
            new String[] {
                    "⠄⠄⠄⢰⣧⣼⣯⠄⣸⣠⣶⣶⣦⣾⠄⠄⠄⠄⡀⠄⢀⣿⣿⠄⠄⠄⢸⡇⠄⠄",
                    "⠄⠄⠄⣾⣿⠿⠿⠶⠿⢿⣿⣿⣿⣿⣦⣤⣄⢀⡅⢠⣾⣛⡉⠄⠄⠄⠸⢀⣿⠄",
                    "⠄⠄⢀⡋⣡⣴⣶⣶⡀⠄⠄⠙⢿⣿⣿⣿⣿⣿⣴⣿⣿⣿⢃⣤⣄⣀⣥⣿⣿⠄",
                    "⠄⠄⢸⣇⠻⣿⣿⣿⣧⣀⢀⣠⡌⢻⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⠿⣿⣿⣿⠄",
                    "⠄⢀⢸⣿⣷⣤⣤⣤⣬⣙⣛⢿⣿⣿⣿⣿⣿⣿⡿⣿⣿⡍⠄⠄⢀⣤⣄⠉⠋⣰",
                    "⠄⣼⣖⣿⣿⣿⣿⣿⣿⣿⣿⣿⢿⣿⣿⣿⣿⣿⢇⣿⣿⡷⠶⠶⢿⣿⣿⠇⢀⣤",
                    "⠘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣽⣿⣿⣿⡇⣿⣿⣿⣿⣿⣿⣷⣶⣥⣴⣿⡗",
                    "⢀⠈⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡟⠄",
                    "⢸⣿⣦⣌⣛⣻⣿⣿⣧⠙⠛⠛⡭⠅⠒⠦⠭⣭⡻⣿⣿⣿⣿⣿⣿⣿⣿⡿⠃⠄",
                    "⠘⣿⣿⣿⣿⣿⣿⣿⣿⡆⠄⠄⠄⠄⠄⠄⠄⠄⠹⠈⢋⣽⣿⣿⣿⣿⣵⣾⠃⠄",
                    "⠄⠘⣿⣿⣿⣿⣿⣿⣿⣿⠄⣴⣿⣶⣄⠄⣴⣶⠄⢀⣾⣿⣿⣿⣿⣿⣿⠃⠄⠄",
                    "⠄⠄⠈⠻⣿⣿⣿⣿⣿⣿⡄⢻⣿⣿⣿⠄⣿⣿⡀⣾⣿⣿⣿⣿⣛⠛⠁⠄⠄⠄",
                    "⠄⠄⠄⠄⠈⠛⢿⣿⣿⣿⠁⠞⢿⣿⣿⡄⢿⣿⡇⣸⣿⣿⠿⠛⠁⠄⠄⠄⠄⠄",
                    "⠄⠄⠄⠄⠄⠄⠄⠉⠻⣿⣿⣾⣦⡙⠻⣷⣾⣿⠃⠿⠋⠁⠄⠄⠄⠄⠄⢀⣠⣴",
                    "⣿⣿⣿⣶⣶⣮⣥⣒⠲⢮⣝⡿⣿⣿⡆⣿⡿⠃⠄⠄⠄⠄⠄⠄⠄⣠⣴⣿⣿⣿"
            },
            new String[] {
                    "⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⣀⣠⣤⣶⣶⣶⣤⣄⣀⣀⠄⠄⠄⠄⠄",
                    "⠄⠄⠄⠄⠄⠄⠄⠄⣀⣤⣤⣶⣿⣿⣿⣿⣿⣿⣿⣟⢿⣿⣿⣿⣶⣤⡀⠄",
                    "⠄⠄⠄⠄⠄⠄⢀⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣜⠿⠿⣿⣿⣧⢓",
                    "⠄⠄⠄⠄⠄⡠⢛⣿⣿⣿⡟⣿⣿⣽⣋⠻⢻⣿⣿⣿⣿⡻⣧⡠⣭⣭⣿⡧",
                    "⠄⠄⠄⠄⠄⢠⣿⡟⣿⢻⠃⣻⣨⣻⠿⡀⣝⡿⣿⣿⣷⣜⣜⢿⣝⡿⡻⢔",
                    "⠄⠄⠄⠄⠄⢸⡟⣷⢿⢈⣚⣓⡡⣻⣿⣶⣬⣛⣓⣉⡻⢿⣎⠢⠻⣴⡾⠫",
                    "⠄⠄⠄⠄⠄⢸⠃⢹⡼⢸⣿⣿⣿⣦⣹⣿⣿⣿⠿⠿⠿⠷⣎⡼⠆⣿⠵⣫",
                    "⠄⠄⠄⠄⠄⠈⠄⠸⡟⡜⣩⡄⠄⣿⣿⣿⣿⣶⢀⢀⣿⣷⣿⣿⡐⡇⡄⣿",
                    "⠄⠄⠄⠄⠄⠄⠄⠄⠁⢶⢻⣧⣖⣿⣿⣿⣿⣿⣿⣿⣿⡏⣿⣇⡟⣇⣷⣿",
                    "⠄⠄⠄⠄⠄⠄⠄⠄⠄⢸⣆⣤⣽⣿⡿⠿⠿⣿⣿⣦⣴⡇⣿⢨⣾⣿⢹⢸",
                    "⠄⠄⠄⠄⠄⠄⠄⠄⠄⢸⣿⠊⡛⢿⣿⣿⣿⣿⡿⣫⢱⢺⡇⡏⣿⣿⣸⡼",
                    "⠄⠄⠄⠄⠄⠄⠄⠄⠄⢸⡿⠄⣿⣷⣾⡍⣭⣶⣿⣿⡌⣼⣹⢱⠹⣿⣇⣧",
                    "⠄⠄⠄⠄⠄⠄⠄⠄⠄⣼⠁⣤⣭⣭⡌⢁⣼⣿⣿⣿⢹⡇⣭⣤⣶⣤⡝⡼",
                    "⠄⣀⠤⡀⠄⠄⠄⠄⠄⡏⣈⡻⡿⠃⢀⣾⣿⣿⣿⡿⡼⠁⣿⣿⣿⡿⢷⢸",
                    "⢰⣷⡧⡢⠄⠄⠄⠄⠠⢠⡛⠿⠄⠠⠬⠿⣿⠭⠭⢱⣇⣀⣭⡅⠶⣾⣷⣶",
                    "⠈⢿⣿⣧⠄⠄⠄⠄⢀⡛⠿⠄⠄⠄⠄⢠⠃⠄⠄⡜⠄⠄⣤⢀⣶⣮⡍⣴",
                    "⠄⠈⣿⣿⡀⠄⠄⠄⢩⣝⠃⠄⠄⢀⡄⡎⠄⠄⠄⠇⠄⠄⠅⣴⣶⣶⠄⣶"
            }
    );
}
