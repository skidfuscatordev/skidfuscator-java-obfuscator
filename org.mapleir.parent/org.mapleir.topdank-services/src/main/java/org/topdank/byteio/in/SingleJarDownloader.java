package org.topdank.byteio.in;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.asm.ASMFactory;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteengineer.commons.data.JarResource;
import org.topdank.byteengineer.commons.data.LocateableJarContents;

import com.google.common.io.ByteStreams;

public class SingleJarDownloader<C extends ClassNode> extends AbstractJarDownloader<C> {

	protected final JarInfo jarInfo;
	protected final int targetRelease;

	public SingleJarDownloader(JarInfo jarInfo) {
		this(jarInfo, Runtime.version().feature());
	}

	public SingleJarDownloader(JarInfo jarInfo, int targetRelease) {
		super(); this.jarInfo = jarInfo; this.targetRelease = targetRelease;
	}

	public SingleJarDownloader(ASMFactory<C> factory, JarInfo jarInfo) {
		super(factory);
		this.jarInfo = jarInfo;
		this.targetRelease = Runtime.version().feature();
	}

	@Override
	public void download() throws IOException {
		URL url = null;
		JarURLConnection connection = (JarURLConnection) (url = new URL(jarInfo.formattedURL())).openConnection();
		connection.setUseCaches(false);
		try (JarFile jarFile = connection.getJarFile()) {
		Enumeration<JarEntry> entries = jarFile.entries();
		contents = new LocateableJarContents(url);
		
		Map<String, ClassNode> map = new HashMap<>();
		
		Map<String, JarEntry> selected = new TreeMap<>();
		Map<String, Integer> versions = new HashMap<>();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement(); String logicalName = entry.getName(); int version = 0;
			if (logicalName.startsWith("META-INF/versions/")) {
				if (!jarFile.isMultiRelease()) continue;
				String[] parts = logicalName.substring("META-INF/versions/".length()).split("/", 2);
				if (parts.length != 2) continue;
				try { version = Integer.parseInt(parts[0]); } catch (NumberFormatException invalid) { continue; }
				if (version < 9 || version > targetRelease) continue;
				logicalName = parts[1];
			}
			if (logicalName.equals("module-info.class")) continue;
			Integer previous = versions.get(logicalName);
			if (previous != null && previous == version) throw new IOException("Duplicate JAR entry: " + entry.getName());
			if (previous == null || version > previous) { selected.put(logicalName,entry); versions.put(logicalName,version); }
		}
		for (Map.Entry<String, JarEntry> selection : selected.entrySet()) {
			JarEntry entry = selection.getValue(); String logicalName = selection.getKey();
			byte[] bytes = read(jarFile.getInputStream(entry));
			if (logicalName.endsWith(".class")) {
				C cn = factory.create(bytes, logicalName);
				if(!map.containsKey(cn.getName())) {
					contents.getClassContents().add(new JarClassData(
							logicalName,
							bytes,
							cn
					));
					map.put(cn.getName(), cn);
				} else {
					throw new IllegalStateException("duplicate: " + cn.getName());
				}
				
				//if(cn.name.equals("org/xmlpull/v1/XmlPullParser")) {
				//	System.out.println("SingleJarDownloader.download() " +entry.getName() + " " + bytes.length);
				//}
			} else {
				JarResource resource = new JarResource(logicalName, bytes);
				contents.getResourceContents().add(resource);
			}
		}
		}
	}

	private byte[] read(InputStream inputStream) throws IOException {
		try (InputStream input = inputStream) { return ByteStreams.toByteArray(input); }
	}
}
