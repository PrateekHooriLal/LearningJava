package com.transaction.manager.serviceTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.transaction.manager.entity.Transaction;
import com.transaction.manager.repository.TransactionRepository;
import com.transaction.manager.service.TransactionService;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private KafkaTemplate<String, Transaction> kafkaTemplate;

	@InjectMocks
	private TransactionService transactionService;

	private Transaction transaction;

	@BeforeEach
	public void setUp() {
		// Initialize a sample transaction object
		transaction = new Transaction();
		transaction.setId(103L);
		transaction.setAccountNumber("123456789");
		transaction.setAmount(new BigDecimal("250.00"));
		transaction.setStatus("PENDING");
		transaction.setTransactionType("debit");
		transaction.setTimeStamp(LocalDateTime.now());
	}
	

	@Test
	public void testCreateTransaction() {
		// Act
		transactionService.createTransaction(transaction);

		// Assert
		verify(kafkaTemplate, times(1)).send(eq("transaction-topic"), eq(transaction));
	}

	// Additional test cases for other CRUD operations if any
}
