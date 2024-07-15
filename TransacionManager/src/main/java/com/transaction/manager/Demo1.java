package com.transaction.manager;

/*Given two sorted arrays:
Array 1: [1, 3, 8, 12, 15, 21, 42, 49, 99]
Array 2: [5, 7, 10, 20, 51, 80, 91]
Create two new arrays containing the even and odd values from both arrays.*/
public class Demo1 {

	public static void main(String[] args) {
		int Arr1[] = { 1, 3, 8, 12, 15, 21, 42, 49, 99 };
		int Arr2[] = { 5, 7, 10, 20, 51, 80, 91 };

		int even[] = new int[Arr1.length + Arr2.length];
		int odd[] = new int[Arr1.length + Arr2.length];

		int indx1 = 0;
		int indx2 = 0;
		int indxE = 0;
		int indxO = 0;

		while (indx1 < Arr1.length && indx2 < Arr2.length) {

			if (Arr1[indx1] < Arr2[indx2]) {
				if (Arr1[indx1] % 2 == 0) {
					even[indxE++] = Arr1[indx1++];
				} else {
					odd[indxO++] = Arr1[indx1++];

				}
			} else if (Arr1[indx1] > Arr2[indx2]) {
				if (Arr2[indx2] % 2 == 0) {
					even[indxE++] = Arr2[indx2++];
				} else {
					odd[indxO++] = Arr2[indx2++];
				}
			} else {
				if (Arr1[indx1] % 2 == 0 && Arr2[indx2] % 2 == 0) {
					even[indxE++] = Arr1[indx1++];
					even[indxE++] = Arr2[indx2++];

				} else {
					odd[indxO++] = Arr1[indx1++];
					odd[indxO++] = Arr2[indx2++];
				}
			}
		}
		if (indx1 < Arr1.length) {

			while (indx1 < Arr1.length) {
				if (Arr1[indx1] % 2 == 0)
					even[indxE++] = Arr1[indx1++];
				else
					odd[indxO++] = Arr1[indx1++];
			}
		}
		if (indx2 < Arr2.length) {

			while (indx2 < Arr2.length) {
				if (Arr2[indx2] % 2 == 0)
					even[indxE++] = Arr2[indx1++];
				else
					odd[indxO++] = Arr2[indx1++];
			}
		}
		System.out.print("Even=" );
		for (int i : even) {
			System.out.print(" "+i);
		}
		System.out.println();
		System.out.print("Odd=");
		for (int i : odd) {
			System.out.print(" "+i);

		}
	}

}
