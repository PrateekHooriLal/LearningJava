package com.print.Mydate;

/*@ author Ritik & Prateek
 * 
 * */
//here using getter setter function wehave initilized the data members.
public class Swapper {

	int day, mon, year;

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getMon() {
		return mon;
	}

	public void setMon(int mon) {
		this.mon = mon;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	void display() {
		System.out.println("day=" + day + " month=" + mon + " year=" + year);
	}

	public static void swap(Swapper[] d) {
		Swapper temp;
		temp = d[0];
		d[0] = d[1];
		d[1] = temp;
		// d[0].display();
		// d[1].display();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// System.out.println("enter the day");

		Swapper d = new Swapper();
		d.setDay(11);
		d.setMon(4);
		d.setYear(12);

		Swapper d1 = new Swapper();
		d1.setDay(32);
		d1.setMon(21);
		d1.setYear(2222);
		System.out.println("--------before swap----");
		d.display();
		d1.display();

		Swapper[] b = { d, d1 };
		System.out.println("----after swap------");
		Swapper.swap(b);
		b[0].display();
		b[1].display();
	}

}
