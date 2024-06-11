package com.string.demo;

import java.util.Arrays;

public class String_demo {
	public static void main(String args[]) {
		int ascii[] = { 65, 66, 67, 68 };
		String s = new String(ascii, 1, 3);
		System.out.println(s);

		String s1 = "JOJO";
		System.out.println("S1 before concat=" + s1);
		s1.concat("LAL");
		System.out.println("S1 after concat=" + s1.concat("LAL"));
		System.out.println(Arrays.toString("\u0041".getBytes()));
	}// main ends
}// class ends
