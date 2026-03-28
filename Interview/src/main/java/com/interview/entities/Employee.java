package com.interview.entities;

import java.util.Arrays;
import java.util.List;

public class Employee {
	private String name;
	private double experience;
	private double salary;
	private int age;
	private List<String> skills;
	private String department;

	public Employee(String name, double experience, double salary, int age, List<String> skills, String department) {
		this.name = name;
		this.experience = experience;
		this.salary = salary;
		this.age = age;
		this.skills = skills;
		this.department = department;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getExperience() {
		return experience;
	}

	public void setExperience(double experience) {
		this.experience = experience;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public List<String> getSkills() {
		return skills;
	}

	public void setSkills(List<String> skills) {
		this.skills = skills;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public double getSalary() {
		return salary;
	}

	public void setSalary(double salary) {
		this.salary = salary;
	}

	// Call Employee.getSampleEmployees() in any class to get test data
	public static List<Employee> getSampleEmployees() {
		return Arrays.asList(new Employee("Alice", 8.5, 75000, 30, Arrays.asList("Java", "Spring"), "Engineering"),
				new Employee("Bob", 3.0, 45000, 25, Arrays.asList("Python"), "Engineering"),
				new Employee("Charlie", 11.0, 90000, 35, Arrays.asList("Java", "AWS"), "Engineering"),
				new Employee("Diana", 6.5, 60000, 28, Arrays.asList("Marketing"), "Marketing"),
				new Employee("Eve", 2.0, 30000, 24, Arrays.asList("SEO"), "Marketing"),
				new Employee("Frank", 9.0, 80000, 32, Arrays.asList("Finance"), "Finance"),
				new Employee("Grace", 4.5, 55000, 29, Arrays.asList("Excel"), "Finance"));
	}

	// NOTE: hashCode not implemented — uses Object.hashCode() (reference-based).
	// equals() also uses Object default (reference equality).
	// Contract is satisfied (equal objects same hashCode) but two Employee objects
	// with same data will NOT be considered equal in HashMap/HashSet — by design
	// for this practice class. If needed, implement both equals + hashCode together.
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "Employee [name=" + name + ", experience=" + experience + ", salary=" + salary + ", age=" + age
				+ ", skills=" + skills + ", department=" + department + "]";
	}

}