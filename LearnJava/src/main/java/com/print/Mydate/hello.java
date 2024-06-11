/**
 * 
 */
package com.print.Mydate;

import java.util.StringTokenizer;

/**
 * @author Prateek & Ritik
 *
 */
public class hello {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("THAT'S fine ");

		String s = "this is the day that the lord has made";
		System.out.println(s);
		StringTokenizer st = new StringTokenizer(s, " ");
		System.out.println(st.countTokens());

	}

}
