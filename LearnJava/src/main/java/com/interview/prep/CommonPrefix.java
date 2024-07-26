package com.interview.prep;

import java.util.Arrays;
import java.util.List;

public class CommonPrefix {
	public static void main(String[] args) {
		// List of strings
		List<String> strings = Arrays.asList("apple", "aanana", "aperry", "adate", "aelderberry", "afaig");

		// Find the common prefix
		String commonPrefix = findCommonPrefix(strings);

		// Print the common prefix
		System.out.println("Common Prefix: " + commonPrefix);
	}

	public static String findCommonPrefix(List<String> strings) {
		if (strings == null || strings.isEmpty()) {
			return "";
		}

		// Start with the first string in the list
		String prefix = strings.get(0);

		// Iterate over the list and update the prefix
		for (String str : strings) {
			// Update the prefix to the longest common prefix between the current prefix and
			// the current string
			System.out.println(prefix);
			System.out.println(str.indexOf(prefix));
			System.out.println("Length="+prefix.length());
			while (str.indexOf(prefix) != 0) {
				prefix = prefix.substring(0, prefix.length() - 1);
				System.out.println("In While="+prefix);
				if (prefix.isEmpty()) {
					return "";
				}
			}
		}

		return prefix;
	}
}
