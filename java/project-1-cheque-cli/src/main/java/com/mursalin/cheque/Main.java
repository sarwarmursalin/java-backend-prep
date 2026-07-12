package com.mursalin.cheque;

import com.mursalin.cheque.model.Cheque;
import com.mursalin.cheque.io.ChequeReader;
import com.mursalin.cheque.io.ReportWriter;
import java.nio.file.Path;

import com.mursalin.cheque.fraud.FraudRules;
import com.mursalin.cheque.validation.Validator;
import java.util.stream.Collectors;
import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        List<Cheque> cheques = new ChequeReader().read(Path.of("inbox/cheques.csv"));

        Validator validator = new Validator();
        FraudRules fraudRules = new FraudRules();

        // Build set of duplicate ids using streams
        Set<String> duplicateIds = cheques.stream()
                .collect(Collectors.groupingBy(Cheque::id, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Partition: valid data (true) vs rejected/malformed (false)
        Map<Boolean, List<Cheque>> byValidity = cheques.stream()
                .collect(Collectors.partitioningBy(c -> validator.validate(c).isEmpty()));

        List<Cheque> validOrFlagged = byValidity.get(true);
        List<Cheque> rejected       = byValidity.get(false);

        // Partition valid ones: clean (true) vs fraud-flagged (false)
        Map<Boolean, List<Cheque>> byFraud = validOrFlagged.stream()
                .collect(Collectors.partitioningBy(c -> fraudRules.check(c, duplicateIds).isEmpty()));

        List<Cheque> clean   = byFraud.get(true);
        List<Cheque> flagged = byFraud.get(false);

        // Print summary
        System.out.println("Processed " + cheques.size() + " cheques");
        System.out.println("  V Valid:    " + clean.size());
        System.out.println("  ! Flagged:  " + flagged.size());
        System.out.println("  X Rejected: " + rejected.size());

        System.out.println("\nFLAGGED:");
        flagged.forEach(c -> System.out.println("  " + c.id() + " -> " + fraudRules.check(c, duplicateIds)));

        System.out.println("\nREJECTED:");
        rejected.forEach(c -> System.out.println("  " + c.id() + " -> " + validator.validate(c)));


        new ReportWriter(validator, fraudRules)
                .write(Path.of("reports/summary.txt"), clean, flagged, rejected, duplicateIds);
        System.out.println("\nReport written to reports/summary.txt");
    }
}
