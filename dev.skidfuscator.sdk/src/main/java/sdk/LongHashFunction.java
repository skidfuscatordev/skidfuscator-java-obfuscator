/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sdk;

import sun.nio.ch.DirectBuffer;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static sdk.CharSequenceAccess.nativeCharSequenceAccess;
import static sdk.UnsafeAccess.*;
import static sdk.Util.*;

/**
 * Hash function producing {@code long}-valued result from byte sequences of any length and
 * a plenty of different sources which "feels like byte sequences". Except {@link
 * #hashBytes(byte[])}, {@link #hashBytes(ByteBuffer)} (with their "sliced" versions) and
 * {@link #hashMemory(long, long)} methods, which actually accept byte sequences, notion of byte
 * sequence is defined as follows:
 * <ul>
 *     <li>For methods accepting arrays of Java primitives, {@code String}s and
 *     {@code StringBuilder}s, byte sequence is how the input's bytes are actually lay in memory.
 *     </li>
 *     <li>For methods accepting single primitive values, byte sequence is how this primitive
 *     would be put into memory with {@link ByteOrder#nativeOrder() native} byte order, or
 *     equivalently, {@code hashXxx(primitive)} has always the same result as {@code
 *     hashXxxs(new xxx[] {primitive})}, where "xxx" is any Java primitive type name.</li>
 *     <li>For {@link #hash(Object, Access, long, long)} method byte sequence abstraction
 *     is defined by the given {@link Access} strategy to the given object.</li>
 * </ul>
 *
 * <p>Hash function implementation could either produce equal results for equal input on platforms
 * with different {@link ByteOrder}, favoring one byte order in terms of performance, or different
 * results, but performing equally well. This choice should be explicitly documented for all
 * {@code LongHashFunction} implementations.
 *
 * <h2>Subclassing</h2>
 * To implement a specific hash function algorithm, this class should be subclassed. Only methods
 * that accept single primitives, {@link #hashVoid()}, and {@link #hash(Object, Access, long, long)}
 * should be implemented; others have default implementations which ultimately delegate to the
 * {@link #hash(Object, Access, long, long)} abstract method.
 *
 * <p>Notes about how exactly methods with default implementations are implemented in doc comments
 * are given for information and could be changed at any moment. However, it could hardly cause
 * any issues with subclassing, except probably little performance degradation. Methods documented
 * as "shortcuts" could either delegate to the referenced method or delegate directly to the method
 * to which the referenced method delegates.
 *
 * <p>{@code LongHashFunction} implementations shouldn't assume that {@code Access} strategies
 * do defensive checks and access only bytes within the requested range.
 */
