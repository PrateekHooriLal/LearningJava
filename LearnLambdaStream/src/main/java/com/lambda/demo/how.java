package com.lambda.demo;

@FunctionalInterface
public interface how {
	// function interface should have only single abstract method (SAM).
	void iamabstract();

	// concrete methods can be now written inside interface using "default" keyword.
	default public void perform() {
		System.out.println("Inside how interface's default method");
	}

}
