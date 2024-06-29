package com.java.multithreading.demo;

public class PrintNumber implements Runnable {

	private int i;
	private int max;
	static Object lock;

	PrintNumber(int i, int max) {
		this.i = i;
		this.max = max;
		PrintNumber.lock = new Object();
	}

	@Override
	public void run() {

		while (i <= max) {
			try {
				/*
				 * Not good Approach as we are relying on the Thread Name to determine code flow
				 */
				if (i % 2 == 0 && Thread.currentThread().getName().equals("Even")) {
					synchronized (lock) {
						System.out.println(Thread.currentThread().getName() + ":: " + i++);
						lock.wait();
					}
				}
				Thread.sleep(100);
				if (i % 2 != 0 && Thread.currentThread().getName().equals("Odd")) {
					synchronized (lock) {
						System.out.println(Thread.currentThread().getName() + " :: " + i++);
						lock.notify();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
