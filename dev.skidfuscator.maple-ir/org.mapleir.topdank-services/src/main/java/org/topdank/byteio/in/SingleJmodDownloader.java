package org.topdank.byteio.in;

import com.google.common.io.ByteStreams;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.asm.ASMFactory;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteengineer.commons.data.JarResource;
import org.topdank.byteengineer.commons.data.LocateableJarContents;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SingleJmodDownloader<C extends ClassNode> extends AbstractJarDownloader<C> {

	protected final JarInfo jarInfo;

	public SingleJmodDownloader(JarInfo jarInfo) {
		super();
		this.jarInfo = jarInfo;
	}

	public SingleJmodDownloader(ASMFactory<C> factory, JarInfo jarInfo) {
		super(factory);
		this.jarInfo = jarInfo;
	}

	@Override
	public void download() throws IOException {
		File file;
		try (ZipFile jarFile = new ZipFile((file = new File(jarInfo.getPath())))) {
			Enumeration<? extends ZipEntry> entries = jarFile.entries();
			contents = new LocateableJarContents(file.toURI().toURL());

			Map<String, ClassNode> map = new HashMap<>();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				byte[] bytes = read(jarFile.getInputStream(entry));
				if (entry.getName().endsWith(".class")) {
					C cn = factory.create(bytes, entry.getName());
					if(!map.containsKey(cn.getName())) {
						contents.getClassContents().add(new JarClassData(
								entry.getName(),
								bytes,
								cn
						));
					} else {
						throw new IllegalStateException("duplicate: " + cn.getName());
					}

					//if(cn.name.equals("org/xmlpull/v1/XmlPullParser")) {
					//	System.out.println("SingleJarDownloader.download() " +entry.getName() + " " + bytes.length);
					//}
				} else {
					JarResource resource = new JarResource(entry.getName(), bytes);
					contents.getResourceContents().add(resource);
				}
			}
		}
	}

	private byte[] read(InputStream inputStream) throws IOException {
		return ByteStreams.toByteArray(inputStream);
	}
}
