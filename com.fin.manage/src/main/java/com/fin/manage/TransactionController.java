package com.fin.manage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fin.entities.Transaction;
import com.fin.transaction.service.TransactionService;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

	@Autowired
	TransactionService transactionService;
	
	@GetMapping("/hello")
	public String sayHello() {
		System.out.println("HELLO");
		return "HELLO";
	}

	@GetMapping("/")
	public ResponseEntity getAllTransactions() {
		return ResponseEntity.ok(transactionService.getAllTransactions());
	}

	@GetMapping("/{id}")
	public ResponseEntity getTransactionById(@PathVariable Long id) {
		Transaction getT = transactionService.getTransactionById(id);
		return ResponseEntity.ok(getT);
	}

	@PostMapping("/create")
	public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
		Transaction createTransaction = transactionService.createTransaction(transaction);
		return ResponseEntity.ok(createTransaction);
	}

	@PutMapping("/{id}")
	public ResponseEntity updateTranaction(@PathVariable Long id, @RequestBody Transaction entity) {
		Transaction updatedTransaction = transactionService.updateTransaction(id, entity);
		return ResponseEntity.ok(updatedTransaction);
	}

	@DeleteMapping("/{id}")

	public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
		transactionService.deleteTransaction(id);
		return ResponseEntity.noContent().build();
	}

}
