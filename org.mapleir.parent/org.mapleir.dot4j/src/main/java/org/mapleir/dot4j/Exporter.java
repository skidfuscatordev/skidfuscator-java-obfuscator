package org.mapleir.dot4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Serialiser;

public class Exporter {
	private static final File DOT_EXECUTABLE = new File("dot/dot.exe");

	private final String src;
	
	private Exporter(String src) {
		this.src = src;
	}
	
	public static Exporter fromString(String src) {
		return new Exporter(src);
	}
	
	public static Exporter fromFile(File src) throws IOException {
		try(InputStream is = new FileInputStream(src)) {
			return fromString(readStream(is));
		}
	}
	
	public static Exporter fromGraph(DotGraph graph) {
		return fromString(new Serialiser(graph).serialise());
	}
	
	public void export(File file) throws IOException {
		Path tempDirPath = Files.createTempDirectory(getTempDirectory().toPath(), null);
		File dotFile = new File(tempDirPath.toString(), "graphsrc.dot");
		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(dotFile), StandardCharsets.UTF_8))) {
			bw.write(src);
		}
		
		String[] args;
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) // windows
			args = new String[] { '"' + DOT_EXECUTABLE.getAbsolutePath() + '"', "-Tpng", '"' + dotFile.getAbsolutePath() + '"', "-o", '"' + file.getAbsolutePath() + '"' };
		else // linux
			args = new String[] { "dot", "-Tpng", dotFile.getAbsolutePath(), "-o", file.getAbsolutePath() };
		ProcessBuilder builder = new ProcessBuilder(args);
		builder.redirectError(ProcessBuilder.Redirect.INHERIT);
		Process process = builder.start();
		builder.redirectError(ProcessBuilder.Redirect.INHERIT);
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}
	
	private File getTempDirectory() {
		File tempDir = new File(String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, "graphsrc"));
		if(!tempDir.exists()) {
			tempDir.mkdir();
		}
		return tempDir;
	}
	
    private static String readStream(InputStream in) throws IOException {
        final byte[] buf = new byte[in.available()];
        int read, total = 0;
        while ((read = in.read(buf, total, Math.min(100000, buf.length - total))) > 0) {
            total += read;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }
}
