package com.manage.fruit;
import javafx.scene.control.*; // Importing required classes from JavaFX library, you may need additional imports based on your project's requirements
public class FruitService {  
    public static void createFruit(String fruitName) throws Exception{ 
        if (fruitName == null || fruitName.trim().isEmpty()){ // Checking for empty or NULL input string, you may need to add more validation based on your project's requirements        
            throw new IllegalArgumentException("Invalid argument: Fruit name cannot be blank");            
        }  else {  
           System.out.println(fruitName + " has been created."); // Printing the fruit creation message, you may need to add more logic based on your project's requirements        
       }}    public static void main (String[] args){     FruitService fs = new FruitService();   try {  if (!fs.createFruit("Apple")){      System.out.println ("Failed creating fruit"); } else {System.exit(0);}} catch (Exception e) {}
