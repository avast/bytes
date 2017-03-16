/*

Parts of this implementations are copied from com.google.protobuf.ByteString.

Because of that we attach Google Protocol Buffers redistribution notice:

    Copyright 2014, Google Inc.  All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:

        * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above
    copyright notice, this list of conditions and the following disclaimer
    in the documentation and/or other materials provided with the
    distribution.
        * Neither the name of Google Inc. nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
    A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
    OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
    SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
    LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.avast.bytes.jdk;

import com.avast.bytes.AbstractBytes;
import com.avast.bytes.Bytes;
import com.avast.bytes.internal.StreamReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link com.avast.bytes.Bytes} based on an array of bytes.
 *
 * You create a new instance by using various static copy methods (if you have a different existing bytes representation),
 * or by using {@link #newBuilder(int)} and writing the bytes to the {@link java.io.OutputStream}.
 *
 */
public final class ByteArrayBytes extends AbstractBytes {

    public static final ByteArrayBytes EMPTY = new ByteArrayBytes(new byte[0]);

    private final byte[] bytes;

    private final int offset;

    private final int length;

    private ByteArrayBytes(final byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    private ByteArrayBytes(final byte[] bytes, int offset, int length) {
        if (offset < 0 || offset > bytes.length) {
            throw new IllegalArgumentException("Invalid offset: " + offset + " for array of length: " + bytes.length);
        }
        if (length < 0 || length + offset > bytes.length) {
            throw new IllegalArgumentException("Invalid length: " + length + " for array of length: " + bytes.length + " and offset: " + offset);
        }
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public byte byteAt(final int index) {
        if (index < 0 || index >= length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return bytes[offset + index];
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public byte[] toByteArray() {
        final byte[] copy = new byte[length];
        System.arraycopy(bytes, offset, copy, 0, length);
        return copy;
    }

    @Override
    public ByteBuffer toReadOnlyByteBuffer() {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, offset, length);
        return byteBuffer.asReadOnlyBuffer();
    }

    @Override
    public String toString(final Charset charset) {
        return new String(bytes, offset, length, charset);
    }

    @Override
    public InputStream newInputStream() {
        return new ByteArrayInputStream(bytes, offset, length);
    }

    @Override
    public ByteArrayBytes view(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(beginIndex);
        }
        if (endIndex > length) {
            throw new ArrayIndexOutOfBoundsException(endIndex);
        }
        int subLen = endIndex - beginIndex;
        if (subLen < 0) {
            throw new ArrayIndexOutOfBoundsException(subLen);
        }
        return new ByteArrayBytes(bytes, beginIndex + offset, endIndex - beginIndex);
    }

    public void copyTo(final byte[] target, final int offset) {
        System.arraycopy(bytes, this.offset, target, offset, this.length);
    }

    public void copyTo(ByteBuffer target) {
        target.put(bytes, offset, length);
    }

    public static ByteArrayBytes copyFrom(final byte[] bytes, final int offset,
                                          final int size) {
        final byte[] copy = new byte[size];
        System.arraycopy(bytes, offset, copy, 0, size);
        return new ByteArrayBytes(copy);
    }

    public static ByteArrayBytes copyFrom(final byte[] bytes) {
        return copyFrom(bytes, 0, bytes.length);
    }

    public static ByteArrayBytes copyFrom(final ByteBuffer bytes, final int size) {
        final byte[] copy = new byte[size];
        bytes.get(copy);
        return new ByteArrayBytes(copy);
    }

    public static ByteArrayBytes copyFrom(final ByteBuffer bytes) {
        return copyFrom(bytes, bytes.remaining());
    }

    public static ByteArrayBytes copyFrom(final String text, final Charset charset) {
        return new ByteArrayBytes(text.getBytes(charset));
    }

    public static ByteArrayBytes copyFromUtf8(final String text) {
        return new ByteArrayBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates new builder with the specified initial capacity (more bytes than this capacity can be written however).
     * @param initialCapacity initial capacity of the builder
     * @return new builder that will create {@link ByteArrayBytes}.
     */
    public static BuilderStream newBuilder(final int initialCapacity) {
        return new ByteArrayBuilder(initialCapacity);
    }

    public static BuilderStream newBuilder() {
        return newBuilder(32);
    }

    /**
     * Completely reads the given stream's bytes into a {@code Bytes}, blocking if necessary until all bytes are
     * read through to the end of the stream.
     *
     * Convenient if the size of the input stream is not known (otherwise use {@link #newBuilder(int)} with the known size and copy the data).
     *
     * <b>Performance notes:</b> The returned {@code Bytes} is
     * {@link com.avast.bytes.ConcatBytes} of {@link ByteArrayBytes} ("chunks") of the stream data.
     * The first chunk is small, with subsequent chunks each being double
     * the size, up to 8K.
     *
     * @param stream The source stream, which is read completely but not closed.
     * @return A new {@code Bytes} which is made up of chunks of various sizes, depending on the behavior of the underlying stream.
     * @throws IOException IOException is thrown if there is a problem reading the underlying stream.
     */
    public static Bytes readFrom(InputStream stream) throws IOException {
        return StreamReader.readFrom(stream, ByteArrayBytes::newBuilder);
    }

    /**
     * Copies all or a subset of bytes from {@code InputStream} to a {@code Bytes}, blocking if necessary until all required bytes are read
     * through.
     *
     * @param stream The source stream, which is read (but not closed).
     * @param offset Number of bytes to skip from input before copying
     * @param len    Number of bytes to read (starting from specified offset)
     * @return A new {@code Bytes} which is made up of read bytes.
     * @throws IOException IOException is thrown if there is a problem reading the underlying stream.
     */
    public static Bytes readFrom(InputStream stream, int offset, int len) throws IOException {
        return StreamReader.readSliceFrom(stream, offset, len, ByteArrayBytes::newBuilder);
    }

    private static final class ByteArrayBuilder extends BuilderStream {

        // we use modified ByteArrayOutputStream
        // that allows us to wrap bytes written to the stream without additional copying
        private static final class InnerStream extends ByteArrayOutputStream {

            InnerStream(int initialCapacity) {
                super(initialCapacity);
            }

            int getCount() {
                return count;
            }

            byte[] getBuffer() {
                return buf;
            }

        }

        private final InnerStream bout;

        private ByteArrayBuilder(int initialCapacity) {
            this.bout = new InnerStream(initialCapacity);
        }

        public ByteArrayBytes toBytes() {
            // it's ok to call this multiple times
            // it would create multiple ByteArrayBytes wrapping (potentially) the same array limited to the current position
            // but that is fine since the array can only be appended to (and modifications outside the view are ok)
            // or reallocated to greater size

            // we have to synchronize on ByteArrayOutputStream to read the current buffer and length atomically
            synchronized (bout) {
                return new ByteArrayBytes(bout.getBuffer(), 0, bout.getCount());
            }
        }

        @Override
        public void write(int b) throws IOException {
            bout.write(b);
        }

    }

}
