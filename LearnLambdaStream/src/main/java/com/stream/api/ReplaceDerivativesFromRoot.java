package com.stream.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/*dictionary   =  ["cat","bat","rat"] cat,bat,rat
  String input =  "The cats were battling the rattles with battless and catlike reflexes.";
*/
public class ReplaceDerivativesFromRoot {

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);

		System.out.println("Input the root list:");

		List<String> dictionary = Arrays.stream(sc.nextLine().split(",")).toList();
		Set<String> set = new HashSet<String>(dictionary);
		System.out.println(dictionary);

		System.out.println("Input the Sentence:");
		String sentence = sc.nextLine();
		for (String root : dictionary) {
			String rootRegex = "\\^(" + root + "\\s$)";
			System.out.println(sentence.replaceAll(rootRegex, root));
		}

	}

}
