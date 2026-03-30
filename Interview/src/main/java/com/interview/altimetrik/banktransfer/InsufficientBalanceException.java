package com.interview.altimetrik.banktransfer;

import java.math.BigDecimal;

/**
 * ============================================================
 * LAYER 5 — Custom Exception
 * ============================================================
 *
 * PURPOSE:
 *   Thrown by AccountService when a debit account doesn't have enough balance.
 *   Caught by @ExceptionHandler in AccountController → returns HTTP 400.
 *
 * ---- WHY extends RuntimeException? (INTERVIEW MUST-KNOW) ----
 *   Java has two types of exceptions:
 *
 *   Checked (extends Exception):
 *     → Compiler FORCES you to either try-catch or declare "throws X"
 *     → Use for recoverable conditions the CALLER should handle:
 *       FileNotFoundException, SQLException, IOException
 *
 *   Unchecked (extends RuntimeException):
 *     → Compiler does NOT force handling
 *     → Propagates up automatically
 *     → Spring's @Transactional ONLY rolls back for RuntimeException by default
 *     → Use for programming errors or business rule violations:
 *       NullPointerException, IllegalArgumentException, InsufficientBalanceException
 *
 *   CRITICAL: If you extend Exception (checked) instead of RuntimeException:
 *     @Transactional will NOT roll back the transaction on this exception!
 *     You would need: @Transactional(rollbackFor = InsufficientBalanceException.class)
 *     This is a common interview trick question.
 *
 * ---- Spring's @Transactional rollback rules ----
 *   Default rollback: RuntimeException and Error → ROLLBACK
 *   Default NO rollback: checked Exception       → COMMIT (bad!)
 *   Override: @Transactional(rollbackFor = Exception.class)  → rollback everything
 *             @Transactional(noRollbackFor = InsufficientBalanceException.class) → override
 * ============================================================
 */
public class InsufficientBalanceException extends RuntimeException {

    // Store the account ID and balance for use in the error message
    private final Long accountId;
    private final BigDecimal currentBalance;

    public InsufficientBalanceException(Long accountId, BigDecimal currentBalance) {
        super("Account " + accountId + " has insufficient balance: " + currentBalance);
        // super(message) → sets the message returned by getMessage() and ex.toString()
        this.accountId = accountId;
        this.currentBalance = currentBalance;
    }

    public Long getAccountId() { return accountId; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
}
