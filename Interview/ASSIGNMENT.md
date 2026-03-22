# Assignment: The Resilient Multi-Currency Wallet (Lead SDE 3 Challenge)

## Context
You are tasked with building a **High-Availability, High-Throughput Multi-Currency Wallet System** for a global fintech platform. As a Lead Engineer, you must focus not just on the "Happy Path," but on **Resilience, Scalability, and Observability**.

## The Problem
A user can have multiple wallets in different currencies (USD, EUR, INR, etc.). They should be able to:
1.  **Transfer** funds between their own wallets (with exchange rate conversion).
2.  **Send** funds to another user's wallet.
3.  **View** their consolidated balance in a "Base Currency" (e.g., USD).

## Core Requirements (SDE 3/Lead Level)
1.  **Atomicity**: Transfers must be transactional. No partial failures.
2.  **Concurrency**: Multiple concurrent transfers to/from the same wallet must be handled safely (Optimistic or Pessimistic locking).
3.  **Resilience**: The system depends on an external "Exchange Rate API". If it's down, the system must use a cached or default rate (Circuit Breaker pattern).
4.  **Auditability**: Every transaction must be immutable and audit-logged (Event Sourcing or Transaction History).
5.  **Performance**: Consolidated balance calculation should be efficient (Caching).

## Architecture Expectation (Clean Code)
Follow **Domain-Driven Design (DDD)** or **Hexagonal Architecture**:
-   `domain`: Core business logic and entities (Wallet, Transaction, Currency).
-   `service`: Application logic and orchestration.
-   `infrastructure`: Persistence (JPA), External API Clients, Messaging (Kafka).
-   `api`: REST Controllers and DTOs.

## Lead-Level Challenge: "The Resilience Gap"
-   **Scenario**: The database is slow, and the Exchange Rate API is intermittent.
-   **Task**: Implement a strategy to ensure the system doesn't crash and user experience is maintained (e.g., using `CompletableFuture` for async processing, `Resilience4j` for circuit breakers, and meaningful `Custom Exceptions`).

---
### Getting Started
1.  Check the `com.interview.assignment.lead` package for the skeleton.
2.  Identify the "Concurrency Strategy" you want to use.
3.  Implement the `TransferService` logic.
4.  Write a unit test that simulates a race condition (concurrent transfers).
