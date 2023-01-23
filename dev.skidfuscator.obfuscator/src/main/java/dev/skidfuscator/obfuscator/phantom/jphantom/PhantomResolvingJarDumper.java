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
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarContents;
import org.topdank.byteengineer.commons.data.JarResource;
import org.topdank.byteio.out.JarDumper;
import org.topdank.byteio.util.Debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
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
		if (file.exists())
			file.delete();
		file.createNewFile();
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(file));
		int classesDumped = 0;
		int resourcesDumped = 0;


		try (ProgressWrapper progressBar = ProgressUtil.progress(contents.getClassContents().size() + contents.getResourceContents().size())) {
			for (JarClassData cn : new LinkedList<>(contents.getClassContents())) {
				try {
					classesDumped += dumpClass(jos, cn);
				} catch (ZipException e) {
					System.out.println("\r[!] Failed to dump " + cn.getName() + "!\n");
					throw e;
				}

				contents.getClassContents().remove(cn);
				jos.flush();
				progressBar.tick();
			}

			for (JarResource res : new LinkedList<>(contents.getResourceContents())) {
				resourcesDumped += dumpResource(jos, res.getName(), res.getData());

				contents.getResourceContents().remove(res);
				progressBar.tick();
			}
		}

		if(!Debug.debugging)
			System.out.println("Dumped " + classesDumped + " classes and " + resourcesDumped + " resources to " + file.getAbsolutePath());
		jos.flush();
		jos.close();
	}

	/**
	 * Writes the {@link ClassNode} to the Jar.
	 *
	 * @param out The {@link JarOutputStream}.
	 * @param classData The {@link JarClassData}
	 * @throws IOException If there is a write error.
	 * @return The amount of things dumped, 1 or if you're not dumping it 0.
	 */
	@Override
	public int dumpClass(JarOutputStream out, JarClassData classData) throws IOException {
		ClassNode cn = classData.getClassNode();
		JarEntry entry = new JarEntry(cn.getName());
		out.putNextEntry(entry);

		if (skidfuscator.getExemptAnalysis().isExempt(cn)) {
			out.write(
					skidfuscator
					.getJarContents()
					.getClassContents()
					.namedMap()
					.get(classData.getName())
					.getData()
			);
			return 1;
		}

		ClassTree tree = source.getClassTree();
		for(MethodNode m : cn.getMethods()) {
			if(m.node.instructions.size() > 10000) {
				System.out.println("large method: " + m + " @" + m.node.instructions.size());
			}
		}

		try {
			try {
				ClassWriter writer = this.buildClassWriter(tree, ClassWriter.COMPUTE_MAXS);
				cn.node.accept(writer); // must use custom writer which overrides getCommonSuperclass
				out.write(writer.toByteArray());
			} catch (Exception e) {
				e.printStackTrace();
				ClassWriter writer = this.buildClassWriter(tree, ClassWriter.COMPUTE_MAXS);
				cn.node.accept(writer); // must use custom writer which overrides getCommonSuperclass
				out.write(writer.toByteArray());
				System.err.println("\rFailed to write " + cn.getName() + "! Writing with COMPUTE_MAXS, " +
						"which may cause runtime abnormalities\n");
			}
		} catch (Exception e) {
			System.err.println("Failed to write " + cn.getName() + "! Skipping class...");
			e.printStackTrace();
		}

		return 1;
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
						System.err.println("[FATAL] Failed to find common superclass due to failed " + type1);
						return "java/lang/Object";
					}

					ccn = c;
				}

				if(dcn == null) {
					ClassNode c;
					try {
						final ClassReader reader = new ClassReader(type1);
						final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
						reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

						c = new SkidClassNode(node, skidfuscator);
						skidfuscator.getClassSource().getClassTree().addVertex(c);
					} catch (IOException e) {
						System.err.println("[FATAL] Failed to find common superclass due to failed " + type1);
						return "java/lang/Object";
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
		out.putNextEntry(entry);
		out.write(file);
		return 1;
	}
}
