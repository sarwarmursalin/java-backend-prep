package com.mursalin.cheque.io;

import com.mursalin.cheque.model.Cheque;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChequeReader {

    public List<Cheque> read(Path path) throws IOException {
        List<Cheque> cheques = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                cheques.add(new Cheque(
                        parts[0],
                        parts[1],
                        parts[2],
                        new BigDecimal(parts[3]),
                        parts[4],
                        LocalDate.parse(parts[5])
                ));
            }
        }
        return cheques;
    }
}