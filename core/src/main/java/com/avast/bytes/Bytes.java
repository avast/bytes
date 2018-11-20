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
     * Note: Implementation was copied from avast.utils.ByteUtils
     * @return hex string representation
     */
    default String toHexString() {
        byte[] arr = toByteArray();
        char[] result = new char[arr.length * 2];
        for (int i = 0; i < arr.length; ++i) {
            result[i * 2] = Constants.HEX_ARRAY[(arr[i] >> 4) & 0xF];
            result[i * 2 + 1] = Constants.HEX_ARRAY[(arr[i] & 0xF)];
        }
        return new String(result);
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
     */
    static Bytes copyFromHex(String hexString) {
        final int length = hexString.length();
        if (length % 2 != 0) {
            throw new IllegalArgumentException("HexString needs to be even-length: " + hexString);
        }
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int high = Character.getNumericValue(hexString.charAt(i));
            int low = Character.getNumericValue(hexString.charAt(i + 1));
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("HexString contains illegal characters: " + hexString);
            }
            result[i / 2] = (byte) (high * 16 + low);
        }
        return copyFrom(result);
    }


}
