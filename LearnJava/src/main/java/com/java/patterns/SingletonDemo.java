package com.java.patterns.singleton;

/**
 * SINGLETON PATTERN — 4 Production-Quality Variants
 *
 * CONCEPT:
 *   Singleton ensures a class has only ONE instance and provides a global access point.
 *   Used when exactly one object is needed to coordinate actions across the system.
 *
 * REAL-WORLD USES IN JAVA/SPRING:
 *   - Spring beans are Singleton scope by default (@Scope("singleton"))
 *   - Logger instances (Log4j LogManager.getLogger())
 *   - Configuration holders (single source of truth for app config)
 *   - Connection pools (only one pool per data source)
 *   - ParkingLot in LLD (see com/interview/lld/parkinglot/)
 *
 * THE 4 VARIANTS:
 *   1. Eager Initialization    — simple, thread-safe, but wastes memory if unused
 *   2. Synchronized Method     — thread-safe but slow (lock on every call)
 *   3. Double-Checked Locking  — fast + thread-safe; requires volatile
 *   4. Enum Singleton          — best: JVM guarantees uniqueness, handles serialization
 *
 * INTERVIEW FREQUENCY: Very high. Almost always asked in Lead/Senior Java rounds.
 *
 * COMMON INTERVIEW QUESTIONS:
 *   1. "Why is volatile needed in double-checked locking?"
 *      Without volatile, the JVM can reorder the instructions in instance = new Singleton()
 *      (allocate memory → assign reference → run constructor). Thread B might see a non-null
 *      but not-yet-initialized instance. volatile prevents this reordering.
 *   2. "How do you break Singleton?" → Reflection, Serialization, Cloning.
 *      Enum Singleton is the only variant immune to all three.
 *   3. "Is Spring's @Singleton the same as Singleton pattern?"
 *      No! Spring's singleton is per-ApplicationContext (one per context), not per JVM.
 *   4. "What is a Bill Pugh Singleton / Initialization-on-Demand Holder?"
 *      → Variant 5 (bonus below) — lazy + thread-safe without synchronization overhead.
 */
