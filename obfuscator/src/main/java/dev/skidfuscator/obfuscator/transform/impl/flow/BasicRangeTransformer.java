package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventPriority;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.FinalMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpEdge;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeUnconditionalJumpStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.ImmediateEdge;
import org.mapleir.flowgraph.edges.TryCatchEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.CaughtExceptionExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.utils.CFGUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * This transformer basically does some whacky stuff:
 *
 * Original flow:
 *
 *     A
 *     ↓
 *     B
 *
 * Obfuscated flow:
 *
 *           A
 *           ↓
 *          Fake
 *        Condition
 *      /          \
 *    Trap       Exception
 *
 *
 *   [Exception catcher]
 *            ↓
 *            B
 */
public class BasicRangeTransformer extends AbstractTransformer {
    public BasicRangeTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public BasicRangeTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Basic Exception Transformer", children);
    }

    @Listen(EventPriority.LOW)
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (methodNode.isAbstract() || methodNode.isInit())
            return;

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
            System.out.println(CFGUtils.printBlock(range.getHandler()));
        }

        for (BasicBlock entry : new HashSet<>(cfg.vertices())) {
            if (entry.size() == 0)
                continue;

            if (entry.isFlagSet(SkidBlock.FLAG_NO_OPAQUE))
                continue;

            // Todo add hashing to amplify difficulty and remove key exposure
            // Todo make this a better system
            for (Stmt stmt : entry) {
                if (!(stmt instanceof UnconditionalJumpStmt)) {
                    continue;
                }

                final UnconditionalJumpStmt jmp = (UnconditionalJumpStmt) stmt;
                // Create hash
                // Todo add more boilerplates + add exception rotation
                final BasicBlock target = jmp.getTarget();


                final BasicBlock fuckup = new SkidBlock(cfg);
                cfg.addVertex(fuckup);

                final Type exceptionType = RandomUtil.nextException();
                fuckup.add(new ThrowStmt(
                        new InitialisedObjectExpr(exceptionType.getInternalName(), "()V", new Expr[0])
                ));
                cfg.removeEdge(jmp.getEdge());

                final BasicBlock targetBridge = new SkidBlock(cfg);
                cfg.addVertex(targetBridge);

                final UnconditionalJumpEdge<BasicBlock> targetBridgeEdge = new UnconditionalJumpEdge<>(targetBridge, target);
                targetBridge.add(new PopStmt(
                        new CaughtExceptionExpr(exceptionType)
                ));
                targetBridge.add(
                        new UnconditionalJumpStmt(target, targetBridgeEdge)
                );
                cfg.addEdge(targetBridgeEdge);


                final SkidBlock bridgeToFuckup = new SkidBlock(cfg);
                cfg.addVertex(bridgeToFuckup);
                // Create hash
                final SkiddedHash hash = NumberManager
                        .randomHasher()
                        .hash(
                                methodNode.getBlockPredicate(bridgeToFuckup),
                                entry,
                                methodNode.getFlowPredicate().getGetter()
                        );
                final ConstantExpr var_const = new ConstantExpr(hash.getHash());

                // Todo add more boilerplates + add exception rotation
                // Todo change blocks to be skiddedblocks to add method to directly add these
                final FakeConditionalJumpStmt bridgeToFuckupStmt = new FakeConditionalJumpStmt(
                        hash.getExpr(),
                        var_const,
                        fuckup,
                        ConditionalJumpStmt.ComparisonType.EQ
                );
                final FakeConditionalJumpEdge<BasicBlock> edgeFromBridgeToFuckup = new FakeConditionalJumpEdge<>(
                        bridgeToFuckup, fuckup, Opcodes.IF_ICMPNE
                );
                bridgeToFuckup.add(bridgeToFuckupStmt);
                bridgeToFuckup.add(new ThrowStmt(new ConstantExpr(null)));
                cfg.addEdge(edgeFromBridgeToFuckup);


                final UnconditionalJumpEdge<BasicBlock> edgeFromEntryToBridge = new UnconditionalJumpEdge<>(entry, bridgeToFuckup);
                jmp.setEdge(edgeFromEntryToBridge);
                jmp.setTarget(bridgeToFuckup);
                cfg.addEdge(edgeFromEntryToBridge);



                final ExceptionRange<BasicBlock> range = new ExceptionRange<>();
                range.addVertex(bridgeToFuckup);
                range.addVertex(fuckup);
                range.setHandler(targetBridge);
                range.setTypes(new HashSet<>(Collections.singleton(exceptionType)));

                cfg.addEdge(new TryCatchEdge<>(fuckup, range));
                cfg.addRange(range);

                event.tick();
            }


            // Regular debug
                /*
                final Local local1 = entry.cfg.getLocals().get(entry.cfg.getLocals().getMaxLocals() + 2);
                entry.add(new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                        new ConstantExpr(entry.getDisplayName() +" : var expect: " + var_const.getConstant())));
                */
        }
    }
}
