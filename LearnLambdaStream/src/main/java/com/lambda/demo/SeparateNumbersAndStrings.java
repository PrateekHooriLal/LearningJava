package com.lambda.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeparateNumbersAndStrings {
	public static void main(String[] args) {
		List<String> sampleStr = new ArrayList<>();
		List<Integer> sampleInt = new ArrayList<>();

		sampleStr.add("He100");
		sampleStr.add("W0r1d");
		sampleStr.add("98.3FM");
		sampleStr.add("1,233.00 USD");

		//sampleStr.stream().mat

		// Regular expression to match one or more digits
		Pattern numberPattern = Pattern.compile("\\d+");

		for (int i = 0; i < sampleStr.size(); i++) {
			String str = sampleStr.get(i);

			// clean string from special characters
			str = str.replaceAll("[^a-zA-Z0-9]", "");

			// find all digits and add them to sampleInt list
			Matcher matcher = numberPattern.matcher(str);
			while (matcher.find()) {
				String number = matcher.group();
				sampleInt.add(Integer.parseInt(number));
			}

			// as we have removed special chars so only number needs to be removed to get
			// string only, here set will replace the
			sampleStr.set(i, str.replaceAll("[0-9]", ""));

		}

		// Print the results
		System.out.println("Numbers: " + sampleInt);
		System.out.println("Strings: " + sampleStr);
	}
}
