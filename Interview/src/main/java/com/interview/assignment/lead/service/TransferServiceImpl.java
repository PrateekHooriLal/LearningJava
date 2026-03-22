package com.interview.assignment.lead.service;

import com.interview.assignment.lead.api.dto.TransferResponse;
import com.interview.assignment.lead.domain.Wallet;
import com.interview.assignment.lead.infrastructure.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * =============================================================================
 * TransferServiceImpl — Application Layer Orchestrator (Updated for Module 2)
 * =============================================================================
 *
 * ROLE IN THE ARCHITECTURE:
 *   This is the "Application Service" in DDD terms. It orchestrates the use case:
 *   1. Load domain entities from the repository.
 *   2. Delegate to domain entities for business logic (debit/credit).
 *   3. Delegate to infrastructure services (ExchangeRateService) for external concerns.
 *   4. Persist and return a result.
 *
 * MODULE 2 CHANGES:
 *   - Extracted ExchangeRateService as a dependency (injected via constructor).
 *     This removes the hardcoded exchange rate map from this class.
 *   - Returns TransferResponse DTO so the controller can pass it to the client.
 *   - The getExchangeRate() method on the interface is now implemented by delegation.
 *
 * @Transactional — CRITICAL for atomicity:
 *   Spring wraps this method in a database transaction. If ANY exception is thrown
 *   after debit() but before save(), the entire transaction rolls back.
 *   Without this, a crash between debit and credit would cause "money to disappear."
 *
 * @RequiredArgsConstructor (Lombok):
 *   Generates a constructor with all `final` fields. Spring uses this constructor
 *   for dependency injection (preferred over @Autowired field injection).
 *   WHY CONSTRUCTOR INJECTION? Dependencies are explicit, immutable, and testable.
 *
 * INTERVIEW TALKING POINT:
 *   "Why is @Transactional on the service and not the controller?"
 *   → Transaction boundaries should align with use-case boundaries, not HTTP boundaries.
 *     A single use case may involve multiple DB operations — they all need to be atomic.
 *     The controller is presentation layer; it shouldn't know about transactions.
 *
 * =============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final WalletRepository walletRepository;

    /**
     * Injecting via interface (ExchangeRateService), not concrete class.
     * Spring will inject MockExchangeRateService at runtime (it's the only @Service implementing it).
     * In tests, you can inject a different mock — this is the Dependency Inversion Principle.
     */
    private final ExchangeRateService exchangeRateService;

    /**
     * Executes an atomic cross-currency transfer between two wallets.
     *
     * ALGORITHM:
     *   1. Load both wallets (throws if not found — fail fast).
     *   2. Debit the sender (throws if insufficient balance).
     *   3. Get the exchange rate (circuit breaker protected — falls back to 1.0 if unavailable).
     *   4. Credit the receiver with the converted amount.
     *   5. Persist both wallets (Optimistic Locking via @Version fires here).
     *   6. Return a structured response.
     *
     * CONCURRENCY CONCERN — DEADLOCK PREVENTION:
     *   In a pessimistic locking scenario (SELECT FOR UPDATE), always lock wallets
     *   in a consistent order (e.g., by wallet ID) to prevent deadlocks.
     *   e.g., Thread A locks wallet 1 then 2; Thread B locks wallet 2 then 1 → deadlock.
     *   With optimistic locking (@Version), we don't acquire locks but accept that one
     *   concurrent thread will fail with OptimisticLockException — the caller retries.
     *
     * @return TransferResponse with debited, credited amounts, and exchange rate used.
     */
    @Override
    @Transactional
    public TransferResponse transfer(Long fromWalletId, Long toWalletId, BigDecimal amount) {
        log.info("Initiating transfer: wallet {} → wallet {} for amount {}", fromWalletId, toWalletId, amount);

        // Step 1: Load domain entities — fail fast if wallets don't exist
        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found: " + fromWalletId));
        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found: " + toWalletId));

        // Step 2: Debit the sender — domain entity enforces business rules (positive amount, sufficient balance)
        fromWallet.debit(amount);

        // Step 3: Determine how much to credit (currency conversion if needed)
        BigDecimal creditAmount = amount;
        BigDecimal exchangeRate = BigDecimal.ONE;

        if (!fromWallet.getCurrency().equals(toWallet.getCurrency())) {
            // This call is circuit-breaker-protected inside MockExchangeRateService.
            // If the exchange rate API is down, it returns 1.0 (fallback) rather than crashing.
            exchangeRate = exchangeRateService.getRate(fromWallet.getCurrency(), toWallet.getCurrency());
            creditAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

            log.info("Currency conversion: {} {} → {} {} @ rate {}",
                    amount, fromWallet.getCurrency(),
                    creditAmount, toWallet.getCurrency(),
                    exchangeRate);
        }

        // Step 4: Credit the receiver
        toWallet.credit(creditAmount);

        // Step 5: Persist — @Version on Wallet triggers optimistic locking check here.
        // If another transaction modified the same wallet concurrently, Hibernate throws
        // OptimisticLockException, which rolls back this transaction. The caller must retry.
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        log.info("Transfer completed. Debited: {} {}, Credited: {} {}",
                amount, fromWallet.getCurrency(), creditAmount, toWallet.getCurrency());

        // Step 6: Return structured response (DTO, not domain entity)
        String message = exchangeRate.compareTo(BigDecimal.ONE) == 0 && fromWallet.getCurrency().equals(toWallet.getCurrency())
                ? "Transfer completed (same currency)"
                : "Transfer completed with exchange rate: " + exchangeRate;

        return TransferResponse.success(fromWalletId, toWalletId, amount, creditAmount, exchangeRate, message);
    }

    /**
     * Delegates to the injected ExchangeRateService.
     * This method satisfies the TransferService interface contract for callers who
     * want to query the rate without performing a transfer (e.g., a "preview" endpoint).
     */
    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        return exchangeRateService.getRate(fromCurrency, toCurrency);
    }
}
