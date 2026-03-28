package com.streams.mistakes;

import java.util.*;
import java.util.stream.*;

/**
 * COMMON STREAM MISTAKES — Wrong vs Correct
 *
 * Every mistake here was made during actual practice sessions.
 * Read this file before every interview.
 *
 * Run main() to see the correct output for each case.
 */
public class CommonMistakes {

    public static void main(String[] args) {

        System.out.println("=== All correct versions compile and run ===");
        System.out.println("=== See comments for WRONG vs CORRECT ===\n");

        mistake1_maxByNeedsComparator();
        mistake2_methodReferenceNoBrackets();
        mistake3_countingNoArgs();
        mistake4_getterNameCaseSensitive();
        mistake5_variableNameConsistency();
        mistake6_systemOutTypo();
        mistake7_methodCallNeedsBrackets();
        mistake8_arrowNoSpace();
        mistake9_streamCannotReuse();
        mistake10_mapVsCollect();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 1 — maxBy needs Comparator, NOT a method reference directly
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake1_maxByNeedsComparator() {

        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        // WRONG — does not compile
        // Collectors.maxBy(String::length)

        // CORRECT — wrap in Comparator.comparing()
        Optional<String> longest = names.stream()
            .collect(Collectors.maxBy(Comparator.comparing(String::length)));
        longest.ifPresent(n -> System.out.println("Mistake 1 correct: " + n));

        // WHY: maxBy needs a Comparator<T> — an object that knows HOW to compare.
        //      String::length only extracts a value (Function<T,R>), not a Comparator.
        //      Comparator.comparing(String::length) turns the extractor into a Comparator.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 2 — Method references never have brackets
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake2_methodReferenceNoBrackets() {

        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        // WRONG — does not compile
        // names.stream().map(String::toUpperCase())

        // CORRECT — no brackets after method name
        names.stream()
            .map(String::toUpperCase)
            .forEach(n -> System.out.print(n + " "));
        System.out.println("← Mistake 2 correct");

        // WHY: String::toUpperCase is a REFERENCE to the method — like a pointer.
        //      Adding () would CALL the method — that is not what :: does.
        //      The stream calls the method for you when it processes each element.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 3 — counting() takes NO arguments
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake3_countingNoArgs() {

        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Anna");

        // WRONG — does not compile
        // Collectors.counting(Comparator.comparing(String::length))

        // CORRECT — no arguments
        Map<Integer, Long> countByLength = names.stream()
            .collect(Collectors.groupingBy(String::length, Collectors.counting()));
        System.out.println("Mistake 3 correct: " + countByLength);

        // WHY: counting() just COUNTS items in each group.
        //      It does not care about any field — no Comparator needed.
        //      Only maxBy/minBy need a Comparator because they compare values.
        //
        // EASY WAY TO REMEMBER:
        //   counting() → no args     (just count, nothing to configure)
        //   maxBy(...)  → needs arg  (needs to know WHICH field to compare)
        //   summingInt(...)→ needs arg (needs to know WHICH field to sum)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 4 — Getter names must match exactly (case sensitive)
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake4_getterNameCaseSensitive() {

        // Java field naming rule:
        // field: firstName  → getter: getFirstName()   (capitalise first letter after "get")
        // field: salary     → getter: getSalary()
        // field: experience → getter: getExperience()  NOT getexp() or getExp()
        // field: department → getter: getDepartment()  NOT getDept()
        // field: isActive   → getter: isActive()       (boolean uses "is" not "get")

        // WRONG — does not exist
        // e.getexp()
        // e.getDept()
        // e.getSal()

        // CORRECT — always use full camelCase getter name
        String name = "Alice";
        System.out.println("Mistake 4: getter rule = get + FieldName(first letter capital): getName() getSalary() getDepartment()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 5 — Variable name inconsistency in lambda
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake5_variableNameConsistency() {

        Map<String, Optional<String>> map = new HashMap<>();
        map.put("Engineering", Optional.of("Charlie"));

        // WRONG — defined as empOut but used as empout (different case)
        // map.forEach((dept, empOut) -> empout.ifPresent(...))

        // CORRECT — pick ONE name and use it consistently
        map.forEach((dept, empOpt) ->
            empOpt.ifPresent(emp ->
                System.out.println("Mistake 5 correct: " + dept + " -> " + emp)
            )
        );

        // TIP: Name lambda parameters what they ARE:
        //   (dept, empOpt)  → clear: dept=department name, empOpt=optional employee
        //   (k, v)          → ok but less readable
        //   (x, y)          → bad — meaningless names
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 6 — System.out.println typo
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake6_systemOutTypo() {

        // WRONG
        // Sysytem.out.println("hello")  ← extra 'y'
        // system.out.println("hello")   ← lowercase 's'

        // CORRECT
        System.out.println("Mistake 6 correct: System (capital S, no typo)");

        // TIP: In Eclipse type "syso" and press Ctrl+Space → auto-completes to System.out.println()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 7 — Method calls need brackets
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake7_methodCallNeedsBrackets() {

        String name = "Alice";

        // WRONG — this is a method REFERENCE, not a call
        // System.out.println(name.toUpperCase)   ← no brackets

        // CORRECT — brackets to actually call the method
        System.out.println("Mistake 7 correct: " + name.toUpperCase());

        // RULE:
        //   name.toUpperCase   → method reference (used in stream operations like .map())
        //   name.toUpperCase() → method call (used everywhere else)
        //
        // In a stream: .map(String::toUpperCase)   ← no brackets (reference)
        // Outside stream: name.toUpperCase()        ← brackets (call)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 8 — Arrow syntax: no space inside ->
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake8_arrowNoSpace() {

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

        // WRONG — space inside arrow
        // numbers.stream().filter(n - > n > 3)

        // CORRECT — arrow is two characters with no space between them
        numbers.stream()
            .filter(n -> n > 3)
            .forEach(n -> System.out.print(n + " "));
        System.out.println("← Mistake 8 correct");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 9 — Stream cannot be reused after terminal operation
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake9_streamCannotReuse() {

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

        // WRONG — reusing stream after terminal op causes IllegalStateException
        // Stream<Integer> stream = numbers.stream();
        // stream.count();     // terminal — stream is now consumed
        // stream.filter(...); // THROWS: stream has already been operated upon or closed

        // CORRECT — create a new stream each time
        Stream<Integer> stream1 = numbers.stream();
        long count = stream1.count();

        Stream<Integer> stream2 = numbers.stream();  // new stream
        List<Integer> filtered = stream2.filter(n -> n > 3).collect(Collectors.toList());

        System.out.println("Mistake 9 correct: count=" + count + " filtered=" + filtered);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISTAKE 10 — groupingBy goes INSIDE collect(), not chained directly
    // ─────────────────────────────────────────────────────────────────────────
    static void mistake10_mapVsCollect() {

        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Anna");

        // WRONG — groupingBy is a Collector, not a stream operation
        // names.stream().filter(...).groupingBy(String::length)  ← does not compile

        // CORRECT — groupingBy goes inside collect()
        Map<Integer, List<String>> grouped = names.stream()
            .collect(Collectors.groupingBy(String::length));
        System.out.println("Mistake 10 correct: " + grouped);

        // REMEMBER: stream operations are chained with .
        //   stream().filter().map().sorted()    ← all intermediate, chained
        //   .collect(Collectors.groupingBy(...)) ← terminal, Collector goes inside collect()
    }
}
