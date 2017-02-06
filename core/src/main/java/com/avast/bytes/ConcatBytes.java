package com.avast.bytes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link Bytes} backed by a list of {@link Bytes} instances.
 */
public final class ConcatBytes extends AbstractBytes {

    private final ArrayList<Bytes> chain;

    /**
     * Global offset into the chain of {@link Bytes}. Imagine you would copy all the {@link Bytes} into a single array
     * and this global offset would tell you where you should start reading that array.
     */
    private final int globalOffset;

    /**
     * The length (size) of this {@link Bytes}.
     */
    private final int length;

    private ConcatBytes(final int globalOffset,
                        final int length,
                        final ArrayList<Bytes> bytes) {
        this.globalOffset = globalOffset;
        this.length = length;
        this.chain = bytes;
    }

    private ConcatBytes(final List<Bytes> bytesN) {
        this(0, computeLength(bytesN), copyList(bytesN));
    }

    private ConcatBytes(final Bytes bytes1, final Bytes bytes2, final Bytes... bytesN) {
        this(0, computeLength(bytes1, bytes2, bytesN), makeList(bytes1, bytes2, bytesN));
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public byte byteAt(final int index) {
        if (index < 0 || index >= length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        int globalIndex = globalOffset + index;
        int scannedBytes = 0;
        for (Bytes bytes : chain) {
            if (globalIndex < scannedBytes + bytes.size()) {
                return bytes.byteAt(globalIndex - scannedBytes);
            } else {
                scannedBytes += bytes.size();
            }
        }

        throw new RuntimeException("This cannot happen.");
    }

    @Override
    public byte[] toByteArray() {
        final byte[] copy = new byte[length];

        int scannedBytes = 0;
        int copiedBytes = 0;
        boolean first = true;
        for (Bytes bytes : chain) {
            // everything is copied already
            if (copiedBytes == length) {
                continue;
            }

            // skipping bytes until we reach globalOffset
            if (scannedBytes + bytes.size() < globalOffset) {
                scannedBytes += bytes.size();
                continue;
            }

            byte[] bytesToCopy;
            if (first) {
                int from = globalOffset - scannedBytes;
                int to = from + Math.min(bytes.size() - from, length);
                bytesToCopy = Arrays.copyOfRange(bytes.toByteArray(), from, to);
                first = false;
            } else {
                bytesToCopy = bytes.toByteArray();
            }

            int copyLength = Math.min(bytesToCopy.length, length - copiedBytes);
            System.arraycopy(bytesToCopy, 0, copy, copiedBytes, copyLength);

            copiedBytes += copyLength;
            scannedBytes += bytes.size();
        }

        return copy;
    }

    @Override
    public ByteBuffer toReadOnlyByteBuffer() {
        return ByteBuffer.wrap(toByteArray()).asReadOnlyBuffer();
    }

    @Override
    public String toString(Charset charset) {
        return new String(toByteArray(), charset);
    }

    @Override
    public InputStream newInputStream() {
        return new ConcatInputStream();
    }

    private class ConcatInputStream extends InputStream {

        private final LinkedList<Bytes> queue;
        private Bytes current;

        private int scannedBytes = 0;
        private int readBytes = 0;
        private int currentOffset = 0;
        private boolean first = true;

        private ConcatInputStream() {
            this.queue = new LinkedList<>(chain);
            this.current = queue.poll();
        }

        @Override
        public int available() {
            return length - readBytes;
        }

        @Override
        public int read() throws IOException {
            while (true) {
                // everything is read already
                if (readBytes == length) {
                    return -1;
                }

                // skipping bytes until we reach globalOffset
                if (scannedBytes + current.size() < globalOffset) {
                    scannedBytes += current.size();
                    current = queue.poll();
                    continue;
                }

                if (first) {
                    first = false;
                    currentOffset = globalOffset - scannedBytes;
                    scannedBytes += globalOffset - scannedBytes;
                }

                int b = current.byteAt(currentOffset) & 0xFF;
                readBytes += 1;
                scannedBytes += 1;
                currentOffset += 1;
                if (currentOffset == current.size()) {
                    current = queue.poll();
                    currentOffset = 0;
                }

                return b;
            }
        }

    }

    @Override
    public Bytes view(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(beginIndex);
        }
        if (endIndex > size()) {
            throw new ArrayIndexOutOfBoundsException(endIndex);
        }
        int subLen = endIndex - beginIndex;
        if (subLen < 0) {
            throw new ArrayIndexOutOfBoundsException(subLen);
        }

        return new ConcatBytes(globalOffset + beginIndex, endIndex - beginIndex, chain);
    }

    /**
     * Wraps existing instances of {@link Bytes} and concatenates them.
     *
     * @return new {@link ConcatBytes} wrapping the specified {@link Bytes} instances
     */
    public static ConcatBytes wrap(final Bytes bytes1, final Bytes bytes2, final Bytes... bytesN) {
        return new ConcatBytes(bytes1, bytes2, bytesN);
    }

    /**
     * Wraps existing instances of {@link Bytes} and concatenates them.
     *
     * @return new {@link ConcatBytes} wrapping the specified {@link Bytes} instances
     */
    public static ConcatBytes wrap(final List<Bytes> bytesN) {
        return new ConcatBytes(bytesN);
    }

    private static int computeLength(final Bytes bytes1, final Bytes bytes2, final Bytes... bytesN) {
        int size = bytes1.size() + bytes2.size();
        for (Bytes bytes : bytesN) {
            size += bytes.size();
        }
        return size;
    }

    private static int computeLength(Iterable<Bytes> bytesN) {
        int size = 0;
        for (Bytes bytes : bytesN) {
            size += bytes.size();
        }
        return size;
    }

    private static ArrayList<Bytes> makeList(Bytes bytes1, Bytes bytes2, Bytes[] bytesN) {
        ArrayList<Bytes> bytes = new ArrayList<>(2 + bytesN.length);
        bytes.add(bytes1);
        bytes.add(bytes2);
        Collections.addAll(bytes, bytesN);
        return bytes;
    }

    private static ArrayList<Bytes> copyList(List<Bytes> bytesN) {
        ArrayList<Bytes> bytes = new ArrayList<>(bytesN.size());
        bytes.addAll(bytesN);
        return bytes;
    }

}
