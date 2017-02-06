package com.avast.bytes;

public abstract class AbstractBytes implements Bytes {
    private final int TO_STRING_SIZE_LIMIT = 100;
    private final String IMPLEMENTATION_CLASS_NAME = this.getClass().getCanonicalName();

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(IMPLEMENTATION_CLASS_NAME).append("(");
        b.append("size:").append(size()).append(", ");
        b.append("bytes: ");
        if(size() > TO_STRING_SIZE_LIMIT) {
            b.append(view(0, TO_STRING_SIZE_LIMIT).toHexString()).append("...");
        } else {
            b.append(toHexString());
        }
        b.append(")");
        return b.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Bytes)) {
            return false;
        }

        final Bytes other = (Bytes) o;
        if (this.size() != other.size()) {
            return false;
        }

        for (int i = 0; i < this.size(); i++) {
            if (this.byteAt(i) != other.byteAt(i)) {
                return false;
            }
        }

        return true;
    }

    private volatile int hash = 0;

    @Override
    public int hashCode() {
        int h = hash;

        if (h == 0) {

            h = size();
            for (int i = 0; i < size(); i++) {
                h = h * 31 + byteAt(i);
            }
            if (h == 0) {
                h = 1;
            }

            hash = h;
        }

        return h;
    }
}
