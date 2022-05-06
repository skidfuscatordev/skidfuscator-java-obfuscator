package org.mapleir.deob.intraproc.eval.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.mapleir.deob.intraproc.eval.EvaluationFunctor;

public class ReflectiveFunctor<T> implements EvaluationFunctor<T> {
	private final Method method;
	
	public ReflectiveFunctor(Method method) {
		this.method = method;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T eval(Object... args) throws IllegalArgumentException {
		try {
			return (T) method.invoke(null, args);
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(String.format("func: %s, expected: %s%n args: %s%n actual: %s", this, Arrays.toString(method.getParameterTypes()), Arrays.toString(args), typeString(args)));
		}
	}
	
	@Override
	public String toString() {
		return method.toString();
	}
	
	private static String typeString(Object[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i] == null ? "NULL" : a[i].getClass().getName());
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
	}
}