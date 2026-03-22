package com.lambda.demo;

import java.util.List;

public class Employee {
	private String name;
	private double experience;
	private int age;
	private List<String> skills;
	private String department;

	public Employee(String name, double experience, int age, List<String> skills, String department) {
		this.name = name;
		this.experience = experience;
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

	@Override
	public boolean equals(Object obj) {
		// BUG FIX: hashCode was overridden without equals — violates Java contract:
		// "objects that are equal must have the same hashCode"
		// Without equals(), two Employee objects with same name/dept would not be
		// considered equal in HashMap/HashSet even though hashCode might match.
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Employee other = (Employee) obj;
		return age == other.age
				&& Double.compare(experience, other.experience) == 0
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(department, other.department);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(name, experience, age, department);
	}

	@Override
	public String toString() {
		return "Employee [name=" + name + ", experience=" + experience + ", age=" + age + ", skills=" + skills
				+ ", department=" + department + "]";
	}
}