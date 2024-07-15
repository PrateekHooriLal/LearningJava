package com.java.multithreading.demo;

class PrintEvenOdd implements Runnable {
	private final int maxNum;
	private final int remainder;

	private static int num;
	static Object lock;

	public PrintEvenOdd(int num, int maxNum, int remainder) {
		this.maxNum = maxNum;
		this.remainder = remainder;
		PrintEvenOdd.lock = new Object();
		PrintEvenOdd.num = num;

	}

	@Override
	public void run() {

		while (num < maxNum) {
			synchronized (lock) {
				// System.out.println(Thread.currentThread().getName() + " Num=" + num + "
				// CalRemainder=" + num % 2+ "::Remainder:" + remainder);

				if (num % 2 != remainder) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (num % 2 == remainder) {
					System.out.println(Thread.currentThread().getName() + "::" + num++);
					lock.notify();
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

	}

	public static void main(String[] args) {
		Thread t1 = new Thread(new PrintEvenOdd(1, 100, 0), "Even");
		Thread t2 = new Thread(new PrintEvenOdd(1, 100, 1), "Odd ");
		t1.start();
		t2.start();
		
	}
}
