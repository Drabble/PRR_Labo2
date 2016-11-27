/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

/**
 * Définis des méthodes utilitaires
 */
public class Util {

    /**
     * Transform un int en tableau de bytes
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
