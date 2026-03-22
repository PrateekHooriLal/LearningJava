package com.stream.api;

public class StringAnagram {

	public static void main(String[] args) {
		System.out.println(StringAnagram.isAnagram("earth", "harte"));
	}// main ends

	private static boolean isAnagram(String s1, String s2) {
		if (s1.length() != s2.length() || s1.isEmpty() || s2.isEmpty())
			return false;

		for (int i = 0; i < s2.length(); i++) {
			s1 = s1.replace(Character.toString(s2.charAt(i)), "");
		}

		return s1.length() == 0 ? true : false;
	}

}