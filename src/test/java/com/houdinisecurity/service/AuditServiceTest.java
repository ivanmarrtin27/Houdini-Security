package com.houdinisecurity.service;

import com.houdinisecurity.domain.PasswordEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditServiceTest {

    @Test
    void detectsWeakAndReused() {
        AuditService audit = new AuditService(new StrengthMeter());
        char[] weak = "password".toCharArray();
        PasswordEntry a = PasswordEntry.create("a", "u", weak, "", "");
        PasswordEntry b = PasswordEntry.create("b", "u", weak, "", "");
        long now = System.currentTimeMillis();
        a.touch(now - 400L * 86_400_000L);
        AuditService.AuditReport report = audit.audit(List.of(a, b), 365, 30, now);
        assertTrue(report.issues().stream().anyMatch(i -> i.type() == AuditService.IssueType.WEAK_PASSWORD));
        assertTrue(report.issues().stream().anyMatch(i -> i.type() == AuditService.IssueType.REUSED_PASSWORD));
        assertTrue(report.issues().stream().anyMatch(i -> i.type() == AuditService.IssueType.OLD_PASSWORD));
        a.wipeSecrets();
        b.wipeSecrets();
        java.util.Arrays.fill(weak, '\0');
    }
}
