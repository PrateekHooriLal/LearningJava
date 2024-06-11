package com.print.Mydate;

class evaluate {
	public static void main(String args[]) {
		int a[] = { 1, 2, 3, 4, 5 };
		int d[] = a;
		int sum = 0;
		System.out.println(d[3]);
		for (int j = 0; j < 3; ++j) {
			System.out.println("Value of j before evaluation cycle :" + j);
			sum += (a[j] * d[j + 1]) + (a[j + 1] * d[j]);

		}
		System.out.println(sum);
		Integer a1 = 12;
		int b = 12;
		System.out.println();
	}
}