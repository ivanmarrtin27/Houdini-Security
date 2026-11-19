package com.houdinisecurity.service;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StrengthMeter {

    private static final Pattern SEQUENTIAL = Pattern.compile(
            "012|123|234|345|456|567|678|789|890|abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{2,}");
    private static final String[] COMMON = {
            "password", "123456", "qwerty", "admin", "welcome", "letmein", "iloveyou", "monkey", "dragon"
    };

    public record StrengthResult(double entropyBits, int score0to100, String labelKey) {
    }

    public StrengthResult evaluate(char[] password) {
        if (password == null || password.length == 0) {
            return new StrengthResult(0, 0, "strength.none");
        }
        String pwd = new String(password);
        int pool = estimatePool(pwd);
        double entropy = password.length * Math.log(pool) / Math.log(2);
        int penalty = 0;
        String lower = pwd.toLowerCase(Locale.ROOT);
        for (String c : COMMON) {
            if (lower.contains(c)) {
                penalty += 25;
            }
        }
        if (SEQUENTIAL.matcher(lower).find()) {
            penalty += 15;
        }
        if (REPEATED.matcher(pwd).find()) {
            penalty += 10;
        }
        if (password.length < 8) {
            penalty += 20;
        }
        entropy = Math.max(0, entropy - penalty);
        int score = (int) Math.max(0, Math.min(100, Math.round(entropy * 1.2)));
        String label;
        if (score < 25) {
            label = "strength.weak";
        } else if (score < 50) {
            label = "strength.fair";
        } else if (score < 75) {
            label = "strength.good";
        } else {
            label = "strength.strong";
        }
        return new StrengthResult(entropy, score, label);
    }

    private static int estimatePool(String pwd) {
        int pool = 0;
        boolean lower = false, upper = false, digit = false, symbol = false;
        for (int i = 0; i < pwd.length(); i++) {
            char c = pwd.charAt(i);
            if (c >= 'a' && c <= 'z') {
                lower = true;
            } else if (c >= 'A' && c <= 'Z') {
                upper = true;
            } else if (c >= '0' && c <= '9') {
                digit = true;
            } else {
                symbol = true;
            }
        }
        if (lower) {
            pool += 26;
        }
        if (upper) {
            pool += 26;
        }
        if (digit) {
            pool += 10;
        }
        if (symbol) {
            pool += 33;
        }
        return Math.max(pool, 1);
    }
}
