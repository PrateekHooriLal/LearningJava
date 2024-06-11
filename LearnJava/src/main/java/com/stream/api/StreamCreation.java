package com.stream.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class StreamCreation {

	public static Employee[] arrayOfEmps = { new Employee(1, "Jeff Bezos", 100000.0),
			new Employee(2, "Bill Gates", 200000.0), new Employee(3, "Mark Zuckerberg", 300000.0) };

	public static ArrayList<Employee> emplist = new ArrayList<Employee>(Arrays.asList(arrayOfEmps));

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// 1: Let’s first obtain a stream from an existing array:
		Stream s = Stream.of(arrayOfEmps);

		emplist.get(0).printEmp();
		arrayOfEmps[2].printEmp();
	}

}
