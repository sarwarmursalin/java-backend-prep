# Project 0 — Setup & Warmup (Week 1)

**Goal:** Get a working Java toolchain and write your first real Java by building a tiny `BankAccount` CLI. This is the "hello, the language is different from Python" project.

**You'll build:** a command-line demo that creates accounts, deposits/withdraws money, rejects overdrafts, and prints balances.

**Concepts this teaches (Python → Java map):**
| Python instinct | Java reality you'll meet here |
|---|---|
| `self.balance` | private fields + methods, everything in a class |
| duck typing | static types, the compiler checks you |
| `a == b` for value equality | `==` is identity; use `.equals()` for value |
| `print(...)` | `System.out.println(...)` |
| `python main.py` | compile + run via Maven (`mvn ...`) |
| raising `ValueError` | `throw new IllegalArgumentException(...)` |

---

## Milestone 0 — Install the toolchain (~45 min)
1. **JDK 21 (Amazon Corretto)** — AWS-friendly distro.
   - macOS: `brew install --cask corretto21` (or download from AWS).
   - Verify: `java -version` → should say 21.
2. **Maven:** `brew install maven` → verify `mvn -version`.
3. **IntelliJ IDEA Community Edition** (free) — the standard Java IDE. Open this folder as a Maven project; it auto-detects `pom.xml`.

✅ Done when `java -version` and `mvn -version` both print 21-based output.

## Milestone 1 — Run the starter (~15 min)
This folder is already a runnable Maven project.
```bash
cd java-aws-projects/java/project-0-setup-warmup
mvn -q compile exec:java
```
You should see the demo in `Main.java` run. If it compiles and prints, your toolchain works.

## Milestone 2 — Build BankAccount yourself (~2–3 hrs)
Open `src/main/java/com/mursalin/warmup/BankAccount.java` (a stub) and implement it:
- private fields: `owner` (String), `balance` (double, or better `long` cents).
- constructor that sets the owner and a starting balance.
- `deposit(double amount)` — reject non-positive amounts with `IllegalArgumentException`.
- `withdraw(double amount)` — reject overdrafts (`balance - amount < 0`) with an exception.
- `getBalance()` and a `toString()` override.
- Override `equals()` and `hashCode()` based on `owner` (you'll feel why in Project 1).

Then wire a small demo in `Main.java`: create two accounts, do some deposits/withdrawals, try an illegal one inside a `try/catch`, print results.

✅ **Project 0 cleared when:**
- [ ] Toolchain installed (`java`/`mvn` are 21).
- [ ] Starter runs via `mvn exec:java`.
- [ ] `BankAccount` rejects overdrafts and non-positive amounts.
- [ ] You overrode `equals()`/`hashCode()` and can explain *why `==` wouldn't work*.
- [ ] Pushed to GitHub with this checklist ticked.

## Stretch (optional)
- Make `BankAccount` immutable (final fields, return a new account on each operation) and notice how that changes the design — immutability is a big concurrency theme later.

## Interview seeds this plants
- "What's the difference between `==` and `.equals()` in Java?"
- "Why do `equals()` and `hashCode()` have to be overridden together?"
- "Coming from Python, what surprised you about Java?" ← have a crisp, honest answer after this week.

## Resources
- Amigoscode — "Java Tutorial for Beginners" (syntax map in one sitting).
- Coding with John — "equals() and hashCode()" (do this before Milestone 2's equals task).
- Oracle Dev.java "Getting Started" trail.

➡️ **Next:** [Project 1 — Cheque Processing CLI](../project-1-cheque-cli/README.md)
