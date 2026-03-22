package com.java.jvm;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * ============================================================
 * JVM GARBAGE COLLECTION — Concepts, Algorithms, Tuning
 * ============================================================
 *
 * CONCEPT:
 *   JVM Garbage Collection automatically manages memory — finds and reclaims
 *   objects that are no longer reachable. "Reachable" = accessible from any
 *   live thread, static field, or JNI reference via a chain of references.
 *
 * GENERATIONS OVERVIEW:
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │ YOUNG GENERATION (short-lived objects — most objects die here)│
 *   │   Eden Space  │ Survivor 0 (S0) │ Survivor 1 (S1)           │
 *   ├──────────────────────────────────────────────────────────────┤
 *   │ OLD/TENURED GENERATION (objects that survived many GC cycles) │
 *   ├──────────────────────────────────────────────────────────────┤
 *   │ METASPACE (class metadata, replaced PermGen in Java 8+)      │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * OBJECT LIFECYCLE:
 *   1. Object allocated in Eden
 *   2. Eden fills → Minor GC runs (stop-the-world, fast ~1-10ms)
 *      Live objects moved to S0 or S1 (alternating). Age incremented.
 *   3. Objects surviving N minor GCs (age > tenuring threshold) → promoted to Old.
 *   4. Old gen fills → Major GC (stop-the-world, slow ~100ms-seconds)
 *   5. Full GC: collects BOTH young and old gen (most disruptive)
 *
 * GC ALGORITHMS COMPARISON:
 * ┌─────────────────┬──────────┬───────────┬──────────────┬──────────┐
 * │ Algorithm       │ Threads  │ STW?      │ Use Case     │ Java ver │
 * ├─────────────────┼──────────┼───────────┼──────────────┼──────────┤
 * │ Serial (-XX:+UseSerialGC)  │ 1 │ Yes (all) │ <100MB heap, single-core │ All  │
 * │ Parallel (-XX:+UseParallelGC) │ N │ Yes (all) │ Throughput-first │ Default 8 │
 * │ G1GC (-XX:+UseG1GC) │ N │ Mostly concurrent │ Balanced │ Default 9+ │
 * │ ZGC (-XX:+UseZGC)   │ N │ <1ms pause │ Large heaps, latency │ 15+  │
 * │ Shenandoah      │ N │ <10ms pause │ Low pause   │ 12+  │
 * └─────────────────┴──────────┴───────────┴──────────────┴──────────┘
 *
 * INTERVIEW ANGLE:
 *   - "What causes a memory leak in Java?" → Even with GC, you CAN have leaks
 *     if reachable objects accumulate (static collections, listeners, ThreadLocal).
 *   - "Difference between Minor GC and Full GC?" → Minor: only Young gen.
 *     Full: entire heap, including Old gen. Full GC is the most disruptive.
 *   - "Why G1GC over Parallel GC?" → G1GC targets pause time (-XX:MaxGCPauseMillis).
 *     Parallel GC maximizes throughput but pauses can be long.
 *
 * GC TUNING FLAGS CHEAT SHEET:
 *   -Xms<size>                   Initial heap size (e.g., -Xms256m)
 *   -Xmx<size>                   Max heap size (e.g., -Xmx4g)
 *   -XX:NewRatio=N               Old:Young ratio (default 2 → Young=33% of heap)
 *   -XX:SurvivorRatio=N          Eden:Survivor ratio in Young gen (default 8)
 *   -XX:MaxGCPauseMillis=200     G1GC pause target
 *   -XX:G1HeapRegionSize=N       G1 region size (1-32MB, power of 2)
 *   -XX:+PrintGCDetails          Print GC logs (old style)
 *   -Xlog:gc*                    Unified GC logging (Java 9+)
 *   -XX:+HeapDumpOnOutOfMemoryError  Auto heap dump on OOM
 *   -XX:HeapDumpPath=/path/dump.hprof
 *   -XX:MaxMetaspaceSize=256m    Limit Metaspace (prevents Metaspace OOM)
 *   -XX:+UseStringDeduplication  G1GC: dedup identical String objects (saves memory)
 */
public class GarbageCollectionDemo {

