package org.topdank.byteio.out;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarOutputStream;

import org.mapleir.asm.ClassNode;

public abstract interface JarDumper {
	
	public void dump(File file) throws IOException;
	
	public abstract int dumpClass(JarOutputStream out, String name, ClassNode cn) throws IOException;
	
	public abstract int dumpResource(JarOutputStream out, String name, byte[] file) throws IOException;
} //?