public abstract class LongHashFunction implements Serializable {
    private static final long serialVersionUID = 0L;
    /**
     * Returns a hash function implementing the <a href="https://github.com/Cyan4973/xxHash">XXH3 64bit
     * algorithm</a> without a seed value (0 is used as default seed value). This implementation
     * produces equal results for equal input on platforms with different {@link
     * ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @return a {@code LongHashFunction} implementing the XXH3 64-bit algorithm without a seed value
     * @see #xx3(long)
     */
    public static LongHashFunction xx3() {
        return XXH3.asLongHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing the <a href="https://github.com/Cyan4973/xxHash">XXH3 64bit
     * algorithm</a> with the given seed value. This implementation produces equal results for equal
     * input on platforms with different {@link ByteOrder}, but is slower on big-endian platforms
     * than on little-endian.
     *
     * @param seed the seed value to be used for hashing
     * @return a {@code LongHashFunction} implementing the XXH3 64-bit algorithm with the given seed value
     * @see #xx3()
     */
    public static LongHashFunction xx3(final long seed) {
        return XXH3.asLongHashFunctionWithSeed(seed);
    }

    /**
     * Returns a hash function implementing the <a href="https://github.com/Cyan4973/xxHash">XXH128 low
     * 64bit algorithm</a> without a seed value (0 is used as default seed value). This
     * implementation produces equal results for equal input on platforms with different {@link
     * ByteOrder}, but is slower on big-endian platforms than on little-endian.
     *
     * @return a {@code LongHashFunction} implementing the XXH128 low 64bit algorithm without a seed value
     * @see #xx128low(long)
     */
    public static LongHashFunction xx128low() {
        return XXH3.asLongTupleLowHashFunctionWithoutSeed();
    }

    /**
     * Returns a hash function implementing the <a href="https://github.com/Cyan4973/xxHash">XXH128 low
     * 64bit algorithm</a> with the given seed value. This implementation produces equal results for
     * equal input on platforms with different {@link ByteOrder}, but is slower on big-endian
     * platforms than on little-endian.
     *
     * @param seed the seed value to be used for hashing
     * @return a {@code LongHashFunction} implementing the XXH128 low 64bit algorithm with the given seed value
     * @see #xx128low()
     */
    public static LongHashFunction xx128low(final long seed) {
        return XXH3.asLongTupleLowHashFunctionWithSeed(seed);
    }
    /**
     * Constructor for use in subclasses.
     */
    protected LongHashFunction() {
    }

    /**
     * Returns the hash code for the given {@code long} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashLong(v)} call is identical to the result of
     * {@code hashLongs(new long[] {v})} call for any {@code long} value.
 *
 * @param input the long value to be hashed
 * @return the hash code for the given long value
     */
    public abstract long hashLong(long input);

    /**
     * Returns the hash code for the given {@code int} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashInt(v)} call is identical to the result of
     * {@code hashInts(new int[] {v})} call for any {@code int} value.
 *
 * @param input the int value to be hashed
 * @return the hash code for the given int value
     */
    public abstract long hashInt(int input);

    /**
     * Returns the hash code for the given {@code short} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashShort(v)} call is identical to the result of
     * {@code hashShorts(new short[] {v})} call for any {@code short} value.
     * As a consequence, {@code hashShort(v)} call produce always the same result as {@code
     * hashChar((char) v)}.
 *
 * @param input the short value to be hashed
 * @return the hash code for the given short value
     */
    public abstract long hashShort(short input);

    /**
     * Returns the hash code for the given {@code char} value; this method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes, assuming the {@code input}
     * value is interpreted in {@linkplain ByteOrder#nativeOrder() native} byte order. For example,
     * the result of {@code hashChar(v)} call is identical to the result of
     * {@code hashChars(new char[] {v})} call for any {@code char} value.
     * As a consequence, {@code hashChar(v)} call produce always the same result as {@code
     * hashShort((short) v)}.
 *
 * @param input the char value to be hashed
 * @return the hash code for the given char value
     */
    public abstract long hashChar(char input);

    /**
     * Returns the hash code for the given {@code byte} value. This method is consistent with
     * {@code LongHashFunction} methods that accept sequences of bytes. For example, the result of
     * {@code hashByte(v)} call is identical to the result of
     * {@code hashBytes(new byte[] {v})} call for any {@code byte} value.
 *
 * @param input the byte value to be hashed
 * @return the hash code for the given byte value
     */
    public abstract long hashByte(byte input);

    /**
     * Returns the hash code for the empty (zero-length) bytes sequence,
     * for example {@code hashBytes(new byte[0])}.
 *
 * @return the hash code for the empty bytes sequence
     */
    public abstract long hashVoid();

    /**
     * Returns the hash code for {@code len} continuous bytes of the given {@code input} object,
     * starting from the given offset. The abstraction of input as ordered byte sequence and
     * "offset within the input" is defined by the given {@code access} strategy.
     *
     * <p>This method doesn't promise to throw a {@code RuntimeException} if {@code
     * [off, off + len - 1]} subsequence exceeds the bounds of the bytes sequence, defined by {@code
     * access} strategy for the given {@code input}, so use this method with caution.
     *
     * @param input  the object to read bytes from
     * @param access access which defines the abstraction of the given input
     *               as ordered byte sequence
     * @param off    offset to the first byte of the subsequence to hash
     * @param len    length of the subsequence to hash
     * @param <T>    the type of the input
     * @return hash code for the specified bytes subsequence
     */
    public abstract <T> long hash(T input, Access<T> access, long off, long len);

    private long unsafeHash(Object input, long off, long len) {
        return hash(input, UnsafeAccess.INSTANCE, off, len);
    }

    /**
     * Shortcut for {@link #hashBooleans(boolean[]) hashBooleans(new boolean[] &#123;input&#125;)}.
     * Note that this is not necessarily equal to {@code hashByte(input ? (byte) 1 : (byte) 0)},
     * because booleans could be stored differently in this JVM.
 *
 * @param input the boolean value to be hashed
 * @return the hash code for the given boolean value
     */
    public long hashBoolean(boolean input) {
        return hashByte(input ? TRUE_BYTE_VALUE : FALSE_BYTE_VALUE);
    }

    /**
     * Shortcut for {@link #hashBooleans(boolean[], int, int) hashBooleans(input, 0, input.length)}.
 *
 * @param input the boolean array to be hashed
 * @return the hash code for the given boolean array
     */
    public long hashBooleans(boolean[] input) {
        return unsafeHash(input, BOOLEAN_BASE, input.length);
    }

    /**
     * Returns the hash code for the specified subsequence of the given {@code boolean} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param input the array to read data from
     * @param off   index of the first {@code boolean} in the subsequence to hash
     * @param len   length of the subsequence to hash
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     *                                   or {@code len < 0}
     */
    public long hashBooleans(boolean[] input, int off, int len) {
        checkArrayOffs(input.length, off, len);
        return unsafeHash(input, BOOLEAN_BASE + off, len);
    }

    /**
     * Shortcut for {@link #hashBytes(byte[], int, int) hashBytes(input, 0, input.length)}.
 *
 * @param input the byte array to be hashed
 * @return the hash code for the given byte array
     */
    public long hashBytes(byte[] input) {
        return unsafeHash(input, BYTE_BASE, input.length);
    }

    /**
     * Returns the hash code for the specified subsequence of the given {@code byte} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param input the array to read bytes from
     * @param off   index of the first {@code byte} in the subsequence to hash
     * @param len   length of the subsequence to hash
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     *                                   or {@code len < 0}
     */
    public long hashBytes(byte[] input, int off, int len) {
        checkArrayOffs(input.length, off, len);
        return unsafeHash(input, BYTE_BASE + off, len);
    }

    /**
     * Shortcut for {@link #hashBytes(ByteBuffer, int, int)
     * hashBytes(input, input.position(), input.remaining())}.
 *
 * @param input the ByteBuffer to be hashed
 * @return the hash code for the given ByteBuffer
     */
    public long hashBytes(ByteBuffer input) {
        return hashByteBuffer(input, input.position(), input.remaining());
    }

    /**
     * Returns the hash code for the specified subsequence of the given {@code ByteBuffer}.
     *
     * <p>This method doesn't alter the state (mark, position, limit or order) of the given
     * {@code ByteBuffer}.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@link Access#toByteBuffer()}.
     *
     * @param input the buffer to read bytes from
     * @param off   index of the first {@code byte} in the subsequence to hash
     * @param len   length of the subsequence to hash
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.capacity()}
     *                                   or {@code len < 0}
     */
    public long hashBytes(ByteBuffer input, int off, int len) {
        checkArrayOffs(input.capacity(), off, len);
        return hashByteBuffer(input, off, len);
    }

    private long hashByteBuffer(ByteBuffer input, int off, int len) {
        if (input.hasArray()) {
            return unsafeHash(input.array(), BYTE_BASE + input.arrayOffset() + off, len);
        } else if (input instanceof DirectBuffer) {
            return unsafeHash(null, ((DirectBuffer) input).address() + off, len);
        } else {
            return hash(input, ByteBufferAccess.INSTANCE, off, len);
        }
    }

    /**
     * Returns the hash code of bytes of the wild memory from the given address. Use with caution.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param address the address of the first byte to hash
     * @param len     length of the byte sequence to hash
     * @return hash code for the specified byte sequence
     */
    public long hashMemory(long address, long len) {
        return unsafeHash(null, address, len);
    }

    /**
     * Shortcut for {@link #hashChars(char[], int, int) hashChars(input, 0, input.length)}.
 *
 * @param input the char array to be hashed
 * @return the hash code for the given char array
     */
    public long hashChars(char[] input) {
        return unsafeHash(input, CHAR_BASE, input.length * 2L);
    }

    /**
     * Returns the hash code for bytes, as they lay in memory, of the specified subsequence
     * of the given {@code char} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param input the array to read data from
     * @param off   index of the first {@code char} in the subsequence to hash
     * @param len   length of the subsequence to hash, in chars (i. e. the length of the bytes
     *              sequence to hash is {@code len * 2L})
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     *                                   or {@code len < 0}
     */
    public long hashChars(char[] input, int off, int len) {
        checkArrayOffs(input.length, off, len);
        return unsafeHash(input, CHAR_BASE + (off * 2L), len * 2L);
    }

    /**
     * Shortcut for {@link #hashChars(String, int, int) hashChars(input, 0, input.length())}.
 *
 * @param input the String to be hashed
 * @return the hash code for the given String
     */
    public long hashChars(String input) {
        return VALID_STRING_HASH.longHash(input, this, 0, input.length());
    }

    /**
     * Returns the hash code for bytes of the specified subsequence of the given {@code String}'s
     * underlying {@code char} array.
     *
     * <p>Default implementation could either delegate to {@link #hash(Object, Access, long, long)}
     * using {@link Access#toNativeCharSequence()}, or to {@link #hashChars(char[], int, int)}.
     *
     * @param input the string which bytes to hash
     * @param off   index of the first {@code char} in the subsequence to hash
     * @param len   length of the subsequence to hash, in chars (i. e. the length of the bytes
     *              sequence to hash is {@code len * 2L})
     * @return the hash code of the given {@code String}'s bytes
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length()}
     *                                   or {@code len < 0}
     */
    public long hashChars(String input, int off, int len) {
        return VALID_STRING_HASH.longHash(input, this, off, len);
    }

    /**
     * Shortcut for {@link #hashChars(StringBuilder, int, int) hashChars(input, 0, input.length())}.
 *
 * @param input the StringBuilder to be hashed
 * @return the hash code for the given StringBuilder
     */
    public long hashChars(StringBuilder input) {
        return hashNativeChars(input);
    }

    /**
     * Returns the hash code for bytes of the specified subsequence of the given
     * {@code StringBuilder}'s underlying {@code char} array.
     *
     * <p>Default implementation could either delegate to {@link #hash(Object, Access, long, long)}
     * using {@link Access#toNativeCharSequence()}, or to {@link #hashChars(char[], int, int)}.
     *
     * @param input the string builder which bytes to hash
     * @param off   index of the first {@code char} in the subsequence to hash
     * @param len   length of the subsequence to hash, in chars (i. e. the length of the bytes
     *              sequence to hash is {@code len * 2L})
     * @return the hash code of the given {@code String}'s bytes
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length()}
     *                                   or {@code len < 0}
     */
    public long hashChars(StringBuilder input, int off, int len) {
        return hashNativeChars(input, off, len);
    }

/**
 * Returns the hash code for the entire CharSequence.
 *
 * @param input the CharSequence to be hashed
 * @return the hash code for the given CharSequence
 */
    long hashNativeChars(CharSequence input) {
        return hashNativeChars(input, 0, input.length());
    }

/**
 * Returns the hash code for a subsequence of the given CharSequence.
 *
 * @param input the CharSequence to be hashed
 * @param off   the index of the first char in the subsequence
 * @param len   the length of the subsequence
 * @return the hash code for the specified subsequence of the given CharSequence
 */
    long hashNativeChars(CharSequence input, int off, int len) {
        return hash(input, nativeCharSequenceAccess(), off * 2L, len * 2L);
    }

    /**
     * Shortcut for {@link #hashShorts(short[], int, int) hashShorts(input, 0, input.length)}.
 *
 * @param input the short array to be hashed
 * @return the hash code for the given short array
     */
    public long hashShorts(short[] input) {
        return unsafeHash(input, SHORT_BASE, input.length * 2L);
    }

    /**
     * Returns the hash code for bytes, as they lay in memory, of the specified subsequence
     * of the given {@code short} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param input the array to read data from
     * @param off   index of the first {@code short} in the subsequence to hash
     * @param len   length of the subsequence to hash, in shorts (i. e. the length of the bytes
     *              sequence to hash is {@code len * 2L})
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     *                                   or {@code len < 0}
     */
    public long hashShorts(short[] input, int off, int len) {
        return unsafeHash(input, SHORT_BASE + (off * 2L), len * 2L);
    }

    /**
     * Shortcut for {@link #hashInts(int[], int, int) hashInts(input, 0, input.length)}.
 *
 * @param input the integer array to be hashed
 * @return the hash code for the given integer array
     */
    public long hashInts(int[] input) {
        return unsafeHash(input, INT_BASE, input.length * 4L);
    }

    /**
     * Returns the hash code for bytes, as they lay in memory, of the specified subsequence
     * of the given {@code int} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param input the array to read data from
     * @param off   index of the first {@code int} in the subsequence to hash
     * @param len   length of the subsequence to hash, in ints (i. e. the length of the bytes
     *              sequence to hash is {@code len * 4L})
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     *                                   or {@code len < 0}
     */
    public long hashInts(int[] input, int off, int len) {
        checkArrayOffs(input.length, off, len);
        return unsafeHash(input, INT_BASE + (off * 4L), len * 4L);
    }

    /**
     * Shortcut for {@link #hashLongs(long[], int, int) hashLongs(input, 0, input.length)}.
 *
 * @param input the long array to be hashed
 * @return the hash code for the given long array
     */
    public long hashLongs(long[] input) {
        return unsafeHash(input, LONG_BASE, input.length * 8L);
    }

    /**
     * Returns the hash code for bytes, as they lay in memory, of the specified subsequence
     * of the given {@code long} array.
     *
     * <p>Default implementation delegates to {@link #hash(Object, Access, long, long)} method
     * using {@linkplain Access#unsafe() unsafe} {@code Access}.
     *
     * @param input the array to read data from
     * @param off   index of the first {@code long} in the subsequence to hash
     * @param len   length of the subsequence to hash, in longs (i. e. the length of the bytes
     *              sequence to hash is {@code len * 8L})
     * @return hash code for the specified subsequence
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     *                                   or {@code len < 0}
     */
    public long hashLongs(long[] input, int off, int len) {
        return unsafeHash(input, LONG_BASE + (off * 8L), len * 8L);
    }
}