package org.mapleir;

public class DynamicExample {

	interface A{
		int func(int x, int y);
	}
	public static void main(String[] args) {
		A sum = (x,y) -> (x+y);
		A mul = (x,y) -> (x*y);

		System.out.println(sum.func(5, 2));
		System.out.println(mul.func(9, 6));
	}
}