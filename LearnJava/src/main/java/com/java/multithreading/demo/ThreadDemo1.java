package com.java.multithreading.demo;

/*Generally we are using these examples between Main Thread and child thread*/

public class ThreadDemo1 {

	static class ThreadDem2 {
		int a = 22;
	}// inner classes can be static.

	public static ThreadDemo1 tm;

	public ThreadDemo1() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws InterruptedException {

		tm = new ThreadDemo1();
		// eg of setname() method
		Mythread t1 = new Mythread();
		t1.setName("ONE");
		tm = new ThreadDemo1();
		/*
		 * Mythread t2 = new Mythread(); t2.setName("TWO"); Mythread t3 = new
		 * Mythread(); t3.setName("THREE");
		 */
		t1.start();
		// t2.start();
		// t3.start();

		// starting t1 thread
		// t1.setPriority(10);

		// Mythread t2 = new Mythread();
		// t2.setName("TWO");
		// t2.setPriority(1);

		/* ===================================================================== */

		// Eg for public static final native yield() method

		Thread.yield();
		/*
		 * if we encounter yield here, main thread will pause Until child thread (i.e
		 * t1) finishes execution.
		 */

		t1.join();
		/*
		 * if we encounter join here, main thread will wait(will go to Wait state) Until
		 * child thread (i.e t1) finishes execution.
		 */

		// Thread.sleep(1);
		// some task for main method
		for (int i = 0; i < 10; i++) {
			System.out.println("MAIN THREAD" + ":Priority=" + Thread.currentThread().getPriority());
		}

		// t1.start();// Starting both threads.
		// t2.start();

		/*
		 * while (i < 3000) {
		 * 
		 * System.out.println("Current thread name:" +
		 * Thread.currentThread().getName()); i++; }// while ends
		 */}// main ends
}// class ends
