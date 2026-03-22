package com.interview.accolite;

public class Accolite_StreamTest implements Runnable {
// method printing numbers 1-10
// t1 - event no
// t2 odd no 
	public static void main(String[] args) {

		var CurrentThread = Thread.currentThread();
	}

	public void printNumber(int n) throws InterruptedException {

		for (int i = 1; i <= n; i++) {
			if (i % 2 == 0) {
				System.out.println(i);
				Thread thread2 = new Thread();
				thread2.wait();
			} else {
				System.out.println(i);
				Thread thread = new Thread();
				thread.notifyAll();
			}

		}
	}

	@Override
	public void run() {
		Accolite_Test obj = new Accolite_Test();
		try {
			obj.printNumber(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
