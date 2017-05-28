/**
 * Copyright (c) 2017 Paul Kania. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */
package pl.tivian.util;

/**
 * <p>This class provides interface to store strings composed of hexadecimal numbers.<p>
 *
 * @author Paul Kania
 * @version 0.2.0
 */
public class HexString {
    private static final String hex = "0123456789abcdef";
    private byte[] data;

    /**
     * <p>Creates HexString containing given byte array.<p>
     *
     * @param data byte array to store
     */
    public HexString(byte[] data) {
        this.data = data;
    }

    /**
     * <p>Creates HexString converted from given string.</p>
     * <p>String can contain at the beginning prefix '0x'</p>
     *
     * @param str string composed of hexadecimal numbers
     * @throws IllegalArgumentException if string does not have valid hexadecimal form
     */
    public HexString(String str) {
        this.data = toArray(str);
    }

    /**
     * <p>Converts given string to byte array.</p>
     * <p>Optionaly string can starts with '0x' prefix.</p>
     *
     * @param str given string of hexadecimal number
     * @return a byte array resulting from the string
     * @throws IllegalArgumentException if invalid string was given
     */
    private byte[] toArray(String str) {
        while (str.startsWith("0") || str.startsWith("x"))
            str = str.substring(1);

        if ((str.length() % 2) != 0)
            str = "0" + str;

        byte[] result = new byte[str.length() / 2];
        String[] bytes = str.split("(?<=\\G.{2})");

        for (int i = 0; i < result.length; i++) {
            try {
                result[i] = Integer.decode("0x" + bytes[i]).byteValue();
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid hex string format.");
            }
        }

        return result;
    }

    /**
     * <p>Returns a byte array stored in HexString.</p>
     *
     * @return a byte array stored in class or converted from string
     */
    public byte[] toArray() {
        return data;
    }

    /**
     * <p>Returns a string representation of the HexString.</p>
     *
     * @return a string representation of stored byte array in hexadecimal form
     */
    public String toString() {
        StringBuilder result = new StringBuilder(data.length * 2);
        for (byte b : data)
            result.append(hex.charAt((b & 0xf0) >> 4)).append(hex.charAt(b & 0xf));
        return result.toString();
    }
}