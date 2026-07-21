# Fraud Lakehouse — Spark + Iceberg mini-project

**Why this exists:** a hands-on build to get real experience with Spark and Iceberg beyond reading about them — a data lakehouse pipeline over synthetic fraud-labeled transaction data. This doubles as a revision doc: written so *future-you* can re-read it and remember both the concepts and the real bugs hit while building it.

---

## 1. The concepts, explained simply (read this first)

**Data lake** — just a big folder of files (in our case, in MinIO, standing in for AWS S3). Historically, "a folder of files" had no real database-like guarantees: no easy way to change data safely, no history, painful schema changes.

**Table format (this is what Iceberg is)** — a layer of bookkeeping on top of that folder of files that makes it behave like a real table. Iceberg never edits files in place. Every write creates new files, then updates a small "table of contents" (called a **snapshot**) to point at the current correct set of files. Old snapshots and old files don't get deleted — they just stop being "current."

**Snapshot** — one version of the table's table-of-contents, created every time you write, update, or change the schema. Because old snapshots stick around, you get two superpowers for free:
- **Time travel** — query the table as it looked at an old snapshot.
- **Schema evolution** — add/change columns without rewriting old files; the schema itself is versioned right alongside the data.

**Catalog** — the service that keeps track of *which* snapshot is "current" for each table, and where to find a table's metadata. We used a **REST catalog** (a catalog you talk to over HTTP) — the `iceberg-rest` container.

**Spark** — the actual compute engine. It reads files, runs your transformations (`.filter`, `.groupBy`, `.join`, etc.), and writes results. Spark on its own knows nothing about "tables with history" — it needs the Iceberg plugin to understand that.

**MinIO** — a stand-in for AWS S3 on your own laptop. Same API, so anything you learn here about "object storage" transfers directly to real S3.

**Lazy evaluation** — Spark doesn't actually run anything when you write `.filter(...)` or `.withColumn(...)`. It just builds a plan. Nothing executes until you call an **action** — `.show()`, `.count()`, or a write like `.writeTo(...).createOrReplace()`. This is why you can chain a dozen transformations with no delay, then everything runs at once when you finally ask for a result.

**Transformation vs. action** — transformation = builds the plan, doesn't run (`.filter`, `.withColumn`, `.groupBy`, `.join`). Action = actually triggers execution (`.show()`, `.count()`, `.collect()`, any write).

**Partition** (in the Spark-processing sense, not the Iceberg storage sense) — Spark splits your data into chunks so it can process them in parallel. Our 6.3M-row CSV got split into 14 partitions automatically.

---

## 2. The architecture — what each container actually does

Four Docker containers, wired together:

| Container | What it is | What it does here |
|---|---|---|
| `spark-iceberg` | Spark + the Iceberg plugin | Runs our Python scripts, does all the actual data processing |
| `iceberg-rest` (`rest`) | The catalog | Tracks which snapshot is "current" for each table |
| `minio` | Object storage (fake S3) | Where the actual Parquet data files and metadata JSON live |
| `mc` | MinIO's setup helper | Runs once at startup to create the `warehouse` bucket, then exits — this is expected, not a failure |

Catalog name in Spark config: `demo`. Full table names look like `demo.fraud.account_summary` — catalog `demo`, database `fraud`, table `account_summary`.

---

## 3. What we actually built, milestone by milestone

### Milestone 1 — load the data
Loaded a 6.36M-row PaySim CSV (fraud dataset, simulated mobile-money transactions) into a real Iceberg table (`demo.fraud.transactions`) via `src/load_iceberg.py`, run with `spark-submit` (the real way production Spark jobs get run — not clicking through notebook cells).

### Milestone 2 — transformation pipeline (`src/transform.py`)
Four pure functions, each taking/returning a DataFrame (this matters — see Testing below):
- `clean_transactions` — filters bad rows, buckets `amount` into small/medium/large.
- `fraud_rate_by_type` — aggregation: fraud rate per transaction type.
- `account_summary` — aggregation: per-account transaction count, total sent, fraud count.
- `enrich_with_account_risk` — a real **join**: attaches each sender's historical fraud count to every transaction (left join, so first-time senders with no history aren't dropped).

Wrote three new Iceberg tables from this: `fraud_rate_by_type`, `account_summary`, `transactions_enriched`.

**Testing:** 5 pytest tests against these functions, run *inside the container* (not on the host — see Detour 3 below for why), using a tiny local Spark session with fake in-memory data. All passing. The reason this works without Docker/Iceberg/MinIO running at all is exactly because these functions take/return plain DataFrames instead of reading/writing the catalog themselves.

### Milestone 3 — the actual "why Iceberg" payoff
Proved two things Iceberg can do that plain Parquet can't, with real before/after query output:
- **Schema evolution**: ran `ALTER TABLE ... ADD COLUMN risk_tier STRING` on `account_summary` — existing rows were untouched, just showed `NULL` for the new column. No rewrite, no downtime.
- **Time travel**: queried the table `VERSION AS OF` the snapshot *before* the schema change — confirmed the `risk_tier` column doesn't appear **at all** in that old snapshot (not even as `NULL`), because Iceberg resolves the schema that was active *at that snapshot*, not the current one. This is the concrete, provable answer to "why does Iceberg matter, versus just files."

