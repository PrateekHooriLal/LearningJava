package com.manage.fruit.legacy;

import java.time.LocalDate;

class Transaction {
	int customerId;
	double amount;
	LocalDate date;

	public Transaction(int i, int j, LocalDate of) {
		// TODO Auto-generated constructor stub

	}

	public int getCustomerId() {
		return customerId;
	}

	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

}
