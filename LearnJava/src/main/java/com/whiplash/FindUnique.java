package com.whiplash;

public class FindUnique {
	public static void main(String[] args) {
		int arr1[] = { 1, 2, 3, 4, 3, 4 };
		int arr2[] = { 3, 4, 5, 6, 5, 6 };
		boolean isfound = false;

		for (int i = 0; i < arr1.length; i++) {
			isfound = false;

			for (int j = 0; j < arr2.length; j++) {
				if (arr1[i] == arr2[j]) {
					isfound = true;
				}

			}
			if (!isfound) {
				System.out.println(":::::" + arr1[i]);
			}

		}
		for (int i = 0; i < arr2.length; i++) {
			isfound = false;

			for (int j = 0; j < arr1.length; j++) {
				if (arr2[i] == arr1[j]) {
					isfound = true;
				}
			}
			if (!isfound) {
				System.out.println(":::::" + arr2[i]);

			}
		}

	}
}
