package com.transaction.manager.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transaction.manager.entity.Transaction;
import com.transaction.manager.service.TransactionService;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

	@Autowired
	private TransactionService transactionService;

	@GetMapping("/hello")
	public String sayHello() {
		return "HELLO FROM APP";
	}

	@GetMapping("/getAll")
	public List<Transaction> getAllTransaction(){
		List<Transaction> Tlist = transactionService.getAllTransaction();
		return Tlist;
	}
	@GetMapping("/getById/{id}")
	public Transaction getById(Long id) {
		return transactionService.getTransactionById(id);
	}
	@PutMapping("/{transactionId}")
	public Transaction updateTransaction(@PathVariable Long transactionId, @RequestBody Transaction transaction) {
		return transactionService.updateTransaction(transactionId, transaction);
	}

	@PostMapping("/create")
	public Transaction createTransaction(@RequestBody Transaction transaction) {
		transaction.setTimeStamp(LocalDateTime.now());
		return transactionService.createTransaction(transaction);
	}

}
