package com.java.generics;

/**
 * CONCEPT: Java Generics — Foundations
 *
 * CORE IDEA: Generics enable compile-time type safety for collections and algorithms,
 * eliminating ClassCastException at runtime and removing the need for explicit casts.
 * Implemented via TYPE ERASURE: generic type information is removed at compile time
 * and replaced with Object (or the bound type). At runtime, there are NO generics.
 *
 * WHEN TO USE:
 *   - Any data structure or algorithm that should work for multiple types
 *   - Utility methods (sort, search, transform) that should be type-safe
 *   - Whenever you find yourself writing the same logic for String and Integer separately
 *
 * TIME/SPACE: N/A (generics are a compile-time construct, zero runtime overhead)
 *
 * INTERVIEW ANGLE:
 *   Q: What is type erasure?
 *   A: Generics in Java are implemented at compile time ONLY. The compiler checks
 *      types and inserts casts, then erases all generic type information before
 *      generating bytecode. At runtime, List<String> and List<Integer> are both
 *      just List. You cannot use instanceof with generic types.
 *
 *   Q: Why can't you create a generic array (new T[])?
 *   A: Arrays are COVARIANT (String[] IS-A Object[]) and carry their component
 *      type at runtime (arrays are reified). Generics are INVARIANT and erased.
 *      Combining them breaks type safety:
 *        T[] arr = new T[10]; // if T=String, creates Object[] at runtime
 *        Object[] objArr = arr; // covariant — OK
 *        objArr[0] = 42; // compiles! but arr[0] is expected to be String → ClassCastException
 *      So Java disallows new T[] entirely.
 *
 *   Q: What is PECS?
 *   A: Producer Extends, Consumer Super.
 *      List<? extends Number>: you can READ (produce) Numbers from it. Cannot write.
 *      List<? super Integer>: you can WRITE (consume) Integers to it. Read returns Object.
 *
 *   Q: What are bridge methods?
 *   A: Compiler-generated methods that handle generic override situations.
 *      When a generic class's method is overridden with a specific type, the compiler
 *      generates a bridge method that delegates to the overriding method, enabling
 *      runtime polymorphism despite type erasure.
 *
 * REAL-WORLD USE:
 *   - Collections framework (ArrayList<T>, HashMap<K,V>)
 *   - Optional<T>, CompletableFuture<T>, Stream<T>
 *   - Generic algorithms: Comparator.comparing(), Collections.sort()
 *
 * GOTCHA:
 *   - At runtime: List<String> and List<Integer> are the SAME class → no type distinction
 *   - Cannot catch generic exceptions: catch (SomeException<T> e) → compile error
 *   - Cannot use primitives as type parameters: List<int> → use List<Integer> (boxing)
 *   - instanceof with generic type is compile error: o instanceof List<String> → use o instanceof List<?>
 */

import java.util.*;
import java.util.function.*;

public class GenericsFoundations {

    // =========================================================================
    // SECTION 1: Type Erasure
    // =========================================================================

