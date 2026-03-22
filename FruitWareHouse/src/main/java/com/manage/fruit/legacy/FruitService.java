package com.manage.fruit.legacy;

// Importing required classes from JavaFX library, you may need additional imports based on your project's requirements

public class FruitService {
	public static boolean createFruit(String fruitName) throws Exception {
		if (fruitName == null || fruitName.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid argument: Fruit name cannot be blank");
		}
		System.out.println(fruitName + " has been created.");
		return true;
	}

	public static void main(String[] args) {
		try {
			if (createFruit("Apple")) {
				System.out.println("Fruit created successfully");
			} else {
				System.out.println("Failed creating fruit");
			}
		} catch (Exception e) {
			System.err.println("Error creating fruit: " + e.getMessage());
		}
	}
}
