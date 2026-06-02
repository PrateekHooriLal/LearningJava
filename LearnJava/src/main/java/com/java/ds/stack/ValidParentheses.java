package com.java.ds.stack;

import java.util.Stack;

public class ValidParentheses {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println(isValidParantheses("y[es{(jhkhdksddbksfjhfkjbddkbsbk)}]"));
	}

	public static boolean isValidParantheses(String s) {

		char[] map = new char[128];
		map['('] = ')';
		map['['] = ']';
		map['{'] = '}';
		Stack<Character> stack = new Stack<Character>();

		for (char ch : s.toCharArray()) {

			//System.out.println(ch);
			if (map[ch] != 0) {
				stack.push(map[ch]);
				//System.out.println(map[ch] != 0);
				//System.out.println("if=" + ch);
				//System.out.println("Stack="+stack.toString());
			} else if (ch == ')' || ch == ']' || ch == '}') {
				//System.out.println("else if=" + ch);
				if (stack.isEmpty() || stack.pop() != ch)
					return false;
				//System.out.println("Stack="+stack.toString());

			}
		}

		return stack.isEmpty();
	}
}
