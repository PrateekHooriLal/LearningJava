package com.java.jvm;

import java.lang.invoke.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ============================================================
 * JAVA MEMORY MODEL (JMM) — Concurrency Correctness Foundation
 * ============================================================
 *
 * CONCEPT:
 *   The Java Memory Model (JMM) defines HOW threads interact through memory.
 *   Without JMM rules, the JIT compiler, CPU, and memory subsystem are free to:
 *   - Reorder instructions (within a thread, results appear the same TO THAT THREAD)
 *   - Cache values in registers (thread's local cache — not flushed to main memory)
 *   - Perform speculative execution
 *   These optimizations are INVISIBLE to a single thread but cause bugs in multi-threaded code.
 *
 * THE CORE GUARANTEE: HAPPENS-BEFORE (HB)
 *   If action A happens-before action B:
 *   1. A's writes are VISIBLE to B (no caching issue)
 *   2. A's ordering PRECEDES B (no reordering across the HB boundary)
 *
 *   HB is NOT about time ordering — it's about VISIBILITY and ORDERING guarantees.
 *   "A happens-before B" means B is guaranteed to see A's effects, EVEN IF they
 *   run on different CPU cores with separate caches.
 *
 * HAPPENS-BEFORE RULES (memorize these):
 *   1. PROGRAM ORDER: Within one thread, each action HB every subsequent action.
 *      (Single thread: all operations appear sequential from that thread's view)
 *   2. MONITOR LOCK: unlock(m) HB lock(m) by any subsequent thread.
 *      → Thread 2's synchronized block sees all writes from Thread 1's synchronized block.
 *   3. VOLATILE WRITE: write to volatile v HB every subsequent read of v.
 *      → Thread 2 reading volatile sees all writes made before the volatile write.
 *   4. THREAD START: Thread.start() HB every action in the started thread.
 *   5. THREAD JOIN: every action in thread T HB Thread.join(T) returning.
 *   6. TRANSITIVITY: if A HB B and B HB C, then A HB C.
 *
 * THREE SEPARATE CONCURRENCY PROPERTIES:
 *   ATOMICITY: operation appears instantaneous (not divisible).
 *     synchronized provides atomicity. volatile does NOT for compound ops.
 *   VISIBILITY: writes by one thread visible to other threads.
 *     volatile provides visibility. synchronized provides visibility.
 *   ORDERING: operations appear in program order across threads.
 *     volatile provides ordering (no reordering across volatile access).
 *     synchronized provides ordering (no reordering in/out of block).
 *
 * INTERVIEW ANGLE:
 *   - "Why is double-checked locking broken without volatile?" → Partial construction
 *   - "What does volatile guarantee vs synchronized?" → volatile: visibility+ordering only
 *   - "What is instruction reordering?" → JIT/CPU reorder for performance, safe within
 *     one thread but can cause bugs across threads
 *   - "What is a data race?" → Two threads access same memory, at least one writes,
 *     no synchronization → undefined behavior in JMM
 */
public class JVMMemoryModel {

    // =========================================================
    // 1. VISIBILITY PROBLEM — Non-volatile flag loop
    // =========================================================
    //
    // PROBLEM: Without volatile, the JIT compiler may hoist the flag check
    //   out of the loop (it's "constant" from the thread's perspective because
    //   no synchronization tells it otherwise). The loop runs FOREVER even after
    //   another thread sets stopFlag = true.
    //
    // JIT OPTIMIZATION (the danger):
    //   The compiler sees: while(!stopFlag) { }
    //   It doesn't see any write to stopFlag WITHIN this thread.
    //   It optimizes to: if(!stopFlag) { while(true) {} }
    //   This is "loop invariant code motion" — valid optimization for single-threaded code.
    //   Multi-threaded: catastrophically wrong — the flag change is never seen.
    //
    // FIX: volatile keyword creates a memory barrier.
    //   volatile READ: flushes the thread's CPU cache → reads from main memory.
    //   volatile WRITE: writes to main memory, invalidates other threads' caches.
    //   Prevents loop hoisting / register caching of the variable.

    // BROKEN: may loop forever
    private static boolean stopFlagBroken = false;  // DO NOT USE — see note above

    // FIXED: volatile ensures every read goes to main memory
    private static volatile boolean stopFlagFixed = false;

    static void demonstrateVisibility() throws InterruptedException {
        System.out.println("--- Visibility Demo (using volatile flag) ---");
        stopFlagFixed = false;

        Thread worker = new Thread(() -> {
            int count = 0;
            // CORRECT: volatile read — JIT cannot hoist this out of loop
            while (!stopFlagFixed) {
                count++;
                if (count % 1_000_000 == 0) {
                    System.out.println("  Worker: still running, count=" + count);
                }
            }
            System.out.println("  Worker: saw stopFlag=true, stopping. count=" + count);
        });

        worker.start();
        Thread.sleep(50);
        stopFlagFixed = true; // volatile write: immediately visible to worker thread
        System.out.println("  Main: set stopFlag=true");
        worker.join(1000); // wait for worker to stop
        System.out.println("  Worker stopped: " + !worker.isAlive());
    }

    // =========================================================
    // 2. VOLATILE SEMANTICS — Detailed
    // =========================================================
    //
    // VOLATILE DOES:
    //   ✓ Visibility: writes immediately visible to all threads
    //   ✓ Ordering: volatile write cannot be reordered before writes BEFORE it;
    //              volatile read cannot be reordered after reads AFTER it.
    //     Effectively: volatile write = StoreStore + StoreLoad barrier
    //                  volatile read = LoadLoad + LoadStore barrier
    //
    // VOLATILE DOES NOT DO:
    //   ✗ Atomicity for compound operations
    //   ✗ i++ on volatile is NOT atomic: it's (read)(increment)(write) — 3 operations.
    //     Two threads doing i++ concurrently can BOTH read 5, both write 6 → lost update.
    //   ✗ Mutual exclusion: two threads can still race on compound checks

    private static volatile int volatileCounter = 0;

    static void demonstrateVolatileNonAtomicity() throws InterruptedException {
        System.out.println("\n--- Volatile Non-Atomicity (i++ is 3 ops) ---");
        volatileCounter = 0;
        int threads = 10;
        int increments = 1000;
        Thread[] workers = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < increments; j++) {
                    volatileCounter++; // NOT ATOMIC even with volatile!
                    // Read-modify-write: three separate ops
                    // Race: T1 reads 5, T2 reads 5, T1 writes 6, T2 writes 6 → lost update
                }
            });
            workers[i].start();
        }

        for (Thread w : workers) w.join();
        System.out.println("  Expected: " + (threads * increments));
        System.out.println("  Actual: " + volatileCounter + " (may be less — lost updates!)");
        System.out.println("  Fix: Use AtomicInteger.incrementAndGet() for atomic compound ops");
    }

    // =========================================================
    // 3. DOUBLE-CHECKED LOCKING — Broken vs Fixed
    // =========================================================
    //
    // PATTERN: Lazy initialization with minimal synchronization.
    //   Goal: don't synchronize after the first initialization (avoid contention).
    //
    // BROKEN VERSION (without volatile):
    //   The problem is instruction REORDERING during object construction.
    //   `new Singleton()` in bytecode is three steps:
    //   1. Allocate memory for object → reference is non-null
    //   2. Initialize fields (run constructor)
    //   3. Assign reference to `instance` field
    //   Steps 1 and 3 CAN be reordered to: allocate, assign to instance, THEN initialize.
    //   If Thread 1 assigns (step 3 early) before completing initialization (step 2),
    //   Thread 2 sees instance != null and uses a PARTIALLY CONSTRUCTED OBJECT.
    //   This is a real bug, documented in https://cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
    //
    // FIXED VERSION (with volatile):
    //   volatile prevents reordering of the assignment relative to construction.
    //   The HB rule for volatile guarantees: all writes BEFORE the volatile write
    //   (including constructor execution) are visible to threads that read the volatile.
    //   So: Thread 2 reads instance != null → guaranteed to see fully constructed object.
    //
    // ALTERNATIVE (BETTER): Initialization-on-demand holder (Bill Pugh Singleton):
    //   Static inner class is initialized on first use (JVM guarantees thread-safe init).
    //   No synchronization needed, lazy, clean. The JVM's class loading is the lock.

    // BROKEN (do not use — for study only)
    static class BrokenSingleton {
        private static BrokenSingleton instance;
        private final int value;

        private BrokenSingleton() { this.value = computeExpensiveValue(); }
        private int computeExpensiveValue() { return 42; }
        public int getValue() { return value; }

        public static BrokenSingleton getInstance() {
            if (instance == null) {                    // check 1: no lock
                synchronized (BrokenSingleton.class) {
                    if (instance == null) {            // check 2: with lock
                        instance = new BrokenSingleton(); // REORDERING DANGER here!
                        // Thread could see instance != null before constructor finishes
                    }
                }
            }
            return instance;
        }
    }

    // FIXED with volatile
    static class FixedSingleton {
        // KEY: volatile prevents reordering of assignment relative to construction
        private static volatile FixedSingleton instance;
        private final int value;

        private FixedSingleton() { this.value = 42; }
        public int getValue() { return value; }

        public static FixedSingleton getInstance() {
            if (instance == null) {                  // first check (no lock)
                synchronized (FixedSingleton.class) {
                    if (instance == null) {          // second check (with lock)
                        instance = new FixedSingleton(); // safe: volatile prevents bad reordering
                    }
                }
            }
            return instance;
        }
    }

    // BEST: Initialization-on-demand holder (no synchronization, thread-safe by JVM spec)
    static class IdealSingleton {
        private final int value;
        private IdealSingleton() { this.value = 42; }

        // Inner class loaded on first REFERENCE to it — JVM guarantees atomic class loading
        private static class Holder {
            static final IdealSingleton INSTANCE = new IdealSingleton();
        }

        public static IdealSingleton getInstance() {
            return Holder.INSTANCE; // triggers class loading on first call
        }

        public int getValue() { return value; }
    }

    // =========================================================
    // 4. ATOMICITY — AtomicInteger vs synchronized vs volatile
    // =========================================================
    //
    // THREE LEVELS OF SYNCHRONIZATION:
    //   AtomicInteger: uses hardware Compare-And-Swap (CAS) instruction.
    //     - Non-blocking (lock-free): no thread suspension, no context switch overhead.
    //     - Works for SINGLE variable operations (incrementAndGet, compareAndSet).
    //     - Does NOT protect multi-step invariants across multiple variables.
    //   synchronized: full mutual exclusion.
    //     - Works for MULTI-STEP invariants (e.g., update two related variables atomically).
    //     - Higher overhead: thread suspension, OS context switch, cache coherence.
    //     - Reentrant: same thread can acquire the same lock multiple times.
    //   volatile: visibility + ordering only.
    //     - No atomicity for compound operations.
    //     - Use for single-writer, multi-reader flag/status variables.
    //     - Use for safely publishing an immutable object (one write, many reads).

    private static final AtomicInteger atomicCount = new AtomicInteger(0);
    private static int synchronizedCount = 0;
    private static final Object LOCK = new Object();

    static void demonstrateAtomicity() throws InterruptedException {
        System.out.println("\n--- Atomicity Comparison ---");
        atomicCount.set(0);
        synchronizedCount = 0;

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    atomicCount.incrementAndGet(); // CAS: atomic at hardware level
                    synchronized (LOCK) {
                        synchronizedCount++;       // mutual exclusion: always correct
                    }
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        System.out.println("  AtomicInteger result: " + atomicCount.get() + " (always 10000)");
        System.out.println("  Synchronized result: " + synchronizedCount + " (always 10000)");

        // COMPARE AND SET demo (optimistic locking / lock-free algorithm building block)
        AtomicInteger ai = new AtomicInteger(5);
        boolean cas1 = ai.compareAndSet(5, 10); // expected=5, new=10 → success
        boolean cas2 = ai.compareAndSet(5, 20); // expected=5, new=20 → FAIL (current is 10)
        System.out.println("  CAS(5→10) when value=5: " + cas1 + ", value=" + ai.get()); // true, 10
        System.out.println("  CAS(5→20) when value=10: " + cas2 + ", value=" + ai.get()); // false, 10
    }

    // =========================================================
    // 5. JAVA 21 VARHANDLE — Fine-grained memory access modes
    // =========================================================
    //
    // CONCEPT: VarHandle (Java 9+) provides access to variables with explicit
    //   memory ordering semantics. More efficient than volatile for some use cases.
    //
    // ACCESS MODES (from weakest to strongest):
    //   PLAIN: no memory ordering guarantee (like non-volatile read/write)
    //   OPAQUE: coherent reads/writes (no inter-thread ordering guarantee)
    //   ACQUIRE/RELEASE: acquire read HB release write (one-way barriers)
    //     - acquire: like volatile read (LoadLoad+LoadStore barrier)
    //     - release: like volatile write (StoreStore+LoadStore barrier)
    //     - More efficient than full volatile (avoids StoreLoad barrier)
    //   VOLATILE: full volatile semantics (StoreLoad barrier included)
    //   COMPARE_AND_SET: CAS with volatile semantics
    //
    // WHY ACQUIRE/RELEASE IS FASTER:
    //   Full volatile (Java's keyword) inserts all 4 barriers.
    //   Acquire/release only inserts 3. The StoreLoad barrier is the expensive one.
    //   For producer-consumer patterns (one writes, one reads), acquire/release is enough.
    //
    // WHEN TO USE VARHANDLE:
    //   - Building lock-free data structures (queues, stacks)
    //   - High-performance concurrent code (avoid lock overhead AND full volatile overhead)
    //   - Not for typical application code — stick with volatile or synchronized

    private int varHandleField = 0;

    static void demonstrateVarHandle() {
        System.out.println("\n--- VarHandle Demo (Java 9+) ---");
        try {
            VarHandle handle = MethodHandles.lookup()
                .findVarHandle(JVMMemoryModel.class, "varHandleField", int.class);

            JVMMemoryModel instance = new JVMMemoryModel();

            // VOLATILE access mode
            handle.setVolatile(instance, 42);
            int val = (int) handle.getVolatile(instance);
            System.out.println("  VarHandle volatile get: " + val);

            // CAS via VarHandle
            boolean success = handle.compareAndSet(instance, 42, 100);
            System.out.println("  CAS (42→100): " + success + ", value=" + handle.get(instance));

            // ACQUIRE/RELEASE (more efficient for producer-consumer)
            handle.setRelease(instance, 200);   // like volatile write (producer side)
            int acquired = (int) handle.getAcquire(instance); // like volatile read (consumer side)
            System.out.println("  Release/Acquire: " + acquired);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("  VarHandle error: " + e.getMessage());
        }
    }

    // =========================================================
    // 6. HAPPENS-BEFORE DEMONSTRATION
    // =========================================================
    //
    // Demonstrating HB via thread start and join rules.
    // Everything before Thread.start() HB everything in the started thread.
    // Everything in thread T HB Thread.join(T) returning.

    static void demonstrateHappensBefore() throws InterruptedException {
        System.out.println("\n--- Happens-Before via start/join ---");

        final int[] sharedData = {0}; // effectively final reference, mutable content

        // Before thread starts: write value
        sharedData[0] = 42; // this HB everything in `worker` thread (Thread.start() rule)

        Thread worker = new Thread(() -> {
            // GUARANTEED: worker sees sharedData[0] = 42
            // Because Thread.start() HB first action in thread (HB rule #4)
            System.out.println("  Worker sees: " + sharedData[0]); // always 42

            sharedData[0] = 100; // write before thread ends
        });

        worker.start(); // Thread.start() creates HB relationship
        worker.join();  // HB rule #5: all actions in worker HB join() returning

        // GUARANTEED: main sees sharedData[0] = 100
        // Because: all actions in worker HB join() returning
        System.out.println("  Main after join: " + sharedData[0]); // always 100
    }

    // =========================================================
    // MAIN
    // =========================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== JAVA MEMORY MODEL (JMM) DEMO ===");

        demonstrateVisibility();
        demonstrateVolatileNonAtomicity();
        demonstrateAtomicity();
        demonstrateVarHandle();
        demonstrateHappensBefore();

        System.out.println("\n--- DCL Singleton Demo ---");
        FixedSingleton s1 = FixedSingleton.getInstance();
        FixedSingleton s2 = FixedSingleton.getInstance();
        System.out.println("  Same instance: " + (s1 == s2)); // true
        System.out.println("  Value: " + s1.getValue());

        System.out.println("  Ideal singleton (Bill Pugh): " + IdealSingleton.getInstance().getValue());

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: What is a happens-before relationship?");
        System.out.println("A: A formal guarantee in JMM. If A HB B: all writes by A are visible");
        System.out.println("   to B, and A's ordering precedes B's. Without HB, there's no");
        System.out.println("   guarantee the write is visible (cache, register, reordering).");
        System.out.println();
        System.out.println("Q: volatile vs synchronized — which guarantees what?");
        System.out.println("A: volatile: visibility + ordering (no atomicity for compound ops).");
        System.out.println("   synchronized: visibility + ordering + atomicity + mutual exclusion.");
        System.out.println("   Use volatile for: simple flags, one-time publication of immutable objects.");
        System.out.println("   Use synchronized for: compound invariants, check-then-act sequences.");
        System.out.println();
        System.out.println("Q: Why is double-checked locking broken without volatile?");
        System.out.println("A: JVM can reorder: allocate memory → assign reference → run constructor.");
        System.out.println("   Another thread sees non-null reference BEFORE constructor completes.");
        System.out.println("   volatile prevents this reordering via memory barriers.");
        System.out.println();
        System.out.println("Q: AtomicInteger vs synchronized?");
        System.out.println("A: AtomicInteger: CAS (hardware instruction), lock-free, no context switch.");
        System.out.println("   Faster for single-variable operations under moderate contention.");
        System.out.println("   synchronized: needed for multi-variable invariants, more overhead.");
        System.out.println("   Under HIGH contention, synchronized may outperform CAS (spinning).");
    }
}
