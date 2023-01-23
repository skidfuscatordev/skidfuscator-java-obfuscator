package org.mapleir.ir.code;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.util.JavaDesc;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	/**
	 * The children of this unit.
	 */
	public Expr[] children;
	/**
	 * Index of the last item in the children array.
	 */
	private int ptr;

	public CodeUnit(int opcode) {
		this.opcode = opcode;
		children = new Expr[8];
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
		
		// TODO: may invalidate the statement if block is null

		for(Expr s : children) {
			if(s != null) {
				if (s == this) {
					throw new IllegalStateException("Self hierarchy? " + this);
				}
				s.setBlock(block);
			}
		}
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
		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				size += children[i].deepSize();
			}
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
		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				size++;
			}
		}
		return size;
	}

	public int capacity() {
		return children.length;
	}

	protected boolean shouldExpand() {
		double max = children.length * 0.50;
		return (double) size() > max;
	}

	protected void expand() {
		if (children.length >= Integer.MAX_VALUE)
			throw new UnsupportedOperationException();
		long len = children.length * 2;
		if (len > Integer.MAX_VALUE)
			len = Integer.MAX_VALUE;
		Expr[] newArray = new Expr[(int) len];
		System.arraycopy(children, 0, newArray, 0, children.length);
		children = newArray;
	}

	public int indexOf(Expr s) {
		for (int i = 0; i < children.length; i++) {
			if (children[i] == s) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Gets the child {@link Expr} at the given index.
	 * 
	 * @param newPtr The index of the child.
	 * @return The child {@link Expr}.
	 */
	public Expr read(int newPtr) {
		if (newPtr < 0 || newPtr >= children.length || (newPtr > 0 && children[newPtr - 1] == null))
			throw new ArrayIndexOutOfBoundsException(String.format("%s, ptr=%d, len=%d, addr=%d", this.getClass().getSimpleName(), ptr, children.length, newPtr));
		return children[newPtr];
	}

	/**
	 * Writes the given expression as a child of the current unit at the given
	 * index. The expression to write must be unassociated with any current
	 * unit, i.e. have no parent already.<br>
	 * When the node is updated the {@link #onChildUpdated(int)} callback is
	 * invoked with the given index.<br>
	 * 
	 * @param s The new expression to write at the given index.
	 * @param index The index of the children to update.
	 * @return The expression that was previously at the given index or null if
	 * there was no change to the state of the children array.
	 * @throws IllegalStateException if the given expression already has a
	 * parent unit (TODO: return null).
	 * @throws ArrayIndexOutOfBoundsException if the index is 
	 */
	public Expr writeAt(Expr s, int index) {
		if (index < 0 || index >= children.length 
				|| (index > 0 && children[index - 1] == null)) {
			throw new ArrayIndexOutOfBoundsException(String.format("ptr=%d, "
					+ "len=%d, addr=%d", ptr, children.length, index));
		}
		Expr prev = children[index];
		/* check this before checking if there is a parent for 's' as the
		 * parent may be this node. */
		if(prev == s) {
			/* no point replacing an expr with itself, no change. */
			return null;
		}
		if(s != null && s.parent != null) {
			throw new IllegalStateException(String.format("%s already belongs "
					+ "to %s (new: %s)", s, s.parent, getRootParent0()));
		}
		
		if(shouldExpand()) {
			expand();
		}
		
		if(prev != null) {
			prev.setParent(null);
		}
		children[index] = s;
		if(s != null) {
			s.setParent(this);
		}
		onChildUpdated(index);
		return prev;
	}

	public void deleteAt(int _ptr) {
		if (_ptr < 0 || _ptr >= children.length || (_ptr > 0 && children[_ptr - 1] == null))
			throw new ArrayIndexOutOfBoundsException(String.format("ptr=%d, len=%d, addr=%d", ptr, children.length, _ptr));
		if (children[_ptr] == null)
			throw new UnsupportedOperationException("No statement at " + _ptr);

		if ((_ptr + 1) < children.length && children[_ptr + 1] == null) {
			// ptr: s5 (4)
			// len = 8
			// before: [s1, s2, s3, s4, s5  , null, null, null]
			// after : [s1, s2, s3, s4, null, null, null, null]
			writeAt(null, _ptr);
		} else {
			// ptr: s2 (1)
			// len = 8
			// before: [s1, s2, s3, s4, s5 ,  null, null, null]
			// del s2 (1)
			// before: [s1, null, s3, s4, s5 ,  null, null, null]
			// ptr+1 = s3 (2)
			// (ptr+1 to len) = {2, 3, 4, 5, 6, 7}
			// shift elements down 1
			// after : [s1, s3, s4, s5, null, null, null, null]
			writeAt(null, _ptr);
			for (int i = _ptr + 1; i < children.length; i++) {
				Expr s = children[i];
				// set the parent to null, since
				// the intermediary step in this
				// shifting looks like:
				//   [s1, s3, s3, s4, s5, null, null, null]
				// then we remove the second one
				//   [s1, s3, null, s4, s5, null, null, null]
				
				// end of active stmts in child array.
				if(s == null) {
					break;
				}
				
				if(s != null) {
					s.setParent(null);
				}
				writeAt(s, i-1);
				writeAt(null, i);
				// we need to set the parent again,
				// because we have 2 of the same
				// node in the children array, which
				// means the last writeAt call, sets
				// the parent as null.
				if(s != null) {
					s.setParent(this);
				}
			}
		}
	}

	public List<Expr> getChildren() {
		List<Expr> list = new ArrayList<>();
		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				list.add(children[i]);
			}
		}
		return list;
	}

	public void setChildPointer(int _ptr) {
		if (_ptr < 0 || _ptr >= children.length || (_ptr > 0 && children[_ptr - 1] == null))
			throw new ArrayIndexOutOfBoundsException(String.format("ptr=%d, len=%d, addr=%d", ptr, children.length, _ptr));
		ptr = _ptr;
	}

	public int getChildPointer() {
		return ptr;
	}

	public void overwrite(final Expr previous, final Expr newest) {
		final int index = this.indexOf(previous);
		writeAt(newest, index);
		assert children[index] == newest;
		//children[index] = newest;
		previous.unlink();
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
			for(Expr c : children) {
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
		for(Expr c : children) {
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
