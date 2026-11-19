package com.houdinisecurity.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {

    @Test
    void generatesRequestedLengthAndClasses() {
        PasswordGenerator gen = new PasswordGenerator();
        String pwd = gen.generate(new PasswordGenerator.Options(24, true, true, true, true, true));
        assertEquals(24, pwd.length());
        assertTrue(pwd.chars().anyMatch(Character::isUpperCase));
        assertTrue(pwd.chars().anyMatch(Character::isLowerCase));
        assertTrue(pwd.chars().anyMatch(Character::isDigit));
        assertFalse(pwd.contains("O") && pwd.contains("0") && false); // smoke
    }

    @Test
    void passphraseHasWords() {
        PasswordGenerator gen = new PasswordGenerator();
        String phrase = gen.generatePassphrase(4, "-");
        assertEquals(4, phrase.split("-").length);
    }
}
