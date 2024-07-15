/**
 * 
 */
package com.string.demo;

/**
 * @author Prateek
 *
 */

/**
 * By the way two String are called anagram, if they contains same characters
 * but on different order e.g. army and mary, stop and pots etc. Anagrams are
 * actually mix-up of characters in String. If you are familiar with String API,
 * i.e. java.lang.String than you can easily solve this problem. In order to
 * check if Strings are anagram, you need to get there character array and see
 * if they are equal or not.
 *
 * Read
 * more:http://javarevisited.blogspot.com/2013/03/Anagram-how-to-check-if-two
 * -string-are-anagrams-example-tutorial.html#ixzz4fqI5sP6K
 */

/*
 * Some imp points: 1 : comparing them, if both char array is equal then Strings
 * are anagram. 2: But before comparing, make sure that both String are in same
 * case e.g. lowercase or uppercase and character arrays are sorted, because
 * equals method of Arrays, return true, only if array contains same length, and
 * each index has same character.
 */
public class StringAnagram {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(StringAnagram.isAnagram("army", "marw"));
	}// main ends

	/**
	 * @param takes two String to be compared
	 * @return true, if both String are anagram
	 */
	public static boolean isAnagram(String a, String b) {

		// comparing the length of both strings
		if (a.length() != b.length() || a.isEmpty() || b.isEmpty())
			return false;

		// convert both strings to lower case
		String i = a.toLowerCase();
		String j = b.toLowerCase();

		// convert any one strng to char array
		char[] ch = i.toCharArray();

		// converting second string to string builder as it will not create garbage
		// string while processing.
		StringBuilder sb = new StringBuilder(j);

		for (char c : ch) {
			int index = sb.indexOf(Character.toString(c));
			// we can pass ""+c which will give indexOf an string input.

			// traverse through the array and delete the characters at the
			// index.s
			if (index != -1) {
				sb = sb.deleteCharAt(index);
			} else {
				return false;
			}
		} // for each loop ends

		return sb.length() == 0 ? true : false;

	}// isAnagram method ends
}// class ends
