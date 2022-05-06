package org.mapleir;

import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.InsnListUtils;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.IRCache;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.codegen.ControlFlowGraphDumper;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class CleanBoot {

    public static void main(String[] args) throws Exception {
        ClassNode cn = ClassHelper.create(new FileInputStream(new File("MemeIn.class")));
        IRCache irFactory = new IRCache();
        for (MethodNode mn : cn.getMethods()) {
            // if (!mn.getName().equals("merge"))
            //     continue;
            // if (mn.getName().equals("merge"))
            //     System.out.println(InsnListUtils.insnListToString(mn.node.instructions));

            ControlFlowGraph cfg = irFactory.getNonNull(mn);

            // if (mn.getName().equals("merge"))
            //     System.out.println(cfg);
            // if (mn.getName().equals("merge"))
            //     CFGUtils.easyDumpCFG(cfg, "pre-destruct");
            cfg.verify();

            BoissinotDestructor.leaveSSA(cfg);

            // if (mn.getName().equals("merge"))
            //     CFGUtils.easyDumpCFG(cfg, "pre-reaalloc");
            LocalsReallocator.realloc(cfg);
            // if (mn.getName().equals("merge"))
            //     CFGUtils.easyDumpCFG(cfg, "post-reaalloc");
            // System.out.println(cfg);
            cfg.verify();
            System.out.println("Rewriting " + mn.getName());
            (new ControlFlowGraphDumper(cfg, mn)).dump();

            System.out.println(InsnListUtils.insnListToString(mn.node.instructions));
        }
        new FileOutputStream(new File("Meme.class")).write(ClassHelper.toByteArray(cn, ClassWriter.COMPUTE_FRAMES));
    }
}
