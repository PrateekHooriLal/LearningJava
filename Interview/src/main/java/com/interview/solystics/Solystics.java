package com.interview.solystics;

/*String s1= JAVA_JSP_ANDROID 
* op=  DIOR_DNA_PSJAVAJ
* return reverse string while preserving position of underscore
*/
public class Solystics {

	public static void main(String[] args) {
		String s1 = "JAVA_JSP_ANDR_OID_AB__C";
		char preserveChar = '_';
		System.out.println("Input:  " + s1);
		String result = reversePreservingChar(s1, preserveChar);
		System.out.println("Output: " + result);
	}

	public static String reversePreservingChar(String input, char preserveChar) {
		// Create a StringBuilder for constructing the final result
		StringBuilder result = new StringBuilder(input);

		// Initialize pointers for start and end of the string
		int start = 0;
		int end = input.length() - 1;

		// Iterate until the start pointer meets the end pointer
		while (start < end) {
			// Skip the preserveChar from the start
			if (input.charAt(start) == preserveChar) {
				start++;
				continue;
			}

			// Skip the preserveChar from the end
			if (input.charAt(end) == preserveChar) {
				end--;
				continue;
			}

			// Swap characters at start and end pointers
			result.setCharAt(start, input.charAt(end));
			result.setCharAt(end, input.charAt(start));

			// Move the pointers towards the center
			start++;
			end--;
		}

		return result.toString();
	}
}
