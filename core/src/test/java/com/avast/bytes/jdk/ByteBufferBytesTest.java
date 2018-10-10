package com.avast.bytes.jdk;

import com.avast.bytes.Bytes;
import com.avast.bytes.BytesTestBase;
import com.avast.bytes.ConcatBytes;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ByteBufferBytesTest extends BytesTestBase {

    @Override
    protected Bytes fromByteArray(byte[] array) {
        return ByteBufferBytes.copyFrom(ByteBuffer.wrap(array));
    }

    @Override
    protected Bytes.BuilderStream newBuilder(int size) {
        return ByteBufferBytes.newBuilder(size);
    }

    @Test
    public void testBuilderGrowsExponentially() throws IOException {
        try (Bytes.BuilderStream builder = ByteBufferBytes.newBuilder(2)) {
            append(builder, 10);
            assertEquals(16, builder.toBytes().toReadOnlyByteBuffer().capacity());
        }
    }

    @Test
    public void testBuilderDoesNotGrowIfCapacityIsSufficient() throws IOException {
        try (Bytes.BuilderStream builder = ByteBufferBytes.newBuilder(4)) {
            append(builder, 4);
            assertEquals(4, builder.toBytes().toReadOnlyByteBuffer().capacity());
        }
    }

    @Test
    public void testReadFromInputStreamSingleChunk() throws IOException {
        InputStream is = new ByteArrayInputStream(TestData);
        Bytes bytes = ByteBufferBytes.readFrom(is);
        assertTrue(bytes instanceof ByteBufferBytes);
        assertEquals(TestString, bytes.toStringUtf8());
    }

    @Test
    public void testReadFromInputStreamMultipleChunks() throws IOException {
        Bytes bigData = Bytes.copyFrom(TestData);
        for (int i = 0; i < 10; i++) { // cca 44 kB
            bigData = bigData.concat(bigData);
        }

        try (InputStream is = bigData.newInputStream()) {
            Bytes bytes = ByteBufferBytes.readFrom(is);
            assertTrue(bytes instanceof ConcatBytes);
            assertEquals(bigData.size(), bytes.size());
        }
    }

    @Test
    public void testReadPartFromInputStream() throws IOException {
        final ByteArrayInputStream is = new ByteArrayInputStream("thisisahelloworldtext".getBytes());

        assertEquals("thisisa", ByteBufferBytes.readFrom(is, 0, 7).toStringUtf8());
        assertEquals("helloworld", ByteBufferBytes.readFrom(is, 0, 10).toStringUtf8());

        final ByteArrayInputStream is2 = new ByteArrayInputStream("thisisahelloworldtext".getBytes());

        assertEquals("helloworld", ByteBufferBytes.readFrom(is2, 7, 10).toStringUtf8());
        assertEquals("text", ByteArrayBytes.readFrom(is2, 0, 10).toStringUtf8());
    }

    private void append(OutputStream out, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            out.write(1);
        }
    }
}
