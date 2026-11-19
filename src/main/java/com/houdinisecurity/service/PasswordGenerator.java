package com.houdinisecurity.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}:,.?/";
    private static final String AMBIGUOUS = "O0oIl1|`'\"";

    private final SecureRandom random;
    private final List<String> wordlist;

    public PasswordGenerator() {
        this(new SecureRandom(), loadEmbeddedWordlist());
    }

    public PasswordGenerator(SecureRandom random, List<String> wordlist) {
        this.random = random;
        this.wordlist = List.copyOf(wordlist);
    }

    public record Options(
            int length,
            boolean upper,
            boolean lower,
            boolean digits,
            boolean symbols,
            boolean excludeAmbiguous
    ) {
        public static Options defaults() {
            return new Options(20, true, true, true, true, true);
        }
    }

    public String generate(Options options) {
        Objects.requireNonNull(options);
        if (options.length() < 4) {
            throw new IllegalArgumentException("length must be >= 4");
        }
        StringBuilder alphabet = new StringBuilder();
        List<String> requiredSets = new ArrayList<>();
        if (options.upper()) {
            String s = filter(UPPER, options.excludeAmbiguous());
            alphabet.append(s);
            requiredSets.add(s);
        }
        if (options.lower()) {
            String s = filter(LOWER, options.excludeAmbiguous());
            alphabet.append(s);
            requiredSets.add(s);
        }
        if (options.digits()) {
            String s = filter(DIGITS, options.excludeAmbiguous());
            alphabet.append(s);
            requiredSets.add(s);
        }
        if (options.symbols()) {
            String s = filter(SYMBOLS, options.excludeAmbiguous());
            alphabet.append(s);
            requiredSets.add(s);
        }
        if (alphabet.isEmpty()) {
            throw new IllegalArgumentException("At least one character class required");
        }
        char[] out = new char[options.length()];
        int i = 0;
        for (String set : requiredSets) {
            if (i < out.length) {
                out[i++] = set.charAt(random.nextInt(set.length()));
            }
        }
        for (; i < out.length; i++) {
            out[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        shuffle(out);
        return new String(out);
    }

    public String generatePassphrase(int wordCount, String separator) {
        if (wordCount < 3) {
            throw new IllegalArgumentException("wordCount must be >= 3");
        }
        if (wordlist.isEmpty()) {
            throw new IllegalStateException("Empty wordlist");
        }
        String sep = separator == null ? "-" : separator;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(wordlist.get(random.nextInt(wordlist.size())));
        }
        return sb.toString();
    }

    private void shuffle(char[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    private static String filter(String alphabet, boolean excludeAmbiguous) {
        if (!excludeAmbiguous) {
            return alphabet;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < alphabet.length(); i++) {
            char c = alphabet.charAt(i);
            if (AMBIGUOUS.indexOf(c) < 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static List<String> loadEmbeddedWordlist() {
        List<String> words = new ArrayList<>();
        InputStream in = PasswordGenerator.class.getResourceAsStream("/wordlists/eff-short.txt");
        if (in == null) {
            return List.of("correct", "horse", "battery", "staple", "alpha", "bravo", "charlie",
                    "delta", "echo", "foxtrot", "golf", "hotel", "india", "juliet", "kilo", "lima");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    words.add(line.toLowerCase());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load wordlist", e);
        }
        return words;
    }
}
