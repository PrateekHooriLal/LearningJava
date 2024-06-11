/**
 * 
 */
package com.code.analysis;

/**
 * @author Prateek
 *
 */
public class Sample1 {

	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int[] arr = { 1, 2, 3, 4, 5, 6 };
		int counter = 0;

		for (int value : arr) {
			if (counter >= 5) {
				System.out.println("break");
				break;
			} else {
				System.out.println("break");
				continue;
			} // any code after this if else will be unreachable code and will
				// not compile.

		} // for each ends

	}// main ends
}// class ends
