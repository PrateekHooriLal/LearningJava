package com.lambda.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================
 * CONCEPT: Advanced Stream Collectors on Object Lists
 * ============================================================
 * TOPIC: groupingBy, averagingDouble, collectingAndThen, mapping
 *
 * These are the most commonly asked Stream operations in Java interviews
 * at mid-to-senior level. Master these patterns.
 *
 * COLLECTORS COVERED:
 *   1. groupingBy + averagingDouble  → average per group
 *   2. groupingBy + collectingAndThen → post-process each group
 *   3. groupingBy + mapping          → transform before collecting
 *   4. stream().max()                → find max element
 *
 * INTERVIEW ANGLE (SDE 2/3 level):
 *   "Group employees by department and find average experience"
 *   "Find the second highest salary employee in each department"
 *   These require chained collectors — key skill to demonstrate.
 * ============================================================
 */
class EmployeeStreamFunction {

	public static void main(String[] args) {

		// ---- Test Data Setup ----
		// skills  → emp1 (A): ["Java", "C"]
		// skills2 → emp4 (D): ["Java", "Python", "NodeJS"]
		List<String> skills = new ArrayList<>();
		skills.add("Java");
		skills.add("C");

		List<String> skills2 = new ArrayList<>();
		skills2.add("Java");    // NOTE: must be skills2, not skills (common copy-paste bug)
		skills2.add("Python");
		skills2.add("NodeJS");

		Employee emp1 = new Employee("A", 2.0,  24, skills,                  "Consultancy");
		Employee emp2 = new Employee("B", 11.0, 32, Collections.emptyList(), "HR");
		Employee emp3 = new Employee("C", 8.0,  30, Collections.emptyList(), "Finance");
		Employee emp4 = new Employee("D", 1.0,  22, skills2,                 "Consultancy");
		Employee emp5 = new Employee("E", 20.0, 42, Collections.emptyList(), "Finance");

		List<Employee> employeeList = new ArrayList<>();
		employeeList.add(emp1);
		employeeList.add(emp2);
		employeeList.add(emp3);
		employeeList.add(emp4);
		employeeList.add(emp5);

		// ============================================================
		// OPERATION 1: Highest experience employee
		// ============================================================
		// stream().max(Comparator) → returns Optional<T>
		//   max() uses the Comparator to find the largest element
		//   Returns Optional because the stream could be empty
		//   ALWAYS use .orElseThrow() or .orElse() — never .get() blindly
		//
		// Comparator.comparing(Employee::getExperience)
		//   Method reference: Employee::getExperience = emp -> emp.getExperience()
		//   comparing() builds a Comparator that compares by the extracted value
		//
		// Time: O(n) — single pass
		Optional<Employee> highestEmp = employeeList.stream()
				.max(Comparator.comparing(Employee::getExperience));
		System.out.println("Highest Experience Employee: " + highestEmp.orElseThrow());

		// ============================================================
		// OPERATION 2: Average experience per department
		// ============================================================
		// groupingBy(classifier, downstream)
		//   classifier  = Employee::getDepartment → groups by dept name (key)
		//   downstream  = averagingDouble(...)    → reduces each group to avg
		//
		// averagingDouble(Employee::getExperience)
		//   = sum of experience / count of employees in that group
		//   Returns Double (boxed), not double
		//
		// Result: Map<String, Double>  e.g. {"Finance"=14.0, "HR"=11.0, "Consultancy"=1.5}
		//
		// INTERVIEW: "What if a department has zero employees?"
		//   groupingBy only creates entries for groups that EXIST in the stream.
		//   A dept with 0 employees simply won't appear in the result map.
		Map<String, Double> avgEmp = employeeList.stream()
				.collect(Collectors.groupingBy(
						Employee::getDepartment,              // key = department name
						Collectors.averagingDouble(Employee::getExperience)  // value = avg exp
				));
		System.out.println("Avg experience per department: " + avgEmp);

		// ============================================================
		// OPERATION 3: Second oldest employee per department
		// ============================================================
		// This is a NESTED COLLECTOR pattern — the hardest kind in interviews.
		//
		// STRUCTURE:
		//   groupingBy(dept)              → splits into groups per department
		//   └─ collectingAndThen(...)     → apply a finisher function to each group
		//      ├─ mapping(identity, toList) → collect group into a List<Employee>
		//      └─ finisher lambda          → sort desc by age, skip(1), findFirst()
		//
		// WHY collectingAndThen?
		//   groupingBy's downstream must be a Collector.
		//   But we need to post-process the group (sort + skip).
		//   collectingAndThen(collector, finisher) = first collect, then transform.
		//   It "wraps" any collector with a post-processing step.
		//
		// WHY mapping(Function.identity(), toList())?
		//   mapping(mapper, downstream) applies mapper to each element before collecting.
		//   Function.identity() = no transformation (just collect as-is into a List).
		//   This is equivalent to just Collectors.toList() here — the identity mapping
		//   is redundant but illustrates the mapping() pattern.
		//
		// SIMPLER EQUIVALENT (avoid mapping when no transformation needed):
		//   Collectors.collectingAndThen(
		//     Collectors.toList(),
		//     list -> list.stream().sorted(...).skip(1).findFirst()
		//   )
		//
		// skip(1): skip the 1st element (highest age) → 2nd element = second oldest
		// findFirst(): returns Optional<Employee> (empty if group has only 1 employee)
		//
		// Result type: Map<String, Optional<Employee>>
		Map<String, Optional<Employee>> depSecHighest = employeeList.stream()
				.collect(Collectors.groupingBy(
						Employee::getDepartment,
						Collectors.collectingAndThen(
								Collectors.mapping(
										Function.identity(),   // no transformation on each element
										Collectors.toList()    // collect group into List<Employee>
								),
								list -> list.stream()
										.sorted(Comparator.comparingInt(Employee::getAge).reversed()) // desc age
										.skip(1)        // skip the oldest → now at 2nd oldest
										.findFirst()    // return Optional<Employee>
						)
				));
		System.out.println("Second oldest per department: " + depSecHighest);
	}
}
