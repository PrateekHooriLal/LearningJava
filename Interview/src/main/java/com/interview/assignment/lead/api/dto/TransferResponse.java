package com.interview.assignment.lead.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * =============================================================================
 * DATA TRANSFER OBJECT — TransferResponse
 * =============================================================================
 *
 * WHAT IS IT?
 * The object your REST API returns to the client after a successful transfer.
 * It wraps the result in a clean, client-friendly shape.
 *
 * KEY DESIGN DECISION — "What to return from a POST endpoint?"
 * Three common patterns in REST APIs:
 *   1. Return 204 No Content  → Simple but gives client no feedback.
 *   2. Return the updated resource → May expose too much domain internals.
 *   3. Return a purpose-built response DTO ← Best practice for fintech.
 *
 * WHY THIS PATTERN?
 * - You control exactly what the client sees (no leaking of @Version fields, etc.)
 * - You can evolve the response independently of the domain entity.
 * - Clients get useful context: status, timestamp, amounts debited/credited.
 *
 * INTERVIEW TALKING POINT:
 *   "A transfer POST should return meaningful state so the client can update its UI
 *   without needing a follow-up GET. We return the transfer ID, status, and amounts."
 *
 * =============================================================================
 */
public class TransferResponse {

    // A human-readable outcome (e.g., "SUCCESS", "PENDING")
    private String status;

    private Long fromWalletId;
    private Long toWalletId;

    // The amount actually debited from the sender (original currency)
    private BigDecimal amountDebited;

    // The amount credited to the receiver (may differ if currencies differ)
    private BigDecimal amountCredited;

    // The exchange rate that was applied (1.0 if same currency)
    private BigDecimal exchangeRateApplied;

    // Server timestamp of when the transfer was executed
    private LocalDateTime transferredAt;

    // Message field — useful for conveying fallback info (e.g., "Using cached rate")
    private String message;

    // Private constructor — use the static factory method below for readability
    private TransferResponse() {}

    /**
     * Static factory method — cleaner than a large constructor call.
     * Commonly used in fintech response builders.
     *
     * INTERVIEW: "Why static factory over constructor?"
     *   - Descriptive name: TransferResponse.success() vs new TransferResponse(...)
     *   - Can return subtype or cached instances if needed.
     *   - Joshua Bloch's "Effective Java" Item 1.
     */
    public static TransferResponse success(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amountDebited,
            BigDecimal amountCredited,
            BigDecimal exchangeRateApplied,
            String message) {

        TransferResponse r = new TransferResponse();
        r.status = "SUCCESS";
        r.fromWalletId = fromWalletId;
        r.toWalletId = toWalletId;
        r.amountDebited = amountDebited;
        r.amountCredited = amountCredited;
        r.exchangeRateApplied = exchangeRateApplied;
        r.transferredAt = LocalDateTime.now();
        r.message = message;
        return r;
    }

    // Getters (Jackson serializes via these)
    public String getStatus()                  { return status; }
    public Long getFromWalletId()              { return fromWalletId; }
    public Long getToWalletId()                { return toWalletId; }
    public BigDecimal getAmountDebited()       { return amountDebited; }
    public BigDecimal getAmountCredited()      { return amountCredited; }
    public BigDecimal getExchangeRateApplied() { return exchangeRateApplied; }
    public LocalDateTime getTransferredAt()    { return transferredAt; }
    public String getMessage()                 { return message; }
}
