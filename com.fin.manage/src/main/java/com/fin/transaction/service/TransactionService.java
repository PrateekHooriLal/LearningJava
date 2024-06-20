package com.fin.transaction.service;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.fin.entities.Transaction;
import com.fin.entities.TransactionRepository;
import com.fin.kafka.KafkaProducerService;

@Service
public class TransactionService {

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private KafkaProducerService kafkaProducerService;

	// create Transaction
	public Transaction createTransaction(Transaction transaction) {

		transaction.setTimeStamp(LocalDateTime.now());
		Transaction savedTransaction = transactionRepository.save(transaction);
		kafkaProducerService.sendMessage(savedTransaction);
		return savedTransaction;

	}

	// Retrieve by id
	public Transaction getTransactionById(Long id) {

		return transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id " + id));

	}

	// Get all transactions
	public List<Transaction> getAllTransactions() {
		return transactionRepository.findAll();
	}

	// Update existing transaction
	public Transaction updateTransaction(Long id, Transaction transactionDetails) {

		Transaction transaction = getTransactionById(id);
		transaction.setAccountNumber(transactionDetails.getAccountNumber());
        transaction.setAmount(transactionDetails.getAmount());
        transaction.setStatus(transactionDetails.getStatus());
        transaction.setTransactionType(transactionDetails.getTransactionType());
        transaction.setTimeStamp(LocalDateTime.now());
        
        Transaction updatedTransaction = transactionRepository.save(transaction);
        kafkaProducerService.sendMessage(updatedTransaction);
        return updatedTransaction;
	}

	// delete by id
	public void deleteTransaction(Long id) {
		Transaction toDelTransct = getTransactionById(id);

		if (toDelTransct.getId() != null)
			transactionRepository.deleteById(id);

	}
}
