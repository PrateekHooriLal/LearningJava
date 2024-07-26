package com.interview.prep;

/*An Array of integers is given, both +ve and -ve. You need to find the two elements such that their sum is closest to zero.
 *  
 * */

public class InfoWays {

	public static void main(String[] args) {

		int arr[] = { 6, 3, -6, 4, 2 };// op=
		int len = arr.length - 1;
		int minSum = arr[0] + arr[1];
		int e1 = 0;
		int e2 = 0;
		for (int i = 0; i <= len; i++) {
			for (int j = i + 1; j <= len; j++) {
				int sum = (arr[i] + arr[j]);
				if (sum > 0 && sum < minSum) {
					minSum = sum;
					e1 = arr[i];
					e2 = arr[j];
				} else if (sum < 0 && sum > minSum) {
					minSum = sum;
					e1 = arr[i];
					e2 = arr[j];
				}

			}
		}
		System.out.println("Minimum Sum =" + minSum);
		System.out.println("Element= " + e1 + " and " + e2);
	}

}
