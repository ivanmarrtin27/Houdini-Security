package com.houdinisecurity.service;

import com.houdinisecurity.domain.PasswordEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports a simple CSV: site,username,password,url,notes (header optional).
 * Plaintext warning is the caller's responsibility in UI.
 */
public final class CsvImportService {

    public List<PasswordEntry> importPasswords(Path csvPath) throws IOException {
        List<PasswordEntry> entries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                if (first && looksLikeHeader(cols)) {
                    first = false;
                    continue;
                }
                first = false;
                String site = col(cols, 0);
                String username = col(cols, 1);
                String password = col(cols, 2);
                String url = col(cols, 3);
                String notes = col(cols, 4);
                char[] pw = password.toCharArray();
                try {
                    entries.add(PasswordEntry.create(site, username, pw, url, notes));
                } finally {
                    java.util.Arrays.fill(pw, '\0');
                }
            }
        }
        return entries;
    }

    private static boolean looksLikeHeader(List<String> cols) {
        if (cols.isEmpty()) {
            return false;
        }
        String c0 = cols.get(0).toLowerCase();
        return c0.contains("site") || c0.contains("url") || c0.contains("name") || c0.contains("title");
    }

    private static String col(List<String> cols, int i) {
        return i < cols.size() ? cols.get(i) : "";
    }

    static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        result.add(cur.toString());
        return result;
    }
}
