package com.string.demo;

public class FirstUniqueElemet {

	public static void main(String[] args) {
		// TODO Auto-generated method stu

		String s = "iloveleetcodei";
		// 256 slots covers EVERY standard and extended ASCII character
		int[] freq = new int[256];
		int len = s.length();

		// Pass 1: Count frequencies using raw character values as indices
		for (int i = 0; i < len; i++) {
			freq[s.charAt(i)]++;
		}

		// Pass 2: Find the first unique character
		for (int i = 0; i < len; i++) {
			if (freq[s.charAt(i)] == 1) {
				System.out.println(i);
				break;//just to print first unique character 
			}
		}

		System.out.println("None");
	}
	
	public static int findFirstUniqueChar(String s) {
		
		int [] freq = new int[256]
		
	}
}
