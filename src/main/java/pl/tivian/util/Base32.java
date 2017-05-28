/**
 * Copyright (c) 2017 Paul Kania. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */
package pl.tivian.util;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/**
 * This class consists exclusively of static methods for obtaining encoders and decoders for
 * the Base32 encoding scheme. The implementation of this class supports the following
 * types of Base32 as specified in <a href="https://tools.ietf.org/html/rfc4648">RFC 4648</a>.
 * <ul><li>
 *  Basic
 *  <p> Uses "The Base32 Alphabet" as specified in
 *      <a href="https://tools.ietf.org/html/rfc4648#page-8" target="_blank">Table 3 of RFC 4648</a>
 *      for encoding and decoding operation. The encoder does not add any line feed (line separator) character.
 *      The decoder rejects data that contains characters outside the base32 alphabet.
 *  </p>
 * </li><li>
 *  Hexadecimal
 *  <p> Uses "The Extended Hex Base32 Alphabet" as specified in
 *      <a href="https://tools.ietf.org/html/rfc4648#page-9" target="_blank">Table 4 of RFC 4648</a>.
 *      One property with this alphabet, which the base64 and base32 alphabets lack,
 *      is that encoded data maintains its sort order when the encoded data is compared bit-wise.
 *  </p>
 * </li></ul>
 *
 * @author Paul Kania
 * @version 0.2.0
 */
public class Base32 {
    private static final String DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567=";
    private static final String HEX_ALPHABET     = "0123456789ABCDEFGHIJKLMNOPQRSTUV=";
    private static final Base32.Decoder DECODER_INSTANCE = new Base32.Decoder(DEFAULT_ALPHABET);
    private static final Base32.Decoder DECODER_HEX_INSTANCE = new Base32.Decoder(HEX_ALPHABET);
    private static final Base32.Encoder ENCODER_INSTANCE = new Base32.Encoder(DEFAULT_ALPHABET);
    private static final Base32.Encoder ENCODER_HEX_INSTANCE = new Base32.Encoder(HEX_ALPHABET);

    private Base32() {}

    /**
     * <p>Returns a Base32.Decoder that decodes using the Basic type base32 encoding scheme.</p>
     *
     * @return A Base32 decoder
     */
    public static Base32.Decoder getDecoder() {
        return DECODER_INSTANCE;
    }

    /**
     * <p>Returns a Base32.Encoder that encodes using the Basic type base32 encoding scheme.</p>
     *
     * @return A Base32 encoder.
     */
    public static Base32.Encoder getEncoder() {
        return ENCODER_INSTANCE;
    }

    /**
     * <p>Returns a Base32.Decoder that decodes using the extended hexadecimal base32 encoding scheme.</p>
     *
     * @return A Base32 decoder
     */
    public static Base32.Decoder getHexDecoder() {
        return DECODER_HEX_INSTANCE;
    }

    /**
     * <p>Returns a Base32.Encoder that decodes using the extended hexadecimal base32 encoding scheme.</p>
     *
     * @return A Base32 encoder
     */
    public static Base32.Encoder getHexEncoder() {
        return ENCODER_HEX_INSTANCE;
    }

    /**
     * <p>This class implements a decoder for decoding byte data using the Base32 encoding scheme as specified in RFC 4648.</p>
     * <p>The Base32 padding character '=' is accepted and interpreted as the end of the encoded byte data, but is not required.
     * So if the final unit of the encoded byte data has seven, five, four or two Base32 characters (without the corresponding
     * padding character(s) padded), they are decoded as if followed by padding character(s). If there is a padding character
     * present in the final unit, the correct number of padding character(s) must be present, otherwise IllegalArgumentException
     * is thrown during decoding.</p>
     *
     * @see Base32.Encoder
     */
    public static class Decoder {
        private static final byte[] PADDING_TABLE = { 0, 1, -1, 2, 3, -1, 4 };
        private final String alphabet;

        private Decoder(String alphabet) {
            this.alphabet = alphabet;
        }

