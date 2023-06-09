package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventPriority;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
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
import dev.skidfuscator.obfuscator.verifier.alertable.AlertableConstantExpr;
import org.mapleir.flowgraph.ExceptionRange;
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
 * Original flow:       Obfuscated Flow:
 *
 * ┌─────────┐            ┌─────────┐
 * │ Block A │            │ Block A │
 * └────┬────┘            └────┬────┘
 *      │                      │
 * ┌────▼────┐         ┌───────▼────────┐
 * │ Block B │         │ Random If Stmt │
 * └─────────┘         └───────┬────────┘
 *                             │
 *                   ┌─────┐◄──┴───►┌─────┐
 *                   │ Yes │        │ No  │
 *                   └─────┘        └──┬──┘
 *                                     │
 *                               ┌─────▼─────┐
 *                               │ Exception │
 *                               └───────────┘
 *
 *                      ┌─────────────┐
 *                      │  Exception  │
 *                      │   Catcher   │
 *                      └──────┬──────┘
 *                             │
 *                        ┌────▼────┐
 *                        │ Block B │
 *                        └─────────┘
 */
public class BasicRangeTransformer extends AbstractTransformer {
    public BasicRangeTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public BasicRangeTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Flow Range", children);
    }

    @Listen(EventPriority.LOW)
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (methodNode.isAbstract() || methodNode.isInit())
            return;

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        for (BasicBlock entry : new HashSet<>(cfg.vertices())) {
            if (entry.size() == 0)
                continue;

            if (entry.isFlagSet(SkidBlock.FLAG_NO_OPAQUE))
                continue;

            for (Stmt stmt : entry) {
                if (!(stmt instanceof UnconditionalJumpStmt) || stmt instanceof FakeUnconditionalJumpStmt) {
                    continue;
                }

                final UnconditionalJumpStmt jmp = (UnconditionalJumpStmt) stmt;
                final BasicBlock target = jmp.getTarget();

                /*
                 * THROW DISPATCHER
                 * ------------------------------------------------------------
                 * Create a block which will guaranteed throw the exception.
                 * This block is our 'dispatcher'. It dispatches an exception.
                 * How clever...
                 *
                 * A --> THROW_BRIDGE --> THIS --> <thrown exception>
                 */
                final BasicBlock blockThrow = new SkidBlock(cfg);
                cfg.addVertex(blockThrow);

                final Type exceptionType = RandomUtil.nextException();
                blockThrow.add(new ThrowStmt(
                        new InitialisedObjectExpr(exceptionType.getInternalName(), "()V", new Expr[0])
                ));
                cfg.removeEdge(jmp.getEdge());

                /*
                 * THROW BRIDGE
                 * ------------------------------------------------------------
                 * This is an indirection condition added on top to prevent
                 * skids from just saying "oh if block throws x exception without
                 * any condition then we can just jump to handler".
                 *
                 * A --> THIS --> THROW_DISPATCHER --> <thrown exception>
                 */
                final SkidBlock throwBridge = new SkidBlock(cfg);
                cfg.addVertex(throwBridge);

                /* Hash the condition to prevent skids from guessing seeds */
                final SkiddedHash hash = NumberManager
                        .randomHasher(skidfuscator)
                        .hash(
                                methodNode.getBlockPredicate(throwBridge),
                                entry,
                                methodNode.getFlowPredicate().getGetter()
                        );
                final ConstantExpr var_const = new AlertableConstantExpr(hash.getHash());

                /* Create a fake conditional if exception that's always true */
                final FakeConditionalJumpStmt bridgeToFuckupStmt = new FakeConditionalJumpStmt(
                        hash.getExpr(),
                        var_const,
                        blockThrow,
                        ConditionalJumpStmt.ComparisonType.EQ
                );
                final FakeConditionalJumpEdge<BasicBlock> edgeFromBridgeToFuckup = new FakeConditionalJumpEdge<>(
                        throwBridge, blockThrow, Opcodes.IF_ICMPNE
                );
                throwBridge.add(bridgeToFuckupStmt);
                cfg.addEdge(edgeFromBridgeToFuckup);

                /* Add a throw null which causes a NullPointerException to confuse decompilers */
                throwBridge.add(new ThrowStmt(new ConstantExpr(null)));

                /*
                 * TARGET BRIDGE
                 * ------------------------------------------------------------
                 * This is our bridge to our target for our jump condition. This
                 * essentially guarantees that we catch the exception, pop it off
                 * the stack, then move on to our desired location
                 *
                 * <caught exception> --> THIS --> B (target)
                 */
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

                /*
                 * JUMP
                 * ------------------------------------------------------------
                 * Now that we've modified and set up a proper exception trap,
                 * we can modify the conditions of the jump statement:
                 *
                 * JMP --> THROW_BRIDGE --> THROW_DISPATCHER
                 */
                jmp.setTarget(throwBridge);
                final UnconditionalJumpEdge<BasicBlock> edgeFromEntryToBridge = new UnconditionalJumpEdge<>(entry, throwBridge);
                jmp.setEdge(edgeFromEntryToBridge);
                cfg.addEdge(edgeFromEntryToBridge);


                /*
                 * EXCEPTION RANGE
                 * ------------------------------------------------------------
                 * Here we add all the details of our exception range, including
                 * a singleton set of our exception type, the blocks involved
                 * (THROW_BRIDGE && THROW_DISPATCHER) and our handler (TARGET_BRIDGE).
                 */
                final ExceptionRange<BasicBlock> range = new ExceptionRange<>();
                range.addVertex(throwBridge);
                range.addVertex(blockThrow);
                range.setHandler(targetBridge);
                range.setTypes(new HashSet<>(Collections.singleton(exceptionType)));
                cfg.addEdge(new TryCatchEdge<>(throwBridge, range));
                cfg.addEdge(new TryCatchEdge<>(blockThrow, range));
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
