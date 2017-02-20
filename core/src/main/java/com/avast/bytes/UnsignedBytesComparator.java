package com.avast.bytes;

/**
 * Bytes comparator that orders Bytes as array of unsigned bytes.
 * <p>
 * {0} < {-1}
 * 0x00 < 0x80
 */
public class UnsignedBytesComparator extends AbstractBytesComparator {

    @Override
    protected int compare(byte a, byte b) {
        return ((int) a & 0xff) - ((int) b & 0xff);
    }
}
