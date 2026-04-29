package com.lambda.demo;

import java.util.Scanner;

public class MoveToStarOrEnd {

	static int[] arr = { 2, 3, 1, 99, 67, 1, 1, 74, 85, 1, 26, 37, 1 };

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		System.out.print("Original Array ={ ");
		for (int i : arr) {
			System.out.print(i + " ,");
		}
		System.out.println("}");

		System.out.println("Enter a number:");
		int pivot = sc.nextInt();

		System.out.print("Given Number Moved to End:   ");
		MoveToEnd(arr, pivot);
		for (int i : arr) {
			System.out.print(i + " ");
		}
		System.out.println();
		System.out.print("Given Number Moved to Start: ");
		MoveToStart(arr, pivot);
		for (int i : arr) {
			System.out.print(i + " ");
		}
	}

	public static int[] MoveToEnd(int[] arr, int pivot) {
		int j = 0;
		for (int i = 0; i < arr.length; i++) {
			System.out.println(i + "= ");
			if (arr[i] != pivot) {
				int temp = arr[j];
				arr[j] = arr[i];
				arr[i] = temp;
				j++;
				for (int c : arr) {
					System.out.print(c + " ");
				}
			}
		}
		return arr;
	}

	public static int[] MoveToStart(int arr[], int pivot) {
		int j = arr.length - 1;
		for (int i = arr.length - 1; i >= 0; i--) {
			if (arr[i] != pivot) {
				// swapping without using extra variable for swapping
				arr[j] += arr[i];
				arr[i] = arr[j] - arr[i];
				arr[j] = arr[j] - arr[i];
				j--;
			}
		}
		return arr;

	}
}
