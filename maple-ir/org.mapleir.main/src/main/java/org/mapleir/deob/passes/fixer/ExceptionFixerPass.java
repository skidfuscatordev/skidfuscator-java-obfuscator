package org.mapleir.deob.passes.fixer;

import com.google.common.collect.Lists;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mapleir.asm.ClassNode;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExceptionFixerPass implements IPass {
    private final Logger logger = LogManager.getLogger(this.getClass());

    @Override
    public PassResult accept(PassContext cxt) {
        final AtomicInteger counter = new AtomicInteger();

        for (ControlFlowGraph value : cxt.getAnalysis().getIRCache().values()) {
            for (ExceptionRange<BasicBlock> range : value.getRanges()) {
                if (range.getTypes().size() <= 1)
                    continue;

                final Stack<ClassNode> stack = new Stack<>();
                for (Type type : range.getTypes()) {
                    final ClassNode classNode = cxt.getAnalysis().getApplication().findClassNode(type.getClassName());

                    final List<ClassNode> classNodeList = cxt.getAnalysis().getApplication().getClassTree().getAllParents(classNode);

                    if (stack.isEmpty()) {
                        stack.add(classNode);
                        stack.addAll(Lists.reverse(classNodeList));
                    } else {
                        final Stack<ClassNode> toIterate = new Stack<>();
                        toIterate.add(classNode);
                        toIterate.addAll(Lists.reverse(classNodeList));

                        runner: {
                            while (!stack.isEmpty()) {
                                for (ClassNode node : toIterate) {
                                    if (node.equals(stack.peek()))
                                        break runner;
                                }

                                stack.pop();
                            }

                            logger.fatal("[*] Could not find common exception type between " + Arrays.toString(range.getTypes().toArray()));
                        }
                    }
                }

                counter.incrementAndGet();
                range.setTypes(new HashSet<>(Collections.singleton(Type.getType(stack.peek().getName()))));
            }
        }

        logger.info("[*] Successfully fixed" + counter.get() + " exception ranges!");
        return PassResult.with(cxt, this).finished().make();
    }
}
