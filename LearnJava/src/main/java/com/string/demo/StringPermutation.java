package com.string.demo;

import java.util.Scanner;

/**
 * @author Prateek
 *
 */

/*
 * Question: Print all permutation of String both iterative and Recursive way?
 * 
 * For Answer refer below link:
 * http://netjs.blogspot.in/2016/05/how-find-all-permutations
 * -of-given-string-java-program.html
 */
/**
 * * Java program to find all permutations of a given String using recursion. *
 * For example, given a String "XYZ", this program will print all 6 possible
 * permutations of * input e.g. XYZ, XZY, YXZ, YZX, ZXY, XYX * *
 * 
 * @author Javin Paul
 */
public class StringPermutation {

	public static void main(String[] args) {

		System.out.println("enter the String for permutation:");
		Scanner sc = new Scanner(System.in);
		String str = sc.nextLine();
		sc.close();
		System.out.println("Permutations for the string are \n" + " below:");

		// calling permutation function.
		permutation(str);

	}// main ends

	/* A method exposed to client to calculate permutation of String in Java. */

	public static void permutation(String input) {
		permutation("", input);
	}

	/**
	 * * Recursive method which actually prints all permutations * of given String,
	 * but since we are passing an empty String * as current permutation to start
	 * with, * I have made this method private and didn't exposed it to client.
	 */

	private static void permutation(String perm, String word) {
		//System.out.println("in the method " + Thread.currentThread().getName());

		if (word.isEmpty()) {
			System.err.println(perm + word);
		} else {
			for (int i = 0; i < word.length(); i++) {
				//System.out.println("in the loop " + i);
				permutation(perm + word.charAt(i), word.substring(0, i) + word.substring(i + 1, word.length()));
			}
		}
	}

	// using iterative technique:
	public static void permutation(char ch[]) {

	}// permutation method ends

}// classs ends
