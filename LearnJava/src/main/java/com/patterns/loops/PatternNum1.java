package com.patterns.loops;

import java.util.Scanner;

/**
 * @author Prateek
 *
 */
public class PatternNum1 {

	/**
	 * TO Print below given Pattern; eg: Start = 4, and n = 5;s SO pattern should
	 * be. 4 55 666 7777 88888 88888 7777 666 55 4
	 * 
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Scanner in = new Scanner(System.in);

		System.out.println("please Enter Start for the series:");
		int Start = in.nextInt();

		System.out.println("entre the rows to be prnted for the series:");
		int rn = in.nextInt();
		in.close();
		System.out.println("Start=" + Start + "\nNo of lines=" + rn * 2);
		// Call the series method
		PatternNum1 pt = new PatternNum1();
		pt.Series(Start, rn);

	}// main ends

	public void Series(int Start, int rn) {
		int n = rn;
		int st = Start;

		for (int i = 1; i <= (n * 2); i++) {
			if (i <= n) {
				for (int j = 0; j < i; j++) {
					System.out.print(st + " ");
				} // j loop ends
				st++;
				System.out.println("");
			} // if ends
			else {
				st--;
				for (int k = (n * 2 - i); k >= 0; k--)// here (n*2-i) is calculating the no of time st should be printed
														// decreasing it by one every time.
				{
					System.out.print(st + " ");
				}
				System.out.println("");
			}
		} // master loop

	}// series method ends
}// class ends
