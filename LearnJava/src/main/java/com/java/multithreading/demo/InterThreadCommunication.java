package com.java.multithreading.demo;

import java.util.LinkedList;
import java.util.Queue;

/**
 * INTER-THREAD COMMUNICATION — Producer-Consumer with wait() / notifyAll()
 *
 * CONCEPT:
 *   The Producer-Consumer problem is the classic inter-thread communication example.
 *   - Producer generates items and puts them in a shared buffer.
 *   - Consumer takes items from the buffer and processes them.
 *   - If the buffer is FULL, the producer must WAIT.
 *   - If the buffer is EMPTY, the consumer must WAIT.
 *   - After changing buffer state, notify waiting threads.
 *
 * HOW wait() / notifyAll() WORKS:
 *   - wait() releases the monitor lock and suspends the thread.
 *   - notifyAll() wakes ALL waiting threads; they compete to re-acquire the lock.
 *   - ALWAYS call wait() inside a while-loop (not if), because:
 *     1. Spurious wakeups: a thread can wake up without being notified.
 *     2. Multiple consumers: after wakeup, the condition may already be false again.
 *   - wait() / notify() must be called inside a synchronized block on the same object.
 *
 * PATTERN TEMPLATE:
 *   synchronized(lock) {
 *     while(conditionNotMet) lock.wait();
 *     // do work
 *     lock.notifyAll();
 *   }
 *
 * SEE ALSO:
 *   - PrintEvenOdd.java — uses the same wait/notify pattern for alternating threads.
 *   - BlockingQueueDemo.java — solves the same problem using ArrayBlockingQueue
 *     (Java's built-in solution that wraps wait/notify internally).
 *
 * INTERVIEW FREQUENCY: Very high. Lead/Senior rounds always ask about this.
 *
 * COMMON FOLLOW-UP QUESTIONS:
 *   1. "What's the difference between notify() and notifyAll()?"
 *      notify() wakes ONE random waiting thread. notifyAll() wakes ALL.
 *      Use notifyAll() when multiple threads wait on different conditions
 *      (e.g., both producers and consumers wait on the same object).
 *   2. "Why use while(condition) instead of if(condition) around wait()?"
 *      Spurious wakeups + multiple consumers stealing the item before you run.
 *   3. "What happens if you call wait() without synchronized?"
 *      IllegalMonitorStateException at runtime.
 *   4. "What is the difference between this and BlockingQueue?"
 *      BlockingQueue wraps this exact mechanism — prefer it in production code.
 *
 * Time: O(1) per produce/consume   Space: O(capacity) for the buffer
 */
public class InterThreadCommunication {

    // -------------------------------------------------------------------------
    // Shared Buffer — the communication channel between Producer and Consumer
    // -------------------------------------------------------------------------

    /**
     * Bounded buffer backed by a Queue.
     * All public methods are synchronized on 'this' (the buffer object).
     */
    static class BoundedBuffer {
        private final Queue<Integer> queue = new LinkedList<>();
        private final int capacity;

        BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        /**
         * Producer calls this: adds item to buffer.
         * If buffer is full → wait (release lock until consumer removes an item).
         *
         * WHY while-loop around wait():
         *   After being notified, the thread re-acquires the lock and MUST
         *   re-check the condition — another producer may have filled the buffer
         *   between the notify() call and this thread waking up.
         */
        public synchronized void produce(int item) throws InterruptedException {
            // Wait while buffer is full
            while (queue.size() == capacity) {
                System.out.println("[Producer] Buffer full (" + capacity + "), waiting...");
                wait(); // Releases lock; woken by notifyAll() from consumer
            }

            queue.add(item); // Add item to buffer
            System.out.println("[Producer] Produced: " + item + "  | Buffer size: " + queue.size());

            // Notify all waiting threads (consumers) that an item is available
            notifyAll();
        }

        /**
         * Consumer calls this: removes and returns item from buffer.
         * If buffer is empty → wait.
         */
        public synchronized int consume() throws InterruptedException {
            // Wait while buffer is empty
            while (queue.isEmpty()) {
                System.out.println("[Consumer] Buffer empty, waiting...");
                wait(); // Releases lock; woken by notifyAll() from producer
            }

            int item = queue.poll(); // Remove from head of queue
            System.out.println("[Consumer] Consumed: " + item + "  | Buffer size: " + queue.size());

            // Notify all waiting threads (producers) that space is available
            notifyAll();

            return item;
        }
    }

    // -------------------------------------------------------------------------
    // Producer Thread
    // -------------------------------------------------------------------------

    /**
     * Produces items 1..count with a small sleep to simulate work.
     */
    static class Producer implements Runnable {
        private final BoundedBuffer buffer;
        private final int count;

        Producer(BoundedBuffer buffer, int count) {
            this.buffer = buffer;
            this.count = count;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= count; i++) {
                    buffer.produce(i);
                    Thread.sleep(100); // Simulate production time
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.out.println("[Producer] Interrupted");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Consumer Thread
    // -------------------------------------------------------------------------

    /**
     * Consumes 'count' items with a larger sleep to simulate slower consumption.
     * The larger sleep forces the buffer to fill up, demonstrating producer waiting.
     */
    static class Consumer implements Runnable {
        private final BoundedBuffer buffer;
        private final int count;

        Consumer(BoundedBuffer buffer, int count) {
            this.buffer = buffer;
            this.count = count;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= count; i++) {
                    buffer.consume();
                    Thread.sleep(300); // Consumer is slower than producer → buffer fills
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Consumer] Interrupted");
            }
        }
    }

    // -------------------------------------------------------------------------
    // main()
    // -------------------------------------------------------------------------

    public InterThreadCommunication() {
    } // constructor ends

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Producer-Consumer with wait()/notifyAll() ===");
        System.out.println("Buffer capacity: 3, Items to produce/consume: 8");
        System.out.println("Producer sleeps 100ms, Consumer sleeps 300ms");
        System.out.println("(Consumer is slower → expect producer to wait when buffer is full)");
        System.out.println();

        BoundedBuffer buffer = new BoundedBuffer(3); // Max 3 items in buffer
        int itemCount = 8;

        Thread producer = new Thread(new Producer(buffer, itemCount), "Producer");
        Thread consumer = new Thread(new Consumer(buffer, itemCount), "Consumer");

        producer.start();
        consumer.start();

        // Wait for both threads to finish before printing summary
        producer.join();
        consumer.join();

        System.out.println("\n=== All items produced and consumed successfully ===");
        System.out.println("NOTE: Compare this with BlockingQueueDemo.java which solves");
        System.out.println("      the same problem using ArrayBlockingQueue (Java built-in).");
    }
}
