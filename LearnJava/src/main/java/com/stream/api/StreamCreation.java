package com.stream.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamCreation {

	public static Employee[] arrayOfEmps = { new Employee(1, "Jeff Bezos", 100000.0),
			new Employee(2, "Bill Gates", 200000.0), new Employee(3, "Mark Zuckerberg", 300000.0),
			new Employee(10, "George", 10000), new Employee(12, "Robert", 15000), new Employee(24, "Kathy", 25000) };

	public static ArrayList<Employee> emplist = new ArrayList<Employee>(Arrays.asList(arrayOfEmps));

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// 1: Let’s first obtain a stream from an existing array:
		Stream<Employee> s = Stream.of(arrayOfEmps);

		System.out.println("Sorted in Ascending:");
		sortEmpSalaryAesc(s,"Salary").forEach(t -> t.printEmp());

		System.out.println("\n \nSorted in Descending:");
		sortEmpSalaryDesc(emplist.stream()).forEach(e -> e.printEmp());

	}

	public static List<Employee> sortEmpSalaryAesc(Stream<Employee> s, String sortBy) {
		
		//return s.sorted().toList();
		
		 return s.sorted(Comparator.comparing(Employee::getName).reversed()).toList();

	}

	public static List<Employee> sortEmpSalaryDesc(Stream<Employee> s) {
		return s.sorted(Comparator.comparing(Employee::getSalary).reversed()).collect(Collectors.toList());
	}

}
