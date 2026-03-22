# Project Progress: The Resilient Multi-Currency Wallet

This document tracks the progress, performance, and learnings for the interview preparation project. It serves as our single source of truth for the project lifecycle.

---

### Module 1: Core Domain & API

- **Status**: `Completed`
- **Estimated Time**: 3 hours
- **Actual Time Spent**: 1 hour
- **Learning Objectives**:
    - [x] Implement the `Wallet` entity, including validation logic for debit/credit operations.
    - [x] Implement Optimistic Locking (`@Version`) to handle concurrent updates.
    - [x] Design and implement the `TransferService` interface and a basic implementation class.
    - [x] Create a `WalletRepository` using Spring Data JPA.
    - [x] Write a unit test that proves the Optimistic Locking prevents a lost update (race condition).
- **Code Quality Score**: 5/5
- **My Notes**: 
    > *Successfully implemented the core domain with JPA and optimistic locking. Verified with concurrent tests that only one transaction succeeds when two threads attempt to modify the same wallet balance simultaneously.*
- **My Review & Next Steps**: 
    > *Great start! The domain model is clean and the transfer logic is transactional. The optimistic locking test is a crucial lead-level verification. Next, we'll move to Module 2 to expose this via a REST API and add resilience.*

---

### Module 2: API Layer & Resilience

- **Status**: `In Progress`
- **Estimated Time**: 4 hours
- **Actual Time Spent**: TBD
- **Learning Objectives**:
    - [ ] Create a REST Controller (`WalletController`) for transfer operations.
    - [ ] Implement DTOs for API requests/responses.
    - [ ] Implement a mock `ExchangeRateService`.
    - [ ] Wrap the `ExchangeRateService` call in a Resilience4j Circuit Breaker with a fallback method.
- **Code Quality Score**: TBD
- **My Notes**: 
    > *Your notes and thoughts during the module will be captured here.*
- **My Review & Next Steps**: 
    > *My feedback will be recorded here upon module completion.*

