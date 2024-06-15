package com.interview.prep;

import java.util.Arrays;

/*Longest Common Subsequence (LCS)
 * Given two strings, S1 and S2, the task is to find the length of the Longest Common Subsequence, 
 * i.e. longest subsequence present in both of the strings. 
*/
public class LargestCommonSubsequence {

	private static int[][] memo;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String s1 = "AGGTAB";
		String s2 = "GXTXAYB";

		int m = s1.length();
		int n = s2.length();

		// Initialize memoization table with -1
		memo = new int[m + 1][n + 1];
		for (int[] row : memo) {
			Arrays.fill(row, -1);
		}
		System.out.println("Length of LCS is " + lcs(s1, s2, m, n));
		System.out.println("Length of LCS is " + findLcs(s1, s2, m, n));
		System.out.println("Length of LCS is " + getlcs(s1, s2, m, n));

	}

	public static int findLcs(String s1, String s2, int m, int n) {

		// Base case: If either string is empty
		if (m == 0 || n == 0) {
			return 0;
		}

		// If the last characters of both sequences match
		if (s1.charAt(m - 1) == s2.charAt(n - 1)) {
			return 1 + lcs(s1, s2, m - 1, n - 1);
		} else {
			// If the last characters don't match
			return Math.max(lcs(s1, s2, m, n - 1), lcs(s1, s2, m - 1, n));
		}

	}

	// Recursive method to find the length of LCS with memoization
	public static int lcs(String s1, String s2, int m, int n) {
		// Base case: If either string is empty
		if (m == 0 || n == 0) {
			return 0;
		}

		// Check if the result is already computed
		if (memo[m][n] != -1) {
			return memo[m][n];
		}

		// If the last characters of both sequences match
		if (s1.charAt(m - 1) == s2.charAt(n - 1)) {
			memo[m][n] = 1 + lcs(s1, s2, m - 1, n - 1);
		} else {
			// If the last characters don't match
			memo[m][n] = Math.max(lcs(s1, s2, m, n - 1), lcs(s1, s2, m - 1, n));
		}

		return memo[m][n];
	}

	public static String getlcs(String s1, String s2, int m, int n) {

		// Backtrack to find the LCS string
		int i = m, j = n;
		StringBuilder lcs = new StringBuilder();
		while (i > 0 && j > 0) {
			if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
				lcs.append(s1.charAt(i - 1));
				i--;
				j--;
			} else if (memo[i - 1][j] > memo[i][j - 1]) {
				i--;
			} else {
				j--;
			}
		}

		// Since we built the lcs string from the end, reverse it before returning
		return lcs.reverse().toString();

	}
}
