package com.interview.prep;

public class StringDemos {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		StringBuffer str = new StringBuffer("string_buffer");
		StringBuilder str1 = new StringBuilder("string_builder");
		String s = "rumaali";
		String s1 = "rumaali";
		String s3 = new String("rumaali");
		String s4 = s3.substring(0);

		System.out.println(str.reverse());
		System.out.println(str1.reverse());
		System.out.println("s == s1 :: " + (s == s3));
		System.out.println(s4);

	}

}
