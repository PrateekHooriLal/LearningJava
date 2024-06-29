package com.lambda.demo;

import java.util.Arrays;
import java.util.List;

public class RemoveDuplicateUsingString {
	static List<Integer> list = Arrays.asList(0, 5, 2, 2, 10, 4, 5, 5, 6);// 0 5 2 10 4 5 6

	public static void main(String[] args) {
		String str = "";

		for (Integer i : list) {

			if (str.indexOf(String.valueOf(i)) == -1)
				str = str + i + " ";

		}
		System.out.println(str);
	}

}
