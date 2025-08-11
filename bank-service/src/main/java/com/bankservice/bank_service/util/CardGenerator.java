package com.bankservice.bank_service.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CardGenerator {
    private static final SecureRandom random = new SecureRandom();

    public static String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String generateCVV() {
        int cvv = 100 + random.nextInt(900);
        return String.valueOf(cvv);
    }

    public static String generateExpirationDate() {
        LocalDate now = LocalDate.now();
        LocalDate exp = now.plusYears(4);
        return exp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
