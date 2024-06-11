package com.string.demo;

/**
 * @author Prateek
 *
 */

/**
 * Java program to remove given character from a given String using loops and
 * recursion, * asked as coding question to Java programmers.
 */
public class RemoveCharFromString {

	public static void main(String... args) {

		RemoveCharFromString obj = new RemoveCharFromString();
		System.out.println(obj.removeRecursive("Some thing is the which keeps on changing", 't'));
	}// main ends

	public static String remove(String word, char unwanted) {
		StringBuilder sb = new StringBuilder();
		char[] letters = word.toCharArray();
		for (char c : letters) {
			if (c != unwanted) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public String removeRecursive(String word, char ch) {
		int index = word.indexOf(ch);
		if (index == -1)
			return word;

		else
			return removeRecursive(word.substring(0, index).concat(word.substring(index + 1, word.length())), ch);
		// OR
		// return removeRecursive(word.substring(0, index) + word.substring(index +1,
		// word.length()), ch);

	}// function ends
	/*
	 * 
	 * Read more:
	 * http://javarevisited.blogspot.com/2015/04/how-to-remove-given-character
	 * from.html#ixzz4fqF8HStW
	 */
}// class ends
