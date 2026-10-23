package com.pharma.erp.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Indian-system amount in words (lakh / crore) for e-receipts, e.g. "Rupees One Lakh and Fifty Paise Only". */
public final class AmountInWords {

    private static final String[] UNITS = {
        "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private AmountInWords() {}

    public static String rupees(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be zero or positive");
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = rounded.longValue();
        int paise = rounded.subtract(BigDecimal.valueOf(rupees)).movePointRight(2).intValue();
        StringBuilder words = new StringBuilder("Rupees ").append(words(rupees));
        if (paise > 0) {
            words.append(" and ").append(words(paise)).append(" Paise");
        }
        return words.append(" Only").toString();
    }

    static String words(long n) {
        if (n < 20) {
            return UNITS[(int) n];
        }
        StringBuilder sb = new StringBuilder();
        n = appendGroup(sb, n, 10_000_000L, "Crore");
        n = appendGroup(sb, n, 100_000L, "Lakh");
        n = appendGroup(sb, n, 1_000L, "Thousand");
        n = appendGroup(sb, n, 100L, "Hundred");
        if (n > 0) {
            append(sb, belowHundred((int) n));
        }
        return sb.toString();
    }

    private static long appendGroup(StringBuilder sb, long n, long divisor, String label) {
        if (n >= divisor) {
            append(sb, words(n / divisor) + " " + label);
        }
        return n % divisor;
    }

    private static String belowHundred(int n) {
        if (n < 20) {
            return UNITS[n];
        }
        return n % 10 == 0 ? TENS[n / 10] : TENS[n / 10] + " " + UNITS[n % 10];
    }

    private static void append(StringBuilder sb, String part) {
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(part);
    }
}
