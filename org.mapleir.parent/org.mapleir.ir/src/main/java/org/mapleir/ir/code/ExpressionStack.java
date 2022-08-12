package org.mapleir.ir.code;

import java.util.Arrays;
import java.util.Objects;

// top(0) -> bottom(size() - 1)
public class ExpressionStack {

	private Expr[] stack;
	private int size;

	public ExpressionStack() {
		this(8 * 8);
	}

	public ExpressionStack(int capacity) {
		capacity = Math.max(capacity, 1);
		stack = new Expr[capacity];
		size = 0;
	}

	private void expand() {
		Expr[] s = new Expr[size * 2];
		System.arraycopy(stack, 0, s, 0, size);
		stack = s;
	}

	public void push(Expr e) {
		int i = size++;
		if (stack.length == size) {
			expand();
		}
		stack[i] = e;
	}

	public Expr peek() {
		return stack[size - 1];
	}

	public Expr peek(int d) {
		return stack[size - d - 1];
	}

	public Expr pop() {
		Expr e = stack[--size];
		stack[size] = null;
		return e;
	}

	public Expr getAt(int i) {
		return stack[i];
	}

	public void copyInto(ExpressionStack other) {
		Expr[] news = new Expr[capacity()];
		System.arraycopy(stack, 0, news, 0, capacity());
		other.stack = news;
		other.size = size;
	}

	public ExpressionStack copy() {
		ExpressionStack stack = new ExpressionStack(size());
		copyInto(stack);
		return stack;
	}

	public void assertHeights(int[] heights) {
		if (heights.length > size()) {
			throw new UnsupportedOperationException(String.format("hlen=%d, size=%d", heights.length, size()));
		} else {
			for (int i = 0; i < heights.length; i++) {
				Expr e = peek(i);
				if (e.getType().getSize() != heights[i]) {
					throw new IllegalStateException(String.format("item at %d, len=%d, expected=%d, expr:%s", i,
							e.getType().getSize(), heights[i], e));
				}
			}
		}
	}

	public void clear() {
		for (int i = size - 1; i >= 0; i--) {
			stack[i] = null;
		}
		size = 0;
	}

	public boolean isEmpty() {
		return size <= 0;
	}

	public int size() {
		return size;
	}

	public int capacity() {
		return stack.length;
	}

	public int height() {
		int count = 0;
		for (int i = 0; i < size(); i++) {
			count += peek(i).getType().getSize();
		}
		return count;
	}

	public Expr[] getStack() {
		return stack;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ExpressionStack)) return false;
		ExpressionStack that = (ExpressionStack) o;
		return size == that.size &&
				Arrays.equals(stack, that.stack);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(size);
		result = 31 * result + Arrays.hashCode(stack);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("top->btm[");
		for (int i = size() - 1; i >= 0; i--) {
			Expr n = stack[i];
			if (n != null) {
				sb.append(n);
				sb.append(":").append(n.getType());
				if (i != 0 && stack[i - 1] != null) {
					sb.append(", ");
				}
			}
		}
		sb.append("]");
		return sb.toString();
	}
}