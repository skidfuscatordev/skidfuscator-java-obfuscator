package org.mapleir.ir.code;

import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Objects;

// top(0) -> bottom(size() - 1)
public class TypeStack {

	protected Type[] stack;
	protected int size;

	public TypeStack() {
		this(8 * 8);
	}

	public TypeStack(int capacity) {
		capacity = Math.max(capacity, 1);
		stack = new Type[capacity];
		size = 0;
	}

	private void expand() {
		Type[] s = new Type[size * 2];
		System.arraycopy(stack, 0, s, 0, size);
		stack = s;
	}

	public void push(Type e) {
		int i = size++;
		if (stack.length == size) {
			expand();
		}
		stack[i] = e;
	}

	public Type peek() {
		return stack[size - 1];
	}

	public Type peek(int d) {
		return stack[size - d - 1];
	}

	public Type pop() {
		Type e = stack[--size];
		stack[size] = null;
		return e;
	}

	public Type getAt(int i) {
		return stack[i];
	}

	public void merge(final TypeStack other) {
		if (other.stack.length >= this.stack.length) {
			Type[] s = new Type[other.capacity()];
			System.arraycopy(stack, 0, s, 0, size);
			stack = s;
		}

		for (int i = 0; i < other.stack.length; i++) {
			final Type selfType = this.stack[i];
			final Type otherType = other.stack[i];

			final boolean selfFilled = selfType != Type.VOID_TYPE && selfType != null;
			final boolean otherFilled = otherType != Type.VOID_TYPE && otherType != null;

			if (selfFilled && otherFilled && selfType != otherType) {
				throw new IllegalStateException("Trying to merge " + selfType
						+ " (self) with " + otherType + " (other) [FAILED] [" + i + "]");
			}

			if (otherFilled && !selfFilled) {
				this.stack[i] = otherType;
			}
		}
	}

	public void copyInto(TypeStack other) {
		Type[] news = new Type[capacity()];
		System.arraycopy(stack, 0, news, 0, capacity());
		other.stack = news;
		other.size = size;
	}

	public TypeStack copy() {
		TypeStack stack = new TypeStack(size());
		copyInto(stack);
		return stack;
	}

	public void assertHeights(int[] heights) {
		if (heights.length > size()) {
			throw new UnsupportedOperationException(String.format("hlen=%d, size=%d", heights.length, size()));
		} else {
			for (int i = 0; i < heights.length; i++) {
				Type e = peek(i);
				if (e.getSize() != heights[i]) {
					throw new IllegalStateException(String.format("item at %d, len=%d, expected=%d, expr:%s", i,
							e.getSize(), heights[i], e));
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
			count += peek(i).getSize();
		}
		return count;
	}

	public Type[] getStack() {
		return stack;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TypeStack)) return false;
		TypeStack that = (TypeStack) o;
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
			Type n = stack[i];
			if (n != null) {
				sb.append(n);
				sb.append(":").append(n);
				if (i != 0 && stack[i - 1] != null) {
					sb.append(", ");
				}
			}
		}
		sb.append("]");
		return sb.toString();
	}
}