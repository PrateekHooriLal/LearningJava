package com.interview.prep;

import java.util.Scanner;

public class PalindromeNumber {

	public static void main(String[] args) {

		System.out.print("Input any Number To Check if it's a Palindrome:");
		Scanner sc = new Scanner(System.in);
		int number = sc.nextInt();
		String flag = isPalindromme(number) ? "is a" : "is not a";
		System.out.println(number + " " + flag + " Palindrome!!! ");
		sc.close();
	}

	public static boolean isPalindromme(int number) {

		int tempNum = number;
		int result = 0;
		while (tempNum != 0) {

			int remainder = tempNum % 10; // 1
			tempNum = tempNum / 10;
			result = result * 10 + remainder;

		}
		return result == number;
	}
}// java 8 , spring
