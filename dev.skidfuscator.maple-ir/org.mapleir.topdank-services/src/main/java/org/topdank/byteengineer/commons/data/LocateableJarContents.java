package org.topdank.byteengineer.commons.data;

import java.net.URL;

import org.mapleir.asm.ClassNode;

public class LocateableJarContents extends JarContents {

	private final URL[] jarUrls;

	public LocateableJarContents(URL... jarUrls) {
		super();
		this.jarUrls = jarUrls;
	}

	public LocateableJarContents(DataContainer<JarClassData> classContents, DataContainer<JarResource> resourceContents, URL... jarUrls) {
		super(classContents, null, resourceContents);
		this.jarUrls = jarUrls;
	}

	public URL[] getJarUrls() {
		return jarUrls;
	}
}
