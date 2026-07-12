package com.mursalin.cheque.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Cheque(String id, String payer, String payee,
                BigDecimal amount, String routingNumber, LocalDate date) {}
