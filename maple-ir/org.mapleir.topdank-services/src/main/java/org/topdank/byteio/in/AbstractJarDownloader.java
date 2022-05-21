package org.topdank.byteio.in;

import java.io.IOException;

import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.asm.ASMFactory;
import org.topdank.byteengineer.commons.asm.DefaultASMFactory;
import org.topdank.byteengineer.commons.data.LocateableJarContents;

public abstract class AbstractJarDownloader<C extends ClassNode> {

	protected final ASMFactory<C> factory;
	protected LocateableJarContents contents;

	@SuppressWarnings("unchecked")
	public AbstractJarDownloader() {
		this((ASMFactory<C>) new DefaultASMFactory());
	}

	public AbstractJarDownloader(ASMFactory<C> factory) {
		this.factory = factory;
	}

	public abstract void download() throws IOException;

	public LocateableJarContents getJarContents() {
		return contents;
	}
}
