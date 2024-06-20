package com.fin.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fin.entities.Transaction;

@Service
public class KafkaConsumerService {

	@KafkaListener(topics = "transactions", groupId = "transaction-group")
    public void consume(Transaction transaction) {
        System.out.println("Consumed transaction: " + transaction);
    }

}
