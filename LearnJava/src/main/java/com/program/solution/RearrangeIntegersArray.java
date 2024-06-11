package com.program.solution;

import com.java.sorting.Sorting;

public class RearrangeIntegersArray extends Sorting {

	static int arr[] = Sorting.array;

	public int[] rearrange(int input[]) {

		int len = input.length;
		int p = 0, n = 0;
		int pos[], neg[];

		for (int i = 0; i < len; i++) {
			if (input[i] < 0)
				n++;
			else
				p++;
		}
		pos = new int[p];
		neg = new int[n];

		for (int i = 0; i < len; i++) {
			int p1 = 0, n1 = 0;
			if (arr[i] < 0)
				neg[n1++] = arr[i];
			else
				pos[p1++] = arr[i];
		}

		printArray(neg);
		return pos;

	}// method ends

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		RearrangeIntegersArray m = new RearrangeIntegersArray();
		System.out.println(m.rearrange(arr));
	}

}
