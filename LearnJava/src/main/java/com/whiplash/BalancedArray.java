package com.whiplash;

public class BalancedArray {

	public static void main(String[] args) {

		int arr[] = { 1, 2, 3, 0, 1 };

		int arr_size = arr.length;
		int sum = 0;
		for (int i = 0; i < arr_size; i++)
			sum += arr[i];

		System.out.println("First equilibrium index is " + BalancedArray.balancedArray(arr, arr_size, sum));

		// System.out.print();

	}// main ends.

	public static int balancedArray(int[] ar, int len, int sum1) {
		int lsum = 0;
		System.out.println("Sum of aray=" + sum1);

		for (int i = 0; i < len; ++i)// calculate sum
		{
			sum1 -= ar[i];
			lsum += ar[i];
			if (lsum == sum1) {
				return i;
			}

		} // for ends
		return 0;

	}// method ends

}// Class ends
