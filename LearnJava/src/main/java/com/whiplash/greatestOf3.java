package com.whiplash;

import java.util.Scanner;

public class greatestOf3 {
	public static void main(String args[]) {

		int x = 0;
		// what we are doing here
		while (x != 1) {
			Scanner cmd = new Scanner(System.in);
			System.out.println("Please enter three different numbers to find largest of them" + Integer.MAX_VALUE + ":"
					+ Integer.MIN_VALUE + ":" + Integer.bitCount(12));
			int first = cmd.nextInt();

			int second = cmd.nextInt();
			int third = cmd.nextInt();
			int largest = greatestOf3(first, second, third);
			System.out.printf("Largest of three numbers, between %d, %d and %d is %d %n", first, second, third,
					largest);
			int greatest = greatestOfThreeUsingTernaryOperator(first, second, third);
			System.out.printf("Greatest of three numbers in Java using ternary operator is %d %n", greatest);
			// close the scanner to prevent resource leak cmd.close(); }

			System.out.println("Want to continue: 2 else 1");
			x = cmd.nextInt();

			if (x == 1)
				System.exit(0);
		}
	}

	private static int greatestOfThreeUsingTernaryOperator(int one, int two, int three) {
		// TODO Auto-generated method stub
		return (one > two) ? (one > three ? one : three) : (two > three ? two : three);// nice logic

	}

	private static int greatestOf3(int first, int second, int third) {
		// TODO Auto-generated method stub
		if (first > second && first > third)// 1st round
		{
			return first;
		} else if (second > first && second > third) // 2nd round
		{
			return second;
		} else if (third > first && third > second)// 3rd round
		{
			return third;
		}
		return Integer.MIN_VALUE;
	}
}
