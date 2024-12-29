package framework;

import java.util.HashMap;
import java.util.Map;

class MemoryClassLoader extends ClassLoader {
    private final Map<String, byte[]> classBytes = new HashMap<>();

    void addClass(String name, byte[] bytes) {
        classBytes.put(name, bytes);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classBytes.get(name);
        if (bytes == null) {
            return super.findClass(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }
}