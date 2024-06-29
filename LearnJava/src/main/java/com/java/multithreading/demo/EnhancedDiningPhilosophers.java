package com.java.multithreading.demo;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.ThreadLocalRandom;

public class EnhancedDiningPhilosophers {

    public static void main(String[] args) {
        Philosopher[] philosophers = new Philosopher[5];
        Chopstick[] chopsticks = new Chopstick[5];
        
        for (int i = 0; i < 5; i++) {
            chopsticks[i] = new Chopstick(i);
        }
        
        for (int i = 0; i < 5; i++) {
            philosophers[i] = new Philosopher(i, chopsticks[i], chopsticks[(i + 1) % 5]);
            new Thread(philosophers[i], "Philosopher " + (i + 1)).start();
        }
    }

    static class Chopstick {
        private final int id;
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        private boolean isTaken = false;
        
        public Chopstick(int id) {
            this.id = id;
        }
        
        public void pickUp() throws InterruptedException {
            lock.lock();
            try {
                while (isTaken) {
                    condition.await();
                }
                isTaken = true;
            } finally {
                lock.unlock();
            }
        }
        
        public void putDown() {
            lock.lock();
            try {
                isTaken = false;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    static class Philosopher implements Runnable {
        private final int id;
        private final Chopstick leftChopstick;
        private final Chopstick rightChopstick;

        public Philosopher(int id, Chopstick leftChopstick, Chopstick rightChopstick) {
            this.id = id;
            this.leftChopstick = leftChopstick;
            this.rightChopstick = rightChopstick;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    think();
                    write();
                    eat();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void think() throws InterruptedException {
            System.out.println("Philosopher " + id + " is thinking.");
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
        }

        private void write() throws InterruptedException {
            System.out.println("Philosopher " + id + " is writing.");
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
        }

        private void eat() throws InterruptedException {
            if (id % 2 == 0) {
                leftChopstick.pickUp();
                System.out.println("Philosopher " + id + " picked up left chopstick.");
                rightChopstick.pickUp();
                System.out.println("Philosopher " + id + " picked up right chopstick.");
            } else {
                rightChopstick.pickUp();
                System.out.println("Philosopher " + id + " picked up right chopstick.");
                leftChopstick.pickUp();
                System.out.println("Philosopher " + id + " picked up left chopstick.");
            }

            System.out.println("Philosopher " + id + " is eating.");
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 4000));

            leftChopstick.putDown();
            System.out.println("Philosopher " + id + " put down left chopstick.");
            rightChopstick.putDown();
            System.out.println("Philosopher " + id + " put down right chopstick.");
        }
    }
}
