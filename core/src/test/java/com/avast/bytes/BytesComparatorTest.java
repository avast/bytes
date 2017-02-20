package com.avast.bytes;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class BytesComparatorTest {

    @Test
    public void orderSignedTest() {

        Bytes[] bytesArray = new Bytes[]{
                Bytes.copyFromHex("8000"),
                Bytes.copyFromHex("0000"),
                Bytes.copyFromHex("800100"),
                Bytes.copyFromHex("8001")
        };

        Arrays.sort(bytesArray, new BytesComparator());

        assertEquals(bytesArray[0], Bytes.copyFromHex("8000"));
        assertEquals(bytesArray[1], Bytes.copyFromHex("8001"));
        assertEquals(bytesArray[2], Bytes.copyFromHex("800100"));
        assertEquals(bytesArray[3], Bytes.copyFromHex("0000"));
    }

    @Test
    public void orderUnsignedTest() {

        Bytes[] bytesArray = new Bytes[]{
                Bytes.copyFromHex("8000"),
                Bytes.copyFromHex("0000"),
                Bytes.copyFromHex("800100"),
                Bytes.copyFromHex("8001")
        };

        Arrays.sort(bytesArray, new UnsignedBytesComparator());

        assertEquals(bytesArray[0], Bytes.copyFromHex("0000"));
        assertEquals(bytesArray[1], Bytes.copyFromHex("8000"));
        assertEquals(bytesArray[2], Bytes.copyFromHex("8001"));
        assertEquals(bytesArray[3], Bytes.copyFromHex("800100"));
    }
}
