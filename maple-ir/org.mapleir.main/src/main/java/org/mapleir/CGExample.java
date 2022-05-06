package org.mapleir;

import javax.annotation.Detainted;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CGExample {

	public @interface MyAnno {
		String val();
		Class<?> type();
	}
	public @interface MyAnno2 {
		String[] val();
		Class<?> type();
	}
	@Nonnull
	@MyAnno2(type = CGExample.class, val = { "ayy", "lmao" })
	private int num = 10;
	@MyAnno(type = CGExample.class, val = "hey")
	@MyAnno2(type = CGExample.class, val = { "nayy", "lNao" })
	private int fibNum = fib(num);
	
	public static void params(@Deprecated int x, @Detainted int y, @Nullable Object o, @Nonnull String s) {
		
	}
	public static int fib0(int n) {
		return n;
	}
	
	public static int fib2(int n) {
		return fib(n-1) + fib(n-2);
	}
	
	public static int fib(int n) {
		if(n <= 1) {
			return fib0(n);
		} else {
			return fib2(n);
		}
	}
	
	public static void main(String[] args) {
		try {
			int f = fib(10);
			System.out.println(f);
		} catch(Throwable t) {
			t.printStackTrace();
			
		}
	}
}