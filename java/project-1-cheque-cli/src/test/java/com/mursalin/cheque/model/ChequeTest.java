package com.mursalin.cheque.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ChequeTest {

    @Test
    void twoChequesWithSameIdButDifferentAmountAreNotEqual() {
        Cheque c1 = new Cheque("CHQ-001", "Alice", "Bob", new BigDecimal("5000.00"), "021000021", LocalDate.of(2024, 1, 1));
        Cheque c2 = new Cheque("CHQ-001", "Alice", "Bob", new BigDecimal("9999.00"), "021000021", LocalDate.of(2024, 1, 1));

        assertNotEquals(c1, c2);
    }

    @Test
    void toStringContainsIdAndAmount() {
        Cheque c = new Cheque("CHQ-042", "Alice", "Bob", new BigDecimal("1500.00"), "021000021", LocalDate.of(2024, 6, 1));

        String result = c.toString();

        assertTrue(result.contains("CHQ-042"));
        assertTrue(result.contains("1500.00"));
    }
}