### Milestone 4 (stretch) — rule-based fraud flagging (`src/flag_fraud.py`)
A real fraud-detection heuristic from the PaySim literature: flag `TRANSFER`/`CASH_OUT` transactions where the sender's account got fully emptied (`oldbalanceOrg == amount` and `newbalanceOrig == 0`). Checked the rule against the real `isFraud` labels — a genuine evaluation, not a guess:

| | rule says NOT fraud | rule says fraud |
|---|---|---|
| **actually not fraud** | 6,354,407 | 0 |
| **actually fraud** | 189 | 8,024 |

**Precision: 100%** (every flagged transaction really was fraud). **Recall: ~97.7%** (8,024 of 8,213 total fraud cases caught). Total fraud count (8,213) independently matches PaySim's well-known published number — a good sanity check that nothing was silently broken upstream.

---

## 4. Real debugging detours

### Detour 1 — port conflict with a leftover process
`docker compose up` failed: `port 8080 already in use`. Root cause: my own **Document API from a separate Java project** was still running in the background on port 8080 (default Spring Boot port), left over from earlier testing. Fixed by finding the process (`lsof -nP -iTCP:8080 -sTCP:LISTEN`), identifying it (`ps -p <pid>`), and killing it. Lesson: always check *what* is holding a port before assuming the new service is broken.

### Detour 2 — OutOfMemoryError writing the Iceberg table
The very first `spark-submit` (Milestone 1) crashed with `java.lang.OutOfMemoryError: Java heap space`, deep in Parquet's dictionary-encoding code, while writing the `amount` column. Root cause: Spark's default driver memory is a tiny **1GB**, and our `amount` column has millions of near-unique decimal values — Parquet tries to build a dictionary (a lookup table of "values I've seen") to compress the column, and that dictionary itself grew too large for the tiny heap before Parquet could fall back to a simpler encoding. Not a code bug — a resource-sizing problem. Fixed with `spark-submit --driver-memory 4g` (comfortably inside the 8GB Docker had available, on a 48GB machine). This is a legitimate "diagnosed a resource-constrained production-style failure from the actual stack trace" story.

### Detour 3 — pytest couldn't find `pyspark`
Running `pytest` inside the container failed: `ModuleNotFoundError: No module named 'pyspark'`. Root cause: the container's default `PYTHONPATH` (`/opt/spark/python:/opt/spark/python/lib/py4j-...zip`) is what makes `pyspark` importable — `spark-submit` sets this up automatically, but a plain `docker exec -e PYTHONPATH=/home/iceberg/src ...` command I gave **overwrote** that variable instead of adding to it, wiping out the path to `pyspark` itself. Fixed by combining both paths with `:`. Lesson: environment variables get *replaced*, not merged, unless you do it yourself.

### Detour 4 — Iceberg metadata-table queries broken against this specific REST catalog
Trying to list a table's snapshot history via SQL (`SELECT * FROM table.snapshots`) failed with a cryptic `400 Suspicious Path Character` error from the REST catalog. Traced it by reading the actual failing URL in the stack trace: Iceberg needs to strip `.snapshots` off the identifier and resolve the real table separately, but instead it tried to treat `fraud.account_summary` as one nested namespace and `snapshots` as a literal table — which the REST catalog server rejected as a malformed path. Confirmed this wasn't just a SQL-parser issue by trying the exact same query through PySpark's DataFrame API — same failure, same URL. This pointed to a real limitation in `apache/iceberg-rest-fixture` itself (it's literally named "fixture" — a lightweight test catalog, not a full production one). Worked around it entirely by switching to **PyIceberg**, a separate Python client library that loads a table's full metadata in one clean call and inspects snapshots locally, rather than asking the catalog to resolve a special "virtual" metadata table over HTTP. Good story: recognized a tool's limitation, found a different tool built for the same job, and explained *why* the workaround actually avoided the bug rather than just happening to work.

---

## 5. Quick command reference

```bash
# bring the stack up / down
docker compose up -d
docker compose down

# run a script
docker exec spark-iceberg spark-submit --driver-memory 4g /home/iceberg/src/<script>.py

# interactive SQL
docker exec -it spark-iceberg spark-sql

# run tests
docker exec -e PYTHONPATH="/opt/spark/python:/opt/spark/python/lib/py4j-0.10.9.7-src.zip:/home/iceberg/src" spark-iceberg pytest /home/iceberg/tests -v

# check snapshots (via PyIceberg, sidesteps the REST fixture bug)
docker exec spark-iceberg python3 /home/iceberg/src/check_snapshots.py
```

---

## 6. What this project demonstrates

- **Hands-on Spark/Iceberg** — a full pipeline (load → transform → evaluate) against a 6.3M-row dataset, plus schema evolution and time travel demonstrated with real before/after query output.
- **Debugging production-style issues** — any of the four detours above; the OOM one (Detour 2) is the strongest, a genuine resource-sizing diagnosis from a real stack trace, not a typo fix.
- **Evaluating whether a rule/model is any good** — the Milestone 4 confusion matrix: don't just eyeball a few rows, check real precision/recall against ground truth.
- **Why Iceberg over plain Parquet** — the Milestone 3 result: added a column with zero downtime, and proved old snapshots don't retroactively gain that column — schema is versioned data, not just a side detail.
- **AI-assisted development** — this whole project was built working iteratively with an AI assistant: real root-causing of real errors (stack traces read, not guessed at), not blindly accepting generated code.
