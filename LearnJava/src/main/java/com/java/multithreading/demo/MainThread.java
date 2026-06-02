package com.java.multithreading.demo;

public class MainThread {

	public static void main(String[] args) throws InterruptedException {

		// Spawning Thread by passing object of class implementing Runnable Interface
		PrintNumber obj = new PrintNumber(1, 100);
		Thread even = new Thread(obj, "Even");
		Thread odd = new Thread(obj, "Odd");
		//even.start();
		//odd.start();
		
		PrintEvenOdd obj2 =  new PrintEvenOdd(1, 30, 0);
		Thread printer = new Thread(obj2);
		printer.start();

		// creating thread using lambda expression
		Thread t3 = new Thread(() -> {
			for (int i = 0; i <= 10; i++) {
				System.out.println(Thread.currentThread().getName() + "=T3 : " + i);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		//System.out.println(Thread.currentThread());
		});
		 t3.start();
		

	}// man ends
}// class ends
