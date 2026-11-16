package dev.skidfuscator.obfuscator.phantom.jphantom;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.util.ProgressUtil;
import dev.skidfuscator.obfuscator.util.progress.ProgressWrapper;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;
import org.topdank.byteengineer.commons.data.JarResource;
import org.topdank.byteio.out.JarDumper;
import org.topdank.byteio.util.Debug;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipException;

/**
 * Dumps ClassNodes and JarResources back into a file on the local system.
 * Todo: make this extend CompleteJarDumper?
 *
 * @author Bibl
 */
public class PhantomResolvingJarDumper implements JarDumper {

	private final Skidfuscator skidfuscator;
	private final JarContents contents;
	private final ApplicationClassSource source;
	/**
	 * Creates a new JarDumper.
	 *
	 * @param contents Contents of jar.
	 */
	public PhantomResolvingJarDumper(Skidfuscator skidfuscator, JarContents contents, ApplicationClassSource source) {
		this.skidfuscator = skidfuscator;
		this.contents = contents;
		this.source = source;
	}

	/**
	 * Dumps the jars contents.
	 *
	 * @param file File to dump it to.
	 */
	@Override
	public void dump(File file) throws IOException {
		Path target = file.toPath().toAbsolutePath();
		List<JarClassData> classes = new ArrayList<>(contents.getClassContents());
		List<JarResource> resources = new ArrayList<>(contents.getResourceContents());
		classes.sort(Comparator.comparing(JarClassData::getName));
		resources.sort(Comparator.comparing(JarResource::getName));
		Path temporary = Files.createTempFile(target.getParent(), ".skidfuscator-", ".tmp");
		try {
			try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(temporary));
				 ProgressWrapper progress = ProgressUtil.progressCheck(classes.size() + resources.size(),
						 "Outputting " + classes.size() + " classes to " + file.getPath())) {
				for (JarClassData data : classes) {
					if (dumpClass(out, data) != 1) {
						throw new IOException("Class was not written: " + data.getName());
					}
					progress.tick();
				}
				for (JarResource resource : resources) {
					if (dumpResource(out, resource.getName(), resource.getData()) != 1) {
						throw new IOException("Resource was not written: " + resource.getName());
					}
					progress.tick();
				}
			}
			validateJar(temporary, classes.size() + resources.size());
			if (skidfuscator != null && skidfuscator.getRunReport() != null) skidfuscator.getRunReport().outputBudget(temporary);
			// If atomic replacement is unsupported, fail and preserve the previous JAR.
			Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static void validateJar(Path path, int expectedEntries) throws IOException {
		try (JarFile jar = new JarFile(path.toFile())) {
			if (jar.size() != expectedEntries) {
				throw new IOException("Incomplete output JAR: expected " + expectedEntries + " entries, found " + jar.size());
			}
			Enumeration<JarEntry> entries = jar.entries();
			byte[] buffer = new byte[8192];
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				boolean isClass = entry.getName().endsWith(".class") || entry.getName().endsWith(".class/");
				CRC32 crc = new CRC32();
				try (InputStream input = jar.getInputStream(entry)) {
					if (isClass) {
						byte[] bytes = input.readAllBytes();
						crc.update(bytes);
						try {
							new ClassReader(bytes).accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
						} catch (RuntimeException e) {
							throw new IOException("Invalid output class: " + entry.getName(), e);
						}
					} else {
						int length;
						while ((length = input.read(buffer)) != -1) crc.update(buffer, 0, length);
					}
				}
				if (crc.getValue() != entry.getCrc()) {
					throw new IOException("Output checksum mismatch: " + entry.getName());
				}
			}
		}
	}

	@Override
	public int dumpClass(JarOutputStream out, JarClassData classData) throws IOException {
		ClassNode cn = classData.getClassNode();
		try {
			byte[] bytes;
			if (skidfuscator.getExemptAnalysis().isExempt(cn)) {
				bytes = classData.getData();
			} else {
				ClassWriter writer = buildClassWriter(source.getClassTree(), ClassWriter.COMPUTE_FRAMES);
				cn.node.accept(writer);
				bytes = writer.toByteArray();
			}
			JarEntry entry = new JarEntry(cn.getName() + ".class"); entry.setTime(0);
			out.putNextEntry(entry);
			out.write(bytes);
			out.closeEntry();
			return 1;
		} catch (Exception e) {
			throw new IOException("Failed to emit class " + cn.getName(), e);
		}
	}

	public ClassWriter buildClassWriter(ClassTree tree, int flags) {
		final ClassWriter writer = new ClassWriter(flags) {

			// this method in ClassWriter uses the systemclassloader as
			// a stream location to load the super class, however, most of
			// the time the class is loaded/read and parsed by us so it
			// isn't defined in the system classloader. in certain cases
			// we may not even want it to be loaded/resolved and we can
			// bypass this by implementing the hierarchy scanning algorithm
			// with ClassNodes rather than Classes.
			@Override
			protected String getCommonSuperClass(String type1, String type2) {
				ClassNode ccn = source.findClassNode(type1);
				ClassNode dcn = source.findClassNode(type2);

				boolean debug = false;

				if(ccn == null) {
					ClassNode c;
					try {
						final ClassReader reader = new ClassReader(type1);
						final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
						reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

						c = new SkidClassNode(node, skidfuscator);
						skidfuscator.getClassSource().getClassTree().addVertex(c);
					} catch (IOException e) {
						throw new IllegalStateException("Cannot resolve frame type " + type1, e);
					}

					ccn = c;
				}

				if(dcn == null) {
					ClassNode c;
					try {
						final ClassReader reader = new ClassReader(type2);
						final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
						reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

						c = new SkidClassNode(node, skidfuscator);
						skidfuscator.getClassSource().getClassTree().addVertex(c);
					} catch (IOException e) {
						throw new IllegalStateException("Cannot resolve frame type " + type2, e);
					}

					dcn = c;
					// classTree.build(c);
					// return getCommonSuperClass(type1, type2);
				}

				if (debug) {
					Collection<ClassNode> c = tree.getAllParents(ccn);
					Collection<ClassNode> d = tree.getAllParents(dcn);

					for (ClassNode classNode : c) {
						System.out.println("1: " + classNode.getDisplayName());
					}

					for (ClassNode classNode : d) {
						System.out.println("2: " + classNode.getDisplayName());
					}
				}

				if (true)
					return skidfuscator
							.getClassSource()
							.getClassTree()
							.getCommonAncestor(Arrays.asList(ccn, dcn))
							.iterator()
							.next()
							.getName();

				{
					throw new IllegalStateException("Could not find common class type between " + Arrays.toString(new Object[]{ccn.getDisplayName(), dcn.getDisplayName()}));
				}

				/*if(Modifier.isInterface(ccn.node.access) || Modifier.isInterface(dcn.node.access)) {
					// enums as well?
					return "java/lang/Object";
				} else {
					do {
						ClassNode nccn = source.findClassNode(ccn.node.superName);
						if(nccn == null)
							break;
						ccn = nccn;
						c = tree.getAllParents(ccn);
					} while(!c.contains(dcn));
					return ccn.getName();
				}*/
			}
		};

		/*try {
			final Field field = ClassWriter.class.getDeclaredField("compute");
			field.setAccessible(true);
			field.set(writer, 2);
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		return writer;
	}

	/**
	 * Writes a resource to the Jar.
	 *
	 * @param out The {@link JarOutputStream}.
	 * @param name The name of the file.
	 * @param file File as a byte[].
	 * @throws IOException If there is a write error.
	 * @return The amount of things dumped, 1 or if you're not dumping it 0.
	 */
	@Override
	public int dumpResource(JarOutputStream out, String name, byte[] file) throws IOException {
		JarEntry entry = new JarEntry(name);
		entry.setTime(0);
		if (name.endsWith(".jar")){
			entry.setMethod(JarEntry.STORED);
			entry.setSize(file.length);
			CRC32 crc = new CRC32();
			crc.update(file, 0, file.length);
			entry.setCrc(crc.getValue());
		}
		out.putNextEntry(entry);
		out.write(file);
		return 1;
	}
}
