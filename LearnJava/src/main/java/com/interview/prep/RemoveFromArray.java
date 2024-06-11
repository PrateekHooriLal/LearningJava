package com.interview.prep;

public class RemoveFromArray {

	/**
	 * 
	 * 
	 * [1,1,0,1,1,1,0,0,0] 3 //private static final long serialVersionUID =
	 * 1104496369737649481L;
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int count = 0;
		int max_count = 0;

		int[] arr = { 1, 0, 1, 1, 0, 1 };

		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == 1)
				count++;
			else {
				max_count = count;
				count = 0;
			}
		}
		if (max_count > count)
			System.out.println(max_count);
		else
			System.out.println(count);

	}

}
