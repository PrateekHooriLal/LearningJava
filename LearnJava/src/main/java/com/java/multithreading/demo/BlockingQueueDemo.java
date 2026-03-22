package com.java.multithreading.demo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * BLOCKING QUEUE DEMO — Producer-Consumer using ArrayBlockingQueue
 *
 * CONCEPT:
 *   ArrayBlockingQueue is Java's built-in thread-safe bounded queue that
 *   internally uses ReentrantLock + two Condition variables (notFull, notEmpty).
 *   It solves the Producer-Consumer problem WITHOUT you writing wait()/notifyAll().
 *
 * INTERNAL IMPLEMENTATION (how it works under the hood):
 *   - Uses a single ReentrantLock (not synchronized blocks)
 *   - Two Condition objects:
 *       notFull  → producers wait here when the queue is full
 *       notEmpty → consumers wait here when the queue is empty
 *   - put()  → locks, waits on notFull if full, adds item, signals notEmpty, unlocks
 *   - take() → locks, waits on notEmpty if empty, removes item, signals notFull, unlocks
 *   This is more efficient than a single Object.wait/notify because producers
 *   and consumers wait on SEPARATE conditions — no false wakeups between them.
 *
 * COMPARISON WITH InterThreadCommunication.java:
 * ┌─────────────────────────┬──────────────────────┬─────────────────────────┐
 * │ Aspect                  │ wait()/notifyAll()   │ ArrayBlockingQueue      │
 * ├─────────────────────────┼──────────────────────┼─────────────────────────┤
 * │ Code complexity         │ Manual, verbose       │ Just put()/take()       │
 * │ Correctness risk        │ Easy to get wrong     │ Handled by library      │
 * │ Condition separation    │ One lock for all      │ Two Conditions          │
 * │ Spurious wakeup guard   │ Must use while-loop   │ Handled internally      │
 * │ Interview relevance     │ Shows deep knowledge  │ Shows practical skill   │
 * └─────────────────────────┴──────────────────────┴─────────────────────────┘
 *
 * RULE OF THUMB:
 *   In production code → use BlockingQueue (less error-prone).
 *   In interviews → know BOTH and be ready to explain the internals.
 *
 * INTERVIEW FREQUENCY: High. Senior rounds often ask: "How would you use Java
 *   concurrency utilities instead of raw wait/notify?"
 *
 * FOLLOW-UP QUESTIONS:
 *   1. "What is the difference between ArrayBlockingQueue and LinkedBlockingQueue?"
 *      → Array: fixed capacity, bounded. Linked: optionally bounded (default unbounded).
 *      → Array uses ONE lock. LinkedBlockingQueue uses TWO locks (put-lock/take-lock)
 *        → higher throughput under heavy load.
 *   2. "What is offer() vs put()?" → offer() returns false immediately if full (non-blocking).
 *      put() blocks until space is available. offer(e, timeout, unit) is timed version.
 *   3. "What happens if a producer thread is interrupted during put()?"
 *      → InterruptedException is thrown. Best practice: call Thread.currentThread().interrupt()
 *        to restore the interrupted status and exit gracefully.
 *
 * Time: O(1) per put/take (amortized)   Space: O(capacity) for the queue buffer
 */
public class BlockingQueueDemo {

    // Sentinel value to signal consumers that production is done
    // Using Integer.MIN_VALUE as a "poison pill" — a convention in producer-consumer patterns
    private static final int POISON_PILL = Integer.MIN_VALUE;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Producer-Consumer with ArrayBlockingQueue ===");
        System.out.println("Capacity: 3, Items: 8, Producers: 1, Consumers: 2");
        System.out.println("(Each consumer receives its own POISON_PILL to stop)");
        System.out.println();

        // ArrayBlockingQueue: bounded, thread-safe, FIFO
        // Capacity 3 → producer will block when it tries to add a 4th item
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);

        int itemCount = 8;
        int consumerCount = 2;

        // Start one producer and two consumers
        Thread producer = new Thread(new Producer(queue, itemCount, consumerCount), "Producer");
        Thread consumer1 = new Thread(new Consumer(queue), "Consumer-1");
        Thread consumer2 = new Thread(new Consumer(queue), "Consumer-2");

        consumer1.start();
        consumer2.start();
        producer.start();

        producer.join();
        consumer1.join();
        consumer2.join();

        System.out.println("\n=== Done. Compare: InterThreadCommunication.java (wait/notifyAll) ===");
    }

    // -------------------------------------------------------------------------
    // Producer
    // -------------------------------------------------------------------------

    static class Producer implements Runnable {
        private final BlockingQueue<Integer> queue;
        private final int count;
        private final int consumerCount; // Need to send one poison pill per consumer

        Producer(BlockingQueue<Integer> queue, int count, int consumerCount) {
            this.queue = queue;
            this.count = count;
            this.consumerCount = consumerCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= count; i++) {
                    // put() BLOCKS if queue is full — releases CPU while waiting
                    // This is the key difference from add() which throws an exception
                    queue.put(i);
                    System.out.printf("[%s] Produced: %-3d | Queue size: %d%n",
                            Thread.currentThread().getName(), i, queue.size());
                    Thread.sleep(80); // Simulate production time
                }

                // Poison pill pattern: send one pill per consumer to signal completion
                // Each consumer will remove one pill and stop — cleaner than a shared flag
                for (int i = 0; i < consumerCount; i++) {
                    queue.put(POISON_PILL);
                    System.out.println("[Producer] Sent POISON_PILL #" + (i + 1));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status — important!
                System.out.println("[Producer] Interrupted");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Consumer
    // -------------------------------------------------------------------------

    static class Consumer implements Runnable {
        private final BlockingQueue<Integer> queue;

        Consumer(BlockingQueue<Integer> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // take() BLOCKS if queue is empty — wakes up when item is available
                    int item = queue.take();

                    if (item == POISON_PILL) {
                        // Received the stop signal — exit loop
                        System.out.printf("[%s] Received POISON_PILL, stopping.%n",
                                Thread.currentThread().getName());
                        break;
                    }

                    System.out.printf("[%s] Consumed: %-3d | Queue size: %d%n",
                            Thread.currentThread().getName(), item, queue.size());
                    Thread.sleep(200); // Consumers are slower → queue fills up
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s] Interrupted%n", Thread.currentThread().getName());
            }
        }
    }
}
