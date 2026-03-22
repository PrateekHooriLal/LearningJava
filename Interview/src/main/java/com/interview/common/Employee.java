package com.interview.common;

public class Employee implements Comparable<Employee> {

	private int id;
	private String name;
	private double salary;
	private String department;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getSalary() {
		return salary;
	}

	public void setSalary(int salary) {
		this.salary = salary;
	}

	public Employee(int id, String name, double d, String department) {
		super();
		this.id = id;
		this.name = name;
		this.salary = d;
		this.department = department;
	}

	public void printEmp() {
		System.out.print("id=" + id + "  name=" + name + "  Salary=" + salary+"Department="+department+"==> \t");
	}

	@Override
	public int compareTo(Employee o) {
		// TODO Auto-generated method stub
		return (int) (this.getSalary() - o.getSalary());

	}

	

}
