package org.mapleir.deob.interproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mapleir.context.AnalysisContext;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.mapleir.asm.MethodNode;

public class IPAnalysis extends IRCallTracer implements Opcode {
	
	public static IPAnalysis create(AnalysisContext cxt, IPAnalysisVisitor... visitors) {
		List<IPAnalysisVisitor> cvs = new ArrayList<>();
		for(IPAnalysisVisitor v : visitors) {
			cvs.add(v);
		}
		return create(cxt, cvs);
	}
	
	public static IPAnalysis create(AnalysisContext cxt, List<IPAnalysisVisitor> visitors) {
		IPAnalysis analysis = new IPAnalysis(cxt, visitors);
		// TODO: from entry points
		for(MethodNode mn : cxt.getIRCache().getActiveMethods()) {
			analysis.trace(mn);
		}
		return analysis;
	}
	
	private final List<IPAnalysisVisitor> visitors;
	
	// TODO: Convert this to struct?
	private final Map<MethodNode, Set<Invocation>> callers;
	private final Map<MethodNode, List<List<Expr>>> parameterInputs;
	private final Map<MethodNode, int[]> paramIndices;
	
	public IPAnalysis(AnalysisContext cxt, List<IPAnalysisVisitor> visitors) {
		super(cxt);
		this.visitors = visitors;
		
		callers = new HashMap<>();
		parameterInputs = new HashMap<>();
		paramIndices = new HashMap<>();
	}
	
	public Set<Invocation> getCallsTo(MethodNode m) {
		return callers.get(m);
	}

	public int getLocalIndex(MethodNode m, int i) {
		int[] idxs = paramIndices.get(m);
		return idxs[i];
	}
	
	public int getParameterCount(MethodNode m) {
		if(paramIndices.containsKey(m) && parameterInputs.containsKey(m)) {
			int i1 = paramIndices.get(m).length;
			int i2 = parameterInputs.get(m).size();
			if(i1 != i2) {
				throw new IllegalStateException(String.format("%s | %d:%d | %s : %s", m, i1, i2, Arrays.toString(paramIndices.get(m)), parameterInputs.get(m)));
			}
			return i1;
		} else {
			throw new UnsupportedOperationException(m.toString());
		}
	}

	public List<List<Expr>> getInputs(MethodNode method) {
		return parameterInputs.get(method);
	}
	
	public List<Expr> getInputs(MethodNode method, int paramIndex) {
		/*if(calls.containsKey(method)) {
			int[] idxs = paramIndices.get(method);
			int lvtIndex = idxs[paramIndex];
			
			List<List<Expr>> mInputs = parameterInputs.get(method);
			return mInputs.get(lvtIndex);
		} else {
			return null;
		}*/
		return parameterInputs.get(method).get(paramIndex);
	}
	
	@Override
	protected void visitMethod(MethodNode m) {
		// Callbacks
		for(IPAnalysisVisitor v : visitors) {
			v.preVisitMethod(this, m);
		}
		
		// Do not trace library calls
		if(context.getApplication().isLibraryClass(m.getOwner())) {
			return;
		}
		
		// Create a mapping between the actual variable table indices and the parameter
		// indices in the method descriptor.
		boolean isStatic = (m.node.access & Opcodes.ACC_STATIC) != 0;
		
		int paramCount = Type.getArgumentTypes(m.getDesc()).length;
		int off = (isStatic ? 0 : 1);
		int synthCount = paramCount + off;
		List<List<Expr>> lists = new ArrayList<>(synthCount);
		
		int[] idxs = new int[synthCount];
		
		// Scan for synthetic copies to populate indices
		ControlFlowGraph cfg = context.getIRCache().getFor(m);
		BasicBlock entry = cfg.getEntries().iterator().next();
		
		/* static:
		 *  first arg = 0
		 *
		 * non-static:
		 *  this = 0
		 *  first arg = 1*/
		int paramIndex = 0;
		for(Stmt stmt : entry) {
			if(stmt.getOpcode() == LOCAL_STORE) {
				CopyVarStmt cvs = (CopyVarStmt) stmt;
				if(cvs.isSynthetic()) {
					int varIndex = cvs.getVariable().getLocal().getIndex();
					if (!isStatic && varIndex == 0)
						continue;
					idxs[paramIndex++] = varIndex;
					continue;
				}
			}
			break;
		}
		
		for(int j=0; j < paramCount; j++) {
			lists.add(new ArrayList<>());
		}
		
		paramIndices.put(m, idxs);
		
		parameterInputs.put(m, lists);
		callers.put(m, new HashSet<>());
		
		// Callbacks
		for(IPAnalysisVisitor v : visitors) {
			v.postVisitMethod(this, m);
		}
	}
	
	@Override
	protected void processedInvocation(MethodNode caller, MethodNode callee, Invocation e) {
		// Callbacks
		for(IPAnalysisVisitor v : visitors) {
			v.preProcessedInvocation(this, caller, callee, e);
		}
		
		// Do not trace library calls.
		if(context.getApplication().isLibraryClass(callee.getOwner())) {
			return;
		}
		
		callers.get(callee).add(e);
		
		// Update parameter information
		Expr[] params = e.getParameterExprs();
		for(int i=0; i < params.length; i++) {
			parameterInputs.get(callee).get(i).add(params[i]);
		}
		
		// Callbacks
		for(IPAnalysisVisitor v : visitors) {
			v.postProcessedInvocation(this, caller, callee, e);
		}
	}
	
}
