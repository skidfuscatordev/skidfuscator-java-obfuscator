package org.mapleir.ir.code;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the shared base between the {@link Stmt} and {@link Expr} classes,
 * which are the parents for all of the instructions that can be represented in
 * the IR. Units, like nodes in a tree, have children and depending on the type
 * of the child, i.e. whether the unit is a statement or an expression, can
 * have a parent also. These children are of the {@link Expr} type and operations
 * manipulating the children of a given node are implemented in this class.<br>
 * Operations include reading, writing and overwriting children of a given unit
 * as well polling for capacity, size and index information.
 * 
 * <p> Each CodeUnit has a unique id therefore two CodeUnit objects can only be
 * equal iff their id's match.<br>
 * Opcodes for each unit are described in {@link Opcode}.<br>
 */
public abstract class CodeUnit implements FastGraphVertex, Opcode {

	/**
	 * Flag position mask used for indicating whether the current CodeUnit is a
	 * {@link Stmt} or an {@link Expr}.
	 */
	public static final int FLAG_STMT = 0x01;

	/**
	 * Global unit identifier counter.
	 */
	private static int G_ID_COUNTER = 1;
	/**
	 * Unique global unit identifier.
	 */
	protected final int id = G_ID_COUNTER++;
	/**
	 * Opcode to encode the sort of instruction this unit is.
	 */
	protected final int opcode;
	/**
	 * Bitfield for boolean state information of this unit.
	 */
	protected int flags;
	/**
	 * The {@link BasicBlock} that this unit belongs to. If the current unit is
	 * a {@link Stmt}, the block is the direct container above this unit,
	 * otherwise for {@link Expr}'s, the block is inherited from the root
	 * parent of this expression.
	 */
	private BasicBlock block;

	public CodeUnit(int opcode) {
		this.opcode = opcode;
	}

	public void setFlag(int flag, boolean val) {
		if(val) {
			flags |= flag;
		} else {
			flags ^= flag;
		}
	}

	public boolean isFlagSet(int f) {
		return ((flags & f) != 0);
	}

