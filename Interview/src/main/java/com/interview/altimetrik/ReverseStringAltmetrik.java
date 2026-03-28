package com.interview.altimetrik;

public class ReverseStringAltmetrik {

	public static void main(String[] args) {
		String s = "Test1ng-Leet=code-Q!";

		char[] ch = s.toCharArray();

		int indS = 0;
		int indE = ch.length - 1;
		while (indS < indE) {

			if (ch[indS] == '-')
				indS++;

			if (ch[indE] == '-')
				indE--;

			if (ch[indS] != '-' && ch[indE] != '-') {
				// swap
				char temp = ch[indS];
				ch[indS] = ch[indE];
				ch[indE] = temp;

				indS++;
				indE--;
			}

		}
		System.out.println(ch);
	}

}
