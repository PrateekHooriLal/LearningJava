package com.transaction.manager.controller;

import java.io.Serializable;

public class Godigit implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {

		int num = 454;
		int rev = 0;

		while (num > 0) {
			rev = rev * 10 + num % 10; // 4
			num = num / 10; // 45
		}

		if (num == rev)
			System.out.println("True");

	}
}
