package com.interview.altimetrik.banktransfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * ============================================================
 * LAYER 2 — Repository (Spring Data JPA)
 * ============================================================
 *
 * PURPOSE:
 *   Database access layer. Spring Data JPA generates the SQL
 *   implementation automatically at startup — no SQL needed.
 *
 * ---- HOW JpaRepository WORKS (INTERVIEW MUST-KNOW) ----
 *   JpaRepository<BankAccount, Long>
 *     ↑ first type  = the entity this repo manages
 *     ↑ second type = the type of the primary key (@Id field)
 *
 *   FREE METHODS you get without writing anything:
 *     findById(Long id)               → Optional<BankAccount>
 *     findAll()                       → List<BankAccount>
 *     save(BankAccount account)       → BankAccount (insert or update)
 *     deleteById(Long id)             → void
 *     existsById(Long id)             → boolean
 *     count()                         → long
 *
 * ---- DERIVED QUERY METHODS (INTERVIEW TALKING POINT) ----
 *   Spring Data JPA parses method names and generates SQL automatically:
 *     findByAccountNumber(String num)     → SELECT * FROM bank_accounts WHERE account_number = ?
 *     findByOwnerNameContaining(String s) → SELECT * FROM bank_accounts WHERE owner_name LIKE ?
 *     findByBalanceGreaterThan(BigDecimal)→ SELECT * FROM bank_accounts WHERE balance > ?
 *
 *   Rules: findBy + FieldName + [Condition]
 *   Conditions: Containing, StartingWith, Between, LessThan, GreaterThan, OrderBy...
 *
 * ---- WHY NO @Repository ANNOTATION? ----
 *   Spring detects any interface that extends JpaRepository (or CrudRepository)
 *   and auto-registers it as a Spring bean. @Repository is NOT required.
 *   (Though adding @Repository does no harm — it just adds an alias.)
 *
 * INTERVIEW FOLLOW-UP:
 *   Q: What is the difference between CrudRepository, JpaRepository, PagingAndSortingRepository?
 *   A: CrudRepository        → basic CRUD (save, findById, delete, count)
 *      PagingAndSortingRepository → extends Crud + adds findAll(Pageable) and findAll(Sort)
 *      JpaRepository          → extends PagingAndSorting + adds flush(), saveAndFlush(),
 *                               deleteInBatch(), findAll(Example<S>) etc.
 *      Use JpaRepository for most Spring Boot apps — it has everything.
 * ============================================================
 */
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // Derived query — Spring generates: SELECT * FROM bank_accounts WHERE account_number = ?
    // Returns Optional because the account might not exist
    Optional<BankAccount> findByAccountNumber(String accountNumber);
}
