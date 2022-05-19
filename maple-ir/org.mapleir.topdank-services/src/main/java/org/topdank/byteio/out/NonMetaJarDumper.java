package org.topdank.byteio.out;

import java.io.IOException;
import java.util.jar.JarOutputStream;

import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarContents;

public class NonMetaJarDumper extends CompleteJarDumper {

	public NonMetaJarDumper(JarContents contents) {
		super(contents);
	}

	@Override
	public int dumpResource(JarOutputStream out, String name, byte[] file) throws IOException {
		if (name.contains("META-INF"))
			return 0;
		return super.dumpResource(out, name, file);
	}
} //?
