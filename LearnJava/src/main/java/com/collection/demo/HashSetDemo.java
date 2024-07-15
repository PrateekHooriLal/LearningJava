package com.collection.demo;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class HashSetDemo {

	static Set<JohnDoe> hashSet = new HashSet<JohnDoe>();

	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();
		// for(int i =0; i<10; i++){

		hashSet.add(new JohnDoe("jojo", 1));
		hashSet.add(new JohnDoe("jojo", 1));
		hashSet.add(new JohnDoe("balu", 2));

		System.out.println(hashSet.size());
		System.out.println(hashSet);
		// }
		long endtTime = System.currentTimeMillis();

		System.out.println("time execution=" + (endtTime - startTime));
	}

}

class JohnDoe {

	private String name;
	private int id;

	public JohnDoe(String name, int id) {
		super();
		this.name = name;
		this.id = id;
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
	public String toString() {
		return " [name=" + name + ", id=" + id + "]";
	}
////////////////////////////////////////////////////////////////

	@Override
	public int hashCode() {
		return 22;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JohnDoe other = (JohnDoe) obj;
		return id == other.id && Objects.equals(name, other.name);
	}

	

}