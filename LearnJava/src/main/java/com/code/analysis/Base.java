package com.code.analysis;

public class Base {
	static {
		System.out.println("Base static block");
	}
	{
		System.out.println("Base instance block");
	}

	public Base() {
		System.out.println("Base constructor");
	}
}
