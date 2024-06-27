package com.transaction.manager.controller;

import java.util.HashMap;
import java.util.Map;

public class Test {

	int max = 0;
	static int count = 0;
	public static Map<String, Integer> map = new HashMap<String, Integer>();

	public static void main(String[] args) {
		String str = "aaabbaaaacccccbbbbcc";
		for (int i = 0; i < str.length(); i++) {

			char a = str.charAt(i);
			if (str.charAt(i + 1) == a) {
				continue;
			} else {
				findMaxSeq(str.substring(count, i));
				count = i;
			}
		}
	}

	public static void findMaxSeq(String s) {

	}
}
