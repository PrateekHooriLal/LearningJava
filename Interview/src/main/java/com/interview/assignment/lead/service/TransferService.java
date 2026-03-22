package com.interview.assignment.lead.service;

import com.interview.assignment.lead.api.dto.TransferResponse;

import java.math.BigDecimal;

/**
 * =============================================================================
 * TransferService — Application Service Interface (Port)
 * =============================================================================
 *
 * WHY DEFINE AN INTERFACE FOR THE SERVICE?
 *   1. TESTABILITY: In tests (e.g., WalletControllerTest), you can mock this interface
 *      with Mockito instead of spinning up the full Spring context.
 *   2. DECOUPLING: The controller depends on the abstraction, not the implementation.
 *      You could swap TransferServiceImpl for an async implementation without touching the controller.
 *   3. AOP-FRIENDLY: Spring's @Transactional works via AOP proxies. For proxies to work
 *      correctly, your dependency should be typed to the interface, not the concrete class.
 *
 * INTERFACE EVOLUTION (Module 1 → Module 2):
 *   - transfer() now returns TransferResponse instead of void.
 *     WHY? REST APIs should return meaningful data. Returning void means the client
 *     must fire a separate GET to know the outcome — wasteful and race-prone.
 *
 * =============================================================================
 */
public interface TransferService {

    /**
     * Executes an atomic, cross-currency transfer between two wallets.
     * Must be transactional. Must handle concurrent access via optimistic locking.
     *
     * @param fromWalletId  ID of the sender's wallet
     * @param toWalletId    ID of the receiver's wallet
     * @param amount        Amount to transfer (in the sender's currency)
     * @return              TransferResponse with debit/credit amounts and exchange rate used
     */
    TransferResponse transfer(Long fromWalletId, Long toWalletId, BigDecimal amount);

    /**
     * Returns the exchange rate between two currencies.
     * Resilience is handled by the ExchangeRateService adapter (circuit breaker).
     *
     * @param fromCurrency  ISO 4217 currency code (e.g., "USD")
     * @param toCurrency    ISO 4217 currency code (e.g., "INR")
     * @return              Exchange rate as BigDecimal
     */
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);
}
