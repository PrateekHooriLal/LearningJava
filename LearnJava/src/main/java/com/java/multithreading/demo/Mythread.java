package com.java.multithreading.demo;

import com.java.multithreading.demo.ThreadDemo1.ThreadDem2;

public class Mythread extends Thread {

	int a = new ThreadDem2().a;

	public Mythread() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {

		/*
		 * eg to get name of the current running Thread.
		 * System.out.println("Here we have implemented run method" +
		 * Thread.currentThread().getName()); for (int i = 0; i < 100; i++)
		 * System.out.println("Child Thread" + Thread.currentThread().getName());
		 */

		// /////////////////////////////////////////////////////////////////////////

		// Example 2:
		for (int i = 0; i < 5; i++) {
			System.out.println("Child Thread" + Thread.currentThread().getName() + ":Priority="
					+ Thread.currentThread().getPriority());
		} // for loop ends

	}// run ends
}// class ends
