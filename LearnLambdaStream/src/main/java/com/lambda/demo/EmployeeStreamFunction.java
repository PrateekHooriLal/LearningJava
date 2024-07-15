package com.lambda.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class EmployeeStreamFunction {
	public static void main(String[] args) {
		List<String> skills = new ArrayList<>();
		skills.add("Java");
		skills.add("C");
		List<String> skills2 = new ArrayList<>();
		skills.add("Java");
		skills.add("Python");
		skills.add("NodeJS");
		Employee emp1 = new Employee("A", 2.0, 24, skills, "Consultancy");
		Employee emp2 = new Employee("B", 11.0, 32, Collections.emptyList(), "HR");
		Employee emp3 = new Employee("C", 8.0, 30, Collections.emptyList(), "Finance");
		Employee emp4 = new Employee("D", 1.0, 22, skills2, "Consultancy");
		Employee emp5 = new Employee("E", 20.0, 42, Collections.emptyList(), "Finance");

		List<Employee> employeeList = new ArrayList<>();
		employeeList.add(emp1);
		employeeList.add(emp2);
		employeeList.add(emp3);
		employeeList.add(emp4);
		employeeList.add(emp5);
		// employeeList.stream().collect(Collectors.groupingBy(Employee::getDepartment),)

		// Highest exp employee
		/*
		 * Optional<Employee> highestEmp =
		 * employeeList.stream().max(Comparator.comparing(Employee::getExperience));
		 * System.out.println("Highest Experience Employee:>>" +
		 * highestEmp.get().toString());
		 */
		// avg exp of emp in each dept{dept,avg}
		Map<String, Double> avgEmp = employeeList.stream().collect(
				Collectors.groupingBy(Employee::getDepartment, Collectors.averagingDouble(Employee::getExperience)));
		System.out.println(avgEmp);

		// 3rd highest salary
		// employeeList.stream().collect(Collectors.groupingBy(Employee::getDepartmen,
		// Collectors.toMa))

		// group by department second high Salary
		Map depSecHighest = employeeList.stream()
				.collect(Collectors.groupingBy(Employee::getDepartment,
						Collectors.collectingAndThen(Collectors.mapping(Function.identity(), Collectors.toList()),
								list -> list.stream().sorted(Comparator.comparingInt(Employee::getAge).reversed())
										.skip(1).findFirst())));

	}
}