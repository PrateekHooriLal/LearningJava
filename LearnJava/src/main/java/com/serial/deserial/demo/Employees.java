/**
 *@author Prateek & Ritik
 *
 */
package com.serial.deserial.demo;

import java.io.Serializable;

public class Employees implements Serializable {

	private static final long serialVersionUID = 1L;
	// declaring data members
	final int empid;
	transient String name;
	Double Basic_sal, Hra, Medical, Pf, Pt, Net_sal, Gross_sal;

	public Employees()// default constructor
	{
		this.empid = 007;
		this.name = "Not_available";
		this.Basic_sal = 100.00;
		this.Hra = (0.5 * Basic_sal);
		this.Medical = 100.0;
		this.Pf = (0.12 * Basic_sal);
		this.Pt = 200.0;
		this.Gross_sal = (Basic_sal + Hra + Medical);
		this.Net_sal = (Gross_sal - (Pf + Pt));
		System.out.println("employ default constroctor");
	}

	/*
	 * void gross_salary() { this.Gross_sal =Basic_sal +Hra + Medical; }
	 * 
	 * void net_sal() { Net_sal=Gross_sal-(Pf+Pt); }
	 */
	public Employees(int empid, String name, Double basic, Double medical)// parametrized constructor
	{
		this.empid = empid;
		this.name = name;
		this.Basic_sal = basic;
		this.Hra = (0.5 * basic);
		this.Medical = medical;
		this.Pf = (0.12 * basic);
		this.Pt = 200.0;
		this.Gross_sal = (this.Basic_sal + this.Hra + this.Medical);
		this.Net_sal = (Gross_sal - (this.Pf + this.Pt));
	}

	void display()// for displaying data member of the calling object
	{
		System.out.println("EMPID=" + empid);
		System.out.println("NAME=" + name);
		System.out.println("BASIC SALARY=" + Basic_sal);
		System.out.println("HRA=" + Hra);
		System.out.println("Medical=" + Medical);
		System.out.println("PF=" + Pf);
		System.out.println("PT=" + Pt);
		System.out.println("GROSS SALARY=" + Gross_sal);
		System.out.println("NET SALARY=" + Net_sal);
	}

	public String toString() {
		return "Name=" + name + " salary=" + Basic_sal + " Medical=" + Medical + " PF=" + Pf + " PT" + Pt
				+ " Gross Salary=" + Gross_sal + "Net Salary=" + Net_sal;
	}

	public static void main(String[] args) {
		Employees e = new Employees();

		e.display();
		System.out.println("-----------------------------------------");
		Employees emp = new Employees();
		System.out.println(emp.hashCode());

		emp.display();
		System.out.println(emp);

	}

}
