package com.program.solution;

import java.util.Scanner;

public class Fibonacci {

	public static int fiboRecursion(int num) {

		if (num == 1 || num == 0)
			return 1;

		return fiboRecursion(num - 1) + fiboRecursion(num - 2);

	}// method ends

	// /one more approach

	public static void fibo(int n1, int n2, int index, int count) {

		if (count < index) {
			int newnum = n1 + n2;
			System.out.println(newnum + ",");
			fibo(n2, newnum, index, ++count);
		}
	}

	public static void fiboloop(int n) {
		int n1 = 0, n2 = 1;
		int sum = 0;
		for (int i = 0; i <= n; i++) {

			sum = n1 + n2;
			n1 = n2;
			n2 = sum;
			System.out.print(" " + sum);

		}
	}

	static public void main(String[] args) {
		Scanner s = new Scanner(System.in);
		System.out.print("Enter the value of n: ");
		int n = s.nextInt();
		for (int i = 0; i <= n; i++) {
			System.out.print(fiboRecursion(i) + " ,");
		}
		fibo(0, 1, 5, 2);
		fiboloop(5);
	}// main ends
}// class ends
