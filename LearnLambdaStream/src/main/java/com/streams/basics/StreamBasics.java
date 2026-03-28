package com.streams.basics;

import java.util.*;
import java.util.stream.*;

/**
 * STREAM BASICS — How Streams Work
 *
 * WHAT IS A STREAM?
 *   A Stream is a sequence of elements that supports functional-style operations.
 *   It is NOT a data structure — it does not store data.
 *   It processes data from a source (List, Set, Array) and produces a result.
 *
 * MENTAL MODEL — Conveyor belt in a factory:
 *
 *   Raw materials        Machines               Final product
 *   (your List) ──────► (operations) ─────────► (result)
 *
 *   employees → filter() → groupingBy() → maxBy() → Map<String, Optional<Employee>>
 *
 * THREE TYPES OF OPERATIONS:
 *   1. SOURCE       — creates the stream:        list.stream(), Arrays.stream(arr)
 *   2. INTERMEDIATE — transforms, LAZY:          filter(), map(), sorted(), distinct()
 *                     *** Nothing runs until terminal is called ***
 *   3. TERMINAL     — triggers execution, EAGER: collect(), forEach(), count(), findFirst()
 *                     *** This is when the belt starts moving ***
 *
 * KEY RULES:
 *   - Chain as many intermediate ops as you want
 *   - Only ONE terminal op per stream
 *   - After terminal, stream is consumed — cannot reuse
 */
public class StreamBasics {

    public static void main(String[] args) {

        System.out.println("=== 1. Creating Streams ===");
        creatingStreams();

        System.out.println("\n=== 2. Intermediate Operations ===");
        intermediateOperations();

        System.out.println("\n=== 3. Terminal Operations ===");
        terminalOperations();

        System.out.println("\n=== 4. Lazy Evaluation Demo ===");
        lazyEvaluationDemo();
    }

    // -------------------------------------------------------------------------
    // 1. CREATING STREAMS
    // -------------------------------------------------------------------------
    static void creatingStreams() {

        // From a List
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        names.stream();

        // From an Array
        String[] arr = {"Alice", "Bob"};
        Arrays.stream(arr);

        // From individual values
        Stream.of("Alice", "Bob", "Charlie");

        // Infinite stream (always use limit() with these)
        Stream.iterate(0, n -> n + 1).limit(10);   // 0,1,2,3...9
        Stream.generate(Math::random).limit(5);     // 5 random numbers

        // IntStream, LongStream, DoubleStream — for primitives (avoids boxing)
        IntStream.range(1, 6);        // 1,2,3,4,5  (excludes 6)
        IntStream.rangeClosed(1, 5);  // 1,2,3,4,5  (includes 5)

        System.out.println("Stream sources: List, Array, Stream.of(), iterate(), generate(), IntStream.range()");
    }