    /**
     * KEY INSIGHT: Type erasure means generic info is compile-time only.
     * The bytecode generated for List<String> and List<Integer> is IDENTICAL.
     *
     * WHAT THE COMPILER DOES:
     *   Source code:  List<String> list = new ArrayList<>();
     *                 list.add("hello");
     *                 String s = list.get(0);  // no cast in source
     *
     *   After erasure: List list = new ArrayList();
     *                  list.add("hello");
     *                  String s = (String) list.get(0);  // compiler inserts cast
     *
     * RUNTIME EVIDENCE: These three expressions are all true:
     *   new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()  → true
     *   new ArrayList<String>() instanceof List<?>                                 → true
     *   new ArrayList<String>() instanceof List<String>                            → COMPILE ERROR
     *
     * PRACTICAL LIMITATIONS due to erasure:
     *   1. Cannot do: T obj = new T()         (no type info at runtime)
     *   2. Cannot do: new T[10]               (array needs type at runtime)
     *   3. Cannot do: catch (Throwable<T>)    (JVM can't distinguish at runtime)
     *   4. Cannot do: if (list instanceof List<String>)  (erased at runtime)
     *   5. Can do:    if (list instanceof List<?>)       (raw type check)
     */
    public static void demonstrateTypeErasure() {
        List<String>  strings  = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();

        // At runtime, both are just ArrayList
        System.out.println("Type erasure evidence:");
        System.out.println("  String list class:  " + strings.getClass().getName());
        System.out.println("  Integer list class: " + integers.getClass().getName());
        System.out.println("  Same class? " + (strings.getClass() == integers.getClass())); // true!

        // Wildcard instanceof works (no type param)
        System.out.println("  instanceof List<?>:   " + (strings instanceof List<?>)); // true
        // Cannot do: strings instanceof List<String> — compile error

        // The compiler inserts the cast — this would throw ClassCastException
        // if the wrong type was somehow inserted (which generics prevent at compile time)
        List raw = strings;  // raw type — compiler warns
        // raw.add(42);  // compiles (raw type, no warning... wait, it does warn)
        // String s = strings.get(0);  // would throw ClassCastException at runtime

        System.out.println("\nBridge methods explanation:");
        System.out.println("  When StringBox overrides Box<String>.getValue(), compiler creates:");
        System.out.println("  Object getValue() { return getStringValue(); } // bridge");
        System.out.println("  String getStringValue() { return value; }      // actual override");
    }

    // =========================================================================
    // SECTION 2: Bounded Type Parameters
    // =========================================================================

    /**
     * KEY INSIGHT: Bounds restrict what types can be used as type arguments.
     * <T extends Number> means T must be Number or a subclass.
     * <T extends Comparable<T>> is a RECURSIVE BOUND — T must be comparable to itself.
     * <T extends Number & Serializable> — multiple bounds (class first, then interfaces).
     *
     * WHAT BOUNDS ENABLE:
     *   Without bound: T is treated as Object — you can only call Object methods on it.
     *   With <T extends Number>: you can call .intValue(), .doubleValue(), etc. on T.
     *   With <T extends Comparable<T>>: you can call .compareTo(other) on T.
     *
     * INTERVIEW TALKING POINT:
     *   "Bounded type parameters allow you to express constraints that let you call
     *    type-specific methods on the generic type parameter."
     */

    // Example: sum requires T to be a Number subtype (to call doubleValue())
    public static <T extends Number> double sum(List<T> numbers) {
        return numbers.stream().mapToDouble(Number::doubleValue).sum();
        // Without <T extends Number>, mapToDouble(Number::doubleValue) would fail
        // because T would be inferred as Object (which has no doubleValue())
    }

    // Recursive bound: T must be comparable to itself
    // Used in: min(), max(), sort() generically
    public static <T extends Comparable<T>> T min(List<T> list) {
        if (list.isEmpty()) throw new NoSuchElementException("Empty list");
        T result = list.get(0);
        for (T item : list) {
            if (item.compareTo(result) < 0) {  // compareTo works because of bound
                result = item;
            }
        }
        return result;
    }

    // Multiple bounds: T must extend Number AND implement Serializable
    // At runtime, T is erased to the FIRST bound (Number), so Number methods are accessible
    public static <T extends Number & java.io.Serializable> void serializableNumber(T value) {
        System.out.println("  Serializable number: " + value.doubleValue());
        // Can call Number methods (doubleValue) because Number is the first bound (erasure target)
        // Can also serialize because Serializable bound is guaranteed at compile time
    }

    // =========================================================================
    // SECTION 3: PECS — Producer Extends, Consumer Super
    // =========================================================================

