package com.constructor.demo;

public class ConstChain {
	int i, j;

	ConstChain() {
		System.out.println("ConstChain()");
	}

	ConstChain(int i) {
		this();// calling default constructor .
		this.i = i;
		System.out.println("ConstChain(int i)");
	}

	ConstChain(int i, int j) {
		this(i);// calling the single parameter of the current object i.e, "ch" of type
				// ConstChain right now.
		// this(j);=> will give "Constructor call must be the first statement in a
		// constructor" error.

		this.j = j;
		System.out.println("ConstChain(int i,int j)");

	}

	public String toString() {
		return "i=" + this.i + "j=" + this.j;

	}

	public static void main(String[] args) {
		ConstChain ch = new ConstChain(10, 20);
		System.out.println(ch.toString());
		A a = new A();

	}
}

class A extends ConstChain {

	A() {
		System.out.println("sub");
	}

}