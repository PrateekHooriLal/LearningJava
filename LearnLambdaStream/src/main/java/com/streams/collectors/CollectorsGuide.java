package com.streams.collectors;

import java.util.*;
import java.util.stream.*;

/**
 * COLLECTORS GUIDE — All Collectors with Examples
 *
 * A Collector is used INSIDE collect() to define what to produce.
 * collect() is the terminal operation. Collector is what goes inside it.
 *
 * QUICK REFERENCE TABLE — memorise this:
 *
 *   What I want                          Collector                              Return type
 *   ──────────────────────────────────────────────────────────────────────────────────────
 *   List                                 toList()                               List<T>
 *   Set (no duplicates)                  toSet()                                Set<T>
 *   Specific collection                  toCollection(TreeSet::new)             Collection<T>
 *   Count                                counting()                             Long
 *   Sum                                  summingInt/Long/Double(field)          int/long/double
 *   Average                              averagingInt/Long/Double(field)        Double
 *   Min                                  minBy(Comparator)                      Optional<T>
 *   Max                                  maxBy(Comparator)                      Optional<T>
 *   Join strings                         joining() / joining(", ") / joining(", ","[","]")  String
 *   Group into lists                     groupingBy(key)                        Map<K, List<V>>
 *   Group + count                        groupingBy(key, counting())            Map<K, Long>
 *   Group + max                          groupingBy(key, maxBy(Comparator))     Map<K, Optional<V>>
 *   Group + sum                          groupingBy(key, summingDouble(field))  Map<K, Double>
 *   Group + transform values             groupingBy(key, mapping(fn, toList())) Map<K, List<R>>
 *   Split true/false                     partitioningBy(predicate)              Map<Boolean, List<V>>
 *   Unwrap Optional after grouping       collectingAndThen(maxBy(...), opt->...) Map<K, V>
 *   Statistics (min,max,sum,avg,count)   summarizingInt/Long/Double(field)      IntSummaryStatistics
 */
public class CollectorsGuide {

    static List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Anna", "Brian", "Carol");
    static List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    public static void main(String[] args) {

        System.out.println("=== 1. Basic Collectors ===");
        basicCollectors();

        System.out.println("\n=== 2. Numeric Collectors ===");
        numericCollectors();

        System.out.println("\n=== 3. String Joining ===");
        stringJoining();

        System.out.println("\n=== 4. groupingBy — The Most Important Collector ===");
        groupingByCollector();

        System.out.println("\n=== 5. partitioningBy ===");
        partitioningByCollector();

        System.out.println("\n=== 6. collectingAndThen — Unwrap Optional ===");
        collectingAndThen();
    }

    // -------------------------------------------------------------------------
    // 1. BASIC COLLECTORS
    // -------------------------------------------------------------------------
    static void basicCollectors() {

        // toList() — most common
        List<String> filtered = names.stream()
            .filter(n -> n.startsWith("A"))
            .collect(Collectors.toList());
        System.out.println("toList: " + filtered);  // [Alice, Anna]

        // toSet() — removes duplicates
        List<Integer> withDupes = Arrays.asList(1, 2, 2, 3, 3, 3);
        Set<Integer> unique = withDupes.stream().collect(Collectors.toSet());
        System.out.println("toSet: " + unique);  // [1, 2, 3]

        // toUnmodifiableList() — immutable list (Java 10+)
        List<String> immutable = names.stream()
            .filter(n -> n.length() > 4)
            .collect(Collectors.toUnmodifiableList());
        System.out.println("toUnmodifiableList: " + immutable);

        // toMap() — collect into a Map
        // toMap(keyMapper, valueMapper)
        Map<String, Integer> nameLengths = names.stream()
            .collect(Collectors.toMap(
                n -> n,           // key: the name itself
                String::length    // value: length of the name
            ));
        System.out.println("toMap (name->length): " + nameLengths);
    }

    // -------------------------------------------------------------------------
    // 2. NUMERIC COLLECTORS
    // -------------------------------------------------------------------------
    static void numericCollectors() {

        // counting() — count elements (returns Long)
        long count = numbers.stream()
            .filter(n -> n > 5)
            .collect(Collectors.counting());
        System.out.println("counting > 5: " + count);  // 5

        // summingInt() — sum all values
        int sum = numbers.stream().collect(Collectors.summingInt(n -> n));
        System.out.println("summingInt: " + sum);  // 55

        // averagingInt() — average
        double avg = numbers.stream().collect(Collectors.averagingInt(n -> n));
        System.out.println("averagingInt: " + avg);  // 5.5

        // minBy() and maxBy() — return Optional
        numbers.stream()
            .collect(Collectors.minBy(Comparator.naturalOrder()))
            .ifPresent(n -> System.out.println("minBy: " + n));  // 1

        numbers.stream()
            .collect(Collectors.maxBy(Comparator.naturalOrder()))
            .ifPresent(n -> System.out.println("maxBy: " + n));  // 10

        // summarizingInt() — gives count, sum, min, max, average all at once
        IntSummaryStatistics stats = numbers.stream()
            .collect(Collectors.summarizingInt(n -> n));
        System.out.println("stats: count=" + stats.getCount()
            + " sum=" + stats.getSum()
            + " min=" + stats.getMin()
            + " max=" + stats.getMax()
            + " avg=" + stats.getAverage());
    }

