package com.program.solution;

public class missingElement {

	public missingElement() {
		// TODO Auto-generated constructor stub
	}// constructor ends

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int[] arr1 = { 1, 3, 4, 6, 8, 9, 15, 20, 80 };
		missingElement mis = new missingElement();
		mis.findMissing(arr1);
	}// main ends

	public void findMissing(int[] arr) {

		int len = arr.length;
		int j = 0;
		int i = 0;
		if (arr[len - 1] <= 1 || arr[len - 1] > 500)
			return;// System.out.printf("Enter valid values");
		System.out.println("length:" + len);

		for (i = arr[0]; i < arr[len - 1]; i++)// 1 to 20
		{
			if (arr[j] == i)// if(arr[j] != i)

				j++; // sopln("i"); else j++;
			else
				System.out.print(i + " ");

		} // for loop ends

	}// findMissing method ends

}// class ends
