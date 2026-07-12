package com.mursalin.cheque;

import com.mursalin.cheque.model.Cheque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        List<Cheque> cheques = List.of(
            new Cheque("CHQ-001", "Alice Corp",   "Bob LLC",        new BigDecimal("5000.00"), "021000021", LocalDate.of(2024, 3, 15)),
            new Cheque("CHQ-001", "Golam Corp",   "Bob LLC",        new BigDecimal("4351.00"), "021000021", LocalDate.of(2024, 4, 12)),
            new Cheque("CHQ-002", "Exon Corp",    "Bob LLC",        new BigDecimal("9950.00"), "021000021", LocalDate.of(2024, 6,  4)),
            new Cheque("CHQ-003", "Neson Corp",   "Shell Corp Ltd", new BigDecimal("1248.00"), "021000021", LocalDate.of(2024, 7, 16)),
            new Cheque("CHQ-004", "Logi Corp",    "Bob LLC",        new BigDecimal("3400.00"), "021000021", LocalDate.of(2023, 1, 21)),
            new Cheque("CHQ-005", "Tanson Corp",  "Bob LLC",        new BigDecimal("1200.00"), "021000021", LocalDate.of(2025, 3, 15))
        );

        cheques.forEach(System.out::println);

        Map<String, Integer> idCount = new HashMap<>();
        for (Cheque c : cheques) {
            idCount.put(c.id(), idCount.getOrDefault(c.id(), 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : idCount.entrySet()) {
            if (entry.getValue() > 1) {
                System.out.println("DUPLICATE: " + entry.getKey() + " appears " + entry.getValue() + " times");
            }
        }
    }
}
