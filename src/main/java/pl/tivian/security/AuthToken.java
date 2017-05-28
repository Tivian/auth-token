/**
 * Copyright (c) 2017 Paul Kania. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */
package pl.tivian.security;

import java.nio.*;
import java.security.*;
import java.time.*;
import java.util.*;

import pl.tivian.util.*;

/**
 * <p>This class implements Time-Based One-Time Password Algorithm as specified in
 *   <a href="https://tools.ietf.org/html/rfc6238" target="_blank">RFC 6238</a>,
 *   <a href="https://tools.ietf.org/html/rfc4226" target="_blank">RFC 4226</a> and
 *   <a href="https://tools.ietf.org/html/rfc2104" target="_blank">RFC 2104</a>.</p>
 * <p>Supported hash functions:</p>
 * <ul>
 *  <li>SHA-1</li>
 *  <li>MD5</li>
 * </ul>
 * <p>Currently AuthToken is using Java build-in hash functions.
 *    In near future is expected to add support for RIPEMD-128/160.</p>
 *
 * @author Paul Kania
 * @version 0.2.0
 */
public class AuthToken {
    private static final Set<String> SUPPORTED_ALGORITHMS =
        new HashSet<>(Arrays.asList("SHA-1", "MD5"));
    private static final String DEFAULT_ALGORITHM = "SHA-1";
    private static final int BLOCK_SIZE = 64;
    private static final int DEFAULT_INTERVAL = 30;
    private static final int DEFAULT_SIZE = 6;
    private static final long[] DIGITS_POWER =
        new long[] { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000 };

    private byte[] secret;
    private String digestAlgorithm;
    private int interval;
    private int digits;

    /**
     * <p>Creates a new authorization token
     *    for given secret phrase stored in base32 format.</p>
     *
     * @param secret phrase stored in Base32 format
     */
    public AuthToken(String secret) {
        this(Base32.getDecoder().decode(secret));
    }

    /**
     * <p>Creates a new authorization token
     *    for given secret stored in byte array.</p>
     *
     * @param secret phrase given by authentication provider
     */
    public AuthToken(byte[] secret) {
        init(secret, DEFAULT_ALGORITHM, DEFAULT_INTERVAL, DEFAULT_SIZE);
    }

    /**
     * <p>Creates a authorization token using provided hash algorithm name.</p>
     *
     * @param secret phrase given by authentication provider
     * @param algorithm name of algorithm which will be used for this AuthToken (default SHA-1)
     * @throws NoSuchAlgorithmException if algorithm is not supported
     */
    public AuthToken(byte[] secret, String algorithm)
            throws NoSuchAlgorithmException {
        this(secret, algorithm, DEFAULT_INTERVAL);
    }

    /**
     * <p>Creates a authorization token using provided hash algorithm name
     *    and specified interval.</p>
     *
     * @param secret phrase given by authentication provider
     * @param algorithm name of algorithm which will be used for this AuthToken (default SHA-1)
     * @param interval indicates how often the key change (default 30 seconds)
     * @throws NoSuchAlgorithmException if algorithm is not supported
     */
    public AuthToken(byte[] secret, String algorithm, int interval)
            throws NoSuchAlgorithmException {
        this(secret, algorithm, interval, DEFAULT_SIZE);
    }

    /**
     * <p>Creates a authorization token using provided hash algorithm name,
     *    specified interval and desired one-time password length.</p>
     *
     * @param secret phrase given by authentication provider
     * @param algorithm name of algorithm which will be used for this AuthToken (default SHA-1)
     * @param interval indicates how often the key change (default 30 seconds)
     * @param digits length of the output (default 6 digits)
     * @throws NoSuchAlgorithmException if algorithm is not supported
     * @throws IllegalArgumentException if given length of the output is outside range of inclusive [6, 8]
     */
    public AuthToken(byte[] secret, String algorithm, int interval, int digits)
            throws NoSuchAlgorithmException, IllegalArgumentException {
        if(!SUPPORTED_ALGORITHMS.contains(algorithm))
            throw new NoSuchAlgorithmException("AuthToken only supports SHA-1 and MD5.");

        if (digits < 6)
            throw new IllegalArgumentException("The authorization token must have at least 6 digits.");
        else if(digits > 8)
            throw new IllegalArgumentException("The authorization token may not be longer than 8 digits.");

        init(secret, algorithm, interval, digits);
    }

