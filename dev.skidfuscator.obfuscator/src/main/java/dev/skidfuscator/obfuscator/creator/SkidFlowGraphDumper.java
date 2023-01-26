package dev.skidfuscator.obfuscator.creator;

import com.google.common.collect.Streams;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.frame.Frame;
import dev.skidfuscator.obfuscator.frame_V2.frame.FrameComputer;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidExpressionPool;
import dev.skidfuscator.obfuscator.skidasm.SkidTypeStack;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidBogusStmt;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import org.mapleir.asm.ClassNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.*;
import org.mapleir.ir.code.expr.CaughtExceptionExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.collections.graph.*;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;
import org.mapleir.stdlib.collections.graph.algorithms.TarjanSCC;
import org.mapleir.stdlib.collections.list.IndexedList;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SkidFlowGraphDumper implements BytecodeFrontend {
	private final Skidfuscator skidfuscator;
	private final ControlFlowGraph cfg;
	private final MethodNode m;
	private IndexedList<BasicBlock> order;
	private LabelNode terminalLabel; // synthetic last label for malformed ranges
	private Map<BasicBlock, LabelNode> labels;

	/* Frame specific stuff */
	private Map<Integer, Set<BasicBlock>> scopeMap = new HashMap<>();
	private Map<Integer, Boolean> scopeSetMap = new HashMap<>();
	private Map<BasicBlock, Type[]> localAccessors = new HashMap<>();
	private Map<BasicBlock, Type[]> localProviders = new HashMap<>();
	private Map<BasicBlock, Type[]> localFrames = new HashMap<>();

	private int beginIndex;

	public static boolean TEST_COMPUTE = false;

	public SkidFlowGraphDumper(Skidfuscator skidfuscator, ControlFlowGraph cfg, MethodNode m) {
		this.skidfuscator = skidfuscator;
		this.cfg = cfg;
		this.m = m;
	}

	public void dump() {
		// Clear methodnode
		m.node.instructions.clear();
		m.node.tryCatchBlocks.clear();
		m.node.visitCode();

		labels = new HashMap<>();
		for (BasicBlock b : cfg.vertices()) {
			labels.put(b, new LabelNode());
		}

		// Fix types
		//fixTypes();

		// Fix ranges
		fixRanges();

		// Linearize
		linearize();

		// Fix edges
		naturalise();

		// Sanity check linearization
		verifyOrdering();

		// Compute frames
		//computeFrames();
		if (TEST_COMPUTE) {
			new FrameComputer(skidfuscator).compute(cfg);
		}

		// Stuff
		/*
		 * If the method is not static, then the first type of the frame
		 * will always be the instance. If the instance is not... instantiated,
		 * then we can safely set it to "uninitialized this". Else, we set it
		 * to the internal name.
		 */
		final Parameter parameter = new Parameter(cfg.getDesc());
		final int parameterSize = parameter.getArgs().size();
		final boolean isStatic = cfg.getMethodNode().isStatic();
		final Object[] initialFrame = new Object[(isStatic ? 0 : 1) + parameter.computeSize()];

		int paramCount = 0;
		int index = 0;
		if (!isStatic) {
			initialFrame[index] = cfg.getMethodNode().isInit()
					? Opcodes.UNINITIALIZED_THIS
					: cfg.getMethodNode().getOwner();
			index++;
			paramCount++;
		}

		/* Method parameters ezzzz */
		for (int i = 0; i < parameter.getArgs().size(); i++) {
			Type type = parameter.getArg(i);
			initialFrame[index] = _getFrameType(type);
			index++;

			if (type.equals(Type.DOUBLE_TYPE) || type.equals(Type.LONG_TYPE)) {
				initialFrame[index] = _getFrameType(Type.VOID_TYPE);
				index++;
			}

			paramCount++;
		}


		Object[] lastFrame = initialFrame;
		Object[] lastStack = null;

		int maxLocal = 0;
		int maxStack = 0;

		// Dump code
		BasicBlock last = null;
		for (BasicBlock b : order) {
			m.node.visitLabel(getLabel(b));

			iter: {
				if (b.isEmpty() || cfg.getJumpReverseEdges(b).isEmpty() || !TEST_COMPUTE)
					break iter;

				final SkidExpressionPool frameTypes = (SkidExpressionPool) b.getPool();
				final Set<FlowEdge<BasicBlock>> predecessors = cfg.getReverseEdges(b);

				boolean fuckingThing = false;

				if (last != null) {
					for (FlowEdge<BasicBlock> predecessor : predecessors) {
						if (predecessor instanceof ImmediateEdge || predecessor instanceof UnconditionalJumpEdge) {
							if (!last.equals(predecessor.src())) {
								fuckingThing = true;
								break;
							}
						}

						fuckingThing = true;
						break;
					}

					if (!fuckingThing) {
						break iter;
					}
				}

				int eeee = isStatic ? 0 : 1 + parameterSize;
				final int frameComputedSize = Math.max(frameTypes.computeSize(), eeee);

				/* Implicit frame */
				/*if (initialFrame.length >= frameComputedSize)
					break iter;*/

				Object[] frameLocal = Arrays.copyOf(
						initialFrame,
						frameComputedSize
				);

				final Stack<Object> params = new Stack<>();
				if (!isStatic) {
					final Object instanze = cfg.getMethodNode().isInit() && b.isFlagSet(SkidBlock.FLAG_NO_OPAQUE)
							? Opcodes.UNINITIALIZED_THIS
							: cfg.getMethodNode().getOwner();

					frameLocal[0] = instanze;
					params.add(instanze);
				}

				/* Whacky first start digit to exempt the first precondition above */
				int paramCountBlock = isStatic ? 0 : 1;
				for (int i = isStatic ? 0 : 1;
					 i < frameComputedSize;
					 i++) {

					final Type type = frameTypes.get(i);
					/*final Type dominatorType = domc.getImmediateDominator(b).getPool().get(i);

					if (predecessors.size() == 1 && !dominatorType.equals(type)) {
						System.out.println("Dominator type overrides: " + type + " vs " + dominatorType);
						break iter;
					}*/

					final Object computed = _getFrameType(type);

					assert computed != null : "How the fuck is the object null: " + type;

					params.add(computed);
					frameLocal[paramCountBlock] = computed;
					paramCountBlock++;

					// Skip next type
					if (type.equals(Type.DOUBLE_TYPE) || type.equals(Type.LONG_TYPE)) {
						i++;
						frameLocal[i] = null;
					}
				}

				Object[] frame = new Object[params.size()];
				int indexLocal2 = 0;

				for (final Object object : params) {
					assert object != null : "Stack has null obj: " + Arrays.toString(params.toArray());

					frame[indexLocal2] = object;
					indexLocal2++;
				}

				//System.out.println("LOCAL: " + paramCount + " new: " + paramCountBlock);
				//System.out.println("INIT: " + Arrays.toString(initialFrame));
				//System.out.println("EGGGGF: " + Arrays.toString(frameLocal));
				//System.out.println("EGGGGA: " + Arrays.toString(frameTypes.getTypes()));
				//System.out.println("EGGGGG: " + Arrays.toString(frame));

				/* Stack */
				final int stackLength = b.getStack().capacity();
				final SkidTypeStack typeStack = (SkidTypeStack) b.getStack();
				final Type[] stackTypes = b.getStack().getStack();
				final Object[] stack = new Object[typeStack.size()];

				for (int i = 0; i < stack.length; i++) {
					final Type type = stackTypes[i];
					stack[i] = _getFrameType(type);
				}

				if (Arrays.equals(frameLocal, lastFrame)) {
					if (stack.length == 1) {
						m.node.visitFrame(Opcodes.F_SAME1, 0, null, 1, stack);
					} else {
						m.node.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
					}
				} else {
					final int diff = lastFrame == null ? 0 : lastFrame.length - frameLocal.length;
					if (Math.abs(diff) < 4 && Math.abs(diff) > 0 && false) {

					} else {
						m.node.visitFrame(Opcodes.F_FULL, frame.length, frame, stack.length, stack);
						//m.node.visitInsn(Opcodes.NOP);
					}
				}

				maxLocal = Math.max(maxLocal, frameLocal.length);

				lastFrame = frameLocal;
				lastStack = stack;

			}

			for (Stmt stmt : b) {
				stmt.toCode(m.node, this);
			}

			last = b;
		}
		terminalLabel = new LabelNode();
		m.node.visitLabel(terminalLabel.getLabel());

		// Dump ranges
		for (ExceptionRange<BasicBlock> er : cfg.getRanges()) {
			dumpRange(er);
		}
		
		// Sanity check
		verifyRanges();
		
		m.node.visitEnd();

		//Verifier.verify(m.node);
	}

	private Object _getFrameType(final Type type) {
		final boolean isInteger = type == Type.BOOLEAN_TYPE
				|| type == Type.SHORT_TYPE
				|| type == Type.BYTE_TYPE
				|| type == Type.INT_TYPE;

		final Object frameType;

		if (isInteger) {
			frameType = Opcodes.INTEGER;
		} else if (type == Type.FLOAT_TYPE) {
			frameType = Opcodes.FLOAT;
		} else if (type == Type.DOUBLE_TYPE) {
			frameType = Opcodes.DOUBLE;
		} else if (type == Type.LONG_TYPE) {
			frameType = Opcodes.LONG;
		} else if (type == Type.VOID_TYPE || type == TypeUtil.UNDEFINED_TYPE) {
			frameType = Opcodes.TOP;
		} else if (type == TypeUtil.NULL_TYPE) {
			frameType = Opcodes.NULL;
		} else if (type.getSort() == Type.ARRAY) {
			frameType = type.getDescriptor();
		} else if (type == TypeUtil.UNINITIALIZED_THIS) {
			frameType = Opcodes.UNINITIALIZED_THIS;
		} else {
			frameType = type.getInternalName();
		}

		return frameType;
	}

	private Object _getArrayType(final StringBuilder builder, final Type array) {
		final Type subType = array.getElementType();

		switch (subType.getSort()) {
			case Type.ARRAY: {
				builder.append("[");
				return _getArrayType(builder, array);
			}
			case Type.OBJECT: {
				builder.append("L").append(subType.getInternalName()).append(";");
				return builder.toString();
			}
			default:
				return subType.getInternalName();
		}
	}

	@Deprecated
	private void computeFramesNew() {
		if (cfg.getEntries().size() != 1)
			throw new IllegalStateException("CFG doesn't have exactly 1 entry");
		BasicBlock entry = cfg.getEntries().iterator().next();

		Map<BasicBlock, Frame> frameMap = new HashMap<>();

		for (BasicBlock vertex : cfg.vertices()) {
			final Frame frame = new Frame(
					skidfuscator,
					vertex
			);

			frameMap.put(vertex, frame);
		}

		final Set<BasicBlock> visited = new HashSet<>();
		final Stack<BasicBlock> bucket = new Stack<>();

		/*
		 * FRAME LOCALS
		 */
		bucket.add(entry);

		final Frame entryFrame = frameMap.get(entry);
		Arrays.fill(entryFrame.getInputTypes(), Type.VOID_TYPE);

		int index = 0;
		int protectedIndex = 0;
		if (!cfg.getMethodNode().isStatic()) {
			entryFrame.setInput(index, Type.getType("L" + cfg.getMethodNode().owner.getName() + ";"));
			protectedIndex = index;
			index++;
		}

		/* Method parameters ezzzz */
		final Parameter parameter = new Parameter(cfg.getDesc());
		for (int i = 0; i < parameter.getArgs().size(); i++) {
			Type type = parameter.getArg(i);

			if (type == Type.BOOLEAN_TYPE
					|| type == Type.BYTE_TYPE
					|| type == Type.CHAR_TYPE
					|| type == Type.SHORT_TYPE) {
				type = Type.INT_TYPE;
			}

			entryFrame.setInput(index, type);
			protectedIndex = index;
			index++;
		}

		final Type[] staticTypes = new Type[index];

		for (int i = 0; i < staticTypes.length; i++) {
			staticTypes[i] = entryFrame.getInputTypes()[i];
		}

		for (Frame value : frameMap.values()) {
			value.setStaticFrame(staticTypes);
		}

		beginIndex = index;

		// TODO:
		while (!bucket.isEmpty()) {
			/* Visit the top of the stack */
			final BasicBlock popped = bucket.pop();
			final Frame poppedFrame = frameMap.get(popped);
			visited.add(popped);

			final Set<BasicBlock> next = new HashSet<>();
			for (Stmt stmt : popped) {
				final Map<BasicBlock, Stmt> vars = new HashMap<>();

				if (stmt instanceof SwitchStmt) {
					final SwitchStmt switchStmt = (SwitchStmt) stmt;

					for (BasicBlock value : switchStmt.getTargets().values()) {
						vars.put(value, stmt);
					}

					vars.put(switchStmt.getDefaultTarget(), switchStmt);
				} else if (stmt instanceof ConditionalJumpStmt) {
					final ConditionalJumpStmt conditionalJumpStmt = (ConditionalJumpStmt) stmt;
					vars.put(conditionalJumpStmt.getTrueSuccessor(), stmt);
				} else if (stmt instanceof UnconditionalJumpStmt) {
					final UnconditionalJumpStmt unconditionalJumpStmt = (UnconditionalJumpStmt) stmt;
					vars.put(unconditionalJumpStmt.getTarget(), stmt);
				}

				next.addAll(vars.keySet());

				vars.forEach((target, _stmt) -> {
					final Frame targetFrame = frameMap.get(target);
					targetFrame.addParent(poppedFrame, _stmt);
				});
			}

			int finalProtectedIndex1 = protectedIndex;
			cfg.getSuccessors(new Predicate<FlowEdge<BasicBlock>>() {
				@Override
				public boolean test(FlowEdge<BasicBlock> basicBlockFlowEdge) {
					return basicBlockFlowEdge instanceof TryCatchEdge
							&& ((TryCatchEdge) basicBlockFlowEdge)
							.erange
							.getNodes()
							.contains(popped);
				}
			}, popped).forEach(e -> {
				final BasicBlock value = e.dst();
				final Frame targetFrame = frameMap.get(value);
				targetFrame.addParent(poppedFrame, new SkidBogusStmt(SkidBogusStmt.BogusType.EXCEPTION));

				next.add(value);
			});

			final BasicBlock value = cfg.getImmediate(popped);
			if (value != null) {
				final Frame targetFrame = frameMap.get(value);
				targetFrame.addParent(poppedFrame, new SkidBogusStmt(SkidBogusStmt.BogusType.IMMEDIATE));

				next.add(value);
			}

			cfg.getEdges(popped)
					.stream()
					.filter(e -> !next.contains(e.dst()))
					.forEach(e -> {
						System.out.println("Failed " + e + " (Despite "
								+ Arrays.toString(next.stream().map(BasicBlock::getDisplayName).toArray())
								+ ")"
						);
					});
			//System.out.println("-----");

			/* Add all the successor nodes */
			/* Add it to the stack to be iterated again */
			next.stream()
					.filter(e -> !visited.contains(e))
					.forEach(bucket::add);
		}

		for (BasicBlock vertex : cfg.vertices()) {
			if (!visited.contains(vertex)) {
				System.err.println("Missed " + vertex.getDisplayName() + " on method " + cfg.getMethodNode().getOwner() + "#" + cfg.getMethodNode().getDisplayName());
			}

			final Frame vertexFrame = frameMap.get(vertex);
			vertexFrame.preprocess();
			vertexFrame.hackyMess();
		}

		final Stack<Frame> frameStack = new Stack<>();
		final Set<Frame> visitedFrames = new HashSet<>();

		/*
		 * Iterate by the roots of the tree then
		 * go up manually.
		 */
		frameMap.values()
				.stream()
				.filter(Frame::isTerminating)
				.forEach(frameStack::add);

		while (!frameStack.isEmpty()) {
			final Frame frame = frameStack.pop();
			visitedFrames.add(frame);

			for (Integer use : frame.getUsesNoDefined()) {
				frame.getParents()
						.stream()
						.filter(e -> !visitedFrames.contains(e))
						.forEach(frameStack::add);
			}
		}

		// TODO here: transmission like a disease

		for (BasicBlock vertex : cfg.vertices()) {
			final Frame vertexFrame = frameMap.get(vertex);
			vertexFrame.compute();

			System.out.println(vertexFrame.toString());

			SkidExpressionPool expressionPool = new SkidExpressionPool(vertexFrame.getFrame(), skidfuscator);
			vertex.setPool(expressionPool);
		}

		/*
		 * FRAME STACK
		 */

		for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
			final TypeStack expressionStack = new SkidTypeStack(
					cfg.getLocals().getMaxStack(),
					skidfuscator
			);
			final Type type = getRangeType(range);

			expressionStack.push(type);
			range.getHandler().setStack(expressionStack);

			visited.clear();
			bucket.add(range.getHandler());

			while (!bucket.isEmpty()) {
				final BasicBlock popped = bucket.pop();
				visited.add(popped);

				if (popped.stream()
						.flatMap(e -> Streams.stream(e.enumerateOnlyChildren()))
						.filter(CaughtExceptionExpr.class::isInstance)
						.map(CaughtExceptionExpr.class::cast)
						.anyMatch(e -> e.getType().equals(type))) {
					break;
				}

				cfg.getSuccessors(popped)
						.filter(e -> !visited.contains(e))
						.forEach(e -> {
							e.setStack(popped.getStack().copy());
							bucket.add(e);
						});
			}
		}

		for (BasicBlock vertex : cfg.vertices()) {
			if (vertex.getStack() == null) {
				vertex.setStack(
						new SkidTypeStack(
								cfg.getLocals().getMaxStack(),
								skidfuscator
						)
				);
			}
		}
		/*visited.clear();
		bucket.add(entry);

		ExpressionStack stack = new ExpressionStack(cfg.getLocals().getMaxStack() + 1);

		entry.setStack(stack);

		while (!bucket.isEmpty()) {
			/* Visit the top of the stack *//*
			final BasicBlock popped = bucket.pop();
			visited.add(popped);

			/* Iterate all the set statements to update the frame *//*
			final ExpressionStack expressionPool = popped.getStack().copy();

			AtomicReference<ExceptionRange<BasicBlock>> range = null;
			iteration: {
				for (Stmt stmt : popped) {
					for (Expr expr : stmt.enumerateOnlyChildren()) {
						if (expr instanceof CaughtExceptionExpr) {
							expressionPool.pop();
						}
						if (stmt instanceof ThrowStmt) {
							final ThrowStmt throwStmt = (ThrowStmt) stmt;
							expressionPool.push(throwStmt.getExpression());


							cfg.getEdges(popped)
									.stream()
									.filter(TryCatchEdge.class::isInstance)
									.map(TryCatchEdge.class::cast)
									.findFirst()
									.ifPresent(e -> range.set(e.erange));

							if (range == null) {
								break iteration;
							}


							range.get().getHandler().setStack(expressionPool);

						}
					}
				}

				/* Add all the successor nodes *//*
				cfg.getSuccessors(popped)
						.filter(e -> !visited.contains(e))
						.forEach(e -> {
							/* Set the expected received pool
							e.setPool(expressionPool);

							/* Add it to the stack to be iterated again
							bucket.add(e);
						});
			}
		}*/
	}

	@Deprecated
	private void computeFrames() {
		if (cfg.getEntries().size() != 1)
			throw new IllegalStateException("CFG doesn't have exactly 1 entry");
		BasicBlock entry = cfg.getEntries().iterator().next();

		// Bundle based iteration
		final Map<BasicBlock, ConstantExpr> exprMap = new HashMap<>();
		for (BasicBlock vertex : cfg.vertices()) {
			/*if (vertex.getPool() == null) {
				System.out.println("Frame >>> FAILED TO COMPUTE");
			} else {
				System.out.println("Frame >>> " + Arrays.toString(vertex.getPool().getRenderedTypes()));
			}
			System.out.println(CFGUtils.printBlock(vertex));*/
			//final Local local1 = cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2);
			//final ConstantExpr expr = new SkidConstantExpr("E", TypeUtil.STRING_TYPE);
			//exprMap.put(vertex, expr);
			/*vertex.add(
					0,
					new SkidCopyVarStmt(
							new VarExpr(local1, Type.getType(String.class)),
							expr
					)
			);*/
		}

		final Set<BasicBlock> visited = new HashSet<>();
		final Stack<BasicBlock> bucket = new Stack<>();

		/*
		 * FRAME LOCALS
		 */
		bucket.add(entry);
		SkidExpressionPool frame = new SkidExpressionPool(
				new Type[cfg.getLocals().getMaxLocals() + 2],
				skidfuscator
		);
		Arrays.fill(frame.getTypes(), Type.VOID_TYPE);

		int index = 0;
		int protectedIndex = 0;
		if (!cfg.getMethodNode().isStatic()) {
			frame.set(index, Type.getType("L" + cfg.getMethodNode().owner.getName() + ";"));
			protectedIndex = index;
			index++;
		}

		/* Method parameters ezzzz */
		final Parameter parameter = new Parameter(cfg.getDesc());
		for (int i = 0; i < parameter.getArgs().size(); i++) {
			Type type = parameter.getArg(i);

			if (type == Type.BOOLEAN_TYPE
					|| type == Type.BYTE_TYPE
					|| type == Type.CHAR_TYPE
					|| type == Type.SHORT_TYPE) {
				type = Type.INT_TYPE;
			}

			frame.set(index, type);
			protectedIndex = index;
			index++;
		}

		beginIndex = index;

		entry.setPool(frame);

		Map<BasicBlock, Set<Integer>> defMap = new HashMap<>();
		Map<Integer, Set<BasicBlock>> reverseDefMap = new HashMap<>();

		final int finalProtectedIndex = protectedIndex;

		cfg.allExprStream()
				.filter(VarExpr.class::isInstance)
				.map(VarExpr.class::cast)
				.filter(e -> e.getLocal().isStoredInLocal())
				//.filter(e -> e.getIndex() > finalProtectedIndex)
				.forEach(e -> {
					defMap.computeIfAbsent(e.getBlock(), b -> new HashSet<>())
							.add(e.getIndex());
					reverseDefMap.computeIfAbsent(e.getIndex(), b -> new HashSet<>())
							.add(e.getBlock());

					localAccessors.computeIfAbsent(e.getBlock(), b -> new Type[frame.size()])
							[e.getIndex()] = e.getType();
				});

		cfg.vertices()
				.stream()
				.flatMap(BasicBlock::stream)
				.filter(CopyVarStmt.class::isInstance)
				.map(CopyVarStmt.class::cast)
				.map(CopyVarStmt::getVariable)
				.filter(e -> e.getLocal().isStoredInLocal())
				//.filter(e -> e.getIndex() > finalProtectedIndex)
				.forEach(e -> {
					defMap.computeIfAbsent(e.getBlock(), b -> new HashSet<>())
							.add(e.getIndex());
					reverseDefMap.computeIfAbsent(e.getIndex(), b -> new HashSet<>())
							.add(e.getBlock());

					localProviders.computeIfAbsent(e.getBlock(), b -> new Type[frame.size()])
							[e.getIndex()] = e.getType();
				});


		// TODO:
		while (!bucket.isEmpty()) {
			/* Visit the top of the stack */
			final BasicBlock popped = bucket.pop();
			visited.add(popped);

			scopeMap.forEach((key, set) -> {
				if (!scopeSetMap.get(key))
					return;

				set.add(popped);
			});

			/* Iterate all the set statements to update the frame */
			final SkidExpressionPool expressionPool = new SkidExpressionPool(
					popped.getPool(),
					skidfuscator
			);

			final SkidExpressionPool clone = expressionPool.copy();
			final Set<Integer> uses = defMap.computeIfAbsent(popped, c -> new HashSet<>());
			for (Integer use : uses) {
				if (!reverseDefMap.containsKey(use)) {
					continue;
				}

				if (!scopeSetMap.containsKey(use)) {
					scopeMap.put(use, new HashSet<>(Collections.singleton(popped)));
					scopeSetMap.put(use, true);
				}

				final Set<BasicBlock> targets = reverseDefMap.get(use);
				if (targets.contains(popped) && targets.size() == 1) {
					expressionPool.set(index, Type.VOID_TYPE);

					scopeSetMap.put(use, false);
				}
			}

			final int maxDefHeight = clone.computeSize();
			for (Integer use : uses) {
				if (!reverseDefMap.containsKey(use)) {
					continue;
				}

				final Set<BasicBlock> targets = reverseDefMap.get(use);
				targets.remove(popped);

				if (targets.isEmpty() && maxDefHeight < use) {
					expressionPool.set(index, Type.VOID_TYPE);
				}
			}


			final Set<BasicBlock> next = new HashSet<>();
			for (Stmt stmt : popped) {
				if (stmt instanceof CopyVarStmt) {
					final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;

					if(copyVarStmt.getExpression() instanceof VarExpr) {
						if(((VarExpr) copyVarStmt.getExpression()).getLocal()
								== copyVarStmt.getVariable().getLocal()) {
							continue;
						}
					}

					if (copyVarStmt.getType().equals(TypeUtil.OBJECT_TYPE)
							&& cfg.getMethodNode().getDisplayName().equals("decrypt")
							&& cfg.getMethodNode().getOwner().equals("dev/sim0n/evaluator/util/crypto/Blowfish$BlowfishCBC")) {
						System.out.println("Debugging " + copyVarStmt);
					}

					Type type = copyVarStmt.getType();

					if (type == Type.BOOLEAN_TYPE
							|| type == Type.BYTE_TYPE
							|| type == Type.CHAR_TYPE
							|| type == Type.SHORT_TYPE) {
						type = Type.INT_TYPE;
					}

					expressionPool.set(copyVarStmt.getIndex(), type);
				} else {
					final Set<BasicBlock> vars = new HashSet<>();

					if (stmt instanceof SwitchStmt) {
						final SwitchStmt switchStmt = (SwitchStmt) stmt;
						vars.addAll(switchStmt.getTargets().values());
						vars.add(switchStmt.getDefaultTarget());
					} else if (stmt instanceof ConditionalJumpStmt) {
						final ConditionalJumpStmt conditionalJumpStmt = (ConditionalJumpStmt) stmt;
						vars.add(conditionalJumpStmt.getTrueSuccessor());
					} else if (stmt instanceof UnconditionalJumpStmt) {
						final UnconditionalJumpStmt unconditionalJumpStmt = (UnconditionalJumpStmt) stmt;
						vars.add(unconditionalJumpStmt.getTarget());
					}

					next.addAll(vars);

					for (BasicBlock value : vars) {
						final SkidExpressionPool pool = new SkidExpressionPool(expressionPool, skidfuscator);

						/* Merging pool values if it has previously been accessed */
						if (value.getPool() != null) {
							//value.getPool().addParent(expressionPool);
							pool.addParent(value.getPool());
							final Type[] otherTypes = value.getPool().getTypes();
							final Type[] selfTypes = expressionPool.getTypes();

							for (int i = 0; i < pool.size(); i++) {
								final Type otherType = value.getPool().get(i);
								final Type selfType = expressionPool.get(i);

								if (!otherType.equals(selfType)) {
									/*
									 * It should be physically impossible for
									 * parameters in the method to be reassigned
									 * a type.
									 */
									if (i <= protectedIndex) {
										throw new IllegalStateException(
												"Tried to override protected index type at " + i + " \n"
														+ cfg.getMethodNode().getOwner()
														+ "#"
														+ cfg.getMethodNode().getDisplayName()
														+ "\n"
														+ " (static: " + cfg.getMethodNode().isStatic()
														+ " desc: " + cfg.getDesc() + ")\n" +
												"\nPrevious: " + Arrays.toString(value.getPool().getTypes()) +
												"\nCurrent: " + Arrays.toString(expressionPool.getTypes())
										);
									}

									/*
									 * Both pools use this same index but compute
									 * different values. This means that it is
									 * __most likely__ a local value which is
									 * forgotten about and hence not used anymore.
									 */
									if (selfType.equals(Type.VOID_TYPE)) {
										pool.set(i, otherType);
									} else if (otherType.equals(Type.VOID_TYPE)) {
										pool.set(i, selfType);
									} else {
										throw new IllegalStateException(
												"Failed to compute frame: " +
														"\nPrevious: " + otherType +
														"\nCurrent: " + selfType
										);
										//pool.set(i, Type.VOID_TYPE);
									}

									if (cfg.getMethodNode().getDisplayName().equals("decrypt")
											&& cfg.getMethodNode().getOwner().equals("dev/sim0n/evaluator/util/crypto/Blowfish$BlowfishCBC")) {
										System.out.println(
												"Overriding debug index type at " + i +
												"\nPrevious: " + Arrays.toString(otherTypes) +
												"\nCurrent: " + Arrays.toString(selfTypes)
										);
									}

									/*throw new IllegalStateException(
											"Failed to compute frame: " +
											"\nPrevious: " + Arrays.toString(value.getPool().getRenderedTypes()) +
											"\nCurrent: " + Arrays.toString(expressionPool.getRenderedTypes())
									);*/
								}
							}

							final int maxHeight = Math.min(
									expressionPool.computeSize(),
									value.getPool().computeSize()
							);

							for (int i = maxHeight; i < pool.size(); i++) {
								pool.set(i, Type.VOID_TYPE);
							}
							//value.getPool().merge(expressionPool);
						}

						value.setPool(pool);
					}
				}
			}

			int finalProtectedIndex1 = protectedIndex;
			cfg.getSuccessors(new Predicate<FlowEdge<BasicBlock>>() {
				@Override
				public boolean test(FlowEdge<BasicBlock> basicBlockFlowEdge) {
					return basicBlockFlowEdge instanceof TryCatchEdge
							&& ((TryCatchEdge) basicBlockFlowEdge)
							.erange
							.getNodes()
							.contains(popped);
				}
			}, popped).forEach(e -> {
				final BasicBlock value = e.dst();
				final SkidExpressionPool pool = new SkidExpressionPool(expressionPool, skidfuscator);

				/* Merging pool values if it has previously been accessed */
				if (value.getPool() != null) {
					value.getPool().addParent(expressionPool);
					final Type[] otherTypes = value.getPool().getTypes();
					final Type[] selfTypes = expressionPool.getTypes();

					for (int i = 0; i < pool.size(); i++) {
						final Type otherType = value.getPool().get(i);
						final Type selfType = expressionPool.get(i);
						if (!otherType.equals(selfType)) {
							/*
							 * It should be physically impossible for
							 * parameters in the method to be reassigned
							 * a type.
							 */
							if (i <= finalProtectedIndex1) {
								throw new IllegalStateException(
										"Tried to override protected index type at " + i + " \n"
												+ cfg.getMethodNode().getOwner()
												+ "#"
												+ cfg.getMethodNode().getDisplayName()
												+ "\n"
												+ " (static: " + cfg.getMethodNode().isStatic()
												+ " desc: " + cfg.getDesc() + ")\n" +
												"\nPrevious: " + Arrays.toString(value.getPool().getTypes()) +
												"\nCurrent: " + Arrays.toString(expressionPool.getTypes())
								);
							}

							/*
							 * Both pools use this same index but compute
							 * different values. This means that it is
							 * __most likely__ a local value which is
							 * forgotten about and hence not used anymore.
							 */
							if (selfType.equals(Type.VOID_TYPE)) {
								pool.set(i, Type.VOID_TYPE);
							} else if (otherType.equals(Type.VOID_TYPE)) {
								pool.set(i, Type.VOID_TYPE);
							} else {
								throw new IllegalStateException(
										"Failed to compute frame: " +
												"\nPrevious: " + otherType +
												"\nCurrent: " + selfType
								);
								//pool.set(i, Type.VOID_TYPE);
							}

							if (cfg.getMethodNode().getDisplayName().equals("decrypt")
									&& cfg.getMethodNode().getOwner().equals("dev/sim0n/evaluator/util/crypto/Blowfish$BlowfishCBC")) {
								System.out.println(
										"Overriding debug index type at " + i +
												"\nPrevious: " + Arrays.toString(otherTypes) +
												"\nCurrent: " + Arrays.toString(selfTypes)
								);
							}

									/*throw new IllegalStateException(
											"Failed to compute frame: " +
											"\nPrevious: " + Arrays.toString(value.getPool().getRenderedTypes()) +
											"\nCurrent: " + Arrays.toString(expressionPool.getRenderedTypes())
									);*/
						}
					}

					final int maxHeight = Math.min(
							expressionPool.computeSize(),
							((SkidExpressionPool) value.getPool()).computeSize()
					);

					for (int i = maxHeight; i < pool.size(); i++) {
						pool.set(i, Type.VOID_TYPE);
					}
					//value.getPool().merge(expressionPool);
				}

				value.setPool(pool);

				next.add(value);
			});

			final BasicBlock value = cfg.getImmediate(popped);
			if (value != null) {
				final SkidExpressionPool pool = new SkidExpressionPool(expressionPool, skidfuscator);

				/* Merging pool values if it has previously been accessed */
				if (value.getPool() != null) {
					value.getPool().addParent(expressionPool);
					final Type[] otherTypes = value.getPool().getTypes();
					final Type[] selfTypes = expressionPool.getTypes();

					for (int i = 0; i < pool.size(); i++) {
						final Type otherType = value.getPool().get(i);
						final Type selfType = expressionPool.get(i);
						if (!otherType.equals(selfType)) {
							/*
							 * It should be physically impossible for
							 * parameters in the method to be reassigned
							 * a type.
							 */
							if (i <= protectedIndex) {
								throw new IllegalStateException(
										"Tried to override protected index type at " + i + " \n"
												+ cfg.getMethodNode().getOwner()
												+ "#"
												+ cfg.getMethodNode().getDisplayName()
												+ "\n"
												+ " (static: " + cfg.getMethodNode().isStatic()
												+ " desc: " + cfg.getDesc() + ")\n" +
												"\nPrevious: " + Arrays.toString(value.getPool().getTypes()) +
												"\nCurrent: " + Arrays.toString(expressionPool.getTypes())
								);
							}

							/*
							 * Both pools use this same index but compute
							 * different values. This means that it is
							 * __most likely__ a local value which is
							 * forgotten about and hence not used anymore.
							 */
							if (selfType.equals(Type.VOID_TYPE)) {
								pool.set(i, Type.VOID_TYPE);
							} else if (otherType.equals(Type.VOID_TYPE)) {
								pool.set(i, Type.VOID_TYPE);
							} else {
								throw new IllegalStateException(
										"Failed to compute frame: " +
												"\nPrevious: " + otherType +
												"\nCurrent: " + selfType
								);
								//pool.set(i, Type.VOID_TYPE);
							}

							if (cfg.getMethodNode().getDisplayName().equals("decrypt")
									&& cfg.getMethodNode().getOwner().equals("dev/sim0n/evaluator/util/crypto/Blowfish$BlowfishCBC")) {
								System.out.println(
										"Overriding debug index type at " + i +
												"\nPrevious: " + Arrays.toString(otherTypes) +
												"\nCurrent: " + Arrays.toString(selfTypes)
								);
							}

									/*throw new IllegalStateException(
											"Failed to compute frame: " +
											"\nPrevious: " + Arrays.toString(value.getPool().getRenderedTypes()) +
											"\nCurrent: " + Arrays.toString(expressionPool.getRenderedTypes())
									);*/
						}
					}

					final int maxHeight = Math.min(
							expressionPool.computeSize(),
							((SkidExpressionPool) value.getPool()).computeSize()
					);

					for (int i = maxHeight; i < pool.size(); i++) {
						pool.set(i, Type.VOID_TYPE);
					}
					//value.getPool().merge(expressionPool);
				}

				value.setPool(pool);
				next.add(value);
			}

			cfg.getEdges(popped)
					.stream()
					.filter(e -> !next.contains(e.dst()))
					.forEach(e -> {
						System.out.println("Failed " + e + " (Despite "
										+ Arrays.toString(next.stream().map(BasicBlock::getDisplayName).toArray())
										+ ")"
						);
					});
			//System.out.println("-----");

			/* Add all the successor nodes */
			/* Add it to the stack to be iterated again */
			next.stream()
					.filter(e -> !visited.contains(e))
					.forEach(bucket::add);
		}

		for (BasicBlock vertex : cfg.vertices()) {
			if (!visited.contains(vertex)) {
				System.err.println("Missed " + vertex.getDisplayName() + " on method " + cfg.getMethodNode().getOwner() + "#" + cfg.getMethodNode().getDisplayName());
			}
		}

		cfg.allExprStream()
				.filter(VarExpr.class::isInstance)
				.map(VarExpr.class::cast)
				.filter(e -> e.getLocal().isStoredInLocal())
				.forEach(e -> {
					Type localType = e.getType();

					if (localType == Type.BOOLEAN_TYPE
							|| localType == Type.BYTE_TYPE
							|| localType == Type.CHAR_TYPE
							|| localType == Type.SHORT_TYPE) {
						localType = Type.INT_TYPE;
					}

					final BasicBlock block = e.getBlock();

					if (block.getPool().get(e.getIndex()).equals(TypeUtil.OBJECT_TYPE))
						block.getPool().set(e.getIndex(), localType);
					/*final Type frameType = e.getBlock().getPool().get(e.getIndex());

					if (!localType.equals(frameType)) {
						final List<CopyVarStmt> stmts = cfg.allExprStream()
								.filter(CopyVarStmt.class::isInstance)
								.map(CopyVarStmt.class::cast)
								.filter(c -> c.getIndex() == e.getIndex())
								.collect(Collectors.toList());
						throw new IllegalStateException(
								"Failed to match frame type on method "
										+ cfg.getMethodNode().getOwner()
										+ "#"
										+ cfg.getMethodNode().getDisplayName()
										+ "\n"
											+ " (index: " + e.getIndex()
											+ " static: " + cfg.getMethodNode().isStatic()
											+ " desc: " + cfg.getDesc() + ")"
										+ "\n Expected: " + localType
										+ "\n Got: " + frameType
										+ "\n "
								        + "\n Definitions: \n" + stmts
														.stream()
														.map(c -> "CopyVarStmt{index=" + c.getIndex()
																+ " block=" + c.getBlock().getDisplayName()
																+ " type=" + c.getType().getInternalName()
																+ " expr=" + c.getExpression().toString()
																+ " frame=" + cfg.getSuccessors(c.getBlock()).map(s -> s.getPool().get(e.getIndex()).toString()).collect(Collectors.joining(","))
																+ "}")
														.collect(Collectors.joining("\n--> "))
										+ "\n Usage: \n"
										+ e.getRootParent().toString()
										+ "\n Scope: \n"
										+ cfg.vertices()
											.stream()
											.map(c -> c.getDisplayName() + " --> " + c.getPool().get(e.getIndex()).toString())
											.collect(Collectors.joining("\n"))
										+ "\n Edges: \n"
										+ cfg.makeDotGraph().toString()
										+ "\n--------\n"
						);
					}*/
				});

		/*
		 * FRAME STACK
		 */

		for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
			final TypeStack expressionStack = new SkidTypeStack(
					cfg.getLocals().getMaxStack(),
					skidfuscator
			);
			final Type type = getRangeType(range);

			expressionStack.push(type);
			range.getHandler().setStack(expressionStack);

			visited.clear();
			bucket.add(range.getHandler());

			while (!bucket.isEmpty()) {
				final BasicBlock popped = bucket.pop();
				visited.add(popped);

				if (popped.stream()
						.flatMap(e -> Streams.stream(e.enumerateOnlyChildren()))
						.filter(CaughtExceptionExpr.class::isInstance)
						.map(CaughtExceptionExpr.class::cast)
						.anyMatch(e -> e.getType().equals(type))) {
					break;
				}

				cfg.getSuccessors(popped)
						.filter(e -> !visited.contains(e))
						.forEach(e -> {
							e.setStack(popped.getStack().copy());
							bucket.add(e);
						});
			}
		}

		for (BasicBlock vertex : cfg.vertices()) {
			if (vertex.getStack() == null) {
				vertex.setStack(
						new SkidTypeStack(
							cfg.getLocals().getMaxStack(),
							skidfuscator
						)
				);
			}
		}
		/*visited.clear();
		bucket.add(entry);

		ExpressionStack stack = new ExpressionStack(cfg.getLocals().getMaxStack() + 1);

		entry.setStack(stack);

		while (!bucket.isEmpty()) {
			/* Visit the top of the stack *//*
			final BasicBlock popped = bucket.pop();
			visited.add(popped);

			/* Iterate all the set statements to update the frame *//*
			final ExpressionStack expressionPool = popped.getStack().copy();

			AtomicReference<ExceptionRange<BasicBlock>> range = null;
			iteration: {
				for (Stmt stmt : popped) {
					for (Expr expr : stmt.enumerateOnlyChildren()) {
						if (expr instanceof CaughtExceptionExpr) {
							expressionPool.pop();
						}
						if (stmt instanceof ThrowStmt) {
							final ThrowStmt throwStmt = (ThrowStmt) stmt;
							expressionPool.push(throwStmt.getExpression());


							cfg.getEdges(popped)
									.stream()
									.filter(TryCatchEdge.class::isInstance)
									.map(TryCatchEdge.class::cast)
									.findFirst()
									.ifPresent(e -> range.set(e.erange));

							if (range == null) {
								break iteration;
							}


							range.get().getHandler().setStack(expressionPool);

						}
					}
				}

				/* Add all the successor nodes *//*
				cfg.getSuccessors(popped)
						.filter(e -> !visited.contains(e))
						.forEach(e -> {
							/* Set the expected received pool
							e.setPool(expressionPool);

							/* Add it to the stack to be iterated again
							bucket.add(e);
						});
			}
		}*/

		exprMap.forEach(((block, constantExpr) -> {
			constantExpr.setConstant("[Frame] " + Arrays.toString(block.getPool().getTypes())
					+ "\n" + Arrays.toString(block.getStack().getStack())
			);
		}));
	}

	private void mergeFrames(final BasicBlock target, final ExpressionPool currentFrame, final ExpressionPool otherFrame) {
		final int selfSize = currentFrame.computeSize();
		final int otherSize = otherFrame.computeSize();

		final int size = Math.max(selfSize, otherSize);

		final ExpressionPool expressionPool = new ExpressionPool(currentFrame);
		expressionPool.addParent(otherFrame);

		for (int i = beginIndex; i < size; i++) {
			/* Simple scope check. If the local is no longer used, we can ditch it */
			scope: {
				final Set<BasicBlock> scopeSet = scopeMap.get(i);

				if (scopeSet == null)
					break scope;

				if (scopeSet.contains(target))
					break scope;

				expressionPool.set(i, Type.VOID_TYPE);
				continue;
			}


			/* Simple self-frame override */
			override: {
				final Type[] defined = localAccessors.get(target);

				if (defined == null)
					break override;

				final Type type = defined[i];

				if (type == null)
					break override;

				if (localProviders.get(target)[i] == null)
					break override;

				expressionPool.set(i, type);
				continue;
			}

			final Type selfType = currentFrame.get(i);
			final Type otherType = otherFrame.get(i);


		}
	}

	private void fixRanges() {
		/*
		 * Short term fix to prevent TryCatchNode-s from being optimized
		 * out, leaving an open range
		 */
		for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
			range.getNodes().stream().filter(BasicBlock::isEmpty).forEach(e -> {
				e.add(new NopStmt());
			});
		}
	}

	private void fixTypes() {
		Map<Integer, Type> types = new HashMap<>();
		int index = 0;

		if (!cfg.getMethodNode().isStatic()) {
			types.put(index, Type.getType("L" + cfg.getMethodNode().getOwner() + ";"));
			index++;
		}

		for (Type argumentType : Type.getArgumentTypes(cfg.getMethodNode().getDesc())) {
			types.put(index, argumentType);

			index++;
			if (argumentType.equals(Type.DOUBLE_TYPE) || argumentType.equals(Type.LONG_TYPE)) {
				//types.put(index, Type.VOID_TYPE);
				index++;
			}
		}

		cfg.allExprStream()
				.filter(VarExpr.class::isInstance)
				.map(VarExpr.class::cast)
				.forEach(e -> {
					final Type type = types.get(e.getIndex());

					if (type == null)
						return;

					e.setType(type);
				});

		cfg.vertices()
				.stream()
				.flatMap(BasicBlock::stream)
				.filter(CopyVarStmt.class::isInstance)
				.map(CopyVarStmt.class::cast)
				.map(CopyVarStmt::getVariable)
				.forEach(e -> {
					final Type type = types.get(e.getIndex());

					if (type == null)
						return;

					e.setType(type);
				});
	}

	private void linearize() {
		if (cfg.getEntries().size() != 1)
			throw new IllegalStateException("CFG doesn't have exactly 1 entry");
		BasicBlock entry = cfg.getEntries().iterator().next();
		
		// Build bundle graph
		Map<BasicBlock, BlockBundle> bundles = new HashMap<>();
		Map<BlockBundle, List<BlockBundle>> bunches = new HashMap<>();
		
		// Build bundles
		List<BasicBlock> topoorder = new SimpleDfs<>(cfg, entry, SimpleDfs.TOPO).getTopoOrder();
		for (BasicBlock b : topoorder) {
			if (bundles.containsKey(b)) // Already in a bundle
				continue;
			
			if (b.cfg.getIncomingImmediateEdge(b) != null) // Look for heads of bundles only
				continue;
			
			BlockBundle bundle = new BlockBundle();
			while (b != null) {
				bundle.add(b);
				bundles.put(b, bundle);
				b = b.cfg.getImmediate(b);
			}
			
			List<BlockBundle> bunch = new ArrayList<>();
			bunch.add(bundle);
			bunches.put(bundle, bunch);
		}
		
		// Group bundles by exception ranges
		for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
			BlockBundle prevBundle = null;
			for (BasicBlock b : range.getNodes()) {
				BlockBundle curBundle = bundles.get(b);
				if (prevBundle == null) {
					prevBundle = curBundle;
					continue;
				}
				if (curBundle != prevBundle) {
					List<BlockBundle> bunchA = bunches.get(prevBundle);
					List<BlockBundle> bunchB = bunches.get(curBundle);
					if (bunchA != bunchB) {
						bunchA.addAll(bunchB);
						for (BlockBundle bundle : bunchB) {
							bunches.put(bundle, bunchA);
						}
					}
					prevBundle = curBundle;
				}
			}
		}
		
		// Rebuild bundles
		bundles.clear();
		for (Map.Entry<BlockBundle, List<BlockBundle>> e : bunches.entrySet()) {
			BlockBundle bundle = e.getKey();
			if (bundles.containsKey(bundle.getFirst()))
				continue;
			BlockBundle bunch = new BlockBundle();
			e.getValue().forEach(bunch::addAll);
			for (BasicBlock b : bunch)
				bundles.put(b, bunch);
		}
		
		// Connect bundle graph
		BundleGraph bundleGraph = new BundleGraph();
		BlockBundle entryBundle = bundles.get(entry);
		bundleGraph.addVertex(entryBundle);
		for (BasicBlock b : topoorder) {
			for (FlowEdge<BasicBlock> e : cfg.getEdges(b)) {
				if (e instanceof ImmediateEdge)
					continue;
				BlockBundle src = bundles.get(b);
				bundleGraph.addEdge(new FastGraphEdgeImpl<>(src, bundles.get(e.dst())));
			}
		}
		
		// Linearize & flatten
		order = new IndexedList<>();
		Set<BlockBundle> bundlesSet = new HashSet<>(bundles.values()); // for efficiency
		SkidFlowGraphDumper.linearize(bundlesSet, bundleGraph, entryBundle).forEach(order::addAll);
	}
	
	// Recursively apply Tarjan's SCC algorithm
	private static List<BlockBundle> linearize(Collection<BlockBundle> bundles, BundleGraph fullGraph, BlockBundle entryBundle) {
		BundleGraph subgraph = GraphUtils.inducedSubgraph(fullGraph, bundles, BundleGraph::new);

		// Experimental: kill backedges
		for (FastGraphEdge<BlockBundle> e : new HashSet<>(subgraph.getReverseEdges(entryBundle))) {
			subgraph.removeEdge(e);
		}
		
		// Find SCCs
		TarjanSCC<BlockBundle> sccComputor = new TarjanSCC<>(subgraph);
		sccComputor.search(entryBundle);
		for(BlockBundle b : bundles) {
			if(sccComputor.low(b) == -1) {
				sccComputor.search(b);
			}
		}
		
		// Flatten
		List<BlockBundle> order = new ArrayList<>();
		List<List<BlockBundle>> components = sccComputor.getComponents();
		if (components.size() == 1)
			order.addAll(components.get(0));
		else for (List<BlockBundle> scc : components) // Recurse
			order.addAll(linearize(scc, subgraph, chooseEntry(subgraph, scc)));
		return order;
	}
	
	private static BlockBundle chooseEntry(BundleGraph graph, List<BlockBundle> scc) {
		Set<BlockBundle> sccSet = new HashSet<>(scc);
		Set<BlockBundle> candidates = new HashSet<>(scc);
		candidates.removeIf(bundle -> { // No incoming edges from within the SCC.
			for (FastGraphEdge<BlockBundle> e : graph.getReverseEdges(bundle)) {
				if (sccSet.contains(e.src()))
					return true;
			}
			return false;
		});
		if (candidates.isEmpty())
			return scc.get(0);
		return candidates.iterator().next();
	}

	private void naturalise() {
		for (int i = 0; i < order.size(); i++) {
			BasicBlock b = order.get(i);
			for (FlowEdge<BasicBlock> e : new HashSet<>(cfg.getEdges(b))) {
				BasicBlock dst = e.dst();
				if (e instanceof ImmediateEdge && order.indexOf(dst) != i + 1) {
					// Fix immediates
					final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(b, dst);
					b.add(new UnconditionalJumpStmt(dst, edge));
					cfg.removeEdge(e);
					cfg.addEdge(edge);
				} else if (e instanceof UnconditionalJumpEdge && order.indexOf(dst) == i + 1) {
					// Remove extraneous gotos
					for (ListIterator<Stmt> it = b.listIterator(b.size()); it.hasPrevious(); ) {
						if (it.previous() instanceof UnconditionalJumpStmt) {
							it.remove();
							break;
						}
					}
					cfg.removeEdge(e);
					cfg.addEdge(new ImmediateEdge<>(b, dst));
				}
			}
		}
	}

	private void verifyOrdering() {
		ListIterator<BasicBlock> it = order.listIterator();
		while(it.hasNext()) {
			BasicBlock b = it.next();
			
			for(FlowEdge<BasicBlock> e: cfg.getEdges(b)) {
				if(e.getType() == FlowEdges.IMMEDIATE) {
					if(it.hasNext()) {
						BasicBlock n = it.next();
						it.previous();
						
						if(n != e.dst()) {
							throw new IllegalStateException("Illegal flow " + e + " > " + n);
						}
					} else {
						throw new IllegalStateException("Trailing " + e);
					}
				}
			}
		}
	}

	private void dumpRange(ExceptionRange<BasicBlock> er) {
		// Determine exception type
		final Type type = getRangeType(er);
		final Label handler = getLabel(er.getHandler());
		List<BasicBlock> range = new ArrayList<>(er.getNodes());
		range.sort(Comparator.comparing(order::indexOf));
		
		Label start;
		int rangeIdx = -1, orderIdx;
		do {
			if (++rangeIdx == range.size()) {
				System.err.println("[warn] range is absent: " + m);
				return;
			}
			BasicBlock b = range.get(rangeIdx);
			orderIdx = order.indexOf(b);
			start = getLabel(b);
		} while (orderIdx == -1);
		
		for (;;) {
			// check for endpoints
			if (orderIdx + 1 == order.size()) { // end of method
				assert start != terminalLabel.getLabel() : "Label assigned is semantically identical.";
				m.node.visitTryCatchBlock(start, terminalLabel.getLabel(), handler, type.getInternalName());
				break;
			} else if (rangeIdx + 1 == range.size()) { // end of range
				Label end = getLabel(order.get(orderIdx + 1));
				assert start != end : "Label assigned is semantically identical.";
				m.node.visitTryCatchBlock(start, end, handler, type.getInternalName());
				break;
			}
			
			// check for discontinuity
			BasicBlock nextBlock = range.get(rangeIdx + 1);
			int nextOrderIdx = order.indexOf(nextBlock);
			if (nextOrderIdx - orderIdx > 1) { // blocks in-between, end the handler and begin anew
				System.err.println("\r\n[warn] Had to split up a range: " + m + "\n");
				Label end = getLabel(order.get(orderIdx + 1));
				assert start != end : "Label assigned is semantically identical.";
				m.node.visitTryCatchBlock(start, end, handler, type.getInternalName());
				start = getLabel(nextBlock);
			}

			// next
			rangeIdx++;
			if (nextOrderIdx != -1)
				orderIdx = nextOrderIdx;
		}
	}

	private Type getRangeType(final ExceptionRange<BasicBlock> er) {
		Type type;
		Set<Type> typeSet = er.getTypes();
		if (typeSet.size() != 1) {
			// TODO: find base exception
			final Set<ClassNode> classNodes = new HashSet<>();
			for (Type typec : er.getTypes()) {
				final String type1 = typec.getClassName().replace(".", "/");
				ClassNode classNode = skidfuscator
						.getClassSource()
						.findClassNode(type1);

				if (classNode == null) {
					System.err.println("\r\nFailed to find class of type " + type1  + "!\n" );
					try {
						final ClassReader reader = new ClassReader(typec.getInternalName());
						final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
						reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

						classNode = new SkidClassNode(node, skidfuscator);
						skidfuscator.getClassSource().getClassTree().addVertex(classNode);
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
				}

				classNodes.add(classNode);
				skidfuscator.getClassSource()
						.getClassTree()
						.addVertex(classNode);
			}

			/* Simple DFS naive common ancestor algorithm */
			final Collection<ClassNode> commonAncestors = skidfuscator
					.getClassSource()
					.getClassTree()
					.getCommonAncestor(classNodes);

			assert commonAncestors.size() > 0 : "No common ancestors between exceptions!";
			ClassNode mostIdealAncestor = null;

			if (commonAncestors.size() == 1) {
				mostIdealAncestor = commonAncestors.iterator().next();

				if (mostIdealAncestor.getName().equals("java/lang/Object")) {
					System.out.println("[WARNING] Failed to find common ancestor between: " + classNodes.stream().map(ClassNode::getDisplayName).collect(Collectors.joining(",")));
					classNodes.stream()
							.flatMap(e -> skidfuscator.getClassSource()
									.getClassTree().getEdges(e).stream())
							.forEach(e -> System.out.println(e.src().getDisplayName() + " --> " + e.dst().getDisplayName()));
					classNodes.stream()
							.flatMap(e -> skidfuscator.getClassSource()
									.getClassTree().getPredecessors(e))
							.forEach(e -> System.out.println("<-- " + e.getDisplayName()));
					return TypeUtil.THROWABLE_TYPE;
				}
			} else {
				iteration: {
					for (ClassNode commonAncestor : commonAncestors) {
						Stack<ClassNode> parents = new Stack<>();
						parents.add(commonAncestor);

						while (!parents.isEmpty()) {
							if (parents.peek()
									.getName()
									.equalsIgnoreCase("java/lang/Throwable")) {
								mostIdealAncestor = commonAncestor;
								break iteration;
							}

							parents.addAll(skidfuscator.getClassSource().getClassTree().getParents(parents.pop()));
						}
					}
				}
			}

			assert mostIdealAncestor != null : "Exception parent not found in dumper!";

			type = Type.getObjectType(mostIdealAncestor.getName());
		} else {
			type = typeSet.iterator().next();
		}

		return type;
	}
	
	private void verifyRanges() {
		for (TryCatchBlockNode tc : m.node.tryCatchBlocks) {
			int start = -1, end = -1, handler = -1;
			for (int i = 0; i < m.node.instructions.size(); i++) {
				AbstractInsnNode ain = m.node.instructions.get(i);
				if (!(ain instanceof LabelNode))
					continue;
				Label l = ((LabelNode) ain).getLabel();
				if (l == tc.start.getLabel())
					start = i;
				if (l == tc.end.getLabel()) {
					if (start == -1)
						throw new IllegalStateException("Try block end before start " + m);
					end = i;
				}
				if (l == tc.handler.getLabel()) {
					handler = i;
				}
			}

			if (start == end) {
				throw new IllegalStateException("Try block ends on starting position in " + m);
			}
			if (start == -1 || end == -1 || handler == -1)
				throw new IllegalStateException("Try/catch endpoints missing: " + start + " " + end + " " + handler + m);
		}
	}

	@Override
	public Label getLabel(BasicBlock b) {
		return labels.get(b).getLabel();
	}

	@Override
	public ControlFlowGraph getGraph() {
		return cfg;
	}

	private static class BundleGraph extends FastDirectedGraph<BlockBundle, FastGraphEdge<BlockBundle>> { }
	@SuppressWarnings("serial")
	private static class BlockBundle extends ArrayList<BasicBlock> implements FastGraphVertex {
		private BasicBlock first = null;
		
		private BasicBlock getFirst() {
			if (first == null)
				first = get(0);
			return first;
		}
		
		@Override
		public String getDisplayName() {
			return getFirst().getDisplayName();
		}
		
		@Override
		public int getNumericId() {
			return getFirst().getNumericId();
		}
		
		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			for (Iterator<BasicBlock> it = this.iterator(); it.hasNext(); ) {
				BasicBlock b = it.next();
				s.append(b.getDisplayName());
				if (it.hasNext())
					s.append("->");
			}
			return s.toString();
		}
		
		@Override
		public int hashCode() {
			return getFirst().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof BlockBundle))
				return false;
			return ((BlockBundle) o).getFirst().equals(getFirst());
		}
	}
}
