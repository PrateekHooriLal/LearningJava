package com.interview.prep;

/*Longest Common Subsequence (LCS)
 * Given two strings, S1 and S2, the task is to find the length of the Longest Common Subsequence, 
 * i.e. longest subsequence present in both of the strings. 
*/
public class LargestCommonSubsequence {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static int findLcs(String s1, String s2, int index1, int index2) {

		if (s1.length() == 0 || s2.length() == 0)
			return index2;
		return index2;
	}

	public static int max(int n, int m) {

		return n > m ? n : m;
	}
}
