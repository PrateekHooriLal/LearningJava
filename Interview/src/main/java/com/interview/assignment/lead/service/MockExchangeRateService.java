package com.interview.assignment.lead.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * =============================================================================
 * MockExchangeRateService — Resilience4j Circuit Breaker Demo
 * =============================================================================
 *
 * WHAT IS A CIRCUIT BREAKER?
 * Inspired by electrical circuit breakers. When a service repeatedly fails, the
 * circuit "opens" — all further calls fail fast without hitting the failing service.
 * After a wait period it goes "half-open" to test if the service recovered.
 *
 * STATES:
 *   CLOSED  → Normal operation. All calls go through. Failures are counted.
 *   OPEN    → Failures exceeded threshold. Calls fail immediately (short-circuit).
 *             No waiting for timeout — instant failure to protect the system.
 *   HALF-OPEN → After a wait duration, limited calls are let through to test recovery.
 *               If successful → CLOSED. If still failing → OPEN again.
 *
 * WHY USE IT IN A FINTECH SYSTEM?
 * Without a circuit breaker:
 *   - If the Exchange Rate API is slow, ALL transfer threads pile up waiting.
 *   - Thread pool exhaustion → your entire service becomes unresponsive (cascading failure).
 * With a circuit breaker:
 *   - After N failures, further calls return the fallback rate immediately.
 *   - System stays responsive. User sees "Using cached rate" instead of a 500 error.
 *
 * HOW RESILIENCE4J WORKS INTERNALLY:
 *   - It uses AOP (Aspect-Oriented Programming) to wrap your annotated method.
 *   - @CircuitBreaker intercepts the call, tracks failures in a sliding window,
 *     and either lets the call through or short-circuits based on thresholds.
 *   - Configured in application.properties under resilience4j.circuitbreaker.*
 *
 * THE FALLBACK METHOD:
 *   - Must have the same return type and parameters as the protected method,
 *     PLUS a Throwable parameter as the last argument.
 *   - Resilience4j calls it when the circuit is open OR when the method throws.
 *
 * INTERVIEW TALKING POINT:
 *   "Resilience4j's @CircuitBreaker is applied here, not on TransferServiceImpl,
 *   because the external dependency is isolated in this adapter. The circuit breaker
 *   is a cross-cutting concern handled by AOP — it doesn't pollute business logic."
 *
 * COMMON FOLLOW-UP: "What's the difference between Circuit Breaker and Retry?"
 *   - Retry: Retries the same call N times (good for transient failures).
 *   - Circuit Breaker: Stops retrying entirely after a threshold (good for systemic failures).
 *   - In production, you'd combine both: retry first, circuit break on persistent failure.
 *
 * =============================================================================
 */
@Service
@Slf4j
public class MockExchangeRateService implements ExchangeRateService {

    /**
     * Hardcoded rates for demonstration. In production this would be an HTTP call
     * to a provider like Open Exchange Rates or Fixer.io.
     *
     * IMPORTANT: Use BigDecimal for financial calculations, NEVER double/float.
     * Reason: floating point representation error: 0.1 + 0.2 ≠ 0.3 in binary IEEE 754.
     */
    private static final Map<String, BigDecimal> HARDCODED_RATES = Map.of(
        "USD_INR", new BigDecimal("83.00"),
        "INR_USD", new BigDecimal("0.012"),
        "USD_EUR", new BigDecimal("0.92"),
        "EUR_USD", new BigDecimal("1.09")
    );

    /**
     * "exchangeRateService" must match the circuit breaker name configured in
     * application.properties under resilience4j.circuitbreaker.instances.exchangeRateService.*
     *
     * fallbackMethod: the method Resilience4j calls when:
     *   1. The circuit is OPEN (threshold breached), OR
     *   2. The method itself throws a RuntimeException.
     *
     * In this mock we simulate failure to demonstrate the fallback. In a real service
     * this annotation wraps an actual HTTP call that might fail.
     */
    @Override
    @CircuitBreaker(name = "exchangeRateService", fallbackMethod = "getFallbackRate")
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE; // No conversion needed
        }

        String key = fromCurrency + "_" + toCurrency;
        BigDecimal rate = HARDCODED_RATES.get(key);

        if (rate == null) {
            // Simulates the external API throwing an exception for unsupported currency pairs.
            // In a real service this would be: restTemplate.getForObject(url, ...) throwing IOException.
            // When this exception propagates, Resilience4j counts it as a failure.
            log.warn("No rate found for {}/{}. Simulating API failure.", fromCurrency, toCurrency);
            throw new RuntimeException("Exchange rate not available for: " + key);
        }

        log.info("Exchange rate for {}/{}: {}", fromCurrency, toCurrency, rate);
        return rate;
    }

    /**
     * FALLBACK METHOD — Resilience4j calls this when the circuit is open or when
     * getRate() throws an exception.
     *
     * SIGNATURE RULES (must follow exactly or Resilience4j won't find it):
     *   1. Same return type as the primary method.
     *   2. Same parameters as the primary method.
     *   3. One additional parameter: Throwable (the cause of failure).
     *
     * STRATEGY: Return a safe default rate (1.0) so the transfer can proceed.
     * In production, you might instead return a cached rate from Redis.
     *
     * INTERVIEW TALKING POINT:
     *   "The fallback returns 1.0 (no conversion) rather than throwing an error.
     *   This is a 'graceful degradation' strategy — the system remains operational
     *   with a known limitation rather than crashing entirely. We log a warning
     *   so the ops team can monitor rate fallback frequency via metrics."
     */
    public BigDecimal getFallbackRate(String fromCurrency, String toCurrency, Throwable cause) {
        log.warn("Circuit breaker fallback triggered for {}/{}. Reason: {}. Using rate: 1.0",
                fromCurrency, toCurrency, cause.getMessage());
        // Returning 1.0 means "treat as same currency" — a safe, predictable fallback.
        return BigDecimal.ONE;
    }
}
