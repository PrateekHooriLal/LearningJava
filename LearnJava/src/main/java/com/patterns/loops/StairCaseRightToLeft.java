package com.patterns.loops;

/**
 * @author Prateek
 *
 */

/*
 * # ## ### #### ##### ######
 * 
 * To print above pattern.
 *
 */
public class StairCaseRightToLeft {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int n = 6;
		for (int i = 0; i < n; i++) {
			for (int j = 1; j <= n; j++) {
				if (n - j == i)
					break;
				else
					System.out.print("-");
			}

			for (int j = i; j >= 0; j--)
				System.out.print("#");

			System.out.println();
		}

	}

}