	@Override
	public int getNumericId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return Integer.toString(id);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}

	public final int getOpcode() {
		return opcode;
	}

	public final String getOpname() {
		return Opcode.opname(opcode);
	}

	public BasicBlock getBlock() {
		return block;
	}

	public void setBlock(BasicBlock block) {
		this.block = block;
		this.children().forEach(c -> c.setBlock(block));
	}

	/**
	 * Computes the complete size of the current unit all the way down to leaf
	 * units/nodes, i.e. the number of nodes contained in a region enclosing
	 * this node, it's children and their children recursively in a node tree.
	 * 
	 * @return The tree branch weight including this node. ( &gt;= 1)
	 */
	public int deepSize() {
		int size = 1;
		for (CodeUnit codeUnit : children()) {
			size += codeUnit.deepSize();
		}
		return size;
	}

	/**
	 * Computes the number of non-null children that this unit has.
	 * 
	 * @return The number of children. ( &gt;= 0)
	 */
	public int size() {
		int size = 0;
		for (CodeUnit codeUnit : children()) {
			size++;
		}
		return size;
	}

	public int capacity() {
		return children().size();
	}

	public int indexOf(Expr s) {
		//for (int i = 0; i < children.length; i++) {
		//	if (children[i] == s) {
		//		return i;
		//	}
		//}
		return -1;
	}

	public List<Expr> getLinkedChildren() {
		return linkedChildren()
				.stream()
				// [notice] explicit forcing behaviour since no
				// 			expr can be a child besides exprs
				.map(Expr.class::cast)
				.toList();
	}

	public List<Expr> getChildren() {
		return children()
				.stream()
				// [notice] explicit forcing behaviour since no
				// 			expr can be a child besides exprs
				.map(Expr.class::cast)
				.toList();
	}

	public void overwrite(final Expr previous, final Expr newest) {
		throw new IllegalArgumentException(String.format(
				"Cannot overwrite %s with %s --> %s",
				this, previous, newest
		));
	}

	public abstract void onChildUpdated(int ptr);

	public abstract void toString(TabbedStringWriter printer);

	public abstract void toCode(MethodVisitor visitor, BytecodeFrontend assembler);

	public abstract boolean canChangeFlow();

	public abstract boolean equivalent(CodeUnit s);

	public abstract CodeUnit copy();

	private Stmt getRootParent0() {
		if((flags & FLAG_STMT) != 0) {
			return (Stmt) this;
		} else {
			return ((Expr) this).getRootParent();
		}
	}

	@Deprecated
	public Set<Expr> _enumerate() {
		Set<Expr> set = new HashSet<>();

		if(opcode == Opcode.PHI) {
			/*CopyPhiStmt phi = (CopyPhiStmt) this;
			for(Expr e : phi.getExpression().getArguments().values()) {
				set.add(e);
				set.addAll(e._enumerate());
			}*/
			PhiExpr phi = (PhiExpr) this;
			for(Expr e : phi.getArguments().values()) {
				set.add(e);
				set.addAll(e._enumerate());
			}
		} else {
			for(Expr c : children().stream()
					.map(Expr.class::cast)
					.toList()) {
				if(c != null) {
					set.add(c);
					set.addAll(c._enumerate());
				}
			}
		}

		return set;
	}

	public Iterable<Expr> enumerateOnlyChildren() {
		return _enumerate();
	}

	protected void dfsStmt(List<CodeUnit> list) {
		for(Expr c : children().stream()
				.map(Expr.class::cast)
				.toList()) {
			if(c != null) {
				c.dfsStmt(list);
			}
		}
		list.add(this);
	}

	public List<CodeUnit> enumerateExecutionOrder() {
		List<CodeUnit> list = new ArrayList<>();
		dfsStmt(list);
		return list;
	}

	@Override
	public String toString() {
		return print(this);
	}

	public static String print(CodeUnit node) {
		TabbedStringWriter printer = new TabbedStringWriter();
		node.toString(printer);
		return printer.toString();
	}

	public final List<CodeUnit> traverseExplicit() {
		final List<CodeUnit> units = new ArrayList<>();

		for (CodeUnit child : children()) {
			units.addAll(child.traverseExplicit());
		}
		units.addAll(self());

		return Collections.unmodifiableList(units);
	}

	// THIS METHOD IS DIFFERENT AS IT TRAVERSES ALL REFERENCED
	// VARIABLES THAT ARE IMPLICITLY CALLED LIKE VAREXPR IN
	// COPY VARS
	public final List<CodeUnit> traverse() {
		final List<CodeUnit> units = new ArrayList<>();

		for (CodeUnit child : linkedChildren()) {
			units.addAll(child.traverse());
		}
		units.addAll(self());

		return Collections.unmodifiableList(units);
	}

	public final List<CodeUnit> traverseChildren() {
		final List<CodeUnit> units = new ArrayList<>();

		for (CodeUnit child : children()) {
			units.addAll(child.traverseExplicit());
		}

		return Collections.unmodifiableList(units);
	}

	public List<CodeUnit> self() {
		return List.of(this);
	}

	public List<CodeUnit> linkedChildren() {
		return children();
	}

	public List<CodeUnit> children() {
		return Collections.emptyList();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CodeUnit codeUnit = (CodeUnit) o;

		return id == codeUnit.id;
	}
	
	/**
	 * Utility function for subclasses to throw a
	 * {@link ChildOutOfBoundsException} indicating that data is being read or
	 * written at an index that is not used by a given type of node.
	 * 
	 * @param index The index that is blacklisted.
	 */
	protected void raiseChildOutOfBounds(int index) {
		throw new ChildOutOfBoundsException(this, index);
	}
}
