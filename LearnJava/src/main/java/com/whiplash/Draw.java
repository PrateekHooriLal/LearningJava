package com.whiplash;

public abstract class Draw {

	public void draw() {
		System.out.println("drawing");
	}
}

class A extends Draw {
	Draw a = new A();

	// a.draw(); why it is showing error here but not inside the method

	public void dothis() {
		a.draw();// why not showing error here.

	}// method end's

}// class ends