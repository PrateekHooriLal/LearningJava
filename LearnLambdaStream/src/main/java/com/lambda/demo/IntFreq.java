package com.lambda.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IntFreq {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<Integer> numList = new ArrayList<Integer>();
		numList.add(1);
		numList.add(2);
		numList.add(2);
		numList.add(3);
		numList.add(3);
		numList.add(5);
		numList.add(4);
		System.out.println(numList.toString());

		System.out.println("FRequency=" + getFreq(numList));
		System.out.println("FRequency=" + getFreqStream(numList));

	}

	public static Map<Integer, Integer> getFreq(ArrayList<Integer> list) {

		Map<Integer, Integer> frequency = new HashMap<Integer, Integer>();

		for (int i : list) {

			if (!frequency.containsKey(i)) {
				frequency.put(i, 1);
			} else {
				frequency.put(i, frequency.get(i) + 1);
			}
		}
		return frequency;
	}

	public static Map<Integer, Long> getFreqStream(ArrayList<Integer> list) {
		return list.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

	}

}