package dev.skidfuscator.test.ssvm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.ClassParseResult;
import org.mapleir.app.service.LocateableClassNode;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class SkidfuscatorBootClassLoader implements BootClassLoader {
	private final Skidfuscator skidfuscator;

	public SkidfuscatorBootClassLoader(Skidfuscator workspace) {
		this.skidfuscator = workspace;
	}

	@Override
	public ClassParseResult findBootClass(String name) {
		ClassNode node = skidfuscator.getClassSource().findClassNode(name);

		if (node == null) {
			return null;
		}

		org.objectweb.asm.tree.ClassNode classNode = node.node;

		final ClassWriter writer = new ClassWriter(0);
		classNode.accept(writer);
		final byte[] data = writer.toByteArray();

		ClassReader reader = new ClassReader(data);
		org.objectweb.asm.tree.ClassNode readNode = new org.objectweb.asm.tree.ClassNode();
		reader.accept(readNode, 0);
		return new ClassParseResult(reader, classNode);
	}
}
