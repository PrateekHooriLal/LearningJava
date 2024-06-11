package com.lambda.demo;

import java.util.function.Predicate;

public class LearnLambda {

	public static void main(String[] args) {

		// calling static Lambda expression
		LearnLambda.sl.iamabstract();
		sl.perform();

		// calling non static lambda Expression
		LearnLambda ll = new LearnLambda();
		ll.nsl.iamabstract();
		ll.nsl.perform();

		System.out.println(ll.fs.findSum(1, 3));
	}

	// static Lambda expression
	static how sl = () -> {
		System.out.println("Testing static Lamda expression");
	};

	// non static Lambda expression
	how nsl = () -> System.out.println("Testing Non static Lamda expression");

	who fs = (a, b) -> a + b;

//Predicate<T>

}
