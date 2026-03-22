package com.stream.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ============================================================
 * CONCEPT: Stream Creation + Sorting + Finding Max Element
 * ============================================================
 * TOPICS COVERED:
 *   1. Stream creation from array and List
 *   2. Natural sort via Comparable (Arrays.sort, Collections.sort)
 *   3. Custom sort via Comparator (stream().sorted())
 *   4. Finding max/min with Comparator
 *   5. Optional — safe container for nullable results
 *
 * HOW TO CREATE A STREAM (4 ways):
 *   1. Stream.of(array)          → from an array
 *   2. list.stream()             → from a Collection
 *   3. Arrays.stream(array)      → from array (can specify range)
 *   4. Stream.of(a, b, c)        → from individual elements (varargs)
 *
 * INTERVIEW ANGLE:
 *   "How do you sort a list of objects by salary descending?"
 *   "How do you find the employee with the maximum salary?"
 *   These are standard warm-up questions at any Java interview.
 *
 * SORT METHODS — KNOW ALL THREE:
 *   Arrays.sort(arr)             → sorts array in-place, uses compareTo (Comparable)
 *   Collections.sort(list)       → sorts list in-place, uses compareTo (Comparable)
 *   list.stream().sorted(comp)   → returns NEW sorted stream (non-destructive)
 * ============================================================
 */
public class StreamCreation {

	// Static array of Employee objects — used to demonstrate array → stream conversion
	public static Employee[] arrayOfEmps = {
			new Employee(1,  "Jeff Bezos",       300000.0, "HR"),
			new Employee(2,  "Bill Gates",        200000.0, "Sales"),
			new Employee(3,  "Mark Zuckerberg",   300000.0, "DEV"),
			new Employee(10, "George",              10000,  "HR"),
			new Employee(12, "Robert",              15000,  "DEV"),
			new Employee(24, "Kathy",               25000,  "Sales"),
			new Employee(5,  "Robert",                140,  "HR"),
			new Employee(-1, "Robert",                140,  "HR")
	};

	// Arrays.asList() wraps the array — FIXED SIZE list (cannot add/remove)
	// Backed by the array — modifying list modifies array and vice versa
	// Use new ArrayList<>(Arrays.asList(arr)) if you need a resizable list
	public static List<Employee> emplist = Arrays.asList(arrayOfEmps);

	public static void main(String[] args) {

		// ---- Natural Sort (uses Employee.compareTo = sort by salary ascending) ----
		// Arrays.sort() uses TimSort internally (hybrid of merge + insertion sort)
		// Requires Employee implements Comparable<Employee>
		Arrays.sort(arrayOfEmps);                // sorts the array in-place
		Collections.sort(emplist);               // sorts the list in-place (same order)

		System.out.println("Sorted ascending by salary:");
		emplist.forEach(e -> { e.printEmp(); System.out.println(); });

		// ---- Custom Sort: ascending salary via Stream ----
		// stream().sorted(comparator) → returns a NEW sorted stream, original list unchanged
		// Comparator.comparing(Employee::getSalary) builds a Comparator from a key extractor
		System.out.println("\nStream sorted ascending by salary:");
		sortEmpSalaryAesc(emplist.stream(), "Salary").forEach(e -> { e.printEmp(); System.out.println(); });

		// ---- Custom Sort: descending by name via Stream ----
		// Comparator.comparing(Employee::getName) → alphabetical ascending
		// .reversed()                             → reverse to Z→A descending
		System.out.println("\nStream sorted descending by name:");
		sortEmpSalaryDesc(emplist.stream()).forEach(e -> { e.printEmp(); System.out.println(); });

		// ---- Find Max Salary ----
		// stream().max(comparator) → returns Optional<T>
		// Optional because if stream is empty, there is no max → returns Optional.empty()
		// NEVER call .get() directly — use .orElseThrow() or .orElse(default)
		System.out.print("\nEmployee with max salary: ");
		findMaxSalary(emplist).ifPresent(Employee::printEmp);
		System.out.println();
	}

	// ============================================================
	// Sort ascending by natural order (uses compareTo from Comparable)
	// ============================================================
	// stream.sorted() with NO argument → uses Comparable.compareTo()
	// Only works if T implements Comparable — otherwise ClassCastException
	// sortBy parameter is unused here (placeholder for extension)
	//
	// toList() (Java 16+) → returns unmodifiable List
	// collect(Collectors.toList()) → returns modifiable ArrayList
	public static List<Employee> sortEmpSalaryAesc(Stream<Employee> s, String sortBy) {
		return s.sorted().toList();  // sorted() uses Employee.compareTo (salary ascending)
	}

	// ============================================================
	// Sort descending by name — uses custom Comparator
	// ============================================================
	// Comparator.comparing(Employee::getName)
	//   = Comparator that compares employees by their name (String natural order A→Z)
	// No .reversed() here → this is ascending by name (A→Z)
	// To get descending: .sorted(Comparator.comparing(Employee::getName).reversed())
	//
	// collect(Collectors.toList()) → mutable ArrayList (pre-Java 16 style)
	public static List<Employee> sortEmpSalaryDesc(Stream<Employee> s) {
		return s.sorted(Comparator.comparing(Employee::getName))
				.collect(Collectors.toList());
	}

	// ============================================================
	// Find employee with maximum salary
	// ============================================================
	// stream().max(comparator) → terminal operation, O(n) single pass
	// Returns Optional<Employee> — handles empty list safely
	//
	// Comparator.comparing(Employee::getSalary) compares by salary value
	// max() returns the element for which comparator says is "greatest"
	//
	// Optional usage patterns:
	//   optional.get()              → UNSAFE, throws NoSuchElementException if empty
	//   optional.orElseThrow()      → throws, but more explicit (Java 10+)
	//   optional.orElse(default)    → returns default if empty
	//   optional.ifPresent(action)  → runs action only if value exists (no NPE risk)
	public static Optional<Employee> findMaxSalary(List<Employee> empList) {
		// Use the parameter empList, NOT the class field emplist
		return empList.stream().max(Comparator.comparing(Employee::getSalary));
	}
}
