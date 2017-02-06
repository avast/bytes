package com.avast.bytes.jdk;

import com.avast.bytes.Bytes;
import com.avast.bytes.BytesTestBase;
import com.avast.bytes.ConcatBytes;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ByteArrayBytesTest extends BytesTestBase {

    @Override
    protected Bytes fromByteArray(byte[] array) {
        return ByteArrayBytes.copyFrom(array);
    }

    @Override
    protected Bytes.BuilderStream newBuilder(int size) {
        return ByteArrayBytes.newBuilder(size);
    }

    @Test
    public void testReadFromInputStreamSingleChunk() throws IOException {
        InputStream is = new ByteArrayInputStream(TestData);
        Bytes bytes = ByteArrayBytes.readFrom(is);
        assertTrue(bytes instanceof ByteArrayBytes);
        assertEquals(TestString, bytes.toStringUtf8());
    }

    @Test
    public void testReadFromInputStreamMultipleChunks() throws IOException {
        Bytes bigData = Bytes.copyFrom(TestData);
        for (int i = 0; i < 10; i++) { // cca 44 kB
            bigData = bigData.concat(bigData);
        }

        Bytes bytes = ByteArrayBytes.readFrom(bigData.newInputStream());
        assertTrue(bytes instanceof ConcatBytes);
        assertEquals(bigData.size(), bytes.size());
    }
}
