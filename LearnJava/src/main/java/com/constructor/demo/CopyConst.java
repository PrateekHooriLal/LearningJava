package com.constructor.demo;

/**
 * @author Prateek
 *
 */

/*
 * Some pointers: 1:Java doesn't create a default copy constructor if you don�t
 * write your own.
 *
 */
public class CopyConst {
	private double re, im;

	public CopyConst(double re, double im) {
		this.re = re;
		this.im = im;
	}

	public CopyConst(CopyConst c1) {
		// TODO Auto-generated constructor stub
		System.out.println("Copy constructor called");
		re = c1.re;
		im = c1.im;
	}

	@Override
	public String toString() {
		return "CopyConst [re=" + re + ", im=" + im + "]";
	}

	public static void main(String[] args) {
		CopyConst c1 = new CopyConst(10, 15);
		CopyConst c2 = new CopyConst(c1); // compiler error here
		System.out.println("C2=" + c2);
	}

}// class ends
