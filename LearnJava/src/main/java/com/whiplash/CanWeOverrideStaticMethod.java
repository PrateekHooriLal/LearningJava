package com.whiplash;

import java.util.Scanner;

public class CanWeOverrideStaticMethod {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int i = 2;

		System.out.println("i=" + i++);
		System.out.println("after post increment=" + i);
		System.out.println(10 / 3.4);
		Scanner scan = new Scanner(System.in);
		double mealCost = scan.nextDouble(); // original meal price
		int tipPercent = scan.nextInt(); // tip percentage
		int taxPercent = scan.nextInt(); // tax percentage
		scan.close();

		// Write your calculation code here.

		double netBill = mealCost + mealCost * tipPercent / 100 + mealCost * taxPercent / 100;

		// cast the result of the rounding operation to an int and save it as
		// totalCost
		int totalCost = (int) Math.round(netBill);

		// Print your result
		System.out.println("The total meal cost is " + totalCost + " dollars.");
	}// main ends

}// class ends
