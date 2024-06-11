package com.print.Mydate;

public class Arrayss {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int[][] arr = new int[3][];

		Arrayss s = new Arrayss();
		s.sum(arr);

	}

	int sum(int[][] arr) {
		int[] a1 = new int[7];
		int[] a2 = new int[3];
		int[] a3 = new int[9];

		int count = 0;

		arr[0] = a1;
		arr[1] = a2;
		arr[2] = a3;
		for (int i = 0; i < arr.length; i++) {
			System.out.println(arr[i].length);

			for (int j = 0; j < 3; j++) {
				arr[i][j] = arr[j].length;
				count += arr[i][j];
			}
		}
		System.out.println(count);

		return 0;

	}

}
