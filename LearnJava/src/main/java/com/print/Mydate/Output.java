package com.print.Mydate;

public class Output {

	public static void main(String args[]) {
		int a = 1;
		int b = 2;
		int c = 3;
		a |= 4;
		System.out.println("a=" + a);
		b >>= 1;
		c <<= 1;
		a ^= c;
		System.out.println(a + " " + b + " " + c);
	}
}
