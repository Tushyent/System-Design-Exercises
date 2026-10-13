package com.library.shorturl;

public class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String encode(long value) {
        if (value == 0) {
            return "0";
        }
        long val = Math.abs(value);
        StringBuilder sb = new StringBuilder();
        while (val > 0) {
            int remainder = (int) (val % 62);
            sb.append(BASE62_CHARS.charAt(remainder));
            val /= 62;
        }
        return sb.reverse().toString();
    }

    public static long decode(String str) {
        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int charIndex = BASE62_CHARS.indexOf(c);
            if (charIndex == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * 62 + charIndex;
        }
        return result;
    }
}
