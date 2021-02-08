package com.avast.bytes;


import java.util.Comparator;

public abstract class AbstractBytesComparator
        implements Comparator<Bytes> {

    private final boolean shorterIsLess;

    protected AbstractBytesComparator() {
        this(true);
    }

    /**
     * @param shorterIsLess Shorter Bytes will be taken as less if true. For example, 0x01 < 0x0102
     */
    protected AbstractBytesComparator(boolean shorterIsLess) {
        this.shorterIsLess = shorterIsLess;
    }

    @Override
    public int compare(Bytes a, Bytes b) {
        int n = Math.min(a.size(), b.size());
        for (int position = 0; position < n; position++) {
            int cmp = compare(a.byteAt(position), b.byteAt(position));
            if (cmp != 0) {
                return cmp;
            }
        }
        return shorterIsLess ? a.size() - b.size() : b.size() - a.size();
    }

    protected abstract int compare(byte a, byte b);
}
