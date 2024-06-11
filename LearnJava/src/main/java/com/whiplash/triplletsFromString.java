package com.whiplash;

public class triplletsFromString {

	public static void main(String[] args) {
		// System.out.println("This is the Day that the Lord has made.");
		String s = "this";
		String st = "this";
		String str = new String("supernatural");

		System.out.println(s == st);// why this different.
		System.out.println("== result:" + (s == str));//
		System.out.println(".Equals result:" + s.equals(st));// same with bracket
		System.out.println("== result:" + (s.equals(st)));// same with bracket
		System.out.println("substing=" + str.substring(2, 5));

	}// main ends

}// class ends
