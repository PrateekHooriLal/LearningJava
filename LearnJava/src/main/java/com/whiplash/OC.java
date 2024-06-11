package com.whiplash;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import com.whiplash.OC.INC;

public class OC {

	INC a = new INC();

	static class INC {
		int data = 50;
	}
}// class ends

class B {
	// -can't store the object of inner class in the reference of outer class..
	// OC a = new INC();

	// inner class reference is used to hold the object of inner class.
	INC in = new INC();

	BlockingDeque<String> BD = new BlockingDeque<String>() {

		@Override
		public String removeLast() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String removeFirst() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String pop() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String pollLast() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String pollFirst() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String peekLast() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String peekFirst() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getLast() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getFirst() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Iterator<String> descendingIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object[] toArray() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean addAll(Collection<? extends String> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int remainingCapacity() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int drainTo(Collection<? super String> c, int maxElements) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int drainTo(Collection<? super String> c) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String takeLast() throws InterruptedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String takeFirst() throws InterruptedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String take() throws InterruptedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean removeLastOccurrence(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeFirstOccurrence(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean remove(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String remove() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void putLast(String e) throws InterruptedException {
			// TODO Auto-generated method stub

		}

		@Override
		public void putFirst(String e) throws InterruptedException {
			// TODO Auto-generated method stub

		}

		@Override
		public void put(String e) throws InterruptedException {
			// TODO Auto-generated method stub

		}

		@Override
		public void push(String e) {
			// TODO Auto-generated method stub

		}

		@Override
		public String pollLast(long timeout, TimeUnit unit) throws InterruptedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String poll(long timeout, TimeUnit unit) throws InterruptedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String poll() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String peek() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean offerLast(String e, long timeout, TimeUnit unit) throws InterruptedException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean offerLast(String e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean offerFirst(String e, long timeout, TimeUnit unit) throws InterruptedException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean offerFirst(String e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean offer(String e, long timeout, TimeUnit unit) throws InterruptedException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean offer(String e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Iterator<String> iterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String element() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean contains(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void addLast(String e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addFirst(String e) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean add(String e) {
			// TODO Auto-generated method stub
			return false;
		}
	};
}