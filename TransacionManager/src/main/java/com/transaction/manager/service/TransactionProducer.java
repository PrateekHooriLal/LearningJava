package com.transaction.manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.transaction.manager.entity.Transaction;

@Service
public class TransactionProducer {

	private static final String TOPIC = "transaction-topic"; // Replace with your topic name

	@Autowired
	private KafkaTemplate<String, Transaction> kafkaTemplate;

	public void sendTransaction(Transaction transaction) {
		kafkaTemplate.send(TOPIC, transaction);
		System.out.println("Sent transaction to Kafka"+TOPIC+":"+ transaction.toString());
	}
}
