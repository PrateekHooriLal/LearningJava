package com.print.Mydate;

public class Mainthread {

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		Thread t1 = Thread.currentThread();
		System.out.println("Current thread=" + t1);

		t1.setName("My thread");
		System.out.println(t1.getName());
		Thread.sleep(5000);
		System.out.println("thread after name change:" + t1);

	}// man ends
}// class ends
