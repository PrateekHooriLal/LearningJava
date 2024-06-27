package com.lambda.demo;

import java.util.Scanner;

public class MoveToStarOrEnd {

	static int[] crunchifyData = { 1, 0, 0, 4, 4, 2, 0, 3, 0, 0, 4, 1 };// 1 2 3 4 0 0 0 0 0

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		System.out.print("Original Array : ");
		for (int i : crunchifyData) {
			System.out.print(i + " ");
		}

		System.out.println("Enter a number:");
		int pivot = sc.nextInt();

		System.out.print("Given Number Moved to End:   ");
		MoveToEnd(crunchifyData, pivot);
		for (int i : crunchifyData) {
			System.out.print(i + " ");
		}
		System.out.println();
		System.out.print("Given Number Moved to Start: ");
		MoveToStart(crunchifyData, pivot);
		for (int i : crunchifyData) {
			System.out.print(i + " ");
		}
	}

	public static int[] MoveToEnd(int[] arr, int pivot) {
		int j = 0;
		for (int i = 0; i < crunchifyData.length; i++) {
			if (crunchifyData[i] != pivot) {
				int temp = crunchifyData[j];
				crunchifyData[j] = crunchifyData[i];
				crunchifyData[i] = temp;
				j++;
			}
		}
		return arr;
	}

	public static int[] MoveToStart(int arr[], int pivot) {//without using extra variable for swapping
		int j = arr.length - 1;
		for (int i = arr.length - 1; i >= 0; i--) {
			if (crunchifyData[i] != pivot) {
				crunchifyData[j] += crunchifyData[i];
				crunchifyData[i] = crunchifyData[j] - crunchifyData[i];
				crunchifyData[j] = crunchifyData[j] - crunchifyData[i];
				j--;
			}
		}
		return arr;
	}
}
