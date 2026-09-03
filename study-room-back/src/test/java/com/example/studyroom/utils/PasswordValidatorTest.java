package com.example.studyroom.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    // 与 application.yml 默认配置一致：min-length=8, require-special-char, require-upper-lower
    private final PasswordValidator validator = new PasswordValidator(8, true, true);

    @Test
    void tooShortIsWeak() {
        assertTrue(validator.isWeakPassword("Ab1!23"));
    }

    @Test
    void missingSpecialCharIsWeak() {
        assertTrue(validator.isWeakPassword("Abcdef12"));
    }

    @Test
    void missingUpperOrLowerIsWeak() {
        assertTrue(validator.isWeakPassword("abcdef12!"));
        assertTrue(validator.isWeakPassword("ABCDEF12!"));
    }

    @Test
    void strongPasswordPasses() {
        assertFalse(validator.isWeakPassword("Abcdef12!"));
        assertFalse(validator.isWeakPassword("Abcd1234!@#"));
    }

    @Test
    void nullOrEmptyIsWeak() {
        assertTrue(validator.isWeakPassword(null));
        assertTrue(validator.isWeakPassword(""));
    }

    @Test
    void strengthScore() {
        assertEquals(0, validator.checkStrength(null));
        assertEquals(6, validator.checkStrength("Abcdef12!xyz"));
    }
}