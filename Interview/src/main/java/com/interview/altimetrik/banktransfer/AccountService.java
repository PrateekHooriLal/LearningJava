package com.interview.altimetrik.banktransfer;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================
 * LAYER 6 — Service (@Service)
 * ============================================================
 *
 * PURPOSE:
 *   Business logic layer. Orchestrates the transfer:
 *   load accounts → validate balance → debit → credit → save → respond.
 *
 * ---- WHY @Transactional ON THE SERVICE, NOT CONTROLLER? ----
 *   The service holds ALL the steps of one business operation.
 *   A transaction is a unit of work — it should wrap one use case.
 *
 *   If @Transactional was on the controller:
 *     → The HTTP layer (parsing, routing) would be inside the DB transaction
 *     → That wastes DB connections and slows everything down
 *
 *   Rule: @Transactional belongs at the SERVICE layer, not controller, not repository.
 *
 * ---- WHAT @Transactional ACTUALLY DOES ----
 *   Spring creates a proxy around this class.
 *   When transfer() is called:
 *     1. Spring opens a DB connection and starts a transaction (BEGIN)
 *     2. Your method runs
 *     3a. If method returns normally → Spring commits (COMMIT)
 *     3b. If RuntimeException is thrown → Spring rolls back (ROLLBACK)
 *
 *   This means: if step 5 (save debit) succeeds but step 6 (save credit) fails,
 *   the debit is ROLLED BACK automatically. Money is never lost.
 *
 * ---- WHY EXPLICIT repo.save() INSIDE @Transactional? ----
 *   JPA's "dirty checking" means: if you modify a managed entity inside
 *   a @Transactional method, JPA detects the change and auto-flushes at commit.
 *   You technically don't NEED repo.save() here.
 *
 *   But: explicit save() is better in interviews because:
 *     1. Makes intent crystal clear to the reader
 *     2. Works regardless of whether the entity is "managed" or "detached"
 *     3. In complex flows dirty checking can be tricky — explicit is safer
 *
 * ---- @RequiredArgsConstructor (Lombok) ----
 *   Generates a constructor for all 'final' fields.
 *   Spring uses that constructor to inject BankAccountRepository.
 *   This is CONSTRUCTOR INJECTION — preferred over field injection (@Autowired on field).
 *
 *   Why constructor injection is better:
 *     → Makes dependencies explicit (visible in constructor)
 *     → Easier to test: new AccountService(mockRepo) — no Spring context needed
 *     → Fields can be final → immutable → thread-safe
 *     → Spring team recommends it since Spring 4.3
 * ============================================================
 */
@Service
@RequiredArgsConstructor  // generates: AccountService(BankAccountRepository repo) { this.repo = repo; }
public class AccountService {

    private final BankAccountRepository repo;
    // 'final' + @RequiredArgsConstructor = constructor injection (no @Autowired needed)

    // ============================================================
    // findById — used by GET /api/bank/accounts/{id}
    // ============================================================
    public BankAccount findById(Long id) {
        return repo.findById(id)
            // findById returns Optional<BankAccount>
            // orElseThrow → if empty, throw EntityNotFoundException → controller returns 404
            .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
    }

    // ============================================================
    // transfer — core business logic
    // @Transactional: all steps are ONE atomic DB transaction
    //   success → COMMIT, RuntimeException → ROLLBACK
    // ============================================================
    @Transactional
    public TransferResponse transfer(TransferRequest req) {

        // Step 1: Load debit account — fail fast if not found
        BankAccount debitAccount = findById(req.getDebitAccountId());

        // Step 2: Load credit account — fail fast if not found
        BankAccount creditAccount = findById(req.getCreditAccountId());

        // Step 3: Check sufficient balance
        // BigDecimal comparison: NEVER use < or > — use .compareTo()
        //   compareTo returns: -1 (less), 0 (equal), 1 (greater)
        //   debitAccount.getBalance().compareTo(req.getAmount()) < 0
        //   means: balance < amount → insufficient
        if (debitAccount.getBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                debitAccount.getId(),
                debitAccount.getBalance()
            );
            // This RuntimeException triggers @Transactional to ROLLBACK
        }

        // Step 4: Debit — subtract from sender
        debitAccount.setBalance(debitAccount.getBalance().subtract(req.getAmount()));
        // BigDecimal arithmetic: always use .subtract(), .add(), .multiply() — never - + *

        // Step 5: Credit — add to receiver
        creditAccount.setBalance(creditAccount.getBalance().add(req.getAmount()));

        // Step 6 + 7: Persist both (explicit save — see explanation above)
        repo.save(debitAccount);
        repo.save(creditAccount);
        // If either save fails → entire transaction rolls back → both accounts unchanged

        // Step 8: Return success response
        return TransferResponse.success(
            debitAccount.getId(),
            creditAccount.getId(),
            req.getAmount()
        );
    }
}
