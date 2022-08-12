package dev.skidfuscator.inflator;

import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@UtilityClass
public class SkidInflator {
    private final int ARRAY_SIZE = 1024 * 8;

    public byte[] compress(byte[] data) throws IOException {
        final Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        final byte[] buffer = new byte[ARRAY_SIZE];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        deflater.end();

        final byte[] output = outputStream.toByteArray();

        return output;
    }

    public byte[] decompress(byte[] data) throws IOException, DataFormatException {
        final Inflater inflater = new Inflater();
        inflater.setInput(data);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        final byte[] buffer = new byte[ARRAY_SIZE];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        inflater.end();

        final byte[] output = outputStream.toByteArray();

        return output;
    }
}
