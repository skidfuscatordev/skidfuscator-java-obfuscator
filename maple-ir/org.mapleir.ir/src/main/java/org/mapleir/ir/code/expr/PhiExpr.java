package org.mapleir.ir.code.expr;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.Map.Entry;

public class PhiExpr extends Expr {

	private final Map<BasicBlock, Expr> arguments;
	private Type type;
	
	protected PhiExpr(int opcode, Map<BasicBlock, Expr> arguments) {
		super(opcode);
		this.arguments = arguments;
	}
	
	public PhiExpr(Map<BasicBlock, Expr> arguments) {
		super(PHI);
		this.arguments = arguments;
	}
	
	public int getArgumentCount() {
		return arguments.size();
	}
	
	public Set<BasicBlock> getSources() {
		return arguments.keySet();
	}
	
	public Map<BasicBlock, Expr> getArguments() {
		return arguments;
	}
	
	public Expr getArgument(BasicBlock b) {
		return arguments.get(b);
	}
	
	public void setArgument(BasicBlock b, Expr e) {
		arguments.put(b, e);
	}
	
	public void removeArgument(BasicBlock b) {
		arguments.remove(b);
	}
	
	@Override
	public void onChildUpdated(int ptr) {
		raiseChildOutOfBounds(ptr);
	}

	@Override
	public PhiExpr copy() {
		Map<BasicBlock, Expr> map = new HashMap<>();
		for(Entry<BasicBlock, Expr> e : arguments.entrySet()) {
			map.put(e.getKey(), e.getValue().copy());
		}
		return new PhiExpr(map);
	}

	@Override
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	protected char getPhiType() {
		return '\u0278';
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print(getPhiType() + "{");
		Iterator<Entry<BasicBlock, Expr>> it = arguments.entrySet().iterator();
		while(it.hasNext()) {
			Entry<BasicBlock, Expr> e = it.next();
			
			printer.print(e.getKey().getDisplayName());
			printer.print(":");
			e.getValue().toString(printer);
			
			if(it.hasNext()) {
				printer.print(", ");
			}
		}
		printer.print("}");
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		throw new UnsupportedOperationException("Phi is not executable.");
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof PhiExpr) {
			PhiExpr phi = (PhiExpr) s;
			
			Set<BasicBlock> sources = new HashSet<>();
			sources.addAll(arguments.keySet());
			sources.addAll(phi.arguments.keySet());
			
			if(sources.size() != arguments.size()) {
				return false;
			}
			
			for(BasicBlock b : sources) {
				Expr e1 = arguments.get(b);
				Expr e2 = phi.arguments.get(b);
				if(e1 == null || e2 == null) {
					return false;
				}
				if(!e1.equivalent(e2)) {
					return false;
				}
			}
			
			return true;
		}
		return false;
	}
}