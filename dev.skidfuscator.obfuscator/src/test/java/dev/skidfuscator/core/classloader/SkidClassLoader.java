package dev.skidfuscator.core.classloader;

import java.net.URL;
import java.net.URLStreamHandlerFactory;

public class SkidClassLoader extends AccessClassLoader {
    private final java.util.Map<String,byte[]> definitions = new java.util.HashMap<>();
    public void register(String name, byte[] bytes) { definitions.put(name.replace('/', '.'),bytes); }
    @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> value = findLoadedClass(name);
            if (value == null) {
                byte[] bytes = definitions.remove(name);
                value = bytes == null ? super.loadClass(name, false) : defineClass(name,bytes,0,bytes.length);
            }
            if (resolve) resolveClass(value);
            return value;
        }
    }

    public SkidClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public SkidClassLoader(URL[] urls) {
        super(urls);
    }

    public SkidClassLoader() {
        super(new URL[0]);
    }

    public SkidClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    @Override
    public Class<?> defineClass(String name, byte[] bytes) throws ClassFormatError {
        return super.defineClass(name.replace("/", "."), bytes);
    }
}
