package com.houdinisecurity.service;

import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.domain.ApiKeyEntry;
import com.houdinisecurity.domain.PasswordEntry;
import com.houdinisecurity.domain.VaultEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AuditService {

    public enum IssueType {
        WEAK_PASSWORD,
        REUSED_PASSWORD,
        OLD_PASSWORD,
        API_KEY_EXPIRING
    }

    public record AuditIssue(IssueType type, String entryId, String title, String detailKey) {
    }

    public record AuditReport(int score0to100, List<AuditIssue> issues) {
    }

    private final StrengthMeter strengthMeter;

    public AuditService(StrengthMeter strengthMeter) {
        this.strengthMeter = strengthMeter;
    }

    public AuditReport audit(List<VaultEntry> entries, int oldDays, int apiWarnDays, long nowMillis) {
        List<AuditIssue> issues = new ArrayList<>();
        Map<String, List<PasswordEntry>> byPassword = new HashMap<>();
        int passwordCount = 0;

        for (VaultEntry entry : entries) {
            if (entry instanceof PasswordEntry p) {
                passwordCount++;
                char[] pw = p.getPasswordCopy();
                try {
                    StrengthMeter.StrengthResult strength = strengthMeter.evaluate(pw);
                    if (strength.score0to100() < 40) {
                        issues.add(new AuditIssue(IssueType.WEAK_PASSWORD, p.getId(), p.displayTitle(), "audit.weak"));
                    }
                    String key = new String(pw);
                    byPassword.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
                } finally {
                    SecureMemory.wipe(pw);
                }
                long ageDays = (nowMillis - p.getUpdatedAt()) / 86_400_000L;
                if (ageDays > oldDays) {
                    issues.add(new AuditIssue(IssueType.OLD_PASSWORD, p.getId(), p.displayTitle(), "audit.old"));
                }
            } else if (entry instanceof ApiKeyEntry a) {
                if (a.getExpiresAt() != null) {
                    long days = (a.getExpiresAt() - nowMillis) / 86_400_000L;
                    if (days <= apiWarnDays) {
                        issues.add(new AuditIssue(IssueType.API_KEY_EXPIRING, a.getId(), a.displayTitle(), "audit.apiExpiring"));
                    }
                }
            }
        }

        for (Map.Entry<String, List<PasswordEntry>> e : byPassword.entrySet()) {
            if (e.getValue().size() > 1 && !e.getKey().isEmpty()) {
                for (PasswordEntry p : e.getValue()) {
                    issues.add(new AuditIssue(IssueType.REUSED_PASSWORD, p.getId(), p.displayTitle(), "audit.reused"));
                }
            }
        }

        int score = 100;
        if (passwordCount == 0 && issues.isEmpty()) {
            score = 100;
        } else {
            score = Math.max(0, 100 - issues.size() * 8);
        }
        return new AuditReport(score, List.copyOf(issues));
    }
}
