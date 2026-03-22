package com.whiplash;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author plal
 *
 */

/*
 * merger two sorted arrays in one without any auxiliary array assuming both
 * arrays are sorted in ascending order
 */
public class MergeSortedArrays {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int a[] = { 1, 2, 3, 4, 0, 0, 0, 0 };
		int b[] = { 2, 6,97, 7, 8 };
		int na = 4;
		int nb = 4;
		int[] result = IntStream.concat(Arrays.stream(a), Arrays.stream(b))
                .sorted()
                .toArray();
		System.out.println(Arrays.toString(result));

		// first blindly copy the array bin a.
		for (int i = 0; i < 8; i++) {

		} // for loop ends
		int A[] = { 1, 3, 8, 9, -1 };
		int B[] = { 2, 4, 5, 6, 7, 0, 0, 0, 0 }; // last four as place holders for the merge

		// start from the end of the larger array;
		int idx = 8;
		// we also need the indices of the largest elements in both arrays
		int idx_a = 3, idx_b = 4;

		while (idx_a >= 0) { // done when A has been traversed
			if (idx_b < 0 || A[idx_a] > B[idx_b]) { // if elements of b are exhausted
				B[idx] = A[idx_a];
				idx_a--;
			} else {
				B[idx] = B[idx_b];
				idx_b--;
			}
			idx--;
		}
		for (int i = 0; i < B.length; i++)
			System.out.print(B[i] + " ");
	}// main ends

}// class ends
