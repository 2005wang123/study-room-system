package com.example.studyroom.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdCardUtilTest {

    @Test
    void defaultPasswordUsesLast6Digits() {
        assertEquals("011234", IdCardUtil.getDefaultPasswordFromIdCard("110101199001011234"));
    }

    @Test
    void defaultPasswordFallbackWhenInvalid() {
        assertEquals("123456", IdCardUtil.getDefaultPasswordFromIdCard(null));
        assertEquals("123456", IdCardUtil.getDefaultPasswordFromIdCard("123"));
        assertEquals("123456", IdCardUtil.getDefaultPasswordFromIdCard(""));
    }

    @Test
    void validIdCardAccepted() {
        assertTrue(IdCardUtil.isValidIdCard("110101199001011234"));
        assertTrue(IdCardUtil.isValidIdCard("11010119900101123X"));
        assertTrue(IdCardUtil.isValidIdCard("11010119900101123x"));
    }

    @Test
    void invalidIdCardRejected() {
        assertFalse(IdCardUtil.isValidIdCard(null));
        assertFalse(IdCardUtil.isValidIdCard("12345"));
        assertFalse(IdCardUtil.isValidIdCard("abcdefghijklmnopqr"));
        assertFalse(IdCardUtil.isValidIdCard("11010119900101123A"));
    }
}