        /**
         * <p>Decodes all bytes from the input byte array using the Base32 encoding scheme, writing the results into
         * a newly-allocated output byte array. The returned byte array is of the length of the resulting bytes.</p>
         *
         * @param src the byte array to decode
         * @return A newly-allocated byte array containing the decoded bytes.
         * @throws IllegalArgumentException if src is not in valid Base32 scheme
         */
        public byte[] decode(byte[] src) {
            if(src.length == 0)
                return src;

            if((src.length % 8) != 0)
                src = Arrays.copyOf(src, src.length + ((8 - src.length % 8) % 8));
            int outSize = (int) (src.length * 5) / 8;
            byte[] result = new byte[outSize];
            int padding = 0;

            for(int i = 0; i < src.length; i++) {
                if(src[i] != 0)
                    src[i] = (byte) alphabet.indexOf(Character.toUpperCase((int) src[i]));

                if(src[i] == -1)
                    throw new IllegalArgumentException("Source array is not in valid Base32 scheme.");
                else if(src[i] == 32)
                    padding++;
            }

            for(int i = 0, j = 0; i < src.length; i += 8) {
                result[j++] = (byte) ((src[i] << 3) | (src[i + 1] >> 2));
                result[j++] = (byte) (((src[i + 1] & 0x3) << 6) | (src[i + 2] << 1) | (src[i + 3] >> 4));
                result[j++] = (byte) (((src[i + 3] & 0xf) << 4) | (src[i + 4] >> 1));
                result[j++] = (byte) (((src[i + 4] & 0x1) << 7) | (src[i + 5] << 2) | (src[i + 6] >> 3));
                result[j++] = (byte) (((src[i + 6] & 0x7) << 5) | src[i + 7]);
            }

            if(PADDING_TABLE[padding] == -1)
                throw new IllegalArgumentException("Source array is not in valid Base32 scheme.");

            return Arrays.copyOf(result, result.length - PADDING_TABLE[padding]);
        }

        /**
         * <p>Decodes all bytes from the input byte array using the Base32 encoding scheme,
         *    writing the results into the given output byte array, starting at offset 0.</p>
         * <p>It is the responsibility of the invoker of this method to make sure
         *    the output byte array dst has enough space for decoding all bytes from the input byte array.
         *    No bytes will be be written to the output byte array if the output byte array is not big enough.</p>
         * <p>If the input byte array is not in valid Base32 encoding scheme then some bytes
         *    may have been written to the output byte array before IllegalargumentException is thrown.</p>
         *
         * @param src the byte array to decode
         * @param dst the output byte array
         * @return The number of bytes written to the output byte array
         * @throws IllegalArgumentException if src is not in valid Base32 scheme,
         *         or dst does not have enough space for decoding all input bytes.
         */
        public int decode(byte[] src, byte[] dst) {
            if(dst.length < outputSize(src.length))
                throw new IllegalArgumentException(
                        "Destination array does not have enough space for encoding all input bytes.");

            byte[] temp = decode(src);

            for(int i = 0; i < temp.length; i++)
                dst[i] = temp[i];

            return temp.length;
        }

        /**
         * <p>Decodes all bytes from the input byte buffer using the Base32 encoding scheme,
         *    writing the results into a newly-allocated ByteBuffer.</p>
         * <p>Upon return, the source buffer's position will be updated to its limit;
         *    its limit will not have been changed. The returned output buffer's position
         *    will be zero and its limit will be the number of resulting decoded bytes</p>
         * <p>IllegalArgumentException is thrown if the input buffer is not in valid Base32 encoding scheme.
         *    The position of the input buffer will not be advanced in this case.</p>
         *
         * @param buffer the ByteBuffer to decode
         * @return A newly-allocated byte buffer containing the decoded bytes
         * @throws IllegalArgumentException if src is not in valid Base32 scheme.
         */
        public ByteBuffer decode(ByteBuffer buffer) {
            return ByteBuffer.wrap(decode(buffer.array()));
        }

        /**
         * <p>Decodes a Base32 encoded String into a newly-allocated byte array using the Base32 encoding scheme.</p>
         * <p>An invocation of this method has exactly the same effect as invoking decode(src.getBytes(StandardCharsets.ISO_8859_1))</p>
         *
         * @param src the string to decode
         * @return A newly-allocated byte array containing the decoded bytes.
         * @throws IllegalArgumentException if src is not in valid Base32 scheme
         */
        public byte[] decode(String src) {
            return decode(src.getBytes(StandardCharsets.ISO_8859_1));
        }

        /**
         * <p>This feature will be added in future for compability with standard {@link java.util.Base64}</p>
         *
         * @param is the input stream
         * @return the input stream for decoding the specified Base32 encoded byte stream (currently this is disabled)
         * @throws UnsupportedOperationException unimplemented functionality
         */
        public InputStream wrap(InputStream is) {
            throw new UnsupportedOperationException("This function is not yet implemented.");
        }

        private int outputSize(int length) {
            return ((length + ((8 - length % 8) % 8)) * 5) / 8;
        }
    }

    /**
     * <p>This class implements an encoder for encoding byte data using the Base32 encoding scheme as specified in RFC 4648.</p>
     *
     * @see Base32.Decoder
     */
    public static class Encoder {
        private static final byte[] PADDING_TABLE = { 0, 6, 4, 3, 1 };
        private final String alphabet;
        private boolean isPadded;

        private Encoder(String alphabet) {
            this(alphabet, true);
        }

        private Encoder(String alphabet, boolean isPadded) {
            this.alphabet = alphabet;
            this.isPadded = isPadded;
        }

