package com.avast.bytes;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public abstract class BytesTestBase {

    protected static final String TestString = "The quick brown fox jumps over the lazy dog";
    protected static final byte[] TestData = TestString.getBytes(StandardCharsets.US_ASCII);

    @Test
    public void testBasics() {
        byte[] src = Arrays.copyOf(TestData, TestData.length);
        Bytes b = fromByteArray(src);
        Arrays.fill(src, (byte) 0);
        assertEquals(TestData.length, b.size());
        assertFalse(b.isEmpty());
        assertArrayEquals(TestData, b.toByteArray());
        assertEquals(TestString, b.toStringUtf8());

        for (int i = 0; i < b.size(); i++) {
            assertEquals(TestData[i], b.byteAt(i));
        }

        ByteBuffer bbuf = b.toReadOnlyByteBuffer();
        assertTrue(bbuf.isReadOnly());
        assertEquals(TestData.length, bbuf.remaining());
        byte[] dest = new byte[b.size()];
        bbuf.get(dest);
        assertArrayEquals(TestData, dest);
    }

    @Test
    public void testInputStream() throws IOException {
        Bytes b = fromByteArray(TestData);
        try (InputStream is = b.newInputStream()) {
            byte[] dest = new byte[b.size()];
            int read = is.read(dest);
            assertEquals(b.size(), read);
            assertArrayEquals(TestData, dest);
            assertEquals(-1, is.read());
        }
    }

    @Test
    public void testBuilder() throws IOException {
        try (Bytes.BuilderStream bldr = newBuilder(TestData.length * 10)) {
            for (int i = 0; i < TestData.length; i++) {
                bldr.write(TestData[i]);
            }
            Bytes once = bldr.toBytes();
            for (int i = 0; i < TestData.length; i++) {
                bldr.write(TestData[i]);
            }
            Bytes twice = bldr.toBytes();
            assertEquals(TestString, once.toStringUtf8());
            assertEquals(TestString + TestString, twice.toStringUtf8());

            Bytes twiceAgain = bldr.toBytes();
            assertTrue(twice.equals(twiceAgain));
            assertEquals(twice.hashCode(), twiceAgain.hashCode());
        }
    }

    @Test
    public void testView() throws IOException {
        Bytes b = fromByteArray(TestData);
        int start = TestString.indexOf("jumps");
        int end = start + 10;

        Bytes v = b.view(start, end);
        assertEquals("jumps over", v.toStringUtf8());

        Bytes head = b.view(0, start);
        Bytes tail = b.view(start, b.size());
        assertEquals("The quick brown fox ", head.toStringUtf8());
        assertEquals("jumps over the lazy dog", tail.toStringUtf8());

        Bytes viewOfView = v.view(6, 10);
        assertEquals("over", viewOfView.toStringUtf8());

        try (InputStream is = viewOfView.newInputStream()) {
            byte[] dest = new byte[viewOfView.size()];
            int read = is.read(dest);
            assertEquals(4, read);
            assertEquals("over", new String(dest, US_ASCII));
            assertEquals(-1, is.read());
        }
    }

    @Test
    public void testToHexString() {
        Bytes bytes = Bytes.copyFromHex("FEEDFACECAFEBEEF");
        assertEquals(bytes.toHexString(), "FEEDFACECAFEBEEF".toLowerCase());
    }

    @Test
    public void testToHexStringIsInverseOfCopyFromHex() {
        Bytes bytes = Bytes.copyFromUtf8(TestString);
        assertEquals(bytes, Bytes.copyFromHex(bytes.toHexString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonHexCharacterLetter() {
        // Input data contains 'v' character
        Bytes.copyFromHex("652B781CC0AFD6D11C42v27A90ADE8074C837E129E29F75B1CE476A3E19A3F3E"); // 'v' is inside
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonHexCharacterBracket() {
        // Input data contains '}' character
        Bytes.copyFromHex("20DB0DCDADB6E64B79E696689FE744B717FFF0FCB88381D252E8}8F8FA60C414"); // '}' is inside
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonHexCharacterDot() {
        // Input data contains '}' character
        Bytes.copyFromHex("20DB0DCDADB6E64B79E696689FE744B717FFF0FCB88381D252E8.8F8FA60C414"); // '.' is inside
    }

    protected abstract Bytes fromByteArray(byte[] array);

    protected abstract Bytes.BuilderStream newBuilder(int size);
}
