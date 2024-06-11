package com.code.analysis;

public class Inc {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int[] i = { 1 };
		Inc in = new Inc();
		in.increment(i);

		System.out.println(i[i.length - 1]);
	}

	void increment(int[] i) {
		i[i.length - 1]++;
	}

}
