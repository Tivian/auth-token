/**
 * Copyright (c) 2017 Paul Kania. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */
package pl.tivian.security.test;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

import pl.tivian.security.*;
import pl.tivian.util.*;

/**
 * <p>Test suite for AuthToken<p>
 *
 * @author Paul Kania
 * @version 0.2.0
 */
public class AuthTokenTest {
    private AuthTokenTest() {
        System.out.println("Testing Base32 functionality: " + (base32Test() ? "pass" : "FAILURE"));
        System.out.println("Testing TOTP functionality:   " + (TOTPTest()   ? "pass" : "FAILURE"));
        System.out.println("Testing HOTP functionality:   " + (HOTPTest()   ? "pass" : "FAILURE"));
        System.out.println("Testing HMAC functionality:   " + (HMACTest()   ? "pass" : "FAILURE"));
    }

    /**
     * <p>Test vectors for HMAC-Based One-Time Password Algorithm</p>
     *
     * @return true if passed test, false otherwise
     * @see <a href="https://tools.ietf.org/html/rfc6238#appendix-B" target="_blank">RFC 6238</a>
     */
    private boolean TOTPTest() {
        final String algorithm = "SHA-1";
        final String secret = "12345678901234567890";
        List<Long> timeStamps = Arrays.asList(59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L);
        List<Long> output = Arrays.asList(94287082L, 7081804L, 14050471L, 89005924L, 69279037L, 65353130L);
        AuthToken token = null;

        try {
            token = new AuthToken(secret.getBytes(), algorithm, 30, 8);
        } catch (NoSuchAlgorithmException | IllegalArgumentException ex) {
            ex.printStackTrace();
            return false;
        }

        for (int i = 0; i < output.size(); i++) {
            if(token.getKey(timeStamps.get(i)) != output.get(i))
                return false;
        }

        return true;
    }

    /**
     * <p>Test vectors for Time-Based One-Time Password Algorithm</p>
     *
     * @return true if passed test, false otherwise
     * @see <a href="https://tools.ietf.org/html/rfc4226#page-32" target="_blank">RFC 4226</a>
    */
    private boolean HOTPTest() {
        final String algorithm = "SHA-1";
        final String secret = "12345678901234567890";
        List<Long> output = Arrays.asList(
            755224L, 287082L, 359152L, 969429L, 338314L,
            254676L, 287922L, 162583L, 399871L, 520489L);
        AuthToken token = null;
        Method hotpMethod = null;

        /*
         * AuthToken.HOTP is private method so usage of reflection is needed
         * in order to test its correctness
         */
        try {
            token = new AuthToken(secret.getBytes(), algorithm);

            hotpMethod = token.getClass().getDeclaredMethod("HOTP", long.class);
            hotpMethod.setAccessible(true);

            for (int i = 0; i < 10; i++) {
                if ((long) hotpMethod.invoke(token, i) != output.get(i))
                    return false;
            }
        } catch (NoSuchAlgorithmException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * <p>Test vectors for Keyed-Hashing algorithm for Message Authentication</p>
     *
     * @return true if passed test, false otherwise
     * @see <a href="https://tools.ietf.org/html/rfc2104#page-9" target="_blank">RFC 2104</a>
     */
    private boolean HMACTest() {
        final String algorithm = "MD5";
        List<byte[]> key = new ArrayList<>();
        List<byte[]> data = new ArrayList<>();
        List<byte[]> digest = new ArrayList<>();

        key.add(new HexString("0x0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b").toArray());
        data.add("Hi There".getBytes());
        digest.add(new HexString("0x9294727a3638bb1c13f48ef8158bfc9d").toArray());

        key.add("Jefe".getBytes());
        data.add("what do ya want for nothing?".getBytes());
        digest.add(new HexString("0x750c783e6ab0b503eaa86e310a5db738").toArray());

        key.add(new HexString("0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").toArray());
        data.add(new HexString("0xDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD").toArray());
        digest.add(new HexString("0x56be34521d144c88dbb8c733f0e8b3f6").toArray());

        /*
         * AuthToken.HMAC is private method so usage of reflection is needed
         * in order to test its correctness
         */
        try {
            for(int i = 0; i < digest.size(); i++) {
                AuthToken token = new AuthToken(key.get(i), algorithm);

                Method hmacMethod = token.getClass().getDeclaredMethod("HMAC", byte[].class);
                hmacMethod.setAccessible(true);
                if (!Arrays.equals((byte[]) hmacMethod.invoke(token, data.get(i)), digest.get(i)))
                    return false;
            }
        } catch (NoSuchAlgorithmException | NoSuchMethodException |
                 IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * <p>Test vectors for Base32</p>
     *
     * @return true if passed test, false otherwise
     * @see <a href="https://tools.ietf.org/html/rfc4648#section-3" target="_blank">RFC 4648</a>
     */
    private boolean base32Test() {
        final Function<byte[], String> base32e = str -> Base32.getEncoder().encodeToString(str);
        final Function<String, byte[]> base32d = str -> Base32.getDecoder().decode(str);
        Map<String, String> data = new HashMap<>();

        data.put("", "");
        data.put("f", "MY======");
        data.put("fo", "MZXQ====");
        data.put("foo", "MZXW6===");
        data.put("foob", "MZXW6YQ=");
        data.put("fooba", "MZXW6YTB");
        data.put("foobar", "MZXW6YTBOI======");

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String encoded = base32e.apply(entry.getKey().getBytes());
            String decoded = new String(base32d.apply(encoded));

            if(!entry.getValue().equals(encoded) || !entry.getKey().equals(decoded))
                return false;
        }

        return true;
    }

    /**
     * <p>Main function for test suite</p>
     *
     * @param args value is ignored
     */
    public static void main(String[] args) {
        new AuthTokenTest();
    }
}