package com.avast.bytes.gpb;

import com.google.protobuf.ByteString;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class ByteStringBytesTest {

    private static final String TestString = "The quick brown fox jumps over the lazy dog";
    private static final ByteString TestData = ByteString.copyFromUtf8(TestString);

    @Test
    public void testBasics() {
        ByteStringBytes b = ByteStringBytes.wrap(TestData);
        assertEquals(TestData.size(), b.size());
        assertFalse(b.isEmpty());
        assertTrue(TestData == b.underlying());
        assertEquals(TestString, b.toStringUtf8());

        for (int i = 0; i < b.size(); i++) {
            assertEquals(TestData.byteAt(i), b.byteAt(i));
        }

        ByteBuffer bbuf = b.toReadOnlyByteBuffer();
        assertTrue(bbuf.isReadOnly());
        assertEquals(TestData.size(), bbuf.remaining());
        byte[] dest = new byte[b.size()];
        bbuf.get(dest);
        assertArrayEquals(TestData.toByteArray(), dest);
    }

    @Test
    public void testInputStream() throws IOException {
        ByteStringBytes b = ByteStringBytes.wrap(TestData);
        InputStream is = b.newInputStream();
        byte[] dest = new byte[b.size()];
        int read = is.read(dest);
        assertEquals(b.size(), read);
        assertArrayEquals(TestData.toByteArray(), dest);
        assertEquals(-1, is.read());
    }

    @Test
    public void testView() {
        ByteStringBytes b = ByteStringBytes.wrap(TestData);
        int start = TestString.indexOf("jumps");
        int end = start + 5;

        ByteStringBytes v = b.view(start, end);
        assertEquals("jumps", v.toStringUtf8());
    }

}
