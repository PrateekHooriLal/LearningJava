package com.stream.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamCreation {

	// Array of Employee object
	public static Employee[] arrayOfEmps = { new Employee(1, "Jeff Bezos", 300000.0, "HR"),
			new Employee(2, "Bill Gates", 200000.0,"Sales"), new Employee(3, "Mark Zuckerberg", 300000.0,"DEV"),
			new Employee(10, "George", 10000,"HR"), new Employee(12, "Robert", 15000,"DEV"), new Employee(24, "Kathy", 25000,"Sales"),
			new Employee(5, "Robert", 140,"HR"), new Employee(-1, "Robert", 140,"HR") };

	// converting to ArrayList
	public static List<Employee> emplist = Arrays.asList(arrayOfEmps);
	

	public static void main(String[] args) {

		
		Arrays.sort(arrayOfEmps);
		// Sort using in compareTo in Employee Class
		Collections.sort(emplist);

		// Let’s first obtain a stream from an existing array:
		Stream<Employee> s = Stream.of(arrayOfEmps);

		System.out.println("Sorted in Ascending:");
		sortEmpSalaryAesc(s, "Salary").forEach(t -> t.printEmp());

		System.out.println("\n \nSorted in Descending:");
		sortEmpSalaryDesc(emplist.stream()).forEach(e -> e.printEmp());

		System.out.print("Find Max Emp with salary = ");
		findMaxSalary(emplist).get().printEmp();
		

	}

	public static List<Employee> sortEmpSalaryAesc(Stream<Employee> s, String sortBy) {

		return s.sorted().toList();

	}

	public static List<Employee> sortEmpSalaryDesc(Stream<Employee> s) {
		return s.sorted(Comparator.comparing(Employee::getName)).collect(Collectors.toList());
	}

	public static Optional<Employee> findMaxSalary(List<Employee> empList) {

		return emplist.stream().max(Comparator.comparing(Employee::getSalary));
	}

	
}