    /**
     * KEY INSIGHT: The PECS rule governs when to use <? extends T> vs <? super T>.
     *
     * PRODUCER EXTENDS: List<? extends Number>
     *   - You can READ from it: Number n = list.get(0)  ✓
     *   - You CANNOT write: list.add(1.0)  ✗  COMPILE ERROR
     *   - WHY you can't write: the list could be List<Integer>, List<Double>, List<Float>...
     *     Adding a Double to what's actually a List<Integer> would be wrong.
     *     The compiler conservatively blocks all writes (except null).
     *
     * CONSUMER SUPER: List<? super Integer>
     *   - You can WRITE Integer: list.add(42)  ✓
     *   - You can only READ as Object: Object o = list.get(0)  ✓  (but not Integer)
     *   - WHY only Object on read: the list could be List<Integer>, List<Number>, List<Object>...
     *     We don't know the exact type, so only Object is safe.
     *
     * MNEMONIC: PECS
     *   P = Producer → Extends  (list produces items for you to read)
     *   C = Consumer → Super    (list consumes items you write into it)
     *
     * REAL-WORLD EXAMPLE: Collections.copy(destination, source)
     *   void copy(List<? super T> dest, List<? extends T> src)
     *   src PRODUCES items → extends
     *   dest CONSUMES items → super
     */

    // Producer: we only READ from 'numbers' (it PRODUCES values for us)
    public static double sumPECSProducer(List<? extends Number> numbers) {
        // Can READ as Number ✓
        double total = 0;
        for (Number n : numbers) {
            total += n.doubleValue();
        }
        // numbers.add(1.0);  // COMPILE ERROR — cannot write to producer wildcard
        return total;
    }

    // Consumer: we only WRITE to 'destination' (it CONSUMES our values)
    public static void addIntegersPECSConsumer(List<? super Integer> destination, int count) {
        for (int i = 0; i < count; i++) {
            destination.add(i);  // Can WRITE Integer ✓
        }
        // Integer x = destination.get(0);  // COMPILE ERROR — can only read as Object
        Object o = destination.get(0);  // reading as Object is allowed ✓
    }

    // Classic PECS: copy from source to destination
    public static <T> void copyList(List<? extends T> src, List<? super T> dst) {
        for (T item : src) {  // src produces T
            dst.add(item);    // dst consumes T
        }
    }

    // =========================================================================
    // SECTION 4: Generic Methods vs Generic Classes
    // =========================================================================

    /**
     * KEY INSIGHT: Type parameters can be on the CLASS or on the METHOD.
     *
     * CLASS-LEVEL: <T> is declared on the class, shared by all methods
     *   class Box<T> { private T value; T get() { return value; } }
     *   The T is fixed when you write Box<String> box = ...
     *
     * METHOD-LEVEL: <T> is declared on the method, resolved per-call
     *   static <T> List<T> listOf(T item) { ... }
     *   Here T is inferred from the argument at each call site.
     *
     * TYPE INFERENCE: The compiler infers T from:
     *   1. Arguments: listOf("hello") → T = String
     *   2. Expected return type: List<Integer> nums = listOf(42) → T = Integer
     *   3. Explicit: GenericsFoundations.<String>listOf("hello")
     *
     * WHEN TO USE METHOD-LEVEL generics:
     *   - Utility/static methods where type varies per call
     *   - When method type is independent of class type
     *   - swap(List<T>, int, int), Collections.sort(List<T>)
     */

