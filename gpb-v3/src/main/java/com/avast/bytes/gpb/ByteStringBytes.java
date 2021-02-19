package com.avast.bytes.gpb;

import com.avast.bytes.AbstractBytes;
import com.avast.bytes.Bytes;
import com.google.protobuf.ByteString;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Implementation of {@link Bytes} that wraps {@link ByteString}.
 * <p>
 * The only way how to create an instance is by calling {@link #wrap(ByteString)}.
 * The underlying {@link ByteString} can be accessed directly by calling {@link #underlying()}.
 */
public final class ByteStringBytes extends AbstractBytes {

    // the ByteString itself is immutable, so this implementation is trivial
    private final ByteString wrapped;

    private ByteStringBytes(ByteString wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public byte byteAt(int index) {
        return wrapped.byteAt(index);
    }

    @Override
    public byte[] toByteArray() {
        return wrapped.toByteArray();
    }

    @Override
    public ByteBuffer toReadOnlyByteBuffer() {
        return wrapped.asReadOnlyByteBuffer();
    }

    @Override
    public String toString(Charset charset) {
        try {
            return wrapped.toString(charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream newInputStream() {
        return wrapped.newInput();
    }

    @Override
    public ByteStringBytes view(int beginIndex, int endIndex) {
        return new ByteStringBytes(wrapped.substring(beginIndex, endIndex));
    }

    /**
     * Returns the {@link ByteString} wrapped by this {@link ByteStringBytes}.
     *
     * @return {@link ByteString} wrapped by this {@link ByteStringBytes}
     */
    public ByteString underlying() {
        return wrapped;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof ByteStringBytes) {
            return ((ByteStringBytes) o).underlying().equals(this.underlying());
        } else return super.equals(o);
    }

    /**
     * Wraps an existing instance of {@link ByteString}
     *
     * @param buf {@link ByteString} to wrap
     * @return new {@link ByteStringBytes} wrapping the specified {@link ByteString}
     */
    public static ByteStringBytes wrap(ByteString buf) {
        return new ByteStringBytes(buf);
    }

}
