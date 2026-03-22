package com.java.java21;

/**
 * CONCEPT: Foreign Function and Memory API (FFM — Stable in Java 21, JEP 454)
 *
 * CORE IDEA: A safe, performant API to:
 *   1. Allocate and access memory OUTSIDE the Java heap (off-heap / native memory)
 *   2. Call native C functions directly without JNI boilerplate
 *
 * This API replaces the dangerous sun.misc.Unsafe which had no bounds checking,
 * no lifetime management, and could crash the JVM with a bad pointer.
 *
 * WHEN TO USE:
 *   - Large buffers where GC pressure is unacceptable (>100MB working sets)
 *   - Interop with native libraries (C, C++, Rust) via JNI-free binding
 *   - Sharing memory with other processes (mmap-style)
 *   - High-performance data structures that need predictable layout (like C structs)
 *   - Zero-copy I/O with OS (memory-mapped files, network buffers)
 *
 * TIME/SPACE:
 *   - Off-heap allocation: O(1) time, exact space (no GC overhead)
 *   - Accessing elements: O(1) with VarHandle
 *   - The big win: NO GARBAGE COLLECTION for off-heap memory (manual lifetime via Arena)
 *
 * INTERVIEW ANGLE:
 *   Q: Why use off-heap memory instead of byte[]?
 *   A: The JVM heap is managed by GC. For very large buffers (Netty, Cassandra),
 *      keeping them on-heap causes long GC pauses ("stop the world") because
 *      the GC must scan/move them. Off-heap memory is never touched by GC.
 *      Trade-off: you must manage its lifetime manually via Arena (like malloc/free in C).
 *
 *   Q: How is FFM different from sun.misc.Unsafe?
 *   A: sun.misc.Unsafe has NO bounds checking — writing past the end of a segment
 *      corrupts memory and can crash the JVM. FFM is bounds-checked: accessing
 *      outside the segment throws IndexOutOfBoundsException safely.
 *      Also, Unsafe has no lifetime management; FFM uses Arena for deterministic cleanup.
 *
 *   Q: What is a MemoryLayout?
 *   A: It describes the structure of memory — like a C struct definition in Java.
 *      It specifies field types, names, offsets, and alignment. With MemoryLayout
 *      you get type-safe VarHandles to read/write specific fields by name.
 *
 *   Q: What is an Arena?
 *   A: The lifetime manager for off-heap memory segments. When you close an Arena
 *      (try-with-resources), all segments allocated from it are freed.
 *      Three types: confined (single-thread), shared (multi-thread), global (never freed).
 *
 * REAL-WORLD USE:
 *   - Netty: off-heap byte buffers for zero-copy network I/O
 *   - Apache Cassandra: off-heap memtable for predictable latency
 *   - Apache Arrow: columnar data in off-heap memory for analytics
 *   - Panama: replacing all JNI bindings with FFM-based bindings
 *
 * GOTCHA:
 *   - Arena must be closed to free memory! Use try-with-resources.
 *   - Arena.ofConfined() is NOT thread-safe. Use Arena.ofShared() for multi-thread access.
 *   - MemorySegment.get/set require the correct ValueLayout — mixing types = exception.
 *   - Off-heap memory is NOT garbage collected — leaks if Arena is never closed.
 *   - Arena.global() never frees — use only for application-lifetime static data.
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.*;

public class ForeignMemoryAPIDemo {

    // -------------------------------------------------------------------------
    // SECTION 1: Arena Types — Lifetime Management
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: Arena is the CORNERSTONE of the FFM API.
     * It defines the lifetime of off-heap memory allocations.
     * When the Arena closes, ALL memory allocated from it is freed atomically.
     *
     * ARENA TYPES:
     *
     *   Arena.ofConfined()
     *     - Single-threaded access only. Fast because no synchronization needed.
     *     - SegmentScope is tied to current thread. Accessing from another thread → exception.
     *     - Use for: all off-heap operations on one thread (e.g., per-request buffer).
     *
     *   Arena.ofShared()
     *     - Multi-threaded access. Uses concurrent reference counting.
     *     - Slightly slower due to synchronization overhead.
     *     - Use for: shared buffers accessed by multiple threads (e.g., Netty ChannelBuffer).
     *
     *   Arena.global()
     *     - Never freed (lives for JVM lifetime).
     *     - No close() method.
     *     - Use for: static/constant data loaded once (e.g., JNI symbol table).
     *
     *   Arena.ofAuto()
     *     - Freed by GC (non-deterministic, like a WeakReference).
     *     - Last resort — prefer deterministic arenas.
     *     - Use for: small segments where close() is inconvenient and exact timing doesn't matter.
     */
    public static void demonstrateArenaTypes() {
        System.out.println("=== Arena Types Demo ===");

        // CONFINED ARENA: fastest, single-thread
        System.out.println("--- Confined Arena (single-thread) ---");
        try (Arena confined = Arena.ofConfined()) {
            // Allocate 1024 bytes off-heap
            MemorySegment segment = confined.allocate(1024);
            System.out.println("Confined segment: " + segment);
            System.out.println("  Byte size: " + segment.byteSize());
            System.out.println("  Is native (off-heap): " + !segment.isNative() + " (actually: " + segment.isNative() + ")");
        }
        // segment is freed here — try-with-resources calls confined.close()
        System.out.println("Confined arena closed — memory freed");

        // SHARED ARENA: multi-thread safe
        System.out.println("\n--- Shared Arena (multi-thread) ---");
        try (Arena shared = Arena.ofShared()) {
            MemorySegment segment = shared.allocate(2048);
            System.out.println("Shared segment: " + segment.byteSize() + " bytes");
            // Multiple threads can safely access this segment
        }

        // GLOBAL ARENA: never freed
        System.out.println("\n--- Global Arena (never freed) ---");
        // Arena.global() — use for static data only
        // MemorySegment staticData = Arena.global().allocate(64);
        // staticData lives for JVM lifetime — no close() available
        System.out.println("Global arena: no close(), lives for JVM lifetime");
        System.out.println("Use for: static constants, JNI symbol tables");
    }

    // -------------------------------------------------------------------------
    // SECTION 2: Off-Heap Array of Integers
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: Off-heap arrays avoid GC pressure for large data.
     * A 100M element int[] on the heap means the GC must scan/move 400MB on every GC.
     * Off-heap: GC never touches it. Predictable latency.
     *
     * ACCESSING ELEMENTS:
     *   segment.setAtIndex(ValueLayout.JAVA_INT, index, value) — write
     *   segment.getAtIndex(ValueLayout.JAVA_INT, index)        — read
     *   Index is element-wise (not byte-wise) — same semantics as array[index].
     *
     * VALUETYPES:
     *   ValueLayout.JAVA_INT    — 4 bytes, maps to int
     *   ValueLayout.JAVA_LONG   — 8 bytes, maps to long
     *   ValueLayout.JAVA_FLOAT  — 4 bytes, maps to float
     *   ValueLayout.JAVA_DOUBLE — 8 bytes, maps to double
     *   ValueLayout.JAVA_BYTE   — 1 byte, maps to byte
     *   ValueLayout.ADDRESS     — pointer size (4 or 8 bytes), maps to MemorySegment
     */
    public static long offHeapArraySum(int elementCount) {
        System.out.println("\n=== Off-Heap Int Array Demo ===");
        System.out.println("Allocating " + elementCount + " ints off-heap ("
                           + (elementCount * 4L / 1024) + " KB)");

        try (Arena arena = Arena.ofConfined()) {
            // Allocate: elementCount * 4 bytes (int = 4 bytes = ValueLayout.JAVA_INT.byteSize())
            MemorySegment segment = arena.allocate(
                ValueLayout.JAVA_INT.byteSize() * elementCount
            );

            long fillStart = System.nanoTime();
            // Fill with values 0, 1, 2, ..., elementCount-1
            for (int i = 0; i < elementCount; i++) {
                // setAtIndex: element-wise indexing (not byte offset)
                // Equivalent to: array[i] = i
                segment.setAtIndex(ValueLayout.JAVA_INT, i, i);
            }
            long fillTime = System.nanoTime() - fillStart;

            // Sum all elements
            long sumStart = System.nanoTime();
            long sum = 0;
            for (int i = 0; i < elementCount; i++) {
                // getAtIndex: element-wise indexing
                // Equivalent to: sum += array[i]
                sum += segment.getAtIndex(ValueLayout.JAVA_INT, i);
            }
            long sumTime = System.nanoTime() - sumStart;

            System.out.printf("  Fill time: %.2f ms%n", fillTime / 1_000_000.0);
            System.out.printf("  Sum time:  %.2f ms%n", sumTime / 1_000_000.0);
            System.out.println("  Sum: " + sum);

            // Mathematical check: sum of 0..n-1 = n*(n-1)/2
            long expected = (long) elementCount * (elementCount - 1) / 2;
            System.out.println("  Expected: " + expected);
            System.out.println("  Correct: " + (sum == expected));

            return sum;
        }
        // Arena.close() called here — all memory freed
    }

    // -------------------------------------------------------------------------
    // SECTION 3: Heap vs Off-Heap Performance Comparison
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: For large arrays, the performance difference comes not from
     * access speed (similar) but from GC behavior.
     *   - On-heap: GC must scan array on every collection. With 1GB arrays, this
     *     causes multi-second "stop the world" pauses.
     *   - Off-heap: GC never touches it. No pause regardless of size.
     *
     * BENCHMARK NOTE:
     *   Access speed: int[] ≈ off-heap (both are cache-line friendly sequential access)
     *   GC pressure: int[] = yes | off-heap = zero
     *   Memory layout: int[] = on-heap object with header | off-heap = raw bytes, no header
     */
    public static void compareHeapVsOffHeap(int elementCount) {
        System.out.println("\n=== Heap vs Off-Heap Comparison ===");
        System.out.println("Element count: " + elementCount);

        // ON-HEAP: standard int array
        long heapStart = System.nanoTime();
        int[] heapArray = new int[elementCount];
        for (int i = 0; i < elementCount; i++) heapArray[i] = i;
        long heapSum = 0;
        for (int v : heapArray) heapSum += v;
        long heapTime = System.nanoTime() - heapStart;

        // OFF-HEAP: MemorySegment
        long offStart = System.nanoTime();
        long offSum;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(ValueLayout.JAVA_INT.byteSize() * elementCount);
            for (int i = 0; i < elementCount; i++) seg.setAtIndex(ValueLayout.JAVA_INT, i, i);
            offSum = 0;
            for (int i = 0; i < elementCount; i++) offSum += seg.getAtIndex(ValueLayout.JAVA_INT, i);
        }
        long offTime = System.nanoTime() - offStart;

        System.out.printf("  Heap (int[]):    %.2f ms, sum=%d%n", heapTime / 1_000_000.0, heapSum);
        System.out.printf("  Off-heap (FFM):  %.2f ms, sum=%d%n", offTime / 1_000_000.0, offSum);
        System.out.println("  Note: raw speed similar; the REAL win is zero GC pressure for large arrays");
    }

    // -------------------------------------------------------------------------
    // SECTION 4: MemoryLayout — Struct-Like Memory Description
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: MemoryLayout describes the structure of a region of memory,
     * like a C struct definition. It lets you:
     *   1. Compute byte offsets of named fields automatically
     *   2. Create type-safe VarHandles to read/write fields
     *   3. Ensure correct alignment (platform-specific padding)
     *
     * ANALOGY: If MemorySegment is a "raw memory buffer",
     *   then MemoryLayout is the "schema" that gives that buffer structure.
     *
     * EXAMPLE LAYOUT for a C struct:
     *   struct Point {
     *       int x;    // 4 bytes
     *       int y;    // 4 bytes
     *   };            // Total: 8 bytes
     *
     * JAVA FFM EQUIVALENT:
     *   StructLayout POINT_LAYOUT = MemoryLayout.structLayout(
     *       ValueLayout.JAVA_INT.withName("x"),
     *       ValueLayout.JAVA_INT.withName("y")
     *   );
     *
     * VARHANDLE: type-safe accessor derived from layout
     *   VarHandle X_HANDLE = POINT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x"));
     *   X_HANDLE.set(pointSegment, 0L, 10);  // set x = 10
     *   int x = (int) X_HANDLE.get(pointSegment, 0L);  // get x
     *
     * INTERVIEW INSIGHT:
     *   MemoryLayout is the Java equivalent of C's struct declaration.
     *   It enables true C struct interop without manual offset arithmetic.
     *   Before FFM, JNI required brittle manual offset calculations.
     */
    public static void demonstrateMemoryLayout() {
        System.out.println("\n=== MemoryLayout Demo (C struct equivalent) ===");

        // Define a Point struct layout: { int x; int y; }
        StructLayout POINT_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("x"),
            ValueLayout.JAVA_INT.withName("y")
        );

        System.out.println("Point struct layout: " + POINT_LAYOUT);
        System.out.println("  Total byte size: " + POINT_LAYOUT.byteSize() + " bytes");  // 8
        System.out.println("  x offset: " + POINT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("x")));  // 0
        System.out.println("  y offset: " + POINT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("y")));  // 4

        // Create VarHandles for type-safe field access
        VarHandle X_HANDLE = POINT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x"));
        VarHandle Y_HANDLE = POINT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("y"));

        // Allocate and use a single Point
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment point = arena.allocate(POINT_LAYOUT);

            // Write fields using VarHandles (type-safe, bounds-checked)
            X_HANDLE.set(point, 0L, 42);   // point.x = 42
            Y_HANDLE.set(point, 0L, 100);  // point.y = 100

            // Read fields
            int x = (int) X_HANDLE.get(point, 0L);
            int y = (int) Y_HANDLE.get(point, 0L);
            System.out.println("  Point: (" + x + ", " + y + ")");  // (42, 100)
        }

        // Array of Points (SequenceLayout)
        SequenceLayout POINTS_LAYOUT = MemoryLayout.sequenceLayout(5, POINT_LAYOUT);
        System.out.println("\n  Array of 5 Points layout: " + POINTS_LAYOUT.byteSize() + " bytes");

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment points = arena.allocate(POINTS_LAYOUT);

            // Write 5 points
            for (int i = 0; i < 5; i++) {
                // For array access: offset = index * structByteSize
                long offset = (long) i * POINT_LAYOUT.byteSize();
                X_HANDLE.set(points, offset, i * 10);
                Y_HANDLE.set(points, offset, i * 20);
            }

            // Read and print
            System.out.print("  Points: ");
            for (int i = 0; i < 5; i++) {
                long offset = (long) i * POINT_LAYOUT.byteSize();
                int x = (int) X_HANDLE.get(points, offset);
                int y = (int) Y_HANDLE.get(points, offset);
                System.out.print("(" + x + "," + y + ") ");
            }
            System.out.println();
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 5: FFM vs sun.misc.Unsafe — Safety Comparison
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: The fundamental safety difference between FFM and Unsafe.
     *
     * sun.misc.Unsafe:
     *   long addr = unsafe.allocateMemory(1024);   // raw pointer — no bounds
     *   unsafe.putInt(addr + 9999, 42);            // write PAST the end → JVM CRASH
     *   // No exception thrown. The JVM writes to arbitrary memory.
     *   // Best case: segfault. Worst case: silent data corruption.
     *
     * FFM (MemorySegment):
     *   MemorySegment seg = arena.allocate(1024);
     *   seg.set(ValueLayout.JAVA_INT, 9999, 42);   // IndexOutOfBoundsException!
     *   // Safe: bounds are checked on EVERY access.
     *   // Cannot write past the end. Cannot use after Arena.close() (IllegalStateException).
     *
     * ADDITIONAL FFM SAFETY FEATURES:
     *   - Temporal safety: closed segment access → IllegalStateException (not crash)
     *   - Spatial safety: out-of-bounds access → IndexOutOfBoundsException (not crash)
     *   - Type safety: VarHandle enforces correct type at every access
     *   - Thread safety: confined arena throws if accessed from wrong thread
     *
     * WHY UNSAFE STILL EXISTS:
     *   - Legacy code (Netty, Cassandra, Kryo) used Unsafe for years
     *   - Migration to FFM takes time
     *   - JDK 23+ plans to restrict Unsafe access with stronger warnings
     */
    public static void demonstrateSafetyFeatures() {
        System.out.println("\n=== FFM Safety Features Demo ===");

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(16); // 16 bytes
            segment.set(ValueLayout.JAVA_INT, 0, 42);  // write at offset 0 — OK

            System.out.println("  Valid write at offset 0: " + segment.get(ValueLayout.JAVA_INT, 0));

            // Attempt out-of-bounds access
            try {
                segment.set(ValueLayout.JAVA_INT, 100, 999); // offset 100 > size 16!
                System.out.println("  ERROR: should have thrown!");
            } catch (IndexOutOfBoundsException e) {
                System.out.println("  Out-of-bounds correctly caught: " + e.getMessage());
            }
        }

        // Attempt use-after-free (after Arena closes)
        MemorySegment freed;
        try (Arena arena = Arena.ofConfined()) {
            freed = arena.allocate(16);
        }
        // Arena is closed — freed segment is invalid
        try {
            freed.get(ValueLayout.JAVA_INT, 0); // should throw
        } catch (IllegalStateException e) {
            System.out.println("  Use-after-free correctly caught: " + e.getMessage());
        }

        System.out.println("  sun.misc.Unsafe: no bounds check = JVM crash possible");
        System.out.println("  FFM MemorySegment: all accesses bounds-checked = safe");
    }

    // -------------------------------------------------------------------------
    // SECTION 6: Linker — Calling Native C Functions (conceptual)
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: The Linker is the other half of the FFM API.
     * It lets you call native C functions directly, without writing JNI C code.
     *
     * BEFORE FFM (JNI approach):
     *   1. Write Java with native keyword: public native int strlen(String s);
     *   2. Run javac → javah to generate .h file
     *   3. Write C code implementing Java_MyClass_strlen(JNIEnv*, jobject, jstring)
     *   4. Compile C to .so/.dll
     *   5. Load library: System.loadLibrary("mylib")
     *   6. Call from Java
     *   → 5+ steps, C code required, fragile, JNI naming conventions
     *
     * AFTER FFM (Linker approach):
     *   Linker linker = Linker.nativeLinker();
     *   SymbolLookup stdlib = linker.defaultLookup();
     *
     *   // Find the C 'strlen' function
     *   MemorySegment strlenAddr = stdlib.find("strlen").orElseThrow();
     *
     *   // Describe C function signature: (pointer) -> long
     *   FunctionDescriptor desc = FunctionDescriptor.of(
     *       ValueLayout.JAVA_LONG,    // return type: long (size_t)
     *       ValueLayout.ADDRESS       // param: const char* (pointer)
     *   );
     *
     *   // Create Java MethodHandle to call it
     *   MethodHandle strlen = linker.downcallHandle(strlenAddr, desc);
     *
     *   // Use it:
     *   try (Arena arena = Arena.ofConfined()) {
     *       MemorySegment cStr = arena.allocateFrom("Hello, World!");
     *       long len = (long) strlen.invokeExact(cStr);
     *       // → 13L
     *   }
     *   → No C code needed! Pure Java FFM binding.
     *
     * JNI vs FFM:
     * ┌──────────────────┬──────────────────────┬────────────────────────┐
     * │ Feature          │ JNI                  │ FFM Linker             │
     * ├──────────────────┼──────────────────────┼────────────────────────┤
     * │ C code needed?   │ Yes (always)         │ No (pure Java)         │
     * │ Type safety      │ Low (void pointers)  │ High (FunctionDescriptor)│
     * │ Boilerplate      │ High                 │ Minimal                │
     * │ Performance      │ Good                 │ Better (no JNI frame)  │
     * │ Safety           │ JVM can crash        │ Bounds-checked         │
     * │ Tooling          │ jextract (helper)    │ jextract (auto-gen)    │
     * └──────────────────┴──────────────────────┴────────────────────────┘
     */
    public static void demonstrateLinkerConcept() {
        System.out.println("\n=== Linker / Native Call Concept ===");
        System.out.println("Linker enables calling C functions directly from Java:");
        System.out.println("  1. Linker.nativeLinker() — get platform linker");
        System.out.println("  2. stdlib.find(\"strlen\") — resolve C symbol");
        System.out.println("  3. FunctionDescriptor.of(JAVA_LONG, ADDRESS) — describe signature");
        System.out.println("  4. linker.downcallHandle(addr, desc) — get MethodHandle");
        System.out.println("  5. handle.invokeExact(cStringSegment) — call it");
        System.out.println("No C code, no .h files, no JNI naming hell.");
        System.out.println("jextract tool auto-generates FFM bindings from .h files.");
    }

    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== Foreign Memory API (FFM) Demo ===");
        System.out.println("Stable in Java 21 (JEP 454)");
        System.out.println();

        demonstrateArenaTypes();

        // Demo: off-heap array of 1 million ints (~4MB)
        long sum = offHeapArraySum(1_000_000);
        System.out.println("1M int sum: " + sum);

        compareHeapVsOffHeap(500_000);

        demonstrateMemoryLayout();
        demonstrateSafetyFeatures();
        demonstrateLinkerConcept();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("MemorySegment: safe, bounds-checked off-heap memory region");
        System.out.println("Arena: lifetime manager (confined/shared/global/auto)");
        System.out.println("MemoryLayout: describes structure (like C struct), enables VarHandles");
        System.out.println("VarHandle: type-safe field accessor derived from layout");
        System.out.println("Linker: call native C functions without JNI C code");
        System.out.println("Key win over Unsafe: bounds-checked, temporal safety, type-safe");
        System.out.println("Key use cases: Netty, Cassandra, Arrow — large buffers, zero GC pressure");
    }
}