        /**
         * <p>Encodes all bytes from the specified byte array into a newly-allocated byte array using the Base32 encoding scheme.
         *    The returned byte array is of the length of the resulting bytes.</p>
         *
         * @param src the byte array to encode
         * @return A newly-allocated byte array containing the resulting encoded bytes.
         */
        public byte[] encode(byte[] src) {
            if(src.length == 0)
                return src;

            int padding = PADDING_TABLE[src.length % 5];
            if ((src.length % 5) != 0)
                src = Arrays.copyOf(src, src.length + ((5 - src.length % 5) % 5));
            int outSize = (int) (src.length * 8) / 5;
            byte[] result = new byte[outSize];

            for (int i = 0, j = 0; i < src.length; i+=5) {
                result[j++] = (byte) alphabet.charAt(src[i] >> 3);
                result[j++] = (byte) alphabet.charAt(((src[i] & 0xf) << 2) | (src[i + 1] >> 6));
                result[j++] = (byte) alphabet.charAt((src[i + 1] >>  1) & 0x1f);
                result[j++] = (byte) alphabet.charAt(((src[i + 1] & 0x1) << 4) | (src[i + 2] >> 4));
                result[j++] = (byte) alphabet.charAt(((src[i + 2] & 0xf) << 1) | (src[i + 3] >> 7));
                result[j++] = (byte) alphabet.charAt((src[i + 3] >>  2) & 0x1f);
                result[j++] = (byte) alphabet.charAt(((src[i + 3] & 0x3) << 3) | (src[i + 4] >> 5));
                result[j++] = (byte) alphabet.charAt(src[i + 4] & 0x1f);
            }

            if(isPadded) {
                int i = result.length - 1;
                while(padding-- != 0)
                    result[i--] = (byte) alphabet.charAt(32);
            } else {
                return Arrays.copyOfRange(result, 0, result.length - padding);
            }

            return result;
        }

        /**
         * <p>Encodes all bytes from the specified byte array using the Base32 encoding scheme,
         *    writing the resulting bytes to the given output byte array, starting at offset 0.</p>
         * <p>It is the responsibility of the invoker of this method to make sure
         *    the output byte array dst has enough space for encoding all bytes from the input byte array.
         *    No bytes will be written to the output byte array if the output byte array is not big enough.</p>
         *
         * @param src the byte array to encode
         * @param dst the output byte array
         * @return The number of bytes written to the output byte array
         * @throws IllegalArgumentException if dst does not have enough space for encoding all input bytes.
         */
        public int encode(byte[] src, byte[] dst) {
            if(dst.length < outputSize(src.length))
                throw new IllegalArgumentException(
                    "Destination array does not have enough space for encoding all input bytes.");

            byte[] temp = encode(src);

            for(int i = 0; i < temp.length; i++)
                dst[i] = temp[i];

            return temp.length;
        }

        /**
         * <p>Encodes all remaining bytes from the specified byte buffer into a newly-allocated ByteBuffer
         *    using the Base32 encoding scheme. Upon return, the source buffer's position will be updated to its limit;
         *    its limit will not have been changed. The returned output buffer's position will be zero and
         *    its limit will be the number of resulting encoded bytes.</p>
         *
         * @param buffer the source ByteBuffer to encode
         * @return A newly-allocated byte buffer containing the encoded bytes.
         */
        public ByteBuffer encode(ByteBuffer buffer) {
            return ByteBuffer.wrap(encode(buffer.array()));
        }

        /**
         * <p>Encodes the specified byte array into a String using the Base32 encoding scheme.</p>
         * <p>This method first encodes all input bytes into a base32 encoded byte array and
         *    then constructs a new String by using the encoded byte array and the ISO-8859-1 charset.</p>
         * <p>In other words, an invocation of this method has exactly the same effect
         *    as invoking new String(encode(src), StandardCharsets.ISO_8859_1).</p>
         *
         * @param src the byte array to encode
         * @return A String containing the resulting Base32 encoded characters
         */
        public String encodeToString(byte[] src) {
            return new String(encode(src), StandardCharsets.ISO_8859_1);
        }

        /**
         * <p>Returns an encoder instance that encodes equivalently to this one,
         *    but without adding any padding character at the end of the encoded byte data.</p>
         * <p>The encoding scheme of this encoder instance is unaffected by this invocation.
         *    The returned encoder instance should be used for non-padding encoding operation.</p>
         *
         * @return an equivalent encoder that encodes without adding any padding character at the end
         */
        public Base32.Encoder withoutPadding() {
            return !isPadded ? this : new Encoder(alphabet, false);
        }

        /**
         * <p>This feature will be added in future for compability with standard {@link java.util.Base64}</p>
         *
         * @param os the output stream.
         * @return the output stream for encoding the byte data into the specified Base32 encoded format (currently this is disabled)
         * @throws UnsupportedOperationException unimplemented functionality
         */
        public OutputStream wrap(OutputStream os) {
            throw new UnsupportedOperationException("This function is not yet implemented.");
        }

        private int outputSize(int length) {
            return ((length + ((5 - length % 5) % 5)) * 8) / 5;
        }
    }
}