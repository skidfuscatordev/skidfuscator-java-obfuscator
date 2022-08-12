package org.topdank.byteio.out;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarOutputStream;

import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarClassData;

public interface JarDumper {
	
	void dump(File file) throws IOException;
	
	int dumpClass(JarOutputStream out, JarClassData classData) throws IOException;
	
	int dumpResource(JarOutputStream out, String name, byte[] file) throws IOException;
} //?
