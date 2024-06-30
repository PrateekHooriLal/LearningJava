package com.program.solution;

/*Print the missing Elements in given array*/
public class PrintMissingElement {

	public static void main(String[] args) {

		int[] arr1 = { 3, 4, 6, 8, 100, 9, 15, 20, 80, 500 };
		PrintMissingElement mis = new PrintMissingElement();
		int[] minMax = mis.findMinMax(arr1);
		System.out.println("Minimum=" + minMax[0] + " Maximum=" + minMax[1]);
		mis.findMissing(arr1, minMax[0], minMax[1]);
	}// main ends

	// 1: we need to find the min and max number.
	// 2: then run a loop from min to max printing numbers which are not present in
	// array

	public int[] findMinMax(int[] arr) {

		int min = Integer.MAX_VALUE, max = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] > max)
				max = arr[i];
			if (arr[i] < min)
				min = arr[i];
		}
		return new int[] { min, max };

	}

	public void findMissing(int[] arr, int min, int max) {

		int len = arr.length - 1;
		int counter = min;
		System.out.println("Missing Elements to be printed are:" + (max - len));

		while (counter <= max) {
			boolean isPresent = false;
			for (int i = 0; i <= len; i++) {
				if (counter == arr[i]) {
					isPresent = true;
					break;
				}
			}
			if (isPresent)
				counter++;
			else
				System.out.print(counter++ + " ");
		}
	}// findMissing method ends

}
// class ends
