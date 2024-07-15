package com.lambda.demo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.stream.api.Employee;

import com.stream.api.Employee;

public class Mphasis {
//list Employee in empList 
	// Array of Employee object
	public static Employee[] arrayOfEmps = { new Employee(1, "Jeff Bezos", 300000.0, "HR"),
			new Employee(2, "Bill Gates", 200000.0, "Sales"), new Employee(3, "Mark Zuckerberg", 300000.0, "DEV"),
			new Employee(10, "George", 10000, "HR"), new Employee(12, "Robert", 15000, "DEV"),
			new Employee(24, "Kathy", 25000, "Sales"), new Employee(5, "Robert", 140, "HR"),
			new Employee(-1, "Robert", 140, "HR") };
	// converting to ArrayList
	public static List<Employee> emplist = Arrays.asList(arrayOfEmps);

	public static void main(String[] args) {

		System.out.println(
		emplist.stream().collect(Collectors.groupingBy(Employee::getDepartment,
				Comparator.comparing(Employee::getSalary).reversed())).toList()
				);
		
		//
	}

}
