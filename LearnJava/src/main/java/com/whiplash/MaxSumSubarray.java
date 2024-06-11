package com.whiplash;

/**
 * @author plal
 *
 */
/**
 * Based on Kadane's algorithm.
 *
 */
public class MaxSumSubarray {

	public int findSubarray(int[] arr) {

		int len = arr.length;

		int max_so_far = 0, max_ending_here = 0, startIndex = 0, lastIndex = 0, maxstartIndextillnow = 0;

		for (int i = 0; i < len; i++) {

			max_ending_here += arr[i];

			// check if the max ending is non negative if yes reset it to zero
			if (max_ending_here < 0) {
				max_ending_here = 0;
				maxstartIndextillnow = i + 1;
			}

			/*
			 * small optimization added, Do not compare for all elements. Compare only when
			 * max_ending_here > 0
			 */
			else if (max_so_far < max_ending_here) {
				max_so_far = max_ending_here;
				startIndex = maxstartIndextillnow;
				lastIndex = i;
			}
		} // for loop end's
		System.out.println("Start index:" + startIndex + "==>last Index:" + lastIndex);

		for (int i = startIndex; i <= lastIndex; i++)
			System.out.print(arr[i] + " + ");

		return max_so_far;

	}// find method ends

	public void findMaxSubArray(int[] inputArray) {

		// to handle scenarios where all the elements are negative.
		int maxStartIndex = 0;
		int maxEndIndex = 0;
		int maxSum = Integer.MIN_VALUE;

		int cumulativeSum = 0;// to hold the increasing sum while traversing the
								// array.
		int maxStartIndexUntilNow = 0;

		for (int currentIndex = 0; currentIndex < inputArray.length; currentIndex++) {

			// get the current item in a variable for comparison.
			int eachArrayItem = inputArray[currentIndex];

			// increasing the cumulative sum by adding each item.
			cumulativeSum += eachArrayItem;

			// checking if cumulative sum is greater than max sum
			if (cumulativeSum > maxSum) {

				maxSum = cumulativeSum;// reset max sum to cumulative sum

				// store the max start index
				maxStartIndex = maxStartIndexUntilNow;

				// store the max end index
				maxEndIndex = currentIndex;
			}
			// and if cumulative sum is less than zero , rather than setting max
			// sum to zero we set cumulative sum to zero and increase the
			// maxStartIndexUntilNow by one.
			else if (cumulativeSum < 0) {
				maxStartIndexUntilNow = currentIndex + 1;
				cumulativeSum = 0;
			}
		}

		System.out.println("Max sum         : " + maxSum);
		System.out.println("Max start index : " + maxStartIndex);
		System.out.println("Max end index   : " + maxEndIndex);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int array[] = { -6, -2, -3, -4, 1, -5, -5 };
		MaxSumSubarray obj = new MaxSumSubarray();
		obj.findMaxSubArray(array);
		System.out.println("=" + obj.findSubarray(array));
	}// main ends

}// class ends
