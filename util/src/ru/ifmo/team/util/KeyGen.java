package ru.ifmo.team.util;

import java.util.Random;

/**
 * User: Daniel Penkin
 * Date: May 7, 2009
 * Version: 1.0
 */
public class KeyGen {

    private static final Random random = new Random();

    public static String generate(int length) {
        byte[] cid = new byte[length];
        random.nextBytes(cid);
        return byte2hex(cid, 0, cid.length);
    }

    private static String byte2hex(byte in[], int offset, int len) {
        String hex;
        StringBuffer result = new StringBuffer(len * 2);
        int i;

        for (i = offset, len += offset; i < len; i++) {
            hex = Integer.toHexString(in[i] & 0xff);
            if (hex.length() == 1) result.append('0').append(hex);
            else result.append(hex);
        }

        return result.toString().toUpperCase();
    }

}
