/**
 * 
 */
package com.whiplash;

/**
 * @author plal
 *
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* * Java Program to demonstrate how to reverse a List. * In this example, you will see two ways to reverse a List, * 
 * first, using Collections.reverse() method and second * by writing your own method using recursion. */
public class get_it_reversed {

	public static void main(String args[]) {

		List<String> books = new ArrayList<>();
		books.add("Beautiful Code");
		books.add("Clean Code");
		books.add("Working Effectively with Legacy Code");

		String temp = books.get(2);

		System.out.println("result of get method:" + temp);

		System.out.println("Last index of books list:" + books.lastIndexOf("Clean Code"));

		System.out.println("Original order of List: " + books + "\n");

		// Easy way to reverse a List in Java, use Collections.reverse()
		// method, use this to reverse ArrayList or LinkedList in
		Collections.reverse(books);
		System.out
				.println("The reversed List using Collection.reverse method: " + books.toString().toUpperCase() + "\n");

		// Now, let's try to reverse a List using recursion
		List<String> output = reverseListRecursively(books);
		System.out.println("Reversed list reversed again using recursion method: " + output + "\n");
	}

	/**
	 * * A recursive algorithm to reverse a List in Java * * @param list * @return
	 */
	private static List<String> reverseListRecursively(List<String> list) {
		if (list.size() <= 1) // terminating condition.
		{
			return list;
		}

		List<String> reversed = new ArrayList<>();
		reversed.add(list.get(list.size() - 1));
		// System.out.println("Size of passed list per recursion:"+list.size());
		reversed.addAll(reverseListRecursively(list.subList(0, list.size() - 1)));
		// from here we are returning a reduced part of the list
		// using sublist method.
		// We can also use "add" here to add the elements recursively but it will make
		// the program type unsafe.
		return reversed;
	}
}
