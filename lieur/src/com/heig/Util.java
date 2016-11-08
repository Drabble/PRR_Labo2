package com.heig;

/**
 * Created by antoi on 08.11.2016.
 */
public class Util {
    /**
     * Transform an int to bytes
     *
     * @param x
     * @param n
     * @return the byte array
     */
    public static byte[] intToBytes(int x, int n) {
        byte[] bytes = new byte[n];
        for (int i = 0; i < n; i++, x >>>= 8)
            bytes[i] = (byte) (x & 0xFF);
        return bytes;
    }
}
