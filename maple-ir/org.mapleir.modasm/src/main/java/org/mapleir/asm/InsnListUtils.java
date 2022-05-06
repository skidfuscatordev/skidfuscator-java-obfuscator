package org.mapleir.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class InsnListUtils {
	public static String insnListToString(InsnList insns) { 
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i< insns.size(); i++) {
			sb.append(insnToString(insns.get(i)));
		}
		return sb.toString();
	}
	
	public static String insnToString(AbstractInsnNode insn) {
		insn.accept(mp);
		StringWriter sw = new StringWriter();
		printer.print(new PrintWriter(sw));
		printer.getText().clear();
		return sw.toString();
	}

	private static Printer printer = new Textifier();
	private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);
}
