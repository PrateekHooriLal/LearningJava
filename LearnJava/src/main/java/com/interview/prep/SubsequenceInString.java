package com.interview.prep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SubsequenceInString {

	// to store generated subsequences
	static List<String> subseqlist = new ArrayList<String>();

	public static void main(String[] args) {

		String s = "Amon";
		HashSet<String> set = new HashSet<String>();

		findSubsequence(s, "");
		System.out.println(subseqlist);

		for (String ans : subseqlist) {
			set.add(ans);
		}

		System.out.println(set.toString() + "\n\n");

		System.out.println("======Using Stream====");
		System.out.println(generateSubsequences(s).toList().toString());

		/*
		 * total number of subsequences for String of length n will be 2^n
		 * (Math.pow(2,n))
		 */
		System.out.println(Math.pow(2, 5));

		generateSubsequences(s).forEach(System.out::print);

	}

	/* Using Recursion and implementing using pick and don't pick approach */

	public static void findSubsequence(String s, String subeq) {

		if (s.length() == 0) {
			subseqlist.add(subeq);
			return;
		}

		/*
		 * While Pick, so in picking we will attach(concatenate) the picked char i,e
		 * chrAt(0) to the subsequence.
		 */
		findSubsequence(s.substring(1), subeq + s.charAt(0));

		/*
		 * while Don't Pick scenario will not concatenate the picked char
		 */
		findSubsequence(s.substring(1), subeq);

	}

	// Using Streams to generate subsequence.
	public static Stream<String> generateSubsequences(String input) {
		return IntStream.range(0, 1 << input.length())
				.mapToObj(num -> IntStream.range(0, input.length()).filter(i -> (num & (1 << i)) > 0)
						.mapToObj(input::charAt)
						.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString())
				.sorted();
	}

}
