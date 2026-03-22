package com.java.generics;

/**
 * CONCEPT: Wildcards and Variance in Java Generics
 *
 * CORE IDEA: "Variance" describes how subtype relationships of parameterized types
 * relate to subtype relationships of their type arguments.
 *   - Covariant:      if Dog IS-A Animal, then Producer<Dog> IS-A Producer<Animal>
 *   - Contravariant:  if Dog IS-A Animal, then Consumer<Animal> IS-A Consumer<Dog>
 *   - Invariant:      List<Dog> is NOT related to List<Animal> regardless of inheritance
 *
 * Java generics are INVARIANT by default, but wildcards add use-site variance.
 *
 * WHEN TO USE:
 *   - Wildcard ? extends T: when method only reads from the collection (covariant)
 *   - Wildcard ? super T: when method only writes to the collection (contravariant)
 *   - Unbounded wildcard ?: when you don't care about the type at all
 *
 * INTERVIEW ANGLE:
 *   Q: Are Java generics covariant or invariant?
 *   A: INVARIANT. List<Dog> is NOT a subtype of List<Animal>. This is unlike arrays,
 *      which are covariant (Dog[] IS-A Animal[]). Invariance is required for type safety.
 *
 *   Q: Why are arrays covariant but generics invariant?
 *   A: Arrays are covariant for historical reasons (pre-generics Java). The problem is
 *      ArrayStoreException at runtime. Generics chose invariance to catch these errors
 *      at compile time. Example:
 *        Animal[] animals = new Dog[3]; // covariant, compiles
 *        animals[0] = new Cat();        // compiles! but throws ArrayStoreException at runtime
 *        // JVM checks array type at runtime. Runtime check = slower + runtime error.
 *
 *        List<Animal> animals = new ArrayList<Dog>(); // COMPILE ERROR — invariant
 *        // Error caught at compile time. No runtime surprise.
 *
 *   Q: What's the difference between use-site variance (Java) and declaration-site variance (Kotlin)?
 *   A: Java: variance is specified at each USE of the type (wildcard at call site).
 *      Kotlin: variance is specified in the CLASS DECLARATION with in/out.
 *      Kotlin out T = covariant (produces T, like ? extends T in Java).
 *      Kotlin in T = contravariant (consumes T, like ? super T in Java).
 *      Kotlin declaration-site is cleaner (specify once) but Java's use-site is more flexible
 *      (a class can be covariant in some contexts and contravariant in others).
 *
 * REAL-WORLD USE:
 *   - Collections.copy(List<? super T> dest, List<? extends T> src) — canonical PECS
 *   - Comparator.comparing(Function<? super T, ? extends U> keyExtractor)
 *   - Stream.flatMap(Function<? super T, ? extends Stream<? extends R>>)
 *   - Collections.sort(List<T>, Comparator<? super T>)
 *
 * GOTCHA:
 *   - List<?> and List<Object> are DIFFERENT.
 *     List<Object> can hold any object but is invariant (not a supertype of List<String>).
 *     List<?> is an unknown type — read returns Object, cannot write anything.
 *   - ? extends T and ? super T cannot be combined in one wildcard (unlike Kotlin's *).
 *   - Nested wildcards: List<? extends List<? extends Number>> — can get ugly fast.
 */

import java.util.*;
import java.util.function.*;

public class WildcardsAndVariance {

    // =========================================================================
    // SECTION 1: Invariance — Why List<Dog> is NOT a List<Animal>
    // =========================================================================

    /**
     * KEY INSIGHT: Java generics are invariant for TYPE SAFETY.
     *
     * THE UNSAFETY ARGUMENT:
     *   Suppose List<Dog> WERE a subtype of List<Animal>. Then:
     *     List<Dog>    dogs    = new ArrayList<>();
     *     List<Animal> animals = dogs;          // hypothetically allowed
     *     animals.add(new Cat());               // Cat IS-A Animal, so this compiles
     *     Dog d = dogs.get(0);                  // ClassCastException! a Cat, not a Dog!
     *
     *   This is EXACTLY what arrays allow (and they pay for it with ArrayStoreException).
     *   Generics disallow the second line entirely → error at compile time, not runtime.
     *
     * ARRAY COVARIANCE PROBLEM (runtime error):
     *   Dog[] dogs    = new Dog[3];
     *   Animal[] arr  = dogs;           // allowed — arrays are covariant
     *   arr[0] = new Cat();             // throws ArrayStoreException at RUNTIME
     *   // JVM checks the actual array type at runtime — this is called "runtime type check"
     *   // Cost: every array write has a hidden type check overhead
     *
     * GENERIC INVARIANCE BENEFIT (compile time error):
     *   List<Dog> dogs       = new ArrayList<>();
     *   List<Animal> animals = dogs;    // COMPILE ERROR — invariant!
     *   // Error caught at compile time. No runtime overhead.
     */

