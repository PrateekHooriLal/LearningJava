package com.string.demo;

/**
 * @author Prateek
 *
 */
public class ReverseStringWords {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ReverseStringWords o = new ReverseStringWords();
		String sc[] = o.revWord("this is the day that the lord has made");

		int i = sc.length - 1;
		while (i >= 0) {
			System.out.print(sc[i] + " ");
			i--;
		} // while loop ends

	}// main ends

	public String[] revWord(String str) {
		String[] rev = new String[str.length()];
		rev = str.split(" ");

		return rev;
	}// method ends

}// class ends