    // =========================================================
    // MEMORY LEAK PATTERNS — with code examples
    // =========================================================
    //
    // Java GC collects UNREACHABLE objects. Memory "leak" = object is reachable
    // but shouldn't be. The heap grows because live objects accumulate.
    //
    // PATTERN 1: Static collection that grows indefinitely
    //   CAUSE: Static field lives for the entire JVM lifetime.
    //          If you add to it and never remove, it grows forever.
    //   SYMPTOMS: Gradual heap growth, eventual OutOfMemoryError.
    //   FIX: Use bounded collections (LinkedHashMap with removeEldestEntry),
    //        or explicitly remove entries when done.

    // LEAK: This static list grows with each call and is never cleared
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final List<byte[]> MEMORY_HOG = new ArrayList<>(); // LEAK!

    // FIX: Bounded cache with eviction
    private static final int MAX_CACHE_SIZE = 100;
    private static final LinkedHashMap<String, byte[]> BOUNDED_CACHE = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > MAX_CACHE_SIZE; // evict oldest when over limit
        }
    };

    // PATTERN 2: Event listener not deregistered
    //   CAUSE: Event source holds strong reference to listener.
    //          If you add a listener but never remove it, the listener
    //          (and everything it references) can't be GC'd.
    //   COMMON IN: Swing event listeners, Observer pattern, Spring ApplicationListeners
    //   FIX: Always deregister listeners (removeActionListener, removeObserver).
    //        Use WeakReference to listener (listener dies when no other references).

    interface EventListener { void onEvent(String event); }

    static class EventSource {
        // LEAK: Strong references to listeners prevent GC
        private final List<EventListener> listeners = new ArrayList<>();
        // FIX: Weak references — listener can be GC'd even if still in this list
        private final List<WeakReference<EventListener>> weakListeners = new ArrayList<>();

        void addListener(EventListener l) { listeners.add(l); }
        void addWeakListener(EventListener l) { weakListeners.add(new WeakReference<>(l)); }

        void fire(String event) {
            listeners.forEach(l -> l.onEvent(event));
            // For weak listeners: must check if still alive
            weakListeners.removeIf(ref -> ref.get() == null); // clean dead refs
            weakListeners.stream()
                .map(WeakReference::get)
                .filter(Objects::nonNull)
                .forEach(l -> l.onEvent(event));
        }
    }

    // PATTERN 3: Non-static inner class captures outer class reference
    //   CAUSE: Non-static inner class and anonymous classes hold implicit reference
    //          to the outer class instance.
    //   CLASSIC: new Thread(() -> ...) where lambda captures outer `this` implicitly.
    //   FIX: Use static inner classes, or pass needed data explicitly (not outer `this`).

    static class OuterService {
        private byte[] heavyState = new byte[1024 * 1024]; // 1MB

        Runnable createLeakyTask() {
            // LEAK: Anonymous Runnable implicitly holds reference to OuterService instance.
            //       If the Runnable lives longer than OuterService should, heavyState is retained.
            return new Runnable() {  // inner class: holds reference to OuterService.this
                @Override public void run() {
                    System.out.println("Task running, heavyState.length=" + heavyState.length);
                }
            };
        }

        static Runnable createNonLeakyTask(int dataLength) {
            // FIX: Static method + pass only what you need. No outer reference.
            return () -> System.out.println("Task running, dataLength=" + dataLength);
        }
    }

    // PATTERN 4: ThreadLocal not removed (critical in thread pools!)
    //   CAUSE: ThreadLocal values persist for the thread's lifetime.
    //          In a thread pool, threads are reused across requests.
    //          ThreadLocal from request 1 still exists in thread during request 2!
    //   SYMPTOMS: Cross-request data contamination, memory leak accumulating per thread.
    //   FIX: Always call threadLocal.remove() in a finally block.

    private static final ThreadLocal<String> REQUEST_CONTEXT = new ThreadLocal<>();

    static void processRequest(String userId) {
        REQUEST_CONTEXT.set(userId);
        try {
            System.out.println("Processing for: " + REQUEST_CONTEXT.get());
            // ... business logic ...
        } finally {
            REQUEST_CONTEXT.remove(); // CRITICAL: always remove in finally!
            // Without this, the thread pool thread retains the value after this request.
        }
    }

    // =========================================================
    // REFERENCE TYPES — WeakReference, SoftReference, PhantomReference
    // =========================================================
    //
    // REFERENCE STRENGTH (from strongest to weakest):
    //
    // STRONG REFERENCE (normal): Object obj = new Object();
    //   GC will NEVER collect it as long as a strong reference exists.
    //   Only becomes eligible when reference goes out of scope.
    //
    // SOFT REFERENCE: new SoftReference<>(obj)
    //   GC collects ONLY when LOW on memory (before throwing OutOfMemoryError).
    //   Perfect for MEMORY-SENSITIVE CACHES: auto-evicted under memory pressure.
    //   JVM tries to clear all soft refs before throwing OOM.
    //
    // WEAK REFERENCE: new WeakReference<>(obj)
    //   GC collects at ANY time when no strong/soft reference exists.
    //   Next GC cycle will collect it.
    //   Perfect for: canonicalized mappings (WeakHashMap), event listeners.
    //   WeakHashMap: entries auto-removed when key becomes weakly reachable.
    //
    // PHANTOM REFERENCE: new PhantomReference<>(obj, refQueue)
    //   GC collects the object, then enqueues the PhantomReference into refQueue.
    //   get() always returns null (no resurrection possible).
    //   Used for: post-mortem cleanup (replaces deprecated finalize()),
    //             off-heap memory management, resource cleanup after GC.
    //   Java 9+: Cleaner is a higher-level API using PhantomReferences internally.
    //
    // INTERVIEW Q: When would you use SoftReference?
    //   A: LRU cache in image viewer — keep images in memory if possible,
    //      but let GC evict them when heap pressure is high. Auto-eviction without
    //      explicit eviction policy logic.

    static void demonstrateReferences() throws InterruptedException {
        System.out.println("\n--- Reference Types Demo ---");

        // SOFT REFERENCE — survives most GC cycles
        SoftReference<byte[]> softRef = new SoftReference<>(new byte[1024]);
        System.out.println("SoftRef before GC: " + (softRef.get() != null ? "alive" : "collected"));
        System.gc();
        Thread.sleep(100);
        System.out.println("SoftRef after GC: " + (softRef.get() != null ? "alive (heap not pressured)" : "collected"));

        // WEAK REFERENCE — collected at next GC
        WeakReference<byte[]> weakRef = new WeakReference<>(new byte[1024]);
        System.out.println("WeakRef before GC: " + (weakRef.get() != null ? "alive" : "collected"));
        System.gc();
        Thread.sleep(100);
        System.out.println("WeakRef after GC: " + (weakRef.get() != null ? "alive" : "likely collected"));

        // PHANTOM REFERENCE — for post-mortem cleanup
        ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
        Object obj = new Object();
        PhantomReference<Object> phantomRef = new PhantomReference<>(obj, refQueue);
        System.out.println("PhantomRef.get() always null: " + phantomRef.get()); // always null!
        obj = null; // remove strong reference
        System.gc();
        Thread.sleep(100);
        Reference<?> queued = refQueue.poll();
        System.out.println("PhantomRef enqueued after GC: " + (queued != null));
        // At this point, do cleanup (close file, release off-heap memory, etc.)

        // WeakHashMap demo: keys auto-removed when weakly reachable
        Map<Object, String> weakMap = new WeakHashMap<>();
        Object key = new Object();
        weakMap.put(key, "value");
        System.out.println("WeakHashMap before GC: size=" + weakMap.size());
        key = null; // remove strong reference to key
        System.gc();
        Thread.sleep(100);
        System.out.println("WeakHashMap after GC: size=" + weakMap.size()); // 0 (key GC'd)
    }

    // =========================================================
    // GC ALGORITHMS — Conceptual Explanation
    // =========================================================

    static void printGCAlgorithms() {
        System.out.println("\n--- GC Algorithms Reference ---");

        System.out.println("1. SERIAL GC (-XX:+UseSerialGC)");
        System.out.println("   Single-threaded stop-the-world. Simple, no overhead.");
        System.out.println("   Use: batch jobs, small heaps (<100MB), embedded systems.");
        System.out.println("   Minor GC: mark-copy in Young gen. Major GC: mark-sweep-compact.");

        System.out.println("2. PARALLEL GC (-XX:+UseParallelGC, default Java 8)");
        System.out.println("   Multi-threaded stop-the-world. Maximizes throughput.");
        System.out.println("   Use: backend batch processing, throughput over latency.");
        System.out.println("   -XX:ParallelGCThreads=N to tune thread count.");

        System.out.println("3. G1GC (-XX:+UseG1GC, default Java 9+)");
        System.out.println("   Region-based (1-32MB regions). Concurrent marking.");
        System.out.println("   Predictable pause targets: -XX:MaxGCPauseMillis=200.");
        System.out.println("   Mixed collections: reclaim Old gen regions incrementally.");
        System.out.println("   Humongous objects (>50% region size) → special handling.");

        System.out.println("4. ZGC (-XX:+UseZGC, Java 15+ production-ready)");
        System.out.println("   Sub-millisecond pauses. Scales to terabyte heaps.");
        System.out.println("   Concurrent everything: marking, relocation, compaction.");
        System.out.println("   Uses colored pointers and load barriers.");
        System.out.println("   Trade-off: higher throughput overhead (~10-15%) for low pause.");

        System.out.println("5. Shenandoah (-XX:+UseShenandoahGC)");
        System.out.println("   Concurrent evacuation: copies objects while app runs.");
        System.out.println("   More CPU overhead than G1, but lower max pause than G1.");
        System.out.println("   Good for: streaming, real-time data processing.");
    }

    // =========================================================
    // MAIN
    // =========================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== JVM GARBAGE COLLECTION DEMO ===");

        System.out.println("\n--- Memory Leak Patterns ---");

        System.out.println("Pattern 1: Static Collection Leak");
        System.out.println("  AVOID: static List<byte[]> that grows indefinitely");
        System.out.println("  FIX: LinkedHashMap with removeEldestEntry for bounded cache");

        System.out.println("\nPattern 2: Listener Not Deregistered");
        EventSource source = new EventSource();
        EventListener listener = event -> System.out.println("  Received: " + event);
        source.addWeakListener(listener);
        source.fire("test event");
        System.out.println("  Using WeakReference for auto-cleanup when listener goes out of scope");

        System.out.println("\nPattern 3: Non-static Inner Class");
        OuterService outer = new OuterService();
        Runnable leaky = outer.createLeakyTask();
        Runnable clean = OuterService.createNonLeakyTask(42);
        System.out.println("  Leaky task retains OuterService reference: " + leaky.getClass().getEnclosingClass());
        System.out.println("  Clean task (lambda): " + clean.getClass().getEnclosingClass());

        System.out.println("\nPattern 4: ThreadLocal in Thread Pool");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> processRequest("user-123"));
        pool.submit(() -> processRequest("user-456"));
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("  ThreadLocal.remove() called in finally — no leak");

        demonstrateReferences();
        printGCAlgorithms();

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: Can you get a memory leak in Java?");
        System.out.println("A: Yes. GC only collects UNREACHABLE objects. If reachable objects");
        System.out.println("   accumulate (static collections, un-deregistered listeners,");
        System.out.println("   ThreadLocal in pools), heap grows → OOM.");
        System.out.println();
        System.out.println("Q: Minor GC vs Major GC vs Full GC?");
        System.out.println("A: Minor: collects Young gen only. Fast (few ms). STW brief.");
        System.out.println("   Major: collects Old gen. Slower. STW or concurrent depending on GC.");
        System.out.println("   Full GC: entire heap + Metaspace. Most disruptive. Avoid in prod.");
        System.out.println();
        System.out.println("Q: G1GC vs ZGC — when to choose?");
        System.out.println("A: G1GC: balanced, predictable pauses ~200ms, default choice.");
        System.out.println("   ZGC: need sub-10ms pauses (real-time, gaming, finance),");
        System.out.println("   heap > 8GB, willing to trade ~10% throughput for low latency.");
        System.out.println();
        System.out.println("Q: SoftReference vs WeakReference?");
        System.out.println("A: Soft: GC only on OOM — use for memory-sensitive caches.");
        System.out.println("   Weak: GC at any cycle — use for canonicalized mappings,");
        System.out.println("   WeakHashMap, avoiding listener retention.");
    }
}
