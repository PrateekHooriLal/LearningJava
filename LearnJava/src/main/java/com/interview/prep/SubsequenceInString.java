package com.interview.prep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SubsequenceInString {

	// to store generated subsequences
	static List<String> subseqlist = new ArrayList<String>();

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String s = "Amon";
		HashSet<String> set = new HashSet<String>();

		findSubsequence(s, "");
		System.out.println(subseqlist);

		for (String ans : subseqlist) {
			set.add(ans);
		}

		System.out.println(set);

		/*
		 * total number of subsequences for String of length n will be 2^n
		 * (Math.pow(2,n))
		 */
		System.out.println(Math.pow(2, 5));

	}

	/* Using Recursion and implementing using pick and don't pick approach */

	public static void findSubsequence(String s, String subeq) {

		if (s.length() == 0) {
			subseqlist.add(subeq);
			return;
		}

		/*
		 * While Pick so in picking we will attach(concatenate) the picked char i,e
		 * chrAt(0) to the subsequence.
		 */
		findSubsequence(s.substring(1), subeq + s.charAt(0));

		/*
		 * while Don't Pick scenario will not concatenate the picked char
		 */
		findSubsequence(s.substring(1), subeq);

	}
	

}
