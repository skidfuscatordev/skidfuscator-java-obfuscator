package framework;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final Map<String, byte[]> classBytes = new HashMap<>();

    MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String name,
                                               JavaFileObject.Kind kind, FileObject sibling) {
        return new SimpleJavaFileObject(
                URI.create("string:///" + name.replace('.', '/') + ".class"),
                JavaFileObject.Kind.CLASS) {
            @Override
            public OutputStream openOutputStream() {
                return new OutputStream() {
                    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    @Override
                    public void write(int b) {
                        baos.write(b);
                    }

                    @Override
                    public void close() throws IOException {
                        super.close();
                        classBytes.put(name, baos.toByteArray());
                    }
                };
            }
        };
    }

    Map<String, byte[]> getClassBytes() {
        return classBytes;
    }
}