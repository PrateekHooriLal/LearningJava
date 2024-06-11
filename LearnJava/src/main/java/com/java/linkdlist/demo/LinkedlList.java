package com.java.linkdlist.demo;

public class LinkedlList {

	Node head;// variable of node type to hold reference to next node.
	Node tail;// Variable to hold reference of the last element of the list.
	private int size = 0; // to hold the size of the list

	public void appendNode(Node n) {
		/** this add appends the node to the link list **/
		if (n != null) {
			if (head == null) {
				head = n;
				tail = head;
				tail.next = null;
				size++;
			} else {
				tail.next = n;
				tail = n;
				tail.next = null;
				size++;
			}
		} else
			return;

	}// add function ends

	public void reverseList(Node rev) {
		Node prev = null;
		Node curr = rev;
		Node next = null;

		while (curr != null) {

			// get the third node in next pointer
			next = curr.next;

			// now , point the current node to the prev node
			curr.next = prev;

			// updating the pointers curr, and prv.
			prev = curr;
			curr = next;

		}
		head = prev;
	}

	public void adjecentSwap(Node swap) {

		if (swap == null || swap.next == null)
			return;

		// initializing the pointers
		Node prev = swap;
		Node curr = prev.next;
		Node next = null;

		// Changing the head of the linked list
		head = curr;

		// now traverse the list and swap the nodes
		while (true) {

			// getting the next from current node
			next = curr.next;

			// change current.next to point previous node
			curr.next = prev;

			// check if the next node is null or if it is pointing to null
			if (next == null || next.next == null) {
				prev.next = next;
				break;
			}

			prev.next = next.next;// pointing the previous node to the node
									// after next as after swapping it will be
									// new node

			// update prev and curr
			prev = next;
			curr = prev.next;
		}

		System.out.println("Node third:" + prev.getData() + ":" + curr.getData() + ":" + next.getData());

	}// adjecentSwap method ends

	private void printNode() {
		// TODO Print the values of linked list
		Node temp = head;
		while (temp != null) {
			System.out.print(temp.getData() + "-->");
			temp = temp.next;
		} // while loop ends

		System.out.println("[null]");
	}// print method ends

	// ================================================== declaring inner class
	// node================================//
	public static class Node {

		public Node next;
		private int data;

		// Parameterized constructor
		public Node(int d) {
			this.data = d;
			this.next = null;
		}

		// Getters and setters
		public Node getNext() {
			return next;
		}

		public void setNext(Node next) {
			this.next = next;
		}

		public int getData() {
			return data;
		}

		public void setData(int data) {
			this.data = data;
		}
	}// inner class ends

	public static void main(String[] args) {

		// create a linked list , which have the reference to the first node or
		// the head node.
		LinkedlList list = new LinkedlList();

		// creating nodes
		Node n1 = new Node(4);
		Node n2 = new Node(3);
		Node n3 = new Node(45);
		Node n4 = new Node(32);
		Node n5 = new Node(25);
		Node n6 = new Node(76);
		Node n7 = new Node(44);
		Node n8 = new Node(98);
		Node n9 = new Node(786);

		// now, add nodes to link list
		list.appendNode(n1);
		list.appendNode(n2);
		list.appendNode(n3);
		list.appendNode(n4);
		list.appendNode(n5);
		list.appendNode(n6);
		list.appendNode(n7);
		list.appendNode(n8);
		list.appendNode(n9);
		list.printNode();
		list.adjecentSwap(list.head);
		list.reverseList(list.head);

		// print the link list by traversing it
		list.printNode();

		// print the size of thr link list
		System.out.println("\n Size =" + list.size);
	}// main ends
}// class ends
