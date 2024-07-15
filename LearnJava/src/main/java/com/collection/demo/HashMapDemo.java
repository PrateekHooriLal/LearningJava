package com.collection.demo;

import java.util.HashMap;
import java.util.Map;

public class HashMapDemo {

	public static void main(String args[]) {
		Map<String, JohnDoe> map = new HashMap<String, JohnDoe>();

		map.put("jojo", new JohnDoe("wq", 1));
		map.put("jojo", new JohnDoe("as", 2));
		map.put("jojo", new JohnDoe("ee", 3));
		map.put("balu", new JohnDoe("balu", 2));
		System.out.println("Size=" + map.size());
		System.out.println(map);
		System.out.println("Get= "+map.get("jojo"));
	}

}// class
