package dev.skidfuscator.core.classloader;

import java.net.URL;
import java.net.URLStreamHandlerFactory;

public class SkidClassLoader extends AccessClassLoader {
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
