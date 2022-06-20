package org.topdank.byteio.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class IOUtil {
	
	public static byte[] read(InputStream in) throws IOException {
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = in.read(buffer)) != -1)
			byteArrayOut.write(buffer, 0, bytesRead);
		byteArrayOut.close();
		return byteArrayOut.toByteArray();
	}
}