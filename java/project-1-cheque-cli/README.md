# Project 1 — Cheque Fraud-Flagging CLI

## Why I built this

After Project 0, I wanted something that forced me through the Core Java pillars that actually come up in backend work — collections, streams, exceptions, file I/O — instead of disconnected tutorials. A cheque-processing tool that reads records, validates them, and flags suspicious ones felt like a real, self-contained domain to build that around, and it mirrors the kind of fraud-detection logic financial-services backends actually run.

---

## What I actually built

A `Cheque` record (`id`, `payer`, `payee`, `amount`, `routingNumber`, `date`) with three pieces of logic around it:

- **`Validator`** (`validation/Validator.java`) — checks a cheque for structural problems: blank id/payer/payee, non-positive amount, a routing number that isn't 9 digits, or a date in the future. Returns a `List<String>` of problems (empty = valid) rather than throwing, so a bad cheque doesn't stop the batch.
- **`FraudRules`** (`fraud/FraudRules.java`) — three rules checked against an already-valid cheque: `STRUCTURING` (amount between $9,900–$9,999.99, just under the $10,000 reporting threshold), `DUPLICATE_ID` (id seen earlier in the same batch), and `WATCHLIST` (payee matches a hardcoded list of flagged entities). A cheque can trigger more than one flag at once.
- **`ChequeReader`** (`io/ChequeReader.java`) — reads a CSV file line by line and parses each row into a `Cheque`.

`Main.java` wires these together: read `inbox/cheques.csv`, use a stream (`groupingBy` + `counting`) to find duplicate ids up front, then `partitioningBy` twice — first into valid/rejected, then the valid ones into clean/flagged — and write a summary report via `ReportWriter` (`io/ReportWriter.java`).

**Tests:** 17 JUnit tests across `FraudRulesTest`, `ValidatorTest`, and `ChequeTest` — each fraud rule and each validation check has a dedicated positive and negative case, plus one test confirming a cheque can carry all three flags simultaneously.

## How to run it

```bash
cd java-aws-projects/java/project-1-cheque-cli
mvn -q compile exec:java -Dexec.mainClass=com.mursalin.cheque.Main
```

This reads `inbox/cheques.csv`, prints a summary to the console, and writes a full report to `reports/summary.txt`.

To run the tests:
```bash
mvn test
```

## What's genuinely not finished here

I want this documented honestly rather than glossed over, since I'll be the one relying on it later:

- **No concurrency.** Everything runs on a single thread. There's no `ExecutorService`/`CompletableFuture` anywhere in the code, despite an earlier commit message calling the concurrency milestone "complete" — that commit message was wrong; the code doesn't back it up.
- **No CLI arguments.** `Main.java` has the input path (`inbox/cheques.csv`) hardcoded. There's no `--report` or `--threads` flag.
- **Single file only, CSV only.** It doesn't scan a folder of multiple files, and there's no JSON support — `ChequeReader` only reads one CSV.
- **An unfinished, undocumented REST layer.** `App.java` and `api/ChequeController.java` add a Spring Boot REST endpoint (`POST /cheques/process`) that duplicates the same validate/flag logic as `Main.java`. I started this as an experiment but never finished or wired it up properly, and it isn't mentioned anywhere else in this project.
- **Dead code.** There's a second `ReportWriter` class at `report/ReportWriter.java` that just throws `UnsupportedOperationException` — it's not used anywhere; the real one is `io/ReportWriter.java`.

If I pick this project back up, the next real milestone is adding concurrency (`ExecutorService`, one task per file) over a folder of files, plus deciding whether to keep or delete the REST layer rather than leaving it half-built.

## What this taught me

- **Streams over loops** — `partitioningBy`/`groupingBy` made the valid/flagged/rejected split and duplicate-id detection much clearer than manual loops with mutable lists would have been.
- **`equals`/`hashCode` mattering for real** — duplicate-id detection via a `Set`/`Map` only works correctly because `Cheque` is a record (which generates both for free based on all fields).
- **Validation returning a list of problems instead of throwing** — this let one bad cheque get reported as "rejected with reasons" instead of crashing the whole batch, which is closer to how a real ingestion pipeline should behave.
