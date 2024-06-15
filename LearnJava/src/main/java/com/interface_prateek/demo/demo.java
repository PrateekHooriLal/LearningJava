package com.interface_prateek.demo;

public interface demo {
	void myMethod();
}

class A {
	public void myMethod() {
		System.out.println("My Method A");
	}
}

class B extends A {
	public void myMethod() {
		System.out.println("My Method B");
	}
}

class C extends B {

}
