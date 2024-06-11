package com.string.demo;

import java.util.ArrayList;

public class ReverseVowelsInString {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println(ReverseVowelsInString
				.revWowel("abcd efgh ijkl mnopq rst uvw xyaaaaaaaaaaaiiiiiiiiiiiieeeeeeeoooooooouuuuuuuzAB"));
	}// main ends

	public static String revWowel(String str) {

		ArrayList<Integer> indx = new ArrayList<Integer>();
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char ch = str.charAt(i);
			if (ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u' || ch == 'A' || ch == 'E' || ch == 'I'
					|| ch == 'O' || ch == 'U') {
				indx.add(i);
			}
		} // for loop ends

		int i = 0;
		int j = indx.size() - 1;

		char st[] = str.toCharArray();
		while (i < j) {
			char temp = st[indx.get(i)];
			st[indx.get(i)] = st[indx.get(j)];
			st[indx.get(j)] = temp;

			i++;
			j--;
		} // while ends
		return new String(st);
	}// method ends
}// class ends
