package com.abstraction.demo;

public class concrete extends DemoAbstract {

	public static void main(String[] args) {

		// creating object.
		concrete obj = new concrete();
		obj.abst_method1();
		obj.concrete_method1();
		obj.abst_method1();
	}

	@Override
	public void abst_method1() {
		// TODO Auto-generated method stub
		System.out.println("Abstract metho defined in child class");
	}

	@Override
	public void abst_method2() {
		// TODO Auto-generated method stub
		System.out.println("Abstract method 2 defined in child class");
	}

}// class ends
