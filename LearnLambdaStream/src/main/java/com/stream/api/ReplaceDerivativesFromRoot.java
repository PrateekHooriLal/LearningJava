package com.stream.api;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/*dictionary   =  ["cat","bat","rat"] cat,bat,rat
  String input =  "The cats were battling the rattles with battless and catlike reflexes.";
*/
public class ReplaceDerivativesFromRoot {

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);

		System.out.println("Input the root list:");

		List<String> dictionary = Arrays.stream(sc.nextLine().split(",")).toList();
		System.out.println(dictionary);

		System.out.println("Input the Sentence:");
		String sentence = sc.nextLine();

		// BUG FIX: regex was "\\^(" + root + "\\s$)" which is completely wrong:
		//   \\^ matches a literal caret character (not start of string)
		//   \\s$ requires whitespace at end of string — word derivatives never match this
		// CORRECT regex: \b<root>\w*\b
		//   \b = word boundary (don't match inside another word)
		//   \w* = zero or more word characters (catches "cats", "battling", "rattles")
		//   replaceAll is called on the accumulating sentence so all roots are applied
		for (String root : dictionary) {
			String rootRegex = "\\b" + root + "\\w*\\b";
			sentence = sentence.replaceAll(rootRegex, root);
		}
		System.out.println(sentence);

		// BUG FIX: Scanner was never closed — resource leak
		sc.close();
	}

}
