package org.mapleir.deob.passes;

import java.util.ListIterator;
import java.util.Set;

import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public class CallgraphPruningPass implements IPass {

	@Override
	public String getId() {
		return "CG-Prune";
	}

	@Override
	public PassResult accept(PassContext cxt) {
		int delta = 0;

		Set<MethodNode> active = cxt.getAnalysis().getIRCache().getActiveMethods();
		for(ClassNode cn : cxt.getAnalysis().getApplication().iterate()) {
			ListIterator<MethodNode> lit = cn.getMethods().listIterator();
			while(lit.hasNext()) {
				MethodNode m = lit.next();
				if(!active.contains(m)) {
					lit.remove();
					delta++;
				}
			}
		}
		
		System.out.println("Removed " + delta + " dead methods.");
		
		return PassResult.with(cxt, this).finished().make();
	}
}
