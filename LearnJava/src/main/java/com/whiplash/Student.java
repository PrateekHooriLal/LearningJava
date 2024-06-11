package com.whiplash;

import java.util.ArrayList;
import java.util.Collections;

public class Student implements Comparable<Student> {

	@Override
	public String toString() {
		return "Student [name=" + name + ", id=" + id + "]";
	}

	String name;
	int id;
	// int DOB;

	public Student(String a, int b) {
		this.name = a;
		this.id = b;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int compareTo(Student stu) {
		// TODO Auto-generated method stub
		return this.id - stu.id;
	}

	@SuppressWarnings("unchecked")
	public static void main(String... args) {
		System.out.println("This is comparable demo:");

		@SuppressWarnings("rawtypes")
		ArrayList ar = new ArrayList();

		ar.add(new Student("this", 5));
		ar.add(new Student("this", 45));
		ar.add(new Student("this", 54));
		ar.add(new Student("this", 59));

		Collections.sort(ar);

		System.out.println("Sorted list:" + ar.toString());

	}
}// class ends
