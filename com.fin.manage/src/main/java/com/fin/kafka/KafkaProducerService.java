package com.fin.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fin.entities.Transaction;

@Service
public class KafkaProducerService {
	@Autowired
    private KafkaTemplate<String, Transaction> kafkaTemplate;

    private static final String TOPIC = "transactions";

    public void sendMessage(Transaction transaction) {
        kafkaTemplate.send(TOPIC, transaction);
    }

}