    /**
     * <p>Get valid key for current timestamp.</p>
     *
     * @return string of time-based one-time password
     */
    public String getKey() {
        return getKey(Instant.now());
    }

    /**
     * <p>Get valid key for given timestamp.</p>
     *
     * @param timestamp corresponding to the time when key supposed to be obtained
     * @return string of time-based one-time password
     */
    public String getKey(Instant timestamp) {
        String result = Long.toString(getKey(timestamp.getEpochSecond()));
        return String.format(String.format("%%%ds", digits), result).replace(' ', '0');
    }

    /**
     * <p>Get valid key for given time from epoch in seconds.</p>
     *
     * @param time given in seconds from Epoch (1970-01-01T00:00:00Z)
     * @return time-based one-time password in form of number
     */
    public long getKey(long time) {
        return TOTP(time);
    }

    private void init(byte[] secret, String algorithm, int interval, int digits) {
        this.secret = secret;
        this.digestAlgorithm = algorithm;
        this.interval = interval;
        this.digits = digits;
    }

    /**
     * <p>A Time-Based One-Time Password Algorithm as described in
     *  <a href="https://tools.ietf.org/html/rfc6238" target="_blank">RFC 6238</a>.</p>
     *
     * @param time given in seconds from Epoch (1970-01-01T00:00:00Z)
     * @return time-based one-time password in form of number
     */
    private long TOTP(long time) {
        long TC = time / interval;
        return HOTP(TC);
    }

    /**
     * <p>An HMAC-Based One-Time Password Algorithm as described in
     *  <a href="https://tools.ietf.org/html/rfc4226" target="_blank">RFC 4226</a>.</p>
     *
     * @param counter 8-byte value, the moving factor
     * @return HOTP calculated value
     */
    private long HOTP(long counter) {
        try {
            byte[] HS = HMAC(longToBytes(counter));
            byte offset = (byte) (HS[19] & 0xf);

            return (((HS[offset    ] & 0x7f) << 24) |
                    ((HS[offset + 1] & 0xff) << 16) |
                    ((HS[offset + 2] & 0xff) <<  8) |
                    (HS[offset + 3] & 0xff)         ) % DIGITS_POWER[digits];
        } catch (NoSuchAlgorithmException ex) {
            return -1;
        }
    }

    /**
     * <p>Keyed-Hashing for Message Authentication as described in
     *  <a href="https://tools.ietf.org/html/rfc2104" target="_blank">RFC 2104</a>.</p>
     *
     * @param text message to authenticate
     * @return HMAC hashed value
     * @throws NoSuchAlgorithmException if given algorithm haven't been found
     */
    private byte[] HMAC(byte[] text)
            throws NoSuchAlgorithmException {
        MessageDigest hash = MessageDigest.getInstance(digestAlgorithm);

        if (secret.length > BLOCK_SIZE) {
            secret = hash.digest(secret);
        } else if (secret.length < BLOCK_SIZE) {
            secret = Arrays.copyOf(secret, BLOCK_SIZE);
        }

        byte o_key_pad[] = Arrays.copyOf(secret, BLOCK_SIZE);
        byte i_key_pad[] = Arrays.copyOf(secret, BLOCK_SIZE);

        for (int i = 0; i < BLOCK_SIZE; i++) {
            o_key_pad[i] ^= 0x5c;
            i_key_pad[i] ^= 0x36;
        }

        return hash.digest(joinArrays(o_key_pad, hash.digest(joinArrays(i_key_pad, text))));
    }

    /**
     * <p>Joins two byte arrays together</p>
     *
     * @param a first array to join
     * @param b second array to join
     * @return joined byte array
     */
    private byte[] joinArrays(byte[] a, byte[] b) {
        byte[] result = Arrays.copyOf(a, a.length + b.length);
        for (int i = a.length; i < result.length; i++)
            result[i] = b[i - a.length];
        return result;
    }

    /**
     * <p>Convert long to array of bytes</p>
     *
     * @param x long value to convert
     * @return byte array convertion of x
     */
    private byte[] longToBytes(long x) {
        return ByteBuffer.allocate(Long.BYTES).putLong(x).array();
    }
}