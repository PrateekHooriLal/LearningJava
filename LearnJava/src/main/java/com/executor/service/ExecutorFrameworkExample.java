package com.executor.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorFrameworkExample {

	public static void main(String[] args) {

		// Create a fixed thread pool with 3 threads

		ExecutorService executor = Executors.newFixedThreadPool(3);

		// creating runnable task using annonymous class
		Runnable task_0 = new Runnable() {
			@Override
			public void run() {
				int i = 0;
				while (i < 20) {
					String threadName = Thread.currentThread().getName();
					System.out.println("Task 0 =>" + threadName);
					i++;
				}

			}
		};

		// creating runnable task using Lambda Expression
		Runnable task_1 = () -> {
			String threadName = Thread.currentThread().getName();
			System.out.println("Task 1 =>" + threadName);
		};

		// assigning the task to executor service to run
		executor.execute(task_0);
		executor.execute(task_1);

		executor.shutdown();
	}

}