    static class Animal { String name; Animal(String name) { this.name = name; } }
    static class Dog extends Animal { Dog(String name) { super(name); } }
    static class Cat extends Animal { Cat(String name) { super(name); } }

    public static void demonstrateInvariance() {
        System.out.println("=== Invariance Demo ===");

        // Array covariance — compiles, fails at runtime:
        Animal[] animalArr = new Dog[3];  // Dog[] IS-A Animal[] (covariant)
        animalArr[0] = new Dog("Rex");    // OK
        try {
            animalArr[1] = new Cat("Whiskers"); // RUNTIME: ArrayStoreException!
        } catch (ArrayStoreException e) {
            System.out.println("  Array covariance RUNTIME error: " + e.getClass().getSimpleName());
            System.out.println("  (JVM checks array component type on every write)");
        }

        // Generic invariance — compile error (commented to allow compilation):
        // List<Animal> animals = new ArrayList<Dog>();  // COMPILE ERROR
        // The compiler prevents this. No runtime check needed.
        System.out.println("  List<Dog> is NOT a subtype of List<Animal> (invariant)");
        System.out.println("  This is checked at COMPILE TIME — no runtime error possible");
    }

    // =========================================================================
    // SECTION 2: Covariance — ? extends T
    // =========================================================================

    /**
     * KEY INSIGHT: List<? extends Animal> is covariant — it accepts
     * List<Animal>, List<Dog>, List<Cat>, or List<any Animal subtype>.
     *
     * READ SAFE:  Animal a = list.get(0)  ✓ (we know it's at least Animal)
     * WRITE UNSAFE: list.add(new Dog())   ✗ (might be List<Cat>, can't add Dog!)
     *
     * MENTAL MODEL: "? extends Animal" means "some specific subtype of Animal,
     * but I don't know which one." You can read Animal, but you can't write
     * anything except null (which is valid for any reference type).
     *
     * COLLECTIONS.UNMODIFIABLELIST vs COVARIANCE:
     *   Collections.unmodifiableList returns a view that throws on write.
     *   Wildcard covariance prevents write AT COMPILE TIME — stronger guarantee.
     */
    public static double sumAnimalWeights(List<? extends Animal> animals) {
        double total = 0;
        for (Animal a : animals) {  // Reading as Animal — safe ✓
            total += a.name.length(); // pretend weight = name length
        }
        // animals.add(new Dog("X")); // COMPILE ERROR — cannot write ✗
        return total;
    }

    // Covariant assignment chain:
    public static void demonstrateCovariance() {
        System.out.println("\n=== Covariance (? extends) Demo ===");

        List<Dog> dogs    = new ArrayList<>(List.of(new Dog("Rex"), new Dog("Max")));
        List<Cat> cats    = new ArrayList<>(List.of(new Cat("Luna"), new Cat("Mimi")));
        List<Animal> animals = new ArrayList<>(List.of(new Animal("Generic")));

        // All three are valid for List<? extends Animal>:
        System.out.println("  Dogs sum: "    + sumAnimalWeights(dogs));    // List<Dog> ✓
        System.out.println("  Cats sum: "    + sumAnimalWeights(cats));    // List<Cat> ✓
        System.out.println("  Animals sum: " + sumAnimalWeights(animals)); // List<Animal> ✓

        // Covariant assignment:
        List<? extends Animal> ref = dogs;  // OK — covariant wildcard
        Animal first = ref.get(0);          // Reading as Animal — safe
        // ref.add(new Dog("X"));           // COMPILE ERROR — write blocked
        System.out.println("  Read from covariant ref: " + first.name);
    }

    // =========================================================================
    // SECTION 3: Contravariance — ? super T
    // =========================================================================

