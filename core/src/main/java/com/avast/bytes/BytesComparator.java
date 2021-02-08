package com.avast.bytes;

/**
 * Bytes comparator that orders Bytes as array of signed bytes.
 * <p>
 * {-1} &lt; {0}
 * 0x80 &lt; 0x00
 * </p>
 */
public class BytesComparator extends AbstractBytesComparator {

    @Override
    protected int compare(byte a, byte b) {
        return a - b;
    }
}
