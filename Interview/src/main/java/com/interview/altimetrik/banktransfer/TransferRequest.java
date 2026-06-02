package com.interview.altimetrik.banktransfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ============================================================
 * LAYER 3 — Input DTO (Data Transfer Object)
 * ============================================================
 *
 * PURPOSE:
 *   Represents the JSON body the client sends in POST /api/bank/transfer.
 *   This is NOT an @Entity — it never touches the database directly.
 *
 * ---- WHAT IS A DTO? (INTERVIEW TALKING POINT) ----
 *   DTO = a simple class to carry data between layers.
 *   Why not use the @Entity directly as @RequestBody?
 *     1. The entity has DB fields (id, version) the client should never set
 *     2. Separating input shape from DB shape lets both evolve independently
 *     3. Validation annotations belong on the DTO, not the entity
 *   Rule: ALWAYS use a separate DTO for @RequestBody.
 *
 * ---- BEAN VALIDATION ANNOTATIONS ----
 *   These are JSR-380 annotations from the jakarta.validation package.
 *   They do NOTHING on their own — they only trigger when @Valid is on
 *   the method parameter in the controller (@Valid @RequestBody TransferRequest).
 *
 *   @NotNull            → field cannot be null (works for any type)
 *   @NotBlank           → String cannot be null or empty (String only)
 *   @NotEmpty           → Collection/String cannot be null or empty
 *   @DecimalMin("0.01") → BigDecimal must be >= 0.01 (inclusive by default)
 *   @Min(1)             → int/long must be >= 1
 *   @Positive           → numeric must be > 0
 *   @Email              → String must be valid email format
 *   @Size(min=2,max=50) → String/Collection length constraint
 *
 * ---- WHY @DecimalMin FOR BigDecimal? ----
 *   @Min works only for int and long.
 *   @Positive works for any numeric but doesn't set a specific minimum.
 *   @DecimalMin("0.01") ensures amount is at least 1 paisa / 1 cent.
 *
 * INTERVIEW JSON EXAMPLE (what the client sends):
 *   {
 *     "debitAccountId":  1,
 *     "creditAccountId": 2,
 *     "amount": 5000.00
 *   }
 * ============================================================
 */
@Getter
@NoArgsConstructor    // Jackson needs this to deserialize JSON → object
@AllArgsConstructor   // convenient for tests: new TransferRequest(1L, 2L, new BigDecimal("5000"))
public class TransferRequest {

    @NotNull(message = "Debit account ID is required")
    private Long debitAccountId;

    @NotNull(message = "Credit account ID is required")
    private Long creditAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    // BigDecimal comparison: "0.01" is the string form, matches BigDecimal scale
    private BigDecimal amount;
}
