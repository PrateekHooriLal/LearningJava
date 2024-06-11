package com.print.Mydate;

import java.util.Scanner;

// variable argument example..
public class Variable_args {
	void getinfo(String name, int... marks) {
		System.out.println("Name=" + name);
		for (int i = 0; i < marks.length; i++) {
			System.out.println("marks=" + marks[i]);
		}
	}

	public static void main(String[] args) {
		Variable_args a = new Variable_args();
		int ab = 0;

		while (ab != 100) {

			System.out.println("enter any number:");
			Scanner in = new Scanner(System.in);
			ab = in.nextInt();
			a.getinfo("chota bheem", 10, 23, 43, 231, 2, 3, 34, 4444, ab);
		}
	}

}
