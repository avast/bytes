package com.avast.bytes;

abstract class Utils {
    /**
     * @throws IllegalArgumentException on invalid character
     */
    static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }

        throw new IllegalArgumentException("Invalid HEX character: '" + ch + "'");
    }
}
