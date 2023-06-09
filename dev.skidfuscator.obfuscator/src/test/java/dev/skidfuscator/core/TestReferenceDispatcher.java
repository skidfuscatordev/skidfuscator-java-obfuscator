package dev.skidfuscator.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class TestReferenceDispatcher {
    public static Object dispatch(
            final MethodHandles.Lookup lookup,
            final String bootName,
            final MethodType methodType,
            final Object stat,
            final Object className,
            final Object name,
            final Object descriptor) throws Exception {
        Class<?> clazz = Class.forName((String) className);
        ClassLoader classLoader = clazz.getClassLoader();
        MethodType methodType2 = MethodType.fromMethodDescriptorString((String) descriptor, classLoader);

        return (Integer) stat == 184
                ? new MutableCallSite(lookup.findStatic(clazz, (String) name, methodType2).asType(methodType))
                : new MutableCallSite(lookup.findVirtual(clazz, (String) name, methodType2).asType(methodType));
    }
}
