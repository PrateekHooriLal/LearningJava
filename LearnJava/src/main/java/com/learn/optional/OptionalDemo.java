package com.learn.optional;

public class OptionalDemo {

	static String str = "java";
	static int[] arr = { 10, 50, 30, 40 };
	

	int a = b = 1;
	public static void main(String[] args) {
s
		
		int max = 0, min=arr[0],tempMax =0;
		for (int i = 0 ; i < arr.length; i++) {
			
			if(min > arr[i])
				min = arr[i];
			if(arr[i]> max)
				max = arr[i];	
				
		}
		System.out.println("Sum="+(min+max));

	}

}

//core java , problem solving, multithreading executor , future   //
