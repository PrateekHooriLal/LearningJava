package com.patterns.loops;

import java.util.Stack;

public class PatternPrint {

	public static void printPattern(int n) {
		Stack<String> stk = new Stack<String>();
		Integer countNumber = 1;
		for (int i = 1; i <= n; i++) {
			for (int j = 0; j < i; j++) {

				if (i % 2 == 0) {
					if (j == 0)
						stk.push(countNumber.toString());
					else
						stk.push(countNumber + "*");

				} else {
					if (j == i - 1)
						System.out.print(countNumber);
					else
						System.out.print(countNumber + "*");
				}
				countNumber++;
			}
			while (!stk.isEmpty()) {
				System.out.print(stk.pop());
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {
		PatternPrint.printPattern(6);
	}

}
