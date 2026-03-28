package com.interview.altimetrik;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.interview.entities.Employee;

/**
 * Altimetrik / Airbnb L1 Interview — Stream Problems Notes:
 * Interview/notes/AltimetrikInterviewNotes.md
 */
public class AltimetrikStreams {

	public static void main(String[] args) {

		System.out.println("=== Problem 1: Filter + GroupBy + MaxBy ===");
		problem1_filterGroupByMax();

		System.out.println("\n=== Problem 2: Common Characters Across All Words ===");
		problem2_commonCharacters();

		System.out.println("\n=== Problem 3: Character Frequency ===");
		problem3_charFrequency();

		System.out.println("\n=== Problem 4: GroupBy + Count + Sort ===");
		problem4_groupByCountSort();
	}

	// Problem 1 — filter + groupingBy + maxBy
	// Q: Filter salary > 50k, group by dept, find highest paid per dept
	// Return type: Map<String, Optional<Employee>>
	// Optional because maxBy protects against empty group after filter
	static void problem1_filterGroupByMax() {

		List<Employee> employees = Employee.getSampleEmployees();

		Map<String, Optional<Employee>> result = employees.stream().filter(e -> e.getSalary() > 50_000)
				.collect(Collectors.groupingBy(Employee::getDepartment,
						Collectors.maxBy(Comparator.comparing(Employee::getSalary))));

		result.forEach((dept, empOpt) -> empOpt
				.ifPresent(emp -> System.out.println(dept + " -> " + emp.getName() + " | " + emp.getSalary())));

		// Follow-up: get just the salary value (unwrap Optional using
		// collectingAndThen)
		Map<String, Double> maxSalaryPerDept = employees.stream().filter(e -> e.getSalary() > 50_000)
				.collect(Collectors.groupingBy(Employee::getDepartment,
						Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparing(Employee::getSalary)),
								opt -> opt.map(Employee::getSalary).orElse(0.0))));
		System.out.println("Max salaries: " + maxSalaryPerDept);
	}

	// Problem 2 — Common characters across all words (with duplicates)
	// Input: ["bella", "label", "roller"] -> Output: [e, l, l]
	// Algorithm: frequency array per word -> take MIN across all words -> expand to
	// list
	// Time: O(n*m) Space: O(1) — array always size 26
	static void problem2_commonCharacters() {

		String[] words = { "bella", "label", "roller" };

		int[] minFreq = getFrequency(words[0]);

		for (int i = 1; i < words.length; i++) {
			int[] wordFreq = getFrequency(words[i]);
			for (int j = 0; j < 26; j++) {
				minFreq[j] = Math.min(minFreq[j], wordFreq[j]);
			}
		}

		List<String> result = new ArrayList<>();
		for (int i = 0; i < 26; i++) {
			for (int count = 0; count < minFreq[i]; count++) {
				result.add(String.valueOf((char) ('a' + i)));
			}
		}

		System.out.println("Common characters: " + result); // [e, l, l]
	}

	private static int[] getFrequency(String word) {
		int[] freq = new int[26];
		for (char c : word.toCharArray())
			freq[c - 'a']++;
		return freq;
	}

	// Problem 3 — Character frequency count
	// Input: "aabbccsrrtvvaa" Output: a=4, b=2, c=2, s=1, r=2, t=1, v=2
	static void problem3_charFrequency() {

		String input = "aabbccsrrtvvaa";

		// Approach 1: Streams (show this first in interview)
		Map<String, Long> freqMap = Arrays.stream(input.split(""))
				.collect(Collectors.groupingBy(c -> c, Collectors.counting()));
		new TreeMap<>(freqMap).forEach((ch, count) -> System.out.print(ch + "=" + count + " "));
		System.out.println();

		// Approach 2: Array — O(1) space, faster (mention as optimisation)
		int[] freq = new int[26];
		for (char c : input.toCharArray())
			freq[c - 'a']++;
		for (int i = 0; i < 26; i++) {
			if (freq[i] > 0)
				System.out.print((char) ('a' + i) + "=" + freq[i] + " ");
		}
		System.out.println();
	}

	// Problem 4 — GroupBy + Count + Sort descending
	// Tests: counting() downstream, sorting Map by value, reversed()
	static void problem4_groupByCountSort() {

		List<Employee> employees = Employee.getSampleEmployees();

		employees.stream().collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting())).entrySet()
				.stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.forEach(e -> System.out.println(e.getKey() + ": " + e.getValue() + " employees"));
	}
}
