package com.stream.api;

import java.util.List;
import java.util.stream.Stream;

public class StringAnagram {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(StringAnagram.isAnagram("army", "abcd"));
	}// main ends

	private static boolean isAnagram(String string, String string2) {
		// TODO Auto-generated method stub
		if (string.length() != string2.length() || string.isEmpty() || string2.isEmpty())
			return false;

		List l1 = Stream.of(string).toList();
		
		int len = string2.chars().distinct().mapToObj(c->l1.contains(c)).toList().size();

		/*
		 * int len = string.chars().distinct().filter(e -> { if (string2.indexOf(e) !=
		 * -1) return true; else return false; }).toString().length();
		 */

		System.out.println(len);
		if (len == string2.length())
			return true;
		else
			return false;

	}

}
