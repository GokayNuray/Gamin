package com.example.gamin.Utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates a broken Minecraft-style twos-complement signed
 * hex digest. Tested and confirmed to match vanilla.
 */
public final class BrokenHash {
    public static String sha1(final byte[]... args) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        for (final byte[] arg : args) {
            md.update(arg);
        }
        return new BigInteger(md.digest()).toString(16);
    }
}
