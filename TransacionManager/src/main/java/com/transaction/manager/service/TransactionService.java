package com.transaction.manager.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.transaction.manager.entity.Transaction;
import com.transaction.manager.repository.TransactionRepository;

@Service
public class TransactionService {

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private KafkaTemplate<String, Transaction> kafkaTemp;

	private static final String Topic = "transaction-topic";

	public Transaction createTransaction(Transaction transaction) {

		kafkaTemp.send(Topic, transaction);
		// Implement transaction creation logic
		return transactionRepository.save(transaction);
	}

	public Transaction updateTransaction(Long transactionId, Transaction transaction) {
		// Implement transaction update logic
		// Fetch transaction from repository, update fields and save
		if (getTransactionById(transactionId) != null) {

			Transaction updatedTransaction = new Transaction();
			updatedTransaction.setAccountNumber(transaction.getAccountNumber());
			updatedTransaction.setAmount(transaction.getAmount());
			updatedTransaction.setStatus(transaction.getStatus());
			updatedTransaction.setTransactionType(transaction.getTransactionType());
			updatedTransaction.setTimeStamp(transaction.getTimeStamp());

			// sending to kafka topic
			kafkaTemp.send(Topic, updatedTransaction);

			// save to db
			return transactionRepository.save(updatedTransaction);
		} else
			return null;

	}

	public Transaction getTransactionById(Long transactionId) {
		// Implement logic to fetch transaction by ID
		return transactionRepository.findById(transactionId)
				.orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));
	}

	public List<Transaction> getAllTransaction() {

		return transactionRepository.findAll();
	}
}
