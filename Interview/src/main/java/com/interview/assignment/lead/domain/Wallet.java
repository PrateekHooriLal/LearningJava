package com.interview.assignment.lead.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Entity: Marks this class as a JPA entity to be mapped to a database table.
 * @Table: Specifies the physical table name in the database ("wallets").
 */
@Entity
@Table(name = "wallets")
/**
 * Lombok Annotations:
 * @Getter/@Setter: Generates accessor and mutator methods at compile time.
 * @NoArgsConstructor: Required by JPA for entity instantiation via reflection.
 * @AllArgsConstructor: Enables the @Builder pattern and easy testing.
 * @Builder: Implements the Builder design pattern for fluent object creation.
 * @ToString: Generates a toString implementation (useful for logging).
 */
@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Wallet {
    /**
     * @Id: Marks this field as the primary key.
     * @GeneratedValue: Defines the strategy for ID generation (IDENTITY relies on DB auto-increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @Column: Defines mapping properties. nullable=false ensures DB-level constraints.
     */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private BigDecimal balance;

    /**
     * @Version: Crucial for Lead-level concurrency control. 
     * Implements Optimistic Locking to prevent the "Lost Update" problem in high-concurrency environments.
     */
    @Version
    private Integer version;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * JPA Lifecycle Hooks:
     * @PrePersist: Executed before the entity is saved for the first time.
     * @PreUpdate: Executed before any update to the entity.
     * Ensures the lastUpdated timestamp is always accurate without manual setter calls.
     */
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in wallet: " + id);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }
}
