package com.abstraction.demo;

/**
 * @author Prateek
 *
 */
/*
 * Using "abstract" we can define a virtual entity in programming for eg:
 * Animal, vehicle etc, you can find more examples at beloe link:
 * http://javaconceptoftheday.com/abstraction-in-java/
 */
/*
 * We can�t create objects to those classes which are declared as abstract. But,
 * we can create objects to sub classes of abstract class, provided they must
 * implement abstract methods.
 */

///////////////////some points to keep in mind:////////////////////////////
/*
 * 1: Abstract Class can be a combination of concrete and abstract methods.
 * 
 * 2:It is not compulsory that abstract class must have abstract methods. It may
 * or may not have abstract methods. But the class which has at least one
 * abstract method must be declared as abstract.
 * 
 * 3:You can�t create objects to abstract class even though it does not contain
 * any abstract methods.
 * 
 * 4:Any class extending an abstract class must implement all abstract methods.
 * If it does not implement, it must be declared as abstract.
 * 
 * 5:Abstract methods can not be private. Because, abstract methods must be
 * implemented somehow in the sub classes. If you declare them as private, then
 * you can�t use them outside the class.
 * 
 * 6:Constructors and fields can not be declared as abstract.
 * 
 * 7: Abstract methods can not be static.
 */
///////////////////////////////////////////////////////////////////////////
public abstract class DemoAbstract {

	public abstract void abst_method1();// declaring a method as unimplemented
										// by using "abstract" keyword.

	public abstract void abst_method2();

	public void concrete_method1() {
		System.out.println("Concrete method defined in abstract class");
	}

}// class ends