    /**
     * KEY INSIGHT: List<? super Dog> is contravariant — it accepts
     * List<Dog>, List<Animal>, List<Object>.
     *
     * WRITE SAFE:  list.add(new Dog())  ✓ (Dog fits in List<Dog>, List<Animal>, List<Object>)
     * READ UNSAFE: Dog d = list.get(0) ✗ (might be List<Animal> containing a Cat!)
     *   Only Object is safe to read: Object o = list.get(0) ✓
     *
     * MENTAL MODEL: "? super Dog" means "some type that is Dog or a supertype of Dog."
     * We can write Dog (or a Dog subtype) into any such list.
     * We don't know the exact type, so we can only read as Object.
     *
     * USE CASE: Comparator<? super T>
     *   If you sort a List<Dog>, you can use a Comparator<Dog>, Comparator<Animal>, or
     *   Comparator<Object> — they all know how to compare Dogs.
     */
    public static void addDogsToPen(List<? super Dog> pen, String... names) {
        for (String name : names) {
            pen.add(new Dog(name)); // Writing Dog — always safe ✓
        }
        // Dog d = pen.get(0);    // COMPILE ERROR — might not be Dog ✗
        Object o = pen.get(0);    // Reading as Object — always safe ✓
        System.out.println("  First element (as Object): " + ((Animal) o).name);
    }

    public static void demonstrateContravariance() {
        System.out.println("\n=== Contravariance (? super) Demo ===");

        List<Dog>    dogPen    = new ArrayList<>();
        List<Animal> animalPen = new ArrayList<>();
        List<Object> objectPen = new ArrayList<>();

        // All three are valid for List<? super Dog>:
        addDogsToPen(dogPen,    "Rex", "Max");
        addDogsToPen(animalPen, "Buddy");
        addDogsToPen(objectPen, "Fido");

        System.out.println("  Dogs in dog pen:    " + dogPen.size());
        System.out.println("  Dogs in animal pen: " + animalPen.size());
        System.out.println("  Dogs in object pen: " + objectPen.size());
    }

    // =========================================================================
    // SECTION 4: Unbounded Wildcard — List<?>
    // =========================================================================

    /**
     * KEY INSIGHT: List<?> means "a list of SOME unknown type."
     * You can only read Object from it. You cannot write anything (except null).
     *
     * DIFFERENCE from List<Object>:
     *   List<Object> is a specific type — a list of Objects.
     *     List<String> ≠ List<Object>  (invariance)
     *   List<?> means we don't know the type parameter.
     *     List<String> IS assignable to List<?> ✓
     *     List<Integer> IS assignable to List<?> ✓
     *
     * WHEN TO USE List<?>:
     *   1. When you don't USE the type at all (just check size, clear, print)
     *   2. When the method works on the container, not the elements (Collections.swap, size)
     *   3. Class<?> — for reflection where the specific class type doesn't matter
     */

    // Unbounded wildcard: just prints, doesn't use element type
    public static void printList(List<?> list) {
        System.out.print("  [");
        for (Object item : list) {  // read as Object ✓
            System.out.print(item + " ");
        }
        System.out.println("]");
        // list.add("hello"); // COMPILE ERROR — can't write ✗
    }

    // Common use: Class<?>
    public static void printClassName(Class<?> clazz) {
        System.out.println("  Class: " + clazz.getName());
    }

    public static void demonstrateUnboundedWildcard() {
        System.out.println("\n=== Unbounded Wildcard Demo ===");
        printList(List.of(1, 2, 3));         // List<Integer> → List<?> ✓
        printList(List.of("a", "b", "c"));   // List<String> → List<?> ✓
        printList(List.of(1.1, 2.2, 3.3));   // List<Double> → List<?> ✓

        printClassName(String.class);
        printClassName(ArrayList.class);
        printClassName(int[].class);
    }

    // =========================================================================
    // SECTION 5: Use-Site vs Declaration-Site Variance (Java vs Kotlin)
    // =========================================================================

