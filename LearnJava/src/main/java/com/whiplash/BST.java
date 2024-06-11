package com.whiplash;

/**
 * @author plal
 *
 */

/**
 * Balanced Binary search tree.
 *
 */
public class BST {

	public class Node {

		int data;
		Node left, right;

		public Node(int i) {
			// TODO Auto-generated constructor stub
			data = i;
			left = right = null;
		}
	}// node ends

	public Node sortedArraytoBST(int[] arr, int start, int end) {

		// base case
		if (start > end)
			return null;

		int mid = (end - start) / 2;
		Node node = new Node(arr[mid]);

		node.left = sortedArraytoBST(arr, start, mid - 1);
		node.right = sortedArraytoBST(arr, mid + 1, end);

		return node;
	}//

	public void preorder(Node node) {// recursive method for traversal

		if (node == null)
			return;

		System.out.println(node.data);
		preorder(node.left);
		preorder(node.right);
	}// preorder method ends

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		BST bst = new BST();
		int arr[] = new int[] { 1, 2, 3, 4, 5, 6, 7 };
		int n = arr.length;
		Node root = bst.sortedArraytoBST(arr, 0, n - 1);
		System.out.println("Preorder traversal of constructed BST");
		bst.preorder(root);
	}// main ends

}// class ends