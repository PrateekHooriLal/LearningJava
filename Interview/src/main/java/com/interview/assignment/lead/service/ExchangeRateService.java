package com.interview.assignment.lead.service;

import java.math.BigDecimal;

/**
 * =============================================================================
 * ExchangeRateService — Interface (Port in Hexagonal Architecture)
 * =============================================================================
 *
 * WHY EXTRACT THIS AS A SEPARATE INTERFACE?
 * In Module 1, exchange rate logic lived directly in TransferServiceImpl.
 * That violated the Single Responsibility Principle: TransferServiceImpl was
 * responsible for both the transfer business logic AND fetching exchange rates.
 *
 * HEXAGONAL ARCHITECTURE (Ports & Adapters):
 *   - This interface is the "Port" — the contract your domain expects.
 *   - The implementation (MockExchangeRateService) is the "Adapter" — the actual
 *     mechanism that fulfills the contract (could be a real HTTP call, a DB lookup,
 *     or a mock for testing).
 *   - In production you'd have: RealExchangeRateService implements ExchangeRateService
 *     which calls an external API (e.g., Open Exchange Rates, Fixer.io).
 *
 * BENEFIT FOR TESTING:
 *   You can inject a different adapter in tests without touching business logic.
 *   This is the dependency inversion principle (D in SOLID).
 *
 * INTERVIEW TALKING POINT:
 *   "I extracted the exchange rate concern into its own service because it has a
 *   different reason to change — if we switch rate providers, TransferServiceImpl
 *   should be untouched. This also lets us wrap only this service with the circuit
 *   breaker, keeping resilience concerns separate from business logic."
 *
 * =============================================================================
 */
public interface ExchangeRateService {

    /**
     * Returns the exchange rate to convert 1 unit of fromCurrency into toCurrency.
     * e.g., getRate("USD", "INR") → 83.00 means 1 USD = 83 INR.
     *
     * @param fromCurrency ISO 4217 currency code (e.g., "USD")
     * @param toCurrency   ISO 4217 currency code (e.g., "INR")
     * @return             exchange rate as BigDecimal
     */
    BigDecimal getRate(String fromCurrency, String toCurrency);
}