    /**
     * KEY INSIGHT: Java uses USE-SITE variance (wildcards at call site).
     *   Same class List can be used as:
     *     List<? extends T>  (covariant) at one call site
     *     List<? super T>    (contravariant) at another call site
     *     List<T>            (invariant) at yet another call site
     *
     * Kotlin uses DECLARATION-SITE variance (in/out in class definition).
     *   class Producer<out T> { fun produce(): T }  // covariant, like <? extends T>
     *   class Consumer<in T> { fun consume(t: T) }  // contravariant, like <? super T>
     *   // At call site: just Producer<Dog>, no wildcards needed
     *
     * JAVA USE-SITE (more flexible, more verbose at call sites):
     *   void copy(List<? extends T> src, List<? super T> dst)  // wildcards every time
     *
     * KOTLIN DECLARATION-SITE (cleaner call sites, requires class-level design):
     *   fun <T> copy(src: Producer<T>, dst: Consumer<T>)  // clean
     *   // Producer<Dog> can be used wherever Producer<Animal> is expected
     *
     * JAVA uses use-site because java.util.List is both producer AND consumer,
     * so you can't declare it as purely covariant or contravariant at the class level.
     * Kotlin handles this via separate read-only (List) and mutable (MutableList) types.
     *
     * INTERVIEW COMPARISON TABLE:
     * ┌────────────────────┬──────────────────────────────┬──────────────────────────────┐
     * │ Feature            │ Java (use-site)              │ Kotlin (declaration-site)    │
     * ├────────────────────┼──────────────────────────────┼──────────────────────────────┤
     * │ Where declared     │ At each call site            │ In class declaration         │
     * │ Covariant syntax   │ List<? extends T>            │ class Box<out T>             │
     * │ Contravariant      │ List<? super T>              │ class Box<in T>              │
     * │ Call site syntax   │ verbose (must repeat)        │ clean (just Box<Dog>)        │
     * │ Flexibility        │ Same class, multiple roles   │ Must design up-front         │
     * │ Language           │ Java                         │ Kotlin, C# (partial)         │
     * └────────────────────┴──────────────────────────────┴──────────────────────────────┘
     */
    public static void demonstrateVariancePhilosophy() {
        System.out.println("\n=== Use-Site vs Declaration-Site Variance ===");
        System.out.println("Java:   wildcards at CALL SITE  — List<? extends T>, List<? super T>");
        System.out.println("Kotlin: variance at DECLARATION — class Box<out T>, class Box<in T>");
        System.out.println("Java List is both producer and consumer → invariant by default");
        System.out.println("Kotlin splits: List (read-only, out T) vs MutableList (invariant)");
    }

    // =========================================================================
    // SECTION 6: Practical Examples — Collections.copy, Comparator.comparing
    // =========================================================================

    /**
     * KEY INSIGHT: Real-world APIs demonstrate PECS perfectly.
     *
     * Collections.copy signature:
     *   public static <T> void copy(List<? super T> dest, List<? extends T> src)
     *   - src PRODUCES T values (we read from src) → ? extends T
     *   - dest CONSUMES T values (we write to dest) → ? super T
     *
     * Comparator.comparing signature:
     *   public static <T, U extends Comparable<? super U>>
     *       Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor)
     *   - keyExtractor CONSUMES a T (receives T input) → ? super T
     *   - keyExtractor PRODUCES a U (returns U output) → ? extends U
     *   - U extends Comparable<? super U>: U can be compared to itself or a supertype
     *     (e.g., Dog compareTo Animal — if Dog extends Animal implements Comparable<Animal>)
     *
     * Stream.flatMap:
     *   <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper)
     *   - mapper CONSUMES T → ? super T
     *   - mapper PRODUCES Stream → ? extends Stream
     *   - Stream elements are R → ? extends R
     */
    public static void demonstratePracticalExamples() {
        System.out.println("\n=== Practical PECS Examples ===");

        // Collections.copy uses PECS
        List<Dog> source = List.of(new Dog("Rex"), new Dog("Max"));
        List<Animal> destination = new ArrayList<>(Collections.nCopies(2, null));
        Collections.copy(destination, source);  // dest=<? super Dog>, src=<? extends Dog>
        // Works! Dog IS-A Animal, so destination (List<Animal>) accepts Dog writes.
        System.out.println("  Collections.copy: " + destination.size() + " items copied");
        System.out.println("  First item name: " + destination.get(0).name);

        // Comparator with PECS
        List<Dog> dogs = new ArrayList<>(List.of(new Dog("Charlie"), new Dog("Bella"), new Dog("Ace")));
        dogs.sort(Comparator.comparing(a -> a.name)); // keyExtractor: Dog → String
        System.out.print("  Sorted dogs: ");
        dogs.forEach(d -> System.out.print(d.name + " "));
        System.out.println();
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Wildcards and Variance Demo ===\n");

        demonstrateInvariance();
        demonstrateCovariance();
        demonstrateContravariance();
        demonstrateUnboundedWildcard();
        demonstrateVariancePhilosophy();
        demonstratePracticalExamples();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("Invariant: List<Dog> NOT a subtype of List<Animal> (prevents Cat-in-Dog-list bug)");
        System.out.println("Covariant (? extends): read-safe, write-blocked (producer)");
        System.out.println("Contravariant (? super): write-safe (for T and subtypes), read returns Object only");
        System.out.println("Unbounded (?): read returns Object, no writes — use when type is irrelevant");
        System.out.println("Arrays: covariant at compile time, ArrayStoreException at runtime");
        System.out.println("Generics: invariant at compile time, error caught earlier, no runtime overhead");
        System.out.println("Java = use-site variance (wildcards). Kotlin = declaration-site (in/out).");
    }
}