    // -------------------------------------------------------------------------
    // 3. STRING JOINING
    // -------------------------------------------------------------------------
    static void stringJoining() {

        // joining() — concatenate with no separator
        String plain = names.stream().collect(Collectors.joining());
        System.out.println("joining(): " + plain);  // AliceBobCharlieAnnaBrianCarol

        // joining(delimiter)
        String csv = names.stream().collect(Collectors.joining(", "));
        System.out.println("joining(', '): " + csv);  // Alice, Bob, Charlie, Anna, Brian, Carol

        // joining(delimiter, prefix, suffix)
        String bracketed = names.stream().collect(Collectors.joining(", ", "[", "]"));
        System.out.println("joining with brackets: " + bracketed);  // [Alice, Bob, Charlie, ...]
    }

    // -------------------------------------------------------------------------
    // 4. groupingBy — THE MOST IMPORTANT COLLECTOR
    // -------------------------------------------------------------------------
    /**
     * groupingBy(classifier)               → Map<K, List<V>>
     * groupingBy(classifier, downstream)   → Map<K, whatever downstream produces>
     *
     * The DOWNSTREAM collector defines what happens to each group.
     */
    static void groupingByCollector() {

        // Basic groupingBy — groups into List (default)
        Map<Integer, List<String>> byLength = names.stream()
            .collect(Collectors.groupingBy(String::length));
        System.out.println("groupBy length: " + byLength);
        // {3=[Bob], 4=[Anna], 5=[Alice, Brian, Carol], 7=[Charlie]}

        // groupingBy + counting() — count per group
        Map<Integer, Long> countByLength = names.stream()
            .collect(Collectors.groupingBy(String::length, Collectors.counting()));
        System.out.println("groupBy + counting: " + countByLength);

        // groupingBy + maxBy() — find max per group  ← INTERVIEW FAVOURITE
        // Return type: Map<K, Optional<V>>
        Map<Integer, Optional<String>> longestByLength = names.stream()
            .collect(Collectors.groupingBy(
                String::length,
                Collectors.maxBy(Comparator.naturalOrder())
            ));
        longestByLength.forEach((len, nameOpt) ->
            nameOpt.ifPresent(n -> System.out.println("length " + len + " -> " + n))
        );

        // groupingBy + summingInt() — sum per group
        Map<Integer, Integer> totalLengthByGroup = names.stream()
            .collect(Collectors.groupingBy(
                n -> n.charAt(0),     // group by first character
                Collectors.summingInt(String::length)
            ));
        System.out.println("sum of lengths by first char: " + totalLengthByGroup);

        // groupingBy + mapping() — transform values before collecting
        // mapping(transformer, downstream)
        Map<Integer, List<String>> upperByLength = names.stream()
            .collect(Collectors.groupingBy(
                String::length,
                Collectors.mapping(String::toUpperCase, Collectors.toList())
            ));
        System.out.println("groupBy + mapping toUpperCase: " + upperByLength);
    }

    // -------------------------------------------------------------------------
    // 5. partitioningBy — splits into exactly 2 groups (true/false)
    // -------------------------------------------------------------------------
    static void partitioningByCollector() {

        // partitioningBy — like groupingBy but key is always Boolean
        Map<Boolean, List<Integer>> evenOdd = numbers.stream()
            .collect(Collectors.partitioningBy(n -> n % 2 == 0));
        System.out.println("even: " + evenOdd.get(true));   // [2, 4, 6, 8, 10]
        System.out.println("odd:  " + evenOdd.get(false));  // [1, 3, 5, 7, 9]

        // partitioningBy + counting()
        Map<Boolean, Long> evenOddCount = numbers.stream()
            .collect(Collectors.partitioningBy(n -> n % 2 == 0, Collectors.counting()));
        System.out.println("even count: " + evenOddCount.get(true));  // 5
        System.out.println("odd count:  " + evenOddCount.get(false)); // 5
    }

    // -------------------------------------------------------------------------
    // 6. collectingAndThen — post-process after collecting
    // -------------------------------------------------------------------------
    /**
     * Use when you want to transform the result of another collector.
     * Most common use: unwrap Optional that maxBy/minBy returns.
     *
     * collectingAndThen(downstream, finisher)
     *   downstream = any collector
     *   finisher   = function applied to the result of downstream
     */
    static void collectingAndThen() {

        // Problem: groupingBy + maxBy returns Map<K, Optional<V>>
        // But you want Map<K, V> — no Optional wrapper
        // Solution: collectingAndThen to unwrap the Optional

        Map<Integer, String> longestByLength = names.stream()
            .collect(Collectors.groupingBy(
                String::length,
                Collectors.collectingAndThen(
                    Collectors.maxBy(Comparator.naturalOrder()),  // gives Optional<String>
                    opt -> opt.orElse("none")                     // unwrap → just String
                )
            ));
        // Result: Map<Integer, String>  — no Optional!
        System.out.println("collectingAndThen (unwrapped): " + longestByLength);

        // Also useful for making the result unmodifiable after collecting
        List<String> unmodifiable = names.stream()
            .filter(n -> n.startsWith("A"))
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                Collections::unmodifiableList
            ));
        System.out.println("unmodifiable: " + unmodifiable);
    }
}
