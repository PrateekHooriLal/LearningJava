package com.interface_prateek.demo;

public interface demo {
	void myMethod();
	default void myDefaultMethod() {
		System.out.println(" Hey I am Default from Interface Deeemooo");
	}
}

class A implements demo {
	public void myMethod() {
		System.out.println("My Method A");
	}
	public static void main(String [] arg) {
		demo d = new demo() {

			@Override
			public void myMethod() {
				// TODO Auto-generated method stub
				
			}
		};
		d.myDefaultMethod();
	}
}

class B extends A {
	public void myMethod() {
		System.out.println("My Method B");
	}
}

class C extends B {

}
