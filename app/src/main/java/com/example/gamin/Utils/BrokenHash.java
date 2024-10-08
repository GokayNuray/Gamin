package com.example.gamin.Utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates a broken Minecraft-style twos-complement signed
 * hex digest. Tested and confirmed to match vanilla.
 * shamelessly stolen from <a href="https://gist.github.com/unascribed/70e830d471d6a3272e3f">unascribed</a>
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
