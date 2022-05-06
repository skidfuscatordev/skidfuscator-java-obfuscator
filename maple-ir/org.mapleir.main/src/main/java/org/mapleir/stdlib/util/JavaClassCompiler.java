package org.mapleir.stdlib.util;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;

public class JavaClassCompiler {
	public byte[] compile(String className, String src) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		JavaFileObject file = new JavaSourceFromString(className, src);
		Iterable<? extends JavaFileObject> compilationUnits = Collections.singletonList(file);
		StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, null, null);
		//uses custom file manager with defined class loader inorder to unload the compiled class when this is done
		ClassFileManager fileManager =  new ClassFileManager(stdFileManager);
		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

		boolean success = task.call();
		for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
			System.out.println(diagnostic.getKind() + " at " + diagnostic.getStartPosition() + "-" + diagnostic.getEndPosition() + ": " + diagnostic.getMessage(null));
		}
		System.out.println("Success: " + success);
		if (success) {
			return fileManager.jclassObject.getBytes();
		} else {
			return null;
		}
	}

    class JavaSourceFromString extends SimpleJavaFileObject {
    	final String code;

    	JavaSourceFromString(String name, String code) {
    		super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
    		this.code = code;
    	}

    	@Override
    	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    		return code;
    	}
    }
    class JavaClassObject extends SimpleJavaFileObject {

    	/**
    	 * Byte code created by the compiler will be stored in this
    	 * ByteArrayOutputStream so that we can later get the
    	 * byte array out of it
    	 * and put it in the memory as an instance of our class.
    	 */
    	protected ByteArrayOutputStream bos =
    			new ByteArrayOutputStream();

    	/**
    	 * Registers the compiled class object under URI
    	 * containing the class full name
    	 *
    	 * @param name
    	 *            Full name of the compiled class
    	 * @param kind
    	 *            Kind of the data. It will be CLASS in our case
    	 */
    	public JavaClassObject(String name, Kind kind) {
    		super(URI.create("string:///" + name.replace('.', '/')
    				+ kind.extension), kind);
    	}

    	/**
    	 * Will be used by our file manager to get the byte code that
    	 * can be put into memory to instantiate our class
    	 *
    	 * @return compiled byte code
    	 */
    	public byte[] getBytes() {
    		return bos.toByteArray();
    	}

    	/**
    	 * Will provide the compiler with an output stream that leads
    	 * to our byte array. This way the compiler will write everything
    	 * into the byte array that we will instantiate later
    	 */
    	@Override
    	public OutputStream openOutputStream() {
    		return bos;
    	}
    }
    class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    	/**
    	 * Instance of JavaClassObject that will store the
    	 * compiled bytecode of our class
    	 */
    	public JavaClassObject jclassObject;

    	/**
    	 * Will initialize the manager with the specified
    	 * standard java file manager
    	 *
    	 * @param standardManager
    	 */
    	public ClassFileManager(StandardJavaFileManager standardManager) {
    		super(standardManager);
    	}

    	/**
    	 * Will be used by us to get the class loader for our
    	 * compiled class. It creates an anonymous class
    	 * extending the SecureClassLoader which uses the
    	 * byte code created by the compiler and stored in
    	 * the JavaClassObject, and returns the Class for it
    	 */
    	@Override
    	public ClassLoader getClassLoader(Location location) {
    		return null;
    	}

    	/**
    	 * Gives the compiler an instance of the JavaClassObject
    	 * so that the compiler can write the byte code into it.
    	 */
    	@Override
    	public JavaFileObject getJavaFileForOutput(Location location,
    			String className, JavaFileObject.Kind kind, FileObject sibling) {
    		return jclassObject = new JavaClassObject(className, kind);
    	}
    }

}
