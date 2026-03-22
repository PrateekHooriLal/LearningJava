package com.java.multithreading.demo;

public class MainThread {

	public static void main(String[] args) throws InterruptedException {

		// Spawning Thread by passing object of class implementing Runnable Interface
		PrintNumber obj = new PrintNumber(1, 100);
		Thread even = new Thread(obj, "Even");
		Thread odd = new Thread(obj, "Odd");
		even.start();
		odd.start();

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