    // Generic static method: type inferred from argument
    public static <T> List<T> singletonListOf(T item) {
        List<T> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    // Generic method with multiple type params
    public static <K, V> Map<K, V> mapOf(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    // =========================================================================
    // SECTION 5: Wildcard Capture Helper Pattern
    // =========================================================================

    /**
     * KEY INSIGHT: Sometimes you need to capture a wildcard type to perform
     * operations that require a concrete type. The pattern is:
     *   public method takes List<?>, delegates to private method with <T>
     *
     * WHY THIS IS NEEDED:
     *   void swap(List<?> list, int i, int j) {
     *       Object temp = list.get(i);
     *       list.set(i, list.get(j));  // COMPILE ERROR: set expects ?, temp is Object
     *       list.set(j, temp);
     *   }
     *   The compiler doesn't know if Object is the right type for set().
     *
     * SOLUTION — capture wildcard with helper:
     *   The private helper <T> has a concrete type T, so set(i, T) works.
     */
    public static void swap(List<?> list, int i, int j) {
        swapHelper(list, i, j);  // capture wildcard
    }

    // Helper: captures ? as T — now T is concrete, set() works
    private static <T> void swapHelper(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    // =========================================================================
    // SECTION 6: Type Tokens — Runtime Type Info Workaround
    // =========================================================================

    /**
     * KEY INSIGHT: Because generics are erased, we sometimes need Class<T> as a
     * "type token" to carry runtime type information explicitly.
     *
     * PROBLEM: You want a method that creates instances of type T.
     *   <T> T create() { return new T(); }  // COMPILE ERROR: can't do new T()
     *
     * SOLUTION: Pass Class<T> as a token
     *   <T> T create(Class<T> type) { return type.getDeclaredConstructor().newInstance(); }
     *
     * REAL-WORLD: ObjectMapper.readValue(json, User.class)
     *   Jackson's readValue uses Class<T> type token to know what to deserialize into.
     *   This is how you "recover" type information that erasure removed.
     *
     * SUPER TYPE TOKEN (Guava TypeToken):
     *   For generic types like List<String>, Class<T> isn't enough (can't write List<String>.class).
     *   Guava's TypeToken uses anonymous subclassing to capture the type at runtime:
     *     TypeToken<List<String>> token = new TypeToken<List<String>>() {};
     *     // The anonymous subclass retains the generic supertype in bytecode!
     */

    // Simple type token: create T using reflection
    public static <T> T createInstance(Class<T> type) throws ReflectiveOperationException {
        return type.getDeclaredConstructor().newInstance();
    }

    // Type token for safe casting
    public static <T> T safeCast(Object obj, Class<T> type) {
        if (type.isInstance(obj)) {
            return type.cast(obj);  // safe, checked cast
        }
        throw new ClassCastException("Cannot cast " + obj.getClass() + " to " + type);
    }

    // =========================================================================
    // SECTION 7: Generic Array Workaround
    // =========================================================================

    /**
     * KEY INSIGHT: Cannot create new T[], but common workarounds:
     *   1. Use List<T> instead of T[]  (preferred)
     *   2. Create Object[] and cast: T[] arr = (T[]) new Object[size]  (unchecked warning)
     *   3. Use Array.newInstance(Class<T>, size): requires type token
     *
     * INTERVIEW ANSWER FOR "Why no generic array?":
     *   Arrays are REIFIED (carry type at runtime): String[] knows it's String[].
     *   Generics are ERASED (type gone at runtime): T[] becomes Object[].
     *   If new T[] were allowed:
     *     T[] arr = new T[10];          // at runtime: Object[10]
     *     Object[] o = arr;             // covariant: OK (Object[] IS-A Object[])
     *     o[0] = "hello"; o[1] = 42;   // both compile! no ArrayStoreException
     *     T first = arr[0];             // compiler inserts cast to T
     *     // ClassCastException if T is Integer and arr[0] is "hello"
     *   → Combining reified covariant arrays with erased invariant generics is unsafe.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] createArray(int size) {
        // UNCHECKED CAST — safe only if caller knows T at call site
        return (T[]) new Object[size];
        // At runtime this IS Object[], so if you actually mix types,
        // ClassCastException will occur when reading. The annotation suppresses the warning.
    }

    // Safer: use Class<T> type token to create the exact array type
    @SuppressWarnings("unchecked")
    public static <T> T[] createTypedArray(Class<T> type, int size) {
        return (T[]) java.lang.reflect.Array.newInstance(type, size);
        // java.lang.reflect.Array.newInstance creates String[size] if type=String.class
        // This is how ArrayList.toArray(T[]) works internally.
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.out.println("=== Generics Foundations Demo ===\n");

        // Section 1: Type Erasure
        System.out.println("--- Type Erasure ---");
        demonstrateTypeErasure();

        // Section 2: Bounded type parameters
        System.out.println("\n--- Bounded Type Parameters ---");
        List<Integer> ints = List.of(3, 1, 4, 1, 5, 9, 2, 6);
        List<Double>  dbls = List.of(1.1, 2.2, 3.3);
        System.out.println("  Sum of ints: "    + sum(ints));    // works — Integer extends Number
        System.out.println("  Sum of doubles: " + sum(dbls));    // works — Double extends Number
        System.out.println("  Min of ints: "    + min(ints));    // works — Integer is Comparable
        System.out.println("  Min of strings: " + min(List.of("banana", "apple", "cherry")));
        serializableNumber(42);     // Integer extends Number AND is Serializable
        serializableNumber(3.14);   // Double extends Number AND is Serializable

        // Section 3: PECS
        System.out.println("\n--- PECS ---");
        List<Integer> intList = new ArrayList<>(List.of(1, 2, 3));
        List<Double>  dblList = new ArrayList<>(List.of(1.5, 2.5));
        System.out.println("  Sum (producer extends): " + sumPECSProducer(intList));
        System.out.println("  Sum (producer extends): " + sumPECSProducer(dblList));

        List<Number> numDest = new ArrayList<>();
        addIntegersPECSConsumer(numDest, 5); // Integer fits in List<Number> (super of Integer)
        System.out.println("  After consumer adds: " + numDest);

        List<String> src = List.of("x", "y", "z");
        List<Object> dst = new ArrayList<>();
        copyList(src, dst); // String extends T, Object super T → T=String works
        System.out.println("  Copied: " + dst);

        // Section 4: Generic methods
        System.out.println("\n--- Generic Methods ---");
        List<String> strList = singletonListOf("hello");
        List<Integer> iList  = singletonListOf(42);
        System.out.println("  singleton String: " + strList);
        System.out.println("  singleton Integer: " + iList);
        System.out.println("  mapOf: " + mapOf("key", 100));

        // Section 5: Wildcard capture
        System.out.println("\n--- Wildcard Capture ---");
        List<Integer> toSwap = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        System.out.println("  Before swap: " + toSwap);
        swap(toSwap, 0, 4);
        System.out.println("  After swap(0,4): " + toSwap);

        // Section 6: Type tokens
        System.out.println("\n--- Type Tokens ---");
        ArrayList<String> created = createInstance(ArrayList.class);
        created.add("via reflection");
        System.out.println("  Created via type token: " + created);

        Object obj = "hello";
        String s = safeCast(obj, String.class);
        System.out.println("  Safe cast result: " + s);
        try {
            safeCast(obj, Integer.class);  // should fail
        } catch (ClassCastException e) {
            System.out.println("  ClassCastException correctly thrown");
        }

        // Section 7: Generic arrays
        System.out.println("\n--- Generic Arrays ---");
        String[] arr = createTypedArray(String.class, 3);
        arr[0] = "a"; arr[1] = "b"; arr[2] = "c";
        System.out.println("  Typed array: " + Arrays.toString(arr));

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("Type erasure: generics are compile-time only; List<String> == List at runtime");
        System.out.println("PECS: Producer Extends (read), Consumer Super (write)");
        System.out.println("Recursive bound: <T extends Comparable<T>> for self-comparison");
        System.out.println("No generic array: arrays are reified+covariant, generics are erased+invariant");
        System.out.println("Type token: Class<T> to recover type info erased at runtime");
        System.out.println("Wildcard capture: public List<?> → private <T> for mutation");
    }
}
