package com.avast.bytes.jdk;

import com.avast.bytes.AbstractBytes;
import com.avast.bytes.Bytes;
import com.avast.bytes.internal.StreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Implementation of {@link Bytes} backed by {@link ByteBuffer}.
 *
 * You create a new instance either by copying an existing {@link ByteBuffer} using {@link #copyFrom(ByteBuffer)}
 * or by calling {@link #newBuilder(int)} and writing the bytes to the {@link java.io.OutputStream}.
 *
 * Note that the implementation of {@link #toReadOnlyByteBuffer()} does not allocate new buffer,
 * because it just delegates to {@link ByteBuffer#asReadOnlyBuffer()}.
 */
public final class ByteBufferBytes extends AbstractBytes {

    private final ByteBuffer buffer;

    private ByteBufferBytes(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int size() {
        return buffer.remaining();
    }

    @Override
    public byte byteAt(int index) {
        return buffer.get(buffer.position() + index);
    }

    @Override
    public byte[] toByteArray() {
        byte[] dest = new byte[size()];
        // create new read-only view so that we don't have to synchronize modifying the buffer's position
        toReadOnlyByteBuffer().get(dest);
        return dest;
    }

    @Override
    public ByteBuffer toReadOnlyByteBuffer() {
        return buffer.asReadOnlyBuffer();
    }

    @Override
    public String toString(Charset charset) {
        return new String(toByteArray(), charset);
    }

    @Override
    public InputStream newInputStream() {
        return new ByteBufferInputStream(toReadOnlyByteBuffer());
    }

    @Override
    public ByteBufferBytes view(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException(String.valueOf(beginIndex));
        }
        if (endIndex > size()) {
            throw new IndexOutOfBoundsException(String.valueOf(endIndex));
        }
        int subLen = endIndex - beginIndex;
        if (subLen < 0) {
            throw new IndexOutOfBoundsException(String.valueOf(subLen));
        }

        ByteBuffer slice = buffer.slice();
        int oldPos = slice.position();
        slice.position(oldPos + beginIndex);
        slice.limit(oldPos + endIndex);
        return new ByteBufferBytes(slice);
    }

    public static ByteBufferBytes copyFrom(final ByteBuffer bytes) {
        ByteBuffer source = bytes.asReadOnlyBuffer();
        source.rewind();
        ByteBuffer dest = ByteBuffer.allocate(source.limit());
        dest.put(source);
        dest.flip();
        return new ByteBufferBytes(dest);
    }

    /**
     * Creates new builder with the specified initial capacity (more bytes than this capacity can be written however).
     * @param initialCapacity initial capacity of the builder
     * @return new builder that will create {@link ByteBufferBytes}.
     */
    public static BuilderStream newBuilder(final int initialCapacity) {
        return new ByteBufferBuilder(initialCapacity);
    }

    /**
     * Completely reads the given stream's bytes into a {@code Bytes}, blocking if necessary until all bytes are
     * read through to the end of the stream.
     *
     * Convenient if the size of the input stream is not known (otherwise use {@link #newBuilder(int)} with the known size and copy the data).
     *
     * <b>Performance notes:</b> The returned {@code Bytes} is
     * {@link com.avast.bytes.ConcatBytes} of {@link ByteBufferBytes} ("chunks") of the stream data.
     * The first chunk is small, with subsequent chunks each being double
     * the size, up to 8K.
     *
     * @param stream The source stream, which is read completely but not closed.
     * @return A new {@code Bytes} which is made up of chunks of various sizes, depending on the behavior of the underlying stream.
     * @throws IOException IOException is thrown if there is a problem reading the underlying stream.
     */
    public static Bytes readFrom(InputStream stream) throws IOException {
        return StreamReader.readFrom(stream, ByteBufferBytes::newBuilder);
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
        return StreamReader.readSliceFrom(stream, offset, len, ByteBufferBytes::newBuilder);
    }

    private static class ByteBufferInputStream extends InputStream {
        private final ByteBuffer bb;

        private ByteBufferInputStream(ByteBuffer bb) {
            this.bb = bb;
        }

        @Override
        public int available() {
            return bb.remaining();
        }

        @Override
        public int read() throws IOException {
            if (!bb.hasRemaining())
                return -1;
            return bb.get() & 0xFF; // Make sure the value is in [0..255]
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (!bb.hasRemaining())
                return -1;
            len = Math.min(len, bb.remaining());
            bb.get(bytes, off, len);
            return len;
        }
    }

    private static final class ByteBufferBuilder extends BuilderStream {

        private static final int MAX_SIZE = Integer.MAX_VALUE - 8;

        private ByteBuffer buffer;

        private ByteBufferBuilder(int initialCapacity) {
            buffer = ByteBuffer.allocate(initialCapacity);
        }

        @Override
        public ByteBufferBytes toBytes() {
            // it's ok to call this multiple times
            // every time we create new view to the underlying buffer up to the current position
            // the buffer can only be appended to or reallocated, which cannot change the existing views
            ByteBuffer wrapped = buffer.asReadOnlyBuffer();
            wrapped.flip();
            return new ByteBufferBytes(wrapped);
        }

        @Override
        public void write(int b) throws IOException {
            ensureCapacity(buffer.position() + 1);
            buffer.put((byte) b);
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            ensureCapacity(buffer.position() + length);
            buffer.put(data, offset, length);
        }

        private void ensureCapacity(int minCapacity) {
            // overflow-conscious code (see http://stackoverflow.com/questions/33147339/difference-between-if-a-b-0-and-if-a-b)
            if (minCapacity - buffer.capacity() > 0) {
                grow(minCapacity);
            }
        }

        private void grow(int minCapacity) {
            // overflow-conscious code
            int oldCapacity = buffer.capacity();
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_SIZE > 0) {
                newCapacity = hugeCapacity(minCapacity);
            }

            ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }

        private static int hugeCapacity(int minCapacity) {
            if (minCapacity < 0) {
                // overflow
                throw new OutOfMemoryError();
            }
            return (minCapacity > MAX_SIZE) ? Integer.MAX_VALUE : MAX_SIZE;
        }
    }
}
