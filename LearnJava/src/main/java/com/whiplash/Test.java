package com.whiplash;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class Test {
	public static void main(String[] args) {
		int result = 0;
		try {
			Student si = new Student("Abhi", 104);
			FileOutputStream fos = new FileOutputStream("student.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(si);
			oos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String a = new Test().recstring("MANHATTAN dssm sda1w@#@#").toString();
		System.out.println("hi" + a);

		String str = "helloslkhellodjladfjhellod";
		String findStr = "hellosd";
		System.out.println(str.split(findStr, -1).length - 1);

		System.out.println(result / 6);
		System.out.println("23 =" + ~23);
		/*
		 * String[] a =str.split(findStr,-1); for(String st: a)
		 * System.out.println(a.length+":"+st);
		 */

	}

	public String recstring(String s) {
		String rev = "";
		int len = s.length();
		if (s.length() <= 1)
			return s;

		rev += s.charAt(len - 1) + recstring(s.substring(0, len - 1));

		double num[] = { 5.5, 10.1, 11, 12.8, 56.9, 2.5 };
		double result;
		result = 0;
		for (int i = 0; i < 6; ++i)
			result = result + num[i];

		return rev;
	}
}
