package com.stream.api;

/**
 * ============================================================
 * CONCEPT: Comparable Interface — Natural Ordering for Objects
 * ============================================================
 * WHAT IS Comparable<T>?
 *   An interface with ONE method: int compareTo(T other)
 *   Defines the "natural order" of a class — used by:
 *     - Arrays.sort(array)
 *     - Collections.sort(list)
 *     - TreeSet / TreeMap (sorted collections)
 *     - Stream.sorted() with no argument
 *
 * compareTo() CONTRACT (must satisfy all three):
 *   1. Reflexive:    x.compareTo(x) == 0
 *   2. Antisymmetric: x.compareTo(y) > 0 → y.compareTo(x) < 0
 *   3. Transitive:  x > y and y > z → x > z
 *
 * RETURN VALUE MEANING:
 *   negative → this < other  (this comes BEFORE other when sorted ascending)
 *   zero     → this == other (equal ordering)
 *   positive → this > other  (this comes AFTER other)
 *
 * Comparable vs Comparator:
 *   Comparable = built into the class, defines ONE natural order
 *   Comparator = external, can define MULTIPLE different orderings
 *   Use Comparable when there's one obvious order (e.g., salary for Employee)
 *   Use Comparator for alternate orders (e.g., sort by name, then by dept)
 *
 * INTERVIEW FOLLOW-UPS:
 *   Q: What happens if you don't implement Comparable but call Collections.sort()?
 *   A: ClassCastException at runtime — the sort tries to cast to Comparable
 *
 *   Q: Can a class implement both Comparable and have Comparators?
 *   A: Yes. Comparable = default sort. Comparator = custom/alternate sorts.
 *      e.g., empList.sort(Comparator.comparing(Employee::getName))
 *
 *   Q: Why use Double.compare() instead of (this.salary - other.salary)?
 *   A: Subtraction can lose precision for doubles close in value.
 *      e.g., 200000.5 - 200000.1 = 0.4 → (int)0.4 = 0 → "equal" (WRONG!)
 *      Double.compare() handles NaN, -infinity, +infinity correctly too.
 * ============================================================
 */
public class Employee implements Comparable<Employee> {

	private int id;
	private String name;
	private double salary;   // double field — be careful with compareTo
	private String department;

	// ---- Constructor ----
	public Employee(int id, String name, double salary, String department) {
		this.id = id;
		this.name = name;
		this.salary = salary;
		this.department = department;
	}

	// ---- Getters ----
	public int getId()            { return id; }
	public String getName()       { return name; }
	public double getSalary()     { return salary; }
	public String getDepartment() { return department; }

	// ---- Setters ----
	public void setId(int id)                   { this.id = id; }
	public void setName(String name)            { this.name = name; }
	public void setSalary(double salary)        { this.salary = salary; } // double, not int
	public void setDepartment(String dept)      { this.department = dept; }

	// ============================================================
	// compareTo — defines natural ordering BY SALARY (ascending)
	// ============================================================
	// After sorting: lowest salary first, highest salary last
	// Arrays.sort() and Collections.sort() use this automatically
	//
	// Double.compare(a, b) internally:
	//   if a < b  → returns -1
	//   if a == b → returns  0
	//   if a > b  → returns +1
	//   Also handles Double.NaN correctly (subtraction doesn't)
	//
	// GOTCHA: Never do (int)(this.salary - other.salary)
	//   For salaries like 140.7 and 140.3: difference = 0.4 → (int)0.4 = 0 → "equal" WRONG
	@Override
	public int compareTo(Employee other) {
		// Sort ascending by salary (lowest → highest)
		return Double.compare(this.salary, other.salary);

		// To sort DESCENDING, reverse: Double.compare(other.salary, this.salary)
		// Or wrap: Comparator.comparing(Employee::getSalary).reversed()
	}

	// ---- Utility: print employee details ----
	public void printEmp() {
		System.out.print("id=" + id + "  name=" + name
				+ "  salary=" + salary + "  dept=" + department + " | ");
	}

	@Override
	public String toString() {
		return "Employee{id=" + id + ", name='" + name + "', salary=" + salary
				+ ", dept='" + department + "'}";
	}
}
