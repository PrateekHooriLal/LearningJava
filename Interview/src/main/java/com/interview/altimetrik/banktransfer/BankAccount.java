package com.interview.altimetrik.banktransfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * ============================================================
 * LAYER 1 — @Entity (JPA / Hibernate)
 * ============================================================
 *
 * PURPOSE:
 *   Represents a bank account row in the database.
 *   JPA maps this class to the "bank_accounts" table automatically.
 *
 * ---- WHY BigDecimal FOR BALANCE? (INTERVIEW MUST-KNOW) ----
 *   Never use double or float for money. Floating-point arithmetic has
 *   precision errors:
 *     double: 0.1 + 0.2 = 0.30000000000000004   ← WRONG
 *     BigDecimal: 0.1 + 0.2 = 0.3               ← CORRECT
 *   For financial calculations always use BigDecimal.
 *   Use .compareTo() to compare BigDecimal values, NOT == or >.
 *     e.g., balance.compareTo(amount) < 0   means   balance < amount
 *
 * ---- LOMBOK ANNOTATIONS USED ----
 *   @Getter / @Setter   → generates all getters and setters
 *   @NoArgsConstructor  → generates no-arg constructor (JPA requires this)
 *   @AllArgsConstructor → generates constructor with all fields
 *   @Builder            → enables BankAccount.builder().name("Alice")...build()
 *   @ToString           → generates readable toString() for logging
 *
 * ---- JPA ANNOTATIONS ----
 *   @Entity             → marks this class as a JPA entity (DB table)
 *   @Table(name=...)    → maps to specific table name (optional, defaults to class name)
 *   @Id                 → marks primary key
 *   @GeneratedValue     → auto-increment (database generates the ID)
 *   @Column             → column-level constraints (unique, nullable, name)
 *
 * INTERVIEW FOLLOW-UP:
 *   Q: Why does JPA require a no-arg constructor?
 *   A: JPA uses reflection to create instances when loading from DB.
 *      It needs to call new BankAccount() then set fields via setters.
 *      Without no-arg constructor → InstantiationException at runtime.
 * ============================================================
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor    // REQUIRED by JPA — used internally when loading from DB
@AllArgsConstructor   // used in tests and data initialization
@Builder
@ToString
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY = auto-increment in DB (MySQL/H2/PostgreSQL compatible)
    // Alternative: GenerationType.SEQUENCE — uses DB sequence (Oracle style)
    private Long id;

    @Column(unique = true, nullable = false)
    // unique = true → DB constraint prevents two accounts with same number
    // nullable = false → NOT NULL constraint in DB
    private String accountNumber;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 4)
    // precision=19, scale=4 → up to 999,999,999,999,999.9999
    // scale=4 means 4 decimal places — enough for financial calculations
    private BigDecimal balance;
}
