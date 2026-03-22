package com.interview.assignment.lead.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * =============================================================================
 * DATA TRANSFER OBJECT (DTO) PATTERN — TransferRequest
 * =============================================================================
 *
 * WHAT IS A DTO?
 * A DTO is a plain object used to carry data across layer boundaries (API → Service).
 * It is NOT a domain entity. It has no business logic, no JPA annotations.
 *
 * WHY USE DTOs INSTEAD OF EXPOSING THE DOMAIN ENTITY DIRECTLY?
 * 1. SECURITY: Your domain entity may have sensitive fields (e.g., @Version, userId).
 *    Exposing it via API leaks internal DB structure.
 * 2. DECOUPLING: Your API contract (what the client sends) can change independently
 *    of your domain model. E.g., you could rename "balance" in Wallet without
 *    breaking the API contract.
 * 3. VALIDATION: DTOs are the right place for @NotNull, @Min constraints on incoming
 *    data. Domain entities should not be responsible for validating HTTP input.
 *
 * INTERVIEW TALKING POINT:
 *   "We use DTOs to enforce a clean boundary between the API layer and domain layer.
 *   It follows the Single Responsibility Principle — the domain model handles business
 *   rules; the DTO handles API contract and input validation."
 *
 * COMMON FOLLOW-UP: "What's the difference between DTO and VO (Value Object)?"
 *   - DTO: For data transfer, may be mutable, no business logic.
 *   - VO: Part of the domain, immutable, equality by value (e.g., Money(100, USD)).
 *
 * =============================================================================
 */
public class TransferRequest {

    /**
     * @NotNull: Bean Validation (JSR-380) annotation. Spring's @Valid in the
     * controller will trigger validation of these constraints before the method body runs.
     * If null, Spring throws MethodArgumentNotValidException → 400 Bad Request.
     */
    @NotNull(message = "Source wallet ID is required")
    private Long fromWalletId;

    @NotNull(message = "Destination wallet ID is required")
    private Long toWalletId;

    /**
     * @DecimalMin: Ensures amount is positive. "0.01" means at minimum one cent.
     * inclusive = false would exclude the boundary value.
     */
    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
    private BigDecimal amount;

    // Constructors (needed for Jackson deserialization from JSON body)
    public TransferRequest() {}

    public TransferRequest(Long fromWalletId, Long toWalletId, BigDecimal amount) {
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
    }

    // Getters (Jackson needs these to serialize/deserialize)
    public Long getFromWalletId() { return fromWalletId; }
    public Long getToWalletId()   { return toWalletId; }
    public BigDecimal getAmount() { return amount; }

    // Setters (needed for Jackson to populate fields from JSON)
    public void setFromWalletId(Long fromWalletId) { this.fromWalletId = fromWalletId; }
    public void setToWalletId(Long toWalletId)     { this.toWalletId = toWalletId; }
    public void setAmount(BigDecimal amount)        { this.amount = amount; }
}
