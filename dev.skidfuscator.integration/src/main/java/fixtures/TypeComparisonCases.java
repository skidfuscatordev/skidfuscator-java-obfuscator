package fixtures;

import java.io.*;
import java.util.*;

/** Distinct defining loaders must never make identically named classes assignable. */
public final class TypeComparisonCases {
    private static boolean sameType(Object value) { return value instanceof TypeIdentity; }
    private static boolean array(Object value) { return value instanceof Object[]; }
    private static boolean list(Object value) { return value instanceof List; }
    public static String run() throws Exception {
        final String name = "fixtures.TypeIdentity";
        byte[] bytes;
        try (InputStream input = TypeComparisonCases.class.getResourceAsStream("TypeIdentity.class")) {
            if (input == null) throw new AssertionError("Type fixture missing");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int count;
            while ((count=input.read(buffer))!=-1) output.write(buffer,0,count);
            bytes = output.toByteArray();
        }
        final byte[] definition = bytes;
        ClassLoader loader = new ClassLoader(TypeComparisonCases.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String candidate, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(candidate)) {
                    if (!candidate.equals(name)) return super.loadClass(candidate,resolve);
                    Class<?> value = findLoadedClass(candidate);
                    if (value == null) value = defineClass(candidate,definition,0,definition.length);
                    if (resolve) resolveClass(value);
                    return value;
                }
            }
        };
        Object foreign = loader.loadClass(name).getDeclaredConstructor().newInstance();
        if (sameType(foreign) || !sameType(new TypeIdentity()) || sameType(null)
                || !array(new String[1][]) || array(new int[1]) || array(null)
                || !list(new ArrayList<Object>()) || list(new Object())) throw new AssertionError("Type identity changed");
        return "types-ok";
    }
}
