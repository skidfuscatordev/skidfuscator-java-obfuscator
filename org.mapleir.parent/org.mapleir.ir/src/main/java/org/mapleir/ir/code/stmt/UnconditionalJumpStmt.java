package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class UnconditionalJumpStmt extends Stmt {

	private BasicBlock target;
	private UnconditionalJumpEdge<BasicBlock> edge;

	public UnconditionalJumpStmt(BasicBlock target, UnconditionalJumpEdge<BasicBlock> edge) {
		super(UNCOND_JUMP);
		this.target = target;
		this.edge = edge;
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("goto " + target.getDisplayName());
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		visitor.visitJumpInsn(Opcodes.GOTO, assembler.getLabel(target));
	}

	@Override
	public boolean canChangeFlow() {
		return true;
	}

	@Override
	public Stmt copy() {
		return new UnconditionalJumpStmt(target, edge);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof UnconditionalJumpStmt) {
			UnconditionalJumpStmt jump = (UnconditionalJumpStmt) s;
			return target == jump.target;
		}
		return false;
	}
}