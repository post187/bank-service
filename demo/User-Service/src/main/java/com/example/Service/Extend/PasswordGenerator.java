package com.example.Service.Extend;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PasswordGenerator {
    private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+";

    public static String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();

        String combinedChars = ALPHA_CAPS + ALPHA + NUMERIC + SPECIAL_CHARS;

        List<Character> pwdChars = IntStream.range(0, length)
                .mapToObj(i -> combinedChars.charAt(random.nextInt(combinedChars.length())))
                .collect(Collectors.toList());

        // Trộn ngẫu nhiên các ký tự
        Collections.shuffle(pwdChars);

        return pwdChars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
