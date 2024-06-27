package com.stream.api;

import java.util.Scanner;

public class DecToBinary {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		// Input from user
		System.out.print("Enter a decimal number: ");
		int decimal = scanner.nextInt();

		// Convert decimal to binary using custom method
		String binary = decimalToBinary(decimal);

		// Output the result
		System.out.println("Binary representation: " + binary);

		scanner.close();
	}

	public static String decimalToBinary(int decimal) {
		if (decimal == 0) {
			return "0";
		}

		StringBuilder binary = new StringBuilder();
		int temp = decimal;
		System.out.println("Using internal function=" + Integer.toBinaryString(decimal));
		while (temp > 0) {
			int remainder = temp % 2;
			System.out.println("remainder=" + remainder);
			binary.insert(0, remainder); // Insert remainder at the beginning of the string
			temp = temp / 2;
		}
		return binary.toString();
	}

}
