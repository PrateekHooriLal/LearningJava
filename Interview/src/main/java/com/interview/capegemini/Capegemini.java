package com.interview.capegemini;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

//1 yoj

//2 name

//3: Take a string find first non repeated character using java 8

class Employee1 implements Comparator {

	int id;
	String name;
	int salary;
	int yearOfJoining;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSalary() {
		return salary;
	}

	public void setSalary(int salary) {
		this.salary = salary;
	}

	public int getYearOfJoining() {
		return yearOfJoining;
	}

	public void setYearOfJoining(int yearOfJoining) {
		this.yearOfJoining = yearOfJoining;
	}

	@Override
	public int compare(Object o1, Object o2) {

		return 0;
	}

}

public class Capegemini {

	/// list of emp
	public static void main(String... args) {
		List<Employee1> list = new ArrayList();
		String str = "abcjccaz";// b
		/*
		 * LinkedHashMap<Character, Long> fmap = str.chars().mapToObj(c -> (char) c)
		 * .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new,
		 * Collectors.counting())); map.entrySet().stream().filter(n -> n
		 * ==1).findFirst().map(fmap.en));
		 */
	}
}

////Employeed
//select * from Employee where max(salary).
