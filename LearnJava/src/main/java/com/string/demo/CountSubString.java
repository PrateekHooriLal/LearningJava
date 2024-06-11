package com.string.demo;

public class CountSubString {

	public static int countSub(String parent, String sub) {
		int parentLength = parent.length();
		int subLength = sub.length();

		parent = parent.toLowerCase();
		sub = sub.toLowerCase();

		String remString = parent.replaceAll(sub, "");
		int newLength = parentLength - remString.length();

		return (newLength / subLength);
	}

	public static void main(String[] args) {
		System.out.println(CountSubString.countSub("AABaabccaabAAB", "Aab"));
	}
}
