package com.mursalin.cheque.io;

import com.mursalin.cheque.fraud.FraudRules;
import com.mursalin.cheque.model.Cheque;
import com.mursalin.cheque.validation.Validator;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class ReportWriter {

    private final Validator validator;
private final FraudRules fraudRules;

public ReportWriter(Validator validator, FraudRules fraudRules) {
    this.validator = validator;
    this.fraudRules = fraudRules;
}

public void write(Path path, List<Cheque> clean, List<Cheque> flagged,
                  List<Cheque> rejected, Set<String> duplicateIds) throws IOException {
    Files.createDirectories(path.getParent());
    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path))) {
        out.println("=== CHEQUE PROCESSING REPORT ===");
        out.println("Processed " + (clean.size() + flagged.size() + rejected.size()) + " cheques");
        out.println("  Valid:    " + clean.size());
        out.println("  Flagged:  " + flagged.size());
        out.println("  Rejected: " + rejected.size());

        out.println("\nFLAGGED:");
        flagged.forEach(c -> out.println("  " + c.id() + " -> " + fraudRules.check(c, duplicateIds)));

        out.println("\nREJECTED:");
        rejected.forEach(c -> out.println("  " + c.id() + " -> " + validator.validate(c)));
    }
}
}