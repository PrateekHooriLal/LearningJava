package com.interface_prateek.demo;

public interface demo1

{
	void myMethod();
}

class B {
	public void myMethod() {
		System.out.println("My Method");
	}
}

class C extends B implements demo1 {

}
