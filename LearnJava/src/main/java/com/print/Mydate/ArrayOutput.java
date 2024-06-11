package com.print.Mydate;

public class ArrayOutput {
	public static void main(String args[]) {
		int array_variable[] = new int[10];
		for (int i = 0; i < 10; ++i) {
			array_variable[i] = i / 2;
			array_variable[i]++;
			System.out.print(array_variable[i] + "->>");
			i++;
		}
		char chars[] = { 'a', 'b', 'c' };
		String s = new String(chars);
		System.out.println(s);

	}

}
/*
 * Explanation:When an array is declared using new operator then all of its
 * elements are initialized to 0 automatically. for loop body is executed 5
 * times as whenever controls comes in the loop i value is incremented twice,
 * first by i++ in body of loop then by ++i in increment condition of for loop.
 * o utput: $ javac array_output.java $ java array_output 1 2 3 4 5
 */