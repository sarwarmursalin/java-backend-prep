package com.mursalin.cheque.report;

import com.mursalin.cheque.model.Cheque;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ReportWriter {

    public void write(Path outputPath,
                      List<Cheque> valid,
                      Map<Cheque, List<String>> flagged,
                      Map<String, String> rejected) throws IOException {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
