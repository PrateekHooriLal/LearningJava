package com.streams.printing;

import java.util.*;
import java.util.stream.*;

/**
 * HOW TO PRINT MAPS AND LISTS — STUDY NOTES
 *
 * Covers every way to print List, Map, Map<K, Optional<V>>
 * Includes common mistakes made during practice.
 *
 * Notes file: Interview/notes/Altimetrik_Notes.md
 */
public class PrintingCollections {

    public static void main(String[] args) {

        System.out.println("=== Printing a List ===");
        printingList();

        System.out.println("\n=== Printing a Map ===");
        printingMap();

        System.out.println("\n=== Printing Map<String, Optional<V>> ===");
        printingMapWithOptional();

        System.out.println("\n=== Mistakes Reference ===");
        mistakesReference();
    }

    // -------------------------------------------------------------------------
    // PRINTING A LIST
    // -------------------------------------------------------------------------
    static void printingList() {

        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        // Way 1: System.out.println — prints whole list at once
        // Output: [Alice, Bob, Charlie]
        System.out.println(names);

        // Way 2: forEach with lambda
        // Output: Alice  Bob  Charlie (each on new line)
        names.forEach(name -> System.out.println(name));

        // Way 3: forEach with method reference (cleaner)
        names.forEach(System.out::println);

        // Way 4: for-each loop (old style, still valid)
        for (String name : names) {
            System.out.println(name);
        }

        // Way 5: stream + collect to string
        String joined = names.stream().collect(Collectors.joining(", "));
        System.out.println(joined); // Alice, Bob, Charlie
    }

    // -------------------------------------------------------------------------
    // PRINTING A MAP
    // -------------------------------------------------------------------------
    static void printingMap() {

        Map<String, Long> countPerDept = new HashMap<>();
        countPerDept.put("Engineering", 3L);
        countPerDept.put("Marketing", 2L);
        countPerDept.put("Finance", 2L);

        // Way 1: System.out.println — prints whole map at once
        // Output: {Engineering=3, Marketing=2, Finance=2}
        System.out.println(countPerDept);

        // Way 2: forEach — KEY is the department, VALUE is the count
        // (dept, count) — you NAME the key and value yourself, any name works
        countPerDept.forEach((dept, count) ->
            System.out.println(dept + " : " + count)
        );

        // Way 3: entrySet loop — when you need Map.Entry object
        for (Map.Entry<String, Long> entry : countPerDept.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }

        // Way 4: keySet loop — when you only need keys
        for (String key : countPerDept.keySet()) {
            System.out.println(key + " -> " + countPerDept.get(key));
        }

        // INTERVIEW TIP: forEach is cleanest for printing.
        // Use entrySet when you need to sort — you can stream entrySet.
        countPerDept.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(e -> System.out.println(e.getKey() + " : " + e.getValue()));
    }

    // -------------------------------------------------------------------------
    // PRINTING Map<String, Optional<V>>
    // -------------------------------------------------------------------------
    /**
     * This is the return type of groupingBy + maxBy.
     * The value is Optional<Employee>, NOT Employee directly.
     * You CANNOT call emp.getName() on an Optional — must unwrap first.
     *
     * Three ways to unwrap Optional:
     *   ifPresent(consumer) — safe, only runs if value exists   ← USE THIS for printing
     *   get()               — unsafe, throws if empty           ← NEVER use without isPresent check
     *   orElse(default)     — returns default if empty          ← use when you need a fallback value
     */
    static void printingMapWithOptional() {

        // Simulate Map<String, Optional<String>> — same pattern as groupingBy + maxBy
        Map<String, Optional<String>> topEarner = new HashMap<>();
        topEarner.put("Engineering", Optional.of("Charlie"));
        topEarner.put("Marketing",   Optional.of("Diana"));
        topEarner.put("Finance",     Optional.empty());   // simulate empty group

        // WAY 1: ifPresent — SAFEST, only prints if value exists
        // empOpt.ifPresent(emp -> ...) means: if Optional has a value, run this lambda
        topEarner.forEach((dept, empOpt) ->
            empOpt.ifPresent(emp ->
                System.out.println(dept + " -> " + emp)
            )
        );
        // Finance is skipped silently — Optional.empty(), ifPresent does nothing

        // WAY 2: orElse — prints a fallback when empty
        topEarner.forEach((dept, empOpt) ->
            System.out.println(dept + " -> " + empOpt.orElse("No employee"))
        );
        // Finance -> No employee

        // WAY 3: isPresent() + get() — old style, verbose, avoid
        topEarner.forEach((dept, empOpt) -> {
            if (empOpt.isPresent()) {
                System.out.println(dept + " -> " + empOpt.get());
            }
        });
    }

    // -------------------------------------------------------------------------
    // MISTAKES REFERENCE — from practice sessions
    // -------------------------------------------------------------------------
    static void mistakesReference() {

        System.out.println("See comments in this method for all mistakes.");

        // MISTAKE 1: maxBy takes Comparator, not method reference directly
        //   WRONG:   Collectors.maxBy(Employee::getSalary)
        //   CORRECT: Collectors.maxBy(Comparator.comparing(Employee::getSalary))

        // MISTAKE 2: Method references never have brackets
        //   WRONG:   Employee::getDepartment()
        //   CORRECT: Employee::getDepartment

        // MISTAKE 3: counting() takes NO arguments
        //   WRONG:   Collectors.counting(Comparator.comparing(Employee::getSalary))
        //   CORRECT: Collectors.counting()

        // MISTAKE 4: Getter names must match class exactly — Java is case sensitive
        //   WRONG:   e.getexp()      e.getDept()
        //   CORRECT: e.getExperience()  e.getDepartment()

        // MISTAKE 5: Variable name inconsistency — pick one name and stick to it
        //   WRONG:   (dept, empOut) -> empout.ifPresent(...)   // Out vs out
        //   CORRECT: (dept, empOpt) -> empOpt.ifPresent(...)

        // MISTAKE 6: Typos in System.out.println
        //   WRONG:   Sysytem.out.println
        //   CORRECT: System.out.println

        // MISTAKE 7: Method calls need brackets
        //   WRONG:   emp.getName     // this is a method reference, not a call
        //   CORRECT: emp.getName()   // () to actually call the method

        // MISTAKE 8: Arrow syntax — no space inside
        //   WRONG:   emp - > System.out.println(...)
        //   CORRECT: emp -> System.out.println(...)

        // ── QUICK REMINDER — forEach parameter names ──────────────────────────
        // You NAME the parameters yourself — Java doesn't care what you call them.
        // map.forEach((dept, count) -> ...)   // dept=key, count=value
        // map.forEach((k, v) -> ...)           // k=key, v=value — both valid
        // map.forEach((x, y) -> ...)           // also valid but less readable
        //
        // Rule: name them what they ARE, not k/v — makes code readable in interview
    }
}
