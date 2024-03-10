package com.xpb.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class StringUtil {
    private static final SecureRandom random = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public static String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be a positive number");
        }

        // 由于Base64编码每4个字符对应3个字节，我们可以根据需要的字符串长度反推需要生成的字节数
        // 为了保证结果字符串至少与请求的长度相等，我们可能需要生成更多的字节
        int byteLength = (length * 3) / 4;

        byte[] randomBytes = new byte[byteLength];
        random.nextBytes(randomBytes);
        String encoded = encoder.encodeToString(randomBytes);

        // 如果编码后的字符串长于请求的长度，则进行截断
        return encoded.length() > length ? encoded.substring(0, length) : encoded;
    }
}
