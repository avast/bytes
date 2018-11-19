package com.avast.bytes;

import com.avast.bytes.jdk.ByteArrayBytes;
import com.avast.bytes.jdk.ByteBufferBytes;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Represents an immutable sequence (or string) of bytes.
 * <p>
 * There are multiple implementations based on different backing structures.
 * The core module provides implementation backed by byte array and {@link java.nio.ByteBuffer}.
 * <p>
 * The encapsulated data can be read either by direct access to absolute index ({@link #byteAt(int)}
 * or via {@link java.io.InputStream} (see {@link #newInputStream()}.
 */
public interface Bytes {

    /**
     * Hidden class with private fields and methods. The only reason for an existence of this class is
     * that Interfaces in jdk8 don't support private methods and fields
     */
    class Hidden {
        private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

        private static int hexToBin(char ch) {
            if ('0' <= ch && ch <= '9') {
                return ch - '0';
            }
            if ('A' <= ch && ch <= 'F') {
                return ch - 'A' + 10;
            }
            if ('a' <= ch && ch <= 'f') {
                return ch - 'a' + 10;
            }
            return -1;
        }
    }

    /**
     * Empty {@link Bytes}.
     */
    static Bytes empty() {
        return ByteArrayBytes.EMPTY;
    }

    /**
     * Returns number of bytes in this {@link Bytes}.
     *
     * @return number of bytes in this {@link Bytes}
     */
    int size();

    /**
     * Returns {@code true} if this {@link Bytes} is empty.
     *
     * @return {@code true} if this {@link Bytes} is empty
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns byte at the specified index.
     *
     * @param index (zero-based) index
     * @return byte at the specified index
     * @throws IndexOutOfBoundsException if the specified index is invalid
     */
    byte byteAt(int index);

    /**
     * Converts this {@link Bytes} to array of bytes.
     * This operation always copying and allocation of a new byte array.
     *
     * @return new array containing data of this {@link Bytes}.
     */
    byte[] toByteArray();

    /**
     * Converts this {@link Bytes} to read-only. {@link java.nio.ByteBuffer}.
     * This operation will involve allocation and copying unless the implementation
     * is backed by {@link java.nio.ByteBuffer}.
     *
     * @return read-only {@link java.nio.ByteBuffer} containing data of this {@link Bytes}
     */
    ByteBuffer toReadOnlyByteBuffer();

    /**
     * Converts this {@link Bytes} to {@link String} in the specified charset.
     * This operation has the same characteristics in terms of allocation and copying
     * as {@link #toByteArray()}
     *
     * @param charset charset to be used when decoding the bytes
     * @return {@link String} decoded from the bytes
     */
    String toString(Charset charset);

    /**
     * Converts content of this {@link Bytes} to hex string. It uses data provided by `toByteArray`, which means it probably
     * creates a copy of the data (but that depends on the implementation of the `toByteArray`).
     *
     * Note: Implementation was copied from javax.xml.bind package because the package is no longer supported in jdk11
     * @return hex string representation
     */
    default String toHexString() {
        byte[] data = toByteArray();
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(Hidden.hexCode[(b >> 4) & 0xF]);
            r.append(Hidden.hexCode[(b & 0xF)]);
        }
        return r.toString().toLowerCase();
    }

    /**
     * Convenience method. Effectively the same as {@code toString(StandardCharsets.UTF_8)}.
     */
    default String toStringUtf8() {
        return toString(StandardCharsets.UTF_8);
    }

    /**
     * Returns new {@link InputStream} that reads data contained in this {@link Bytes}.
     *
     * @return new {@link InputStream} that reads data contained in this {@link Bytes}
     */
    InputStream newInputStream();

    /**
     * Returns {@link Bytes} that is a view into this one. The
     * view begins at the specified {@code beginIndex} and
     * extends to the byte at index {@code endIndex - 1}.
     * Thus the length of the view is {@code endIndex-beginIndex}.
     *
     * @param beginIndex the beginning index, inclusive
     * @param endIndex   the ending index, exclusive
     * @return the specified view
     * @throws IndexOutOfBoundsException if the
     *                                   {@code beginIndex} is negative, or
     *                                   {@code endIndex} is larger than the size of
     *                                   this {@code Bytes} object, or
     *                                   {@code beginIndex} is larger than
     *                                   {@code endIndex}.
     */
    Bytes view(int beginIndex, int endIndex);

    /**
     * Concatenates this and another instance of {@link Bytes}.
     *
     * @param other Bytes to concatenate with this
     * @return concatenated Bytes
     */
    default Bytes concat(Bytes other) {
        return ConcatBytes.wrap(this, other);
    }

    /**
     * Allows creating new {@link Bytes} by writing to an {@link OutputStream}.
     * After the data has been written, call {@link #toBytes()} to obtain {@link Bytes}.
     *
     * Unless explicitly stated otherwise, implementations are NOT thread-safe.
     */
    abstract class BuilderStream extends OutputStream {

        /**
         * Returns new {@link Bytes} containing the data written to this {@link OutputStream}.
         *
         * @return new {@link Bytes} containing the data written to this {@link OutputStream}
         */
        public abstract Bytes toBytes();

    }

    /**
     * Convenience method for creating {@link Bytes} from byte array.
     * Equivalent to {@code ByteArrayBytes.copyFrom(bytes)}.
     */
    static Bytes copyFrom(byte[] bytes) {
        return ByteArrayBytes.copyFrom(bytes);
    }

    /**
     * Convenience method for creating {@link Bytes} from byte array.
     * Equivalent to {@code ByteArrayBytes.copyFrom(bytes, off, len)}.
     */
    static Bytes copyFrom(byte[] bytes, int off, int len) {
        return ByteArrayBytes.copyFrom(bytes, off, len);
    }

    /**
     * Convenience method for creating {@link Bytes} from {@link ByteBuffer}.
     * Equivalent to {@code ByteBufferBytes.copyFrom(bytes)}.
     */
    static Bytes copyFrom(ByteBuffer bytes) {
        return ByteBufferBytes.copyFrom(bytes);
    }

    /**
     * Convenience method for creating {@link Bytes} from {@link String} in given {@link Charset}.
     * Equivalent to {@code Bytes.copyFrom(string.getBytes(charset))}.
     */
    static Bytes copyFrom(String string, Charset charset) {
        return copyFrom(string.getBytes(charset));
    }

    /**
     * Convenience method for creating {@link Bytes} from UTF-8 {@link String}.
     */
    static Bytes copyFromUtf8(String string) {
        return copyFrom(string, StandardCharsets.UTF_8);
    }

    /**
     * Convenience method for creating {@link Bytes} from HEX {@link String}.
     * Note: Implementation was copied from javax.xml.bind package because the package is no longer supported in jdk11
     */
    static Bytes copyFromHex(String hexString) {
        final int len = hexString.length();
        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + hexString);
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int h = Hidden.hexToBin(hexString.charAt(i));
            int l = Hidden.hexToBin(hexString.charAt(i + 1));
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException("contains illegal character for hexBinary: " + hexString);
            }
            out[i / 2] = (byte) (h * 16 + l);
        }
        return copyFrom(out);
    }


}
