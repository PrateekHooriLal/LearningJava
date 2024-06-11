package com.print.Mydate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.serial.deserial.demo.Movies;

public class CollectionPractise {

	public static void main(String[] args) // main starts
	{
		/*
		 * ArrayList<Employees>list = new ArrayList<Employees>();//within the <>
		 * operator is the generics Employees emp = new Employees(); Employees[] arr
		 * ={emp,new Employees(500,"jaadu1",565.0,900.0), new
		 * Employees(500,"jaadu2",565.0,900.0),new Employees(500,"jaadu3",565.0,900.0)};
		 * 
		 * //////inserting value in list of type Integer//////////////
		 * System.out.println("using for loop:"); for(int i =0; i<arr.length; i++) {
		 * list.add(arr[i]); }
		 * 
		 * //list.add(e);
		 * System.out.println("size of array list="+list.size());//displaying size of
		 * list System.out.println(list);
		 * 
		 * ////////////using iterator /////////////////////////
		 * System.out.println("using iterators:"); Iterator<Employees> ref
		 * =list.iterator();
		 * 
		 * while(ref.hasNext())//has next function gives true or false depending on the
		 * existence of next entry in the list. { Employees element = ref.next();
		 * System.out.println(element+" "); }
		 * 
		 * HashMap<String,String> hm=new HashMap<String, String>();
		 * hm.put("name","ritik"); hm.put("roll", "1002"); hm.put("marks","456");
		 * hm.put("address", "go to hell"); System.out.println(hm); hm.remove("roll");
		 * System.out.println("hash map value="+hm);
		 */

		/*
		 * eg of array list storing object of movies type and movie object itself
		 * storing movie's in form of hashmap.
		 */

		ArrayList<Movies> movielist = new ArrayList<Movies>();// declaring array list in which object will be stored.
		Movies m = new Movies();
		HashMap<String, String> one = new HashMap<String, String>();
		one.put("NAME", "BADASHA");
		one.put("Rating", "5.3");

		one.put("buisness", "1 cr");
		m.setMovieDetail(one);// passing the hashmap one to the movie object.
		movielist.add(m);// adding the object in the movie list.
		System.out.println((movielist.get(0)).getMovieDetail());

		HashMap<String, String> two = new HashMap<String, String>();
		Movies m1 = new Movies();
		two.put("NAME", "Now YOU SEE ME");
		two.put("Rating", "9");
		two.put("buisness", "100 cr");
		m1.setMovieDetail(two);
		movielist.add(m1);
		System.out.println((movielist.get(1)).getMovieDetail());
		System.out.println("Size of arrayList=" + movielist.size());
		Collection<Movies> myShapesCollection = null;
		myShapesCollection.stream().filter(e -> e.getColor() == Color.RED)
				.forEach(e -> System.out.println(e.getName()));

	}// main ends

}// class ends
