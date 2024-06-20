package com.transaction.manager.service;

import org.apache.kafka.common.internals.Topic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.transaction.manager.entity.Transaction;

@Service
public class TransactionConsumer {

	@KafkaListener(topics = "transaction-topic", groupId = "transaction-consumer-group")
	public void consumeTransaction(Transaction transaction) {
		System.out.println("Received transaction from Kafka:"+Topic.CLUSTER_METADATA_TOPIC_PARTITION+":" + transaction.toString());
		// Process the received transaction (e.g., save to database, perform business
		// logic)
	}
}
