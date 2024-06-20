package com.transaction.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transaction.manager.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