public class SingletonDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Variant 1: Eager Singleton ===");
        EagerSingleton e1 = EagerSingleton.getInstance();
        EagerSingleton e2 = EagerSingleton.getInstance();
        System.out.println("Same instance: " + (e1 == e2)); // Expected: true

        System.out.println("\n=== Variant 2: Synchronized Method Singleton ===");
        SynchronizedSingleton s1 = SynchronizedSingleton.getInstance();
        SynchronizedSingleton s2 = SynchronizedSingleton.getInstance();
        System.out.println("Same instance: " + (s1 == s2)); // Expected: true

        System.out.println("\n=== Variant 3: Double-Checked Locking Singleton ===");
        DCLSingleton d1 = DCLSingleton.getInstance();
        DCLSingleton d2 = DCLSingleton.getInstance();
        System.out.println("Same instance: " + (d1 == d2)); // Expected: true

        System.out.println("\n=== Variant 4: Enum Singleton (RECOMMENDED) ===");
        EnumSingleton enum1 = EnumSingleton.INSTANCE;
        EnumSingleton enum2 = EnumSingleton.INSTANCE;
        System.out.println("Same instance: " + (enum1 == enum2)); // Expected: true
        enum1.doWork();

        System.out.println("\n=== Variant 5 (Bonus): Bill Pugh / Holder Singleton ===");
        BillPughSingleton bp1 = BillPughSingleton.getInstance();
        BillPughSingleton bp2 = BillPughSingleton.getInstance();
        System.out.println("Same instance: " + (bp1 == bp2)); // Expected: true

        System.out.println("\n--- Summary ---");
        System.out.println("Use Enum Singleton in production (safe from reflection/serialization).");
        System.out.println("Use Double-Checked Locking if Enum is not suitable (e.g., need to extend a class).");
    }

    // =========================================================================
    // VARIANT 1: Eager Initialization
    // =========================================================================

    /**
     * Instance is created when the class is loaded — before any call to getInstance().
     *
     * PROS: Simple, thread-safe (JVM guarantees class loading is thread-safe).
     * CONS: Instance is created even if never used → wastes resources.
     *       Also, if the constructor throws a checked exception, you can't catch it here.
     *
     * WHEN TO USE: When the singleton is always needed and creation is cheap.
     */
    static class EagerSingleton {
        // Created at class load time — JVM guarantees only one class loader call
        private static final EagerSingleton INSTANCE = new EagerSingleton();

        private EagerSingleton() {
            // Private constructor prevents external instantiation
            System.out.println("  EagerSingleton: instance created");
        }

        public static EagerSingleton getInstance() {
            return INSTANCE; // No synchronization needed — INSTANCE is final and already created
        }
    }

    // =========================================================================
    // VARIANT 2: Synchronized Method (Thread-Safe but Slow)
    // =========================================================================

    /**
     * Uses synchronized on the whole getInstance() method.
     *
     * PROS: Thread-safe, lazy initialization (created only when first requested).
     * CONS: Every call to getInstance() acquires a lock — unnecessary after first init.
     *   In high-throughput systems (e.g., accessed millions of times/sec), this is a bottleneck.
     *
     * WHEN TO USE: When simplicity is preferred over performance.
     */
    static class SynchronizedSingleton {
        private static SynchronizedSingleton instance;

        private SynchronizedSingleton() {
            System.out.println("  SynchronizedSingleton: instance created");
        }

        // synchronized = only one thread can execute this method at a time
        public static synchronized SynchronizedSingleton getInstance() {
            if (instance == null) {
                instance = new SynchronizedSingleton();
            }
            return instance;
        }
    }

    // =========================================================================
    // VARIANT 3: Double-Checked Locking (DCL) — Fast + Thread-Safe
    // =========================================================================

    /**
     * Optimizes Variant 2: synchronize ONLY on the first creation, not every call.
     *
     * WHY TWO null CHECKS?
     *   First check (outside sync): Avoids locking on every call after initialization.
     *     Most calls hit the fast path: instance != null → return immediately.
     *   Second check (inside sync): Handles the race condition where two threads
     *     both pass the first check simultaneously. Without the second check,
     *     both would create a new instance.
     *
     * WHY volatile?
     *   "instance = new DCLSingleton()" compiles to 3 steps:
     *     1. allocate memory
     *     2. call constructor (initialize fields)
     *     3. assign address to 'instance' variable
     *   Without volatile, JVM can reorder steps 2 and 3 (valid for single-thread).
     *   Thread B might see 'instance != null' (step 3 done) but read an
     *   uninitialized object (step 2 not yet done) → subtle, hard-to-reproduce bug.
     *   volatile prevents reordering and ensures visibility across CPU caches.
     *
     * WHEN TO USE: High-performance lazy singleton in multi-threaded environments.
     */
    static class DCLSingleton {
        // volatile: prevents instruction reordering during object construction
        private static volatile DCLSingleton instance;

        private DCLSingleton() {
            System.out.println("  DCLSingleton: instance created");
        }

        public static DCLSingleton getInstance() {
            if (instance == null) {                     // First check — fast path (no lock)
                synchronized (DCLSingleton.class) {    // Lock only if instance might be null
                    if (instance == null) {             // Second check — safe to create
                        instance = new DCLSingleton();
                    }
                }
            }
            return instance;
        }
    }

    // =========================================================================
    // VARIANT 4: Enum Singleton — THE RECOMMENDED APPROACH (Effective Java Item 3)
    // =========================================================================

    /**
     * Uses Java's enum to guarantee singleton behavior.
     *
     * WHY ENUM IS BEST:
     *   1. JVM guarantees each enum constant is instantiated exactly ONCE (class loading).
     *   2. Serialization: Enums are automatically handled by Java — no readResolve() needed.
     *      Other variants, when serialized and deserialized, create a NEW instance.
     *   3. Reflection: Cannot break it. Calling newInstance() on an enum throws an exception.
     *   4. Thread-safe: Same as eager initialization — JVM handles it.
     *
     * LIMITATION: Cannot extend another class (enums implicitly extend Enum<E>).
     *   If you need inheritance, use Double-Checked Locking instead.
     *
     * WHEN TO USE: Almost always — unless inheritance is required.
     */
    enum EnumSingleton {
        INSTANCE; // The one and only instance

        public void doWork() {
            System.out.println("  EnumSingleton: doing work. Instance: " + this);
        }
    }

    // =========================================================================
    // VARIANT 5 (BONUS): Bill Pugh / Initialization-on-Demand Holder
    // =========================================================================

    /**
     * Uses a static inner class to achieve lazy initialization without synchronization.
     *
     * HOW IT WORKS:
     *   The inner class SingletonHolder is NOT loaded when BillPughSingleton loads.
     *   It's loaded only when getInstance() is first called and SingletonHolder.INSTANCE
     *   is accessed for the first time.
     *   JVM guarantees static initializers run exactly once, thread-safely (class loading).
     *
     * PROS: Lazy + thread-safe + no synchronization overhead on getInstance() calls.
     * CONS: Not protected against reflection attacks (unlike Enum).
     *
     * INTERVIEW NOTE: If asked for the "best" lazy singleton without using Enum,
     *   this is the answer. It's cleaner than DCL.
     */
    static class BillPughSingleton {
        private BillPughSingleton() {
            System.out.println("  BillPughSingleton: instance created");
        }

        // Inner class is loaded lazily when getInstance() is first called
        private static class SingletonHolder {
            private static final BillPughSingleton INSTANCE = new BillPughSingleton();
        }

        public static BillPughSingleton getInstance() {
            return SingletonHolder.INSTANCE; // Triggers inner class loading on first call
        }
    }
}
