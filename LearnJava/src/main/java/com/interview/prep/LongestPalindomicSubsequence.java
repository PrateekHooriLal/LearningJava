package com.interview.prep;

public class LongestPalindomicSubsequence {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String s = "bbbba";
		System.out.println(find_palindrome(s));
	}

	public static int longestPalindromeSubseq(String s) {
		return 0;
	}

	// Method to find palindrome in strings
	public static boolean find_palindrome(String st) {
		StringBuffer str = new StringBuffer(st);
		str.reverse();
		st.contains(str);
		System.out.println(str);
		System.out.println(st);
		if (st.equals(str.toString()))
			return true;
		else
			return false;
	}

	public static String string_subsequence(String s, int i, int j) {
		return s;
	}

}
