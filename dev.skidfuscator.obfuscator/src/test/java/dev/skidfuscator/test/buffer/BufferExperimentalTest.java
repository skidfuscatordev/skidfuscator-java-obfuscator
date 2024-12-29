package dev.skidfuscator.test.buffer;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public class BufferExperimentalTest {

    @Test
    public void testCharBuffer() {
        final String test = "Hello, World!";
        final char[] buffer = test.toCharArray();
        final CharBuffer charBuffer = CharBuffer.wrap(buffer);

        assert charBuffer.toString().equals(test) : "CharBuffer failed: " + charBuffer.toString() + " != " + test;
    }

    @Test
    public void testByteBuffer() {
        final String test = "Hello, World!";
        final char[] buffer = test.toCharArray();
        final byte[] byteBuffer = test.getBytes(StandardCharsets.UTF_16BE);
        final CharBuffer charBuffer = ByteBuffer.wrap(byteBuffer).asCharBuffer();

        assert charBuffer.toString().equals(test) : "CharBuffer failed: " + charBuffer.toString() + " != " + test;
    }

    @Test
    public void testByteBufferEncryption() {

    }

    private static String storage;

    public static String decrypt(byte[] index, int key) {
        // decrypt the length from the input buffer
        final int length = ((index[0] & 0xFF) << 24) | ((index[1] & 0xFF) << 16) | ((index[2] & 0xFF) << 8) | (index[3] & 0xFF);
        final int size = ((index[4] & 0xFF) << 24) | ((index[5] & 0xFF) << 16) | ((index[6] & 0xFF) << 8) | (index[7] & 0xFF);

        // Super simple converting our integer to string, and getting bytes.
        final byte[] keyBytes = Integer.toString(key).getBytes();
        final byte[] input = storage.substring(size, size + length).getBytes(StandardCharsets.UTF_16BE);
        final byte[] keys = { 15, 24, 9};
        // Super simple XOR
        for (int i = 0; i < input.length; i++) {
            input[i] ^= keyBytes[i % keyBytes.length];
            input[i] ^= keys[i % keys.length];
        }

        // Base64 encode it for testing
        return new String(input, StandardCharsets.UTF_16);
    }
}
