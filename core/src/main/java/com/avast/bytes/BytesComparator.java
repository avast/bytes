package com.avast.bytes;

/**
 * Bytes comparator that orders Bytes as array of signed bytes.
 * <p>
 * {-1} < {0}
 * 0x80 < 0x00
 */
public class BytesComparator extends AbstractBytesComparator {

    @Override
    protected int compare(byte a, byte b) {
        return a - b;
    }
}
