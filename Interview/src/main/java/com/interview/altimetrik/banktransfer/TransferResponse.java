package com.interview.altimetrik.banktransfer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * ============================================================
 * LAYER 4 — Output DTO
 * ============================================================
 *
 * PURPOSE:
 *   Represents the JSON body sent BACK to the client after a transfer.
 *   Again, NOT an @Entity — only carries data for the HTTP response.
 *
 * ---- STATIC FACTORY METHOD PATTERN (INTERVIEW TALKING POINT) ----
 *   Instead of forcing the caller to call new TransferResponse(...)
 *   with multiple args (error-prone), we expose static factory methods:
 *
 *     TransferResponse.success(1L, 2L, new BigDecimal("5000"))
 *     TransferResponse.failure("Insufficient balance")
 *
 *   Benefits:
 *     1. Self-documenting — the method name says what kind of response it is
 *     2. Hides constructor arg order — no risk of mixing up debitId and creditId
 *     3. The failure() factory fills irrelevant fields with null cleanly
 *
 *   This is "Item 1: Consider static factory methods instead of constructors"
 *   from Effective Java (Joshua Bloch).
 *
 * ---- WHAT JACKSON DOES WITH THIS ----
 *   Spring uses Jackson to serialize this object to JSON for the response.
 *   Jackson reads fields via getters (because of @Getter).
 *   Null fields are included by default — can exclude with @JsonInclude(NON_NULL).
 *
 * SUCCESS RESPONSE (HTTP 200):
 *   {
 *     "status": "SUCCESS",
 *     "debitAccountId": 1,
 *     "creditAccountId": 2,
 *     "amount": 5000.00,
 *     "message": "Transfer successful"
 *   }
 *
 * FAILURE RESPONSE (HTTP 400):
 *   {
 *     "status": "FAILED",
 *     "debitAccountId": null,
 *     "creditAccountId": null,
 *     "amount": null,
 *     "message": "Account 2 has insufficient balance: 20000"
 *   }
 * ============================================================
 */
@Getter
@AllArgsConstructor
public class TransferResponse {

    private String status;
    private Long debitAccountId;
    private Long creditAccountId;
    private BigDecimal amount;
    private String message;

    // ---- Static factory method: success ----
    // Caller: TransferResponse.success(debitId, creditId, amount)
    public static TransferResponse success(Long debitId, Long creditId, BigDecimal amount) {
        return new TransferResponse("SUCCESS", debitId, creditId, amount, "Transfer successful");
    }

    // ---- Static factory method: failure ----
    // Caller: TransferResponse.failure("Insufficient balance: 200")
    public static TransferResponse failure(String reason) {
        return new TransferResponse("FAILED", null, null, null, reason);
    }
}
