# Project 0 — Bank Account CLI

## Why I built this

I hadn't written real Java before — my background was Python. Before starting anything bigger, I wanted a small, self-contained project to get the toolchain working and feel the actual differences between Python and Java: static types, explicit classes, `==` vs `.equals()`. A tiny `BankAccount` CLI was small enough to finish in one sitting but touches all of that.

---

## What I actually built

A `BankAccount` class (`src/main/java/com/mursalin/warmup/BankAccount.java`) with:
- private fields `owner` (String) and `balance` (double), set through the constructor, which rejects a null/blank owner with `IllegalArgumentException`.
- `deposit(double amount)` — rejects non-positive amounts.
- `withdraw(double amount)` — rejects non-positive amounts and rejects overdrafts (`amount > balance`).
- `getBalance()`, `getOwner()`, and a `toString()` override.
- `equals()`/`hashCode()` overridden on `owner` only — two accounts with the same owner are "equal" even with different balances. I did this deliberately so I'd have to actually explain the identity-vs-equality distinction, not just read about it.

`Main.java` wires up a small demo: create two accounts, deposit/withdraw on one, trigger an overdraft inside a `try/catch` to show the rejection working, and print `==` vs `.equals()` on two different `BankAccount` instances that share an owner to show the difference concretely.

## How to set up the toolchain

1. **JDK 21 (Amazon Corretto)** — I used the AWS-friendly distro since I knew I'd be deploying to EC2 later.
   - macOS: `brew install --cask corretto21`
   - Verify: `java -version` → should print a 21-based version.
2. **Maven**: `brew install maven`, then verify with `mvn -version`.
3. **IntelliJ IDEA Community Edition** — opened this folder directly as a Maven project; it auto-detects `pom.xml`.

## How to run it

```bash
cd java-aws-projects/java/project-0-setup-warmup
mvn -q compile exec:java
```

Expected output: `Alice` after a deposit and withdrawal (balance 120.0), a message confirming Bob's overdraft attempt was rejected, and a demonstration that `==` returns `false` for two distinct `BankAccount` objects while `.equals()` returns `true` when they share an owner.

## What this taught me

| Python instinct | What I actually hit in Java |
|---|---|
| `self.balance` | private fields + methods, everything lives inside a class |
| duck typing | static types — the compiler checks you, not the runtime |
| `a == b` for value equality | `==` is identity; `.equals()` is what you override for value equality |
| `print(...)` | `System.out.println(...)` |
| `python main.py` | compile + run through Maven |
| raising `ValueError` | `throw new IllegalArgumentException(...)` |

The concrete lesson that stuck: `equals()` and `hashCode()` have to be overridden together, because collections like `HashMap`/`HashSet` use `hashCode()` first to find the right bucket, then `equals()` to confirm a match inside it — override one without the other and lookups silently break. I felt this directly in Project 1, where duplicate-cheque-id detection depends on it.
