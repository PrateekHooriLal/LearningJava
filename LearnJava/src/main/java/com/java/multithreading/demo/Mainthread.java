package com.java.multithreading.demo;

public class Mainthread {

	public static void main(String[] args) throws InterruptedException {

		PrintNumber obj = new PrintNumber(1, 50);
		Thread t1 = new Thread(obj, "Even");
		t1.start();

		// Spawning Thread by passing object of class implementing Runnable Interface
		Thread t2 = new Thread(obj, "Odd");
		t2.start();

		// creating thread using lambda expression
		Thread t3 = new Thread(() -> {
			for (int i = 0; i < 10; i++) {
				System.out.println(Thread.currentThread().getName() + "=T3 : " + i);
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		// t3.start();

	}// man ends
}// class ends
