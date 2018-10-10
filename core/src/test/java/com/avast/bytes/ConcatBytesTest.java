package com.avast.bytes;

import com.avast.bytes.jdk.ByteArrayBytes;
import com.avast.bytes.jdk.ByteBufferBytes;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public class ConcatBytesTest {

    private static final String TestString = "The quick brown fox jumps over the lazy dog";
    private static final byte[] TestData = TestString.getBytes(US_ASCII);

    @Test
    public void testBasics() {
        Bytes bytes1 = ByteArrayBytes.copyFrom(TestData);
        Bytes bytes2 = ByteBufferBytes.copyFrom(ByteBuffer.wrap(TestData));
        Bytes concatenated = bytes1.concat(bytes2);

        byte[] checkArray = (TestString + TestString).getBytes(US_ASCII);
        assertEquals(TestData.length * 2, concatenated.size());
        assertFalse(concatenated.isEmpty());
        assertArrayEquals(checkArray, concatenated.toByteArray());
        assertEquals(TestString + TestString, concatenated.toStringUtf8());

        for (int i = 0; i < concatenated.size(); i++) {
            assertEquals(checkArray[i], concatenated.byteAt(i));
        }

        ByteBuffer bbuf = concatenated.toReadOnlyByteBuffer();
        assertTrue(bbuf.isReadOnly());
        assertEquals(checkArray.length, bbuf.remaining());
        byte[] dest = new byte[concatenated.size()];
        bbuf.get(dest);
        assertArrayEquals(checkArray, dest);
    }

    @Test
    public void testInputStream() throws IOException {
        Bytes bytes1 = ByteArrayBytes.copyFrom(TestData);
        Bytes bytes2 = ByteBufferBytes.copyFrom(ByteBuffer.wrap(TestData));
        Bytes concatenated = bytes1.concat(bytes2);

        try (InputStream is = concatenated.newInputStream()) {
            byte[] dest = new byte[concatenated.size()];
            int read = is.read(dest);
            assertEquals(concatenated.size(), read);
            assertArrayEquals((TestString + TestString).getBytes(US_ASCII), dest);
            assertEquals(-1, is.read());
        }
    }

    @Test
    public void testView() throws IOException {
        Bytes bytes1 = ByteArrayBytes.copyFrom(TestData);
        Bytes bytes2 = ByteBufferBytes.copyFrom(ByteBuffer.wrap(TestData));
        Bytes concatenated = bytes1.concat(bytes2);

        int start = TestString.indexOf("jumps");
        int end = start + 10;

        Bytes v = concatenated.view(start, end);
        assertEquals("jumps over", v.toStringUtf8());

        Bytes head = concatenated.view(0, start);
        Bytes tail = concatenated.view(start, concatenated.size());
        assertEquals("The quick brown fox ", head.toStringUtf8());
        assertEquals("jumps over the lazy dogThe quick brown fox jumps over the lazy dog", tail.toStringUtf8());

        Bytes viewOfView = v.view(6, 10);
        assertEquals("over", viewOfView.toStringUtf8());

        try (InputStream is = viewOfView.newInputStream()) {
            byte[] dest = new byte[viewOfView.size()];
            int read = is.read(dest);
            assertEquals(4, read);
            assertEquals("over", new String(dest, US_ASCII));
            assertEquals(-1, is.read());
        }

        int dogStart = TestString.indexOf("dog");
        Bytes bridgeView = concatenated.view(TestString.indexOf("dog"), dogStart + 6);
        assertEquals("dogThe", bridgeView.toStringUtf8());

        try (InputStream is = bridgeView.newInputStream()) {
            byte[] dest = new byte[bridgeView.size()];
            int read = is.read(dest);
            assertEquals(6, read);
            assertEquals("dogThe", new String(dest, US_ASCII));
            assertEquals(-1, is.read());
        }
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testViewComplex() {
        Bytes concatenated = Bytes.copyFromUtf8("test1")
                .concat(Bytes.copyFromUtf8("test2"))
                .concat(Bytes.copyFromUtf8("test3"))
                .concat(Bytes.copyFromUtf8(""))
                .concat(Bytes.copyFromUtf8("test4"));

        assertEquals("test1", concatenated.view(0, 5).toStringUtf8());
        assertEquals("test2", concatenated.view(5, 10).toStringUtf8());
        assertEquals("test4", concatenated.view(15, 20).toStringUtf8());
        concatenated.view(15, 21);
    }


    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testByteAt() {
        Bytes bytes1 = ByteArrayBytes.copyFrom(TestData);
        Bytes bytes2 = ByteBufferBytes.copyFrom(ByteBuffer.wrap(TestData));
        Bytes concatenated = bytes1.concat(bytes2);

        assertEquals(TestData[0], concatenated.byteAt(0));
        assertEquals(TestData[TestData.length - 1], concatenated.byteAt(TestData.length - 1));
        concatenated.byteAt(concatenated.size());
    }

    @Test
    public void testEmptyInConcat() throws IOException {
        Bytes bytes1 = ByteArrayBytes.EMPTY;
        Bytes bytes2 = ByteArrayBytes.copyFromUtf8(TestString);
        Bytes concatenated = bytes1.concat(bytes2);

        assertEquals(TestString, concatenated.toStringUtf8());
        try (InputStream is = concatenated.newInputStream()) {
            assertEquals(TestString, ByteArrayBytes.readFrom(is).toStringUtf8());
        }
    }

}