    // -------------------------------------------------------------------------
    // 2. INTERMEDIATE OPERATIONS — all are LAZY
    // -------------------------------------------------------------------------
    static void intermediateOperations() {

        List<Integer> numbers = Arrays.asList(5, 3, 8, 1, 9, 2, 7, 4, 6);

        // filter() — keep elements matching condition
        numbers.stream()
            .filter(n -> n > 5)
            .forEach(n -> System.out.print(n + " "));  // 8 9 7 6
        System.out.println();

        // map() — transform each element (one in, one out)
        numbers.stream()
            .map(n -> n * 2)
            .forEach(n -> System.out.print(n + " "));  // 10 6 16 2 18 4 14 8 12
        System.out.println();

        // flatMap() — transform each element into multiple, then flatten (one in, many out)
        List<List<Integer>> nested = Arrays.asList(
            Arrays.asList(1, 2, 3),
            Arrays.asList(4, 5, 6)
        );
        nested.stream()
            .flatMap(Collection::stream)  // flatten List<List<Integer>> → List<Integer>
            .forEach(n -> System.out.print(n + " "));  // 1 2 3 4 5 6
        System.out.println();

        // sorted() — natural order
        numbers.stream().sorted().forEach(n -> System.out.print(n + " "));
        System.out.println();

        // sorted() with Comparator — reverse order
        numbers.stream()
            .sorted(Comparator.reverseOrder())
            .forEach(n -> System.out.print(n + " "));
        System.out.println();

        // distinct() — remove duplicates
        Arrays.asList(1, 2, 2, 3, 3, 3).stream()
            .distinct()
            .forEach(n -> System.out.print(n + " "));  // 1 2 3
        System.out.println();

        // limit() — take first N elements
        numbers.stream().limit(3).forEach(n -> System.out.print(n + " "));  // 5 3 8
        System.out.println();

        // skip() — skip first N elements
        numbers.stream().skip(6).forEach(n -> System.out.print(n + " "));   // 7 4 6
        System.out.println();

        // peek() — look at elements without changing them (for debugging)
        numbers.stream()
            .filter(n -> n > 5)
            .peek(n -> System.out.print("after filter: " + n + " "))
            .map(n -> n * 10)
            .forEach(n -> System.out.print("after map: " + n + " "));
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // 3. TERMINAL OPERATIONS — trigger the pipeline
    // -------------------------------------------------------------------------
    static void terminalOperations() {

        List<Integer> numbers = Arrays.asList(5, 3, 8, 1, 9, 2, 7, 4, 6);

        // collect() — most important terminal op, collects into a collection
        List<Integer> filtered = numbers.stream()
            .filter(n -> n > 5)
            .collect(Collectors.toList());
        System.out.println("collect toList: " + filtered);

        // count() — how many elements
        long count = numbers.stream().filter(n -> n > 5).count();
        System.out.println("count > 5: " + count);  // 4

        // findFirst() — first element matching filter, returns Optional
        Optional<Integer> first = numbers.stream().filter(n -> n > 5).findFirst();
        first.ifPresent(n -> System.out.println("first > 5: " + n));  // 8

        // anyMatch() — does ANY element match?
        boolean anyBig = numbers.stream().anyMatch(n -> n > 8);
        System.out.println("any > 8: " + anyBig);  // true (9)

        // allMatch() — do ALL elements match?
        boolean allPositive = numbers.stream().allMatch(n -> n > 0);
        System.out.println("all positive: " + allPositive);  // true

        // noneMatch() — do NO elements match?
        boolean noneNegative = numbers.stream().noneMatch(n -> n < 0);
        System.out.println("none negative: " + noneNegative);  // true

        // min() and max() — returns Optional
        numbers.stream().min(Comparator.naturalOrder())
            .ifPresent(n -> System.out.println("min: " + n));  // 1
        numbers.stream().max(Comparator.naturalOrder())
            .ifPresent(n -> System.out.println("max: " + n));  // 9

        // reduce() — combine all elements into one
        int sum = numbers.stream().reduce(0, Integer::sum);
        System.out.println("sum: " + sum);  // 45

        // forEach() — iterate, no return value (side effects only)
        numbers.stream().limit(3).forEach(n -> System.out.print(n + " "));
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // 4. LAZY EVALUATION DEMO
    // -------------------------------------------------------------------------
    /**
     * Streams are LAZY — intermediate operations do NOTHING until terminal is called.
     * This means:
     *   - If terminal never called → zero work done
     *   - short-circuit ops (findFirst, anyMatch) stop as soon as answer found
     */
    static void lazyEvaluationDemo() {

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        System.out.println("Finding first number > 5:");
        Optional<Integer> result = numbers.stream()
            .filter(n -> {
                System.out.println("  filter checking: " + n);
                return n > 5;
            })
            .findFirst();  // stops after finding first match — does NOT process all elements

        result.ifPresent(n -> System.out.println("Found: " + n));
        // Only checks 1,2,3,4,5,6 — stops at 6, never checks 7,8,9,10

        // INTERVIEW Q: Why are streams lazy?
        // A: Performance — avoids unnecessary computation.
        //    filter().map().findFirst() on 1M elements only processes until first match.
        //    Without lazy evaluation, ALL 1M would be filtered and mapped first.
    }
}
