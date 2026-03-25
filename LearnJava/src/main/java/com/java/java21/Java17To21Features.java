package com.java.java21;

/**
 * ============================================================
 * Java 17 to 21 Features — Comprehensive Interview Study File
 * ============================================================
 *
 * CONCEPT:
 *   Covers the full feature progression from Java 14 through Java 21.
 *   Each section shows OLD vs NEW style, explains the WHY, and surfaces
 *   every angle that regularly appears in SDE 2/3 interviews.
 *
 * WHEN TO USE THIS FILE:
 *   Read top-to-bottom once for context, then scan the INTERVIEW ANGLE
 *   comments before any Java-focused interview.
 *
 * INTERVIEW ANGLE (top-level):
 *   Interviewers don't just ask "name a Java 21 feature." They ask:
 *     1. "Why was this added? What problem does it solve?"
 *     2. "What are the gotchas / limitations?"
 *     3. "How does it interact with existing code / frameworks?"
 *   Every section below is organized around those three questions.
 *
 * COMPILATION:
 *   javac --release 21 Java17To21Features.java
 *   java  com.java.java21.Java17To21Features
 *   (No --enable-preview needed — preview features are shown as comments only)
 *
 * VERSION TIMELINE (LTS markers are critical for interviews):
 *   Java 14 (Mar 2020) — Switch Expressions [final]
 *   Java 15 (Sep 2020) — Text Blocks [final]
 *   Java 16 (Mar 2021) — Records [final], Pattern Matching instanceof [final]
 *   Java 17 (Sep 2021) — Sealed Classes [final]  *** LTS ***
 *   Java 21 (Sep 2023) — Virtual Threads, Record Patterns, Pattern Switch,
 *                        Sequenced Collections, Scoped Values  *** LTS ***
 *
 * WHY LTS MATTERS IN INTERVIEWS:
 *   Most enterprises run LTS releases. Java 11 → Java 17 → Java 21 is the
 *   typical upgrade path. Knowing which features are LTS-final vs preview
 *   signals production readiness awareness.
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class Java17To21Features {

    // =========================================================================
    // SECTION 1: SWITCH EXPRESSIONS — Java 14 (final)
    // =========================================================================
    /**
     * CONCEPT:
     *   Java 12 introduced switch expressions as preview; Java 14 made them final.
     *   The KEY insight: a switch EXPRESSION produces a VALUE, unlike a switch
     *   STATEMENT which just executes side effects.
     *
     * PROBLEM WITH OLD SWITCH STATEMENTS:
     *   1. Fall-through: forgetting a `break` silently executes the next case.
     *   2. Cannot use as expression (cannot assign result directly).
     *   3. Not exhaustive — compiler doesn't force you to handle all enum values.
     *
     * INTERVIEW ANGLE:
     *   Q: "What's the difference between a switch statement and a switch expression?"
     *   A: Statement = side effects only, no value returned, fall-through possible.
     *      Expression = returns a value, no fall-through with -> syntax,
     *      compiler enforces exhaustiveness when used with sealed types or enums.
     *
     *   Q: "When would you use yield?"
     *   A: When you need a block (multiple statements) inside a switch expression arm.
     *      yield is to switch expressions what return is to methods — it "returns"
     *      the value from the block.
     *
     * GOTCHA:
     *   The colon-case style (case X:) still has fall-through even inside expressions.
     *   Only the arrow style (case X ->) is fall-through free.
     *   You can mix arrow arms but NOT colon and arrow arms in the same switch.
     */
    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    static void demonstrateSwitchExpressions() {
        System.out.println("\n--- SWITCH EXPRESSIONS (Java 14) ---");

        Day day = Day.WED;

        // OLD STYLE — switch statement, verbose, fall-through risk
        int oldResult;
        switch (day) {
            case MON:
            case TUE:
                oldResult = 1;
                break;
            case WED:
            case THU:
                oldResult = 2;
                break;
            default:
                oldResult = 3;
        }
        System.out.println("Old switch result: " + oldResult); // 2

        // NEW STYLE — switch expression, returns a value directly
        // Arrow (->) prevents fall-through entirely
        // Multiple labels on one arm: case MON, TUE
        int newResult = switch (day) {
            case MON, TUE -> 1;
            case WED, THU -> 2;
            case FRI, SAT, SUN -> 3;
            // No default needed here because all enum values are covered.
            // Compiler ENFORCES exhaustiveness — this is a big interview point.
        };
        System.out.println("New switch result: " + newResult); // 2

        // YIELD — used when you need a block in an arrow arm
        // yield is a "contextual keyword" — only means something inside switch expressions
        String label = switch (day) {
            case MON, TUE -> "early week";
            case WED -> {
                // Block arm: multiple statements allowed
                String mid = "mid";
                System.out.println("Computing label for WED");
                yield mid + "-week"; // yield = "return value from this block"
            }
            case THU, FRI -> "late week";
            case SAT, SUN -> "weekend";
        };
        System.out.println("Day label: " + label); // mid-week

        // SWITCH EXPRESSION AS ARGUMENT — very concise
        System.out.println("Is workday: " + switch (day) {
            case MON, TUE, WED, THU, FRI -> true;
            case SAT, SUN -> false;
        });
    }


    // =========================================================================
    // SECTION 2: TEXT BLOCKS — Java 15 (final)
    // =========================================================================
    /**
     * CONCEPT:
     *   Text blocks (""" ... """) allow multi-line string literals without
     *   explicit \n and escape characters. The JVM represents them as ordinary
     *   String objects — zero runtime overhead.
     *
     * THE INDENTATION ALGORITHM (interview critical):
     *   Java strips "incidental whitespace" — the common leading indent of all
     *   non-blank lines PLUS the closing """. This is called stripIndent().
     *   The position of the closing """ controls how much indent is removed.
     *
     *   Example:
     *     """
     *             line one    <- 12 spaces leading
     *             line two    <- 12 spaces leading
     *             """         <- closing delimiter at column 12
     *     Result: "line one\nline two\n"  (12 spaces stripped from each)
     *
     * TRAILING NEWLINE:
     *   If the closing """ is on its own line, the text block ends with \n.
     *   If closing """ is on the same line as last content, no trailing \n.
     *
     * INTERVIEW ANGLE:
     *   Q: "What is incidental vs essential whitespace?"
     *   A: Incidental = indent that only exists because the code is indented.
     *      Essential = indent that's actually part of the string value.
     *      Java removes incidental whitespace automatically.
     *
     *   Q: "How do you use String.formatted() with text blocks?"
     *   A: textBlock.formatted(args) — cleaner than String.format() when combined
     *      with text blocks. Equivalent to String.format(textBlock, args).
     *
     * GOTCHA:
     *   Text blocks are NOT raw strings. Escape sequences (\n, \t, \\) are still
     *   processed. Use \\ if you need a literal backslash.
     *   New in Java 15: \<line-terminator> (line continuation) and \s (space, prevents
     *   trailing whitespace stripping).
     */
    static void demonstrateTextBlocks() {
        System.out.println("\n--- TEXT BLOCKS (Java 15) ---");

        // OLD — escape-heavy, hard to read and maintain
        String oldJson = "{\n  \"name\": \"Alice\",\n  \"age\": 30\n}";
        System.out.println("Old JSON string:\n" + oldJson);

        // NEW — text block, clean, readable, matches actual format
        // The 8-space indent in code is stripped (incidental whitespace)
        String newJson = """
                {
                  "name": "Alice",
                  "age": 30
                }
                """;
        System.out.println("Text block JSON:\n" + newJson);

        // String.formatted() — the companion method for text blocks
        String template = """
                Name:  %s
                Score: %d
                Grade: %s
                """;
        String report = template.formatted("Bob", 95, "A");
        System.out.println("Formatted text block:\n" + report);

        // HTML example — demonstrates how useful text blocks are for templates
        String html = """
                <html>
                    <body>
                        <p>Hello, %s!</p>
                    </body>
                </html>
                """.formatted("World");
        System.out.println("HTML template:\n" + html);

        // LINE CONTINUATION — \<newline> joins lines, no newline in result
        String continuation = """
                This is one \
                long line""";
        System.out.println("Line continuation: '" + continuation + "'");
        // prints: 'This is one long line'

        // \s — explicit space, prevents trailing-space stripping
        String padded = """
                col1  \s
                col2  \s
                """;
        // Each line ends with two spaces (preserved by \s)
        System.out.println("Padded line length: " + padded.lines().findFirst().map(String::length).orElse(0));
    }


    // =========================================================================
    // SECTION 3: RECORDS — Java 16 (final)
    // =========================================================================
    /**
     * CONCEPT:
     *   A record is a transparent, immutable data carrier. Declaring
     *   `record Point(int x, int y) {}` auto-generates:
     *     - private final fields: x, y
     *     - canonical constructor: Point(int x, int y)
     *     - accessor methods: x(), y()  (NOT getX/getY — important!)
     *     - equals() based on all components
     *     - hashCode() based on all components
     *     - toString() showing all components
     *
     * WHEN TO USE:
     *   DTOs (Data Transfer Objects), value objects, method return types
     *   when you need to return multiple values, immutable config objects.
     *
     * INTERVIEW ANGLE:
     *   Q: "How is a Record different from a regular class?"
     *   A: Records are implicitly final (cannot be extended), implicitly extend
     *      java.lang.Record. Their state is fixed at construction (immutable).
     *      Boilerplate (equals/hashCode/toString/constructor) is auto-generated.
     *
     *   Q: "Can a Record implement an interface?"
     *   A: YES. Records can implement interfaces but CANNOT extend classes
     *      (they already extend Record implicitly).
     *
     *   Q: "What is a compact constructor?"
     *   A: A constructor form with no parameter list — the parameters are implicitly
     *      present and automatically assigned AFTER the body runs. Use it for
     *      validation and normalization without repeating field assignment.
     *
     *   Q: "Are Records JavaBeans?"
     *   A: NO. JavaBeans require no-arg constructors and getX()/setX() methods.
     *      Records use x() accessors and have no setters (immutable). Frameworks
     *      that rely on JavaBean conventions (old Hibernate, some Spring configs)
     *      may not work with Records directly.
     *
     *   Q: "Can you add custom methods to a Record?"
     *   A: Yes. You can add instance methods, static methods, and even override
     *      equals/hashCode/toString — but you cannot add instance fields beyond
     *      the declared components.
     *
     * GOTCHA:
     *   Records are SHALLOWLY immutable. If a component is a mutable type (e.g., List),
     *   the reference is final but the list itself can be mutated.
     *   Defensive copying in compact constructor is needed for true immutability.
     *
     * COMPLEXITY:
     *   equals/hashCode: O(n) where n = number of components (field comparison).
     */

    // Basic record — one line replaces ~40 lines of boilerplate
    record Point(int x, int y) {}

    // Record with interface implementation
    interface Describable {
        String describe();
    }

    record NamedPoint(int x, int y, String name) implements Describable {
        @Override
        public String describe() {
            return name + " at (" + x + ", " + y + ")";
        }

        // Custom instance method — allowed
        public double distanceFromOrigin() {
            return Math.sqrt(x * x + y * y);
        }
    }

    // Compact constructor — no parentheses, body runs before implicit field assignment
    record Person(String name, int age) {
        // Compact constructor: validates and normalizes before components are stored
        Person {
            // 'name' and 'age' are in scope but NOT yet assigned to fields
            if (age < 0) throw new IllegalArgumentException("Age cannot be negative: " + age);
            if (age > 150) throw new IllegalArgumentException("Unrealistic age: " + age);
            name = name.trim(); // can reassign the parameter — it gets stored in the field
            // After body completes, this.name = name and this.age = age happen automatically
        }
    }

    // Record with defensive copy for deep immutability
    record ImmutableTeam(String name, List<String> members) {
        ImmutableTeam {
            // Without this copy, external code can mutate the list via the original reference
            members = List.copyOf(members); // unmodifiable copy — true immutability
        }
    }

    static void demonstrateRecords() {
        System.out.println("\n--- RECORDS (Java 16) ---");

        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        Point p3 = new Point(1, 2);

        // Accessor methods are component name, NOT getX()
        System.out.println("p1.x() = " + p1.x() + ", p1.y() = " + p1.y());

        // equals() compares by value (all components)
        System.out.println("p1.equals(p2) = " + p1.equals(p2)); // true
        System.out.println("p1.equals(p3) = " + p1.equals(p3)); // false

        // toString() is human-readable out of the box
        System.out.println("p1.toString() = " + p1); // Point[x=3, y=4]

        // hashCode() consistent with equals
        System.out.println("p1.hashCode() == p2.hashCode(): " + (p1.hashCode() == p2.hashCode())); // true

        // Custom methods
        NamedPoint np = new NamedPoint(3, 4, "Origin");
        System.out.println(np.describe());
        System.out.printf("Distance from origin: %.2f%n", np.distanceFromOrigin()); // 5.00

        // Compact constructor validation
        try {
            new Person("Alice", -5);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage()); // Age cannot be negative: -5
        }
        Person alice = new Person("  Alice  ", 30);
        System.out.println("Trimmed name: '" + alice.name() + "'"); // 'Alice'

        // Deep immutability via defensive copy
        List<String> original = new ArrayList<>(List.of("Alice", "Bob"));
        ImmutableTeam team = new ImmutableTeam("Dev", original);
        original.add("Mallory"); // mutate original
        System.out.println("Team members unchanged: " + team.members()); // [Alice, Bob]

        // Records work well as Map keys (consistent equals/hashCode)
        Map<Point, String> pointLabels = new HashMap<>();
        pointLabels.put(new Point(0, 0), "origin");
        System.out.println("Map lookup: " + pointLabels.get(new Point(0, 0))); // origin
    }


    // =========================================================================
    // SECTION 4: PATTERN MATCHING FOR instanceof — Java 16 (final)
    // =========================================================================
    /**
     * CONCEPT:
     *   Combines the instanceof type check with a variable binding in one step.
     *   Eliminates the redundant cast that always followed instanceof in old code.
     *
     * SCOPE RULES (interview trap):
     *   The binding variable (e.g., `s` in `obj instanceof String s`) is in scope
     *   only where the match is DEFINITELY true:
     *   - In the true branch of an if
     *   - In the right side of && (short-circuit ensures check passed)
     *   - NOT in the else branch
     *   - NOT in the right side of || (can reach even if check failed)
     *
     * INTERVIEW ANGLE:
     *   Q: "What is the scope of the binding variable?"
     *   A: It's scoped to the region where the match is guaranteed true.
     *      `if (obj instanceof String s && s.length() > 5)` — s is valid in
     *      the condition because && short-circuits (s can only be accessed if
     *      instanceof succeeded).
     *
     *   Q: "Does this change bytecode?"
     *   A: Yes, but it's a pattern matching feature compiled to checkcast.
     *      The binding variable avoids a second cast instruction.
     *
     * GOTCHA:
     *   You can shadow a local variable with the binding variable name — avoid this.
     *   The binding variable is NOT the same as the original — it's a new local.
     */
    static void demonstratePatternMatchingInstanceof() {
        System.out.println("\n--- PATTERN MATCHING instanceof (Java 16) ---");

        Object obj = "Hello, Java 21!";

        // OLD — tedious: check, then cast, then use
        if (obj instanceof String) {
            String s = (String) obj; // redundant cast — compiler KNOWS it's a String
            System.out.println("OLD: length = " + s.length());
        }

        // NEW — check and bind in one expression
        if (obj instanceof String s) {
            // s is automatically cast and bound — no explicit cast needed
            System.out.println("NEW: length = " + s.length());
            System.out.println("NEW: uppercase = " + s.toUpperCase());
        }

        // SCOPE IN && — s is valid in the condition because && short-circuits
        if (obj instanceof String s && s.length() > 5) {
            System.out.println("Long string: " + s);
        }

        // NEGATION — s is valid in the ELSE or after early return
        if (!(obj instanceof String s)) {
            System.out.println("Not a string");
            return;
        }
        // s is in scope here because we know we didn't return
        System.out.println("Definitely a string: " + s.length());

        // Works with any type — useful for polymorphic dispatch without casting
        processObject("text");
        processObject(42);
        processObject(3.14);
        processObject(List.of(1, 2, 3));
        processObject(null);
    }

    static void processObject(Object obj) {
        // Before pattern matching: if-else instanceof chain with manual casts
        // After pattern matching: same structure but binding is automatic
        if (obj instanceof String s) {
            System.out.println("  String of length " + s.length() + ": " + s);
        } else if (obj instanceof Integer i) {
            System.out.println("  Integer, doubled: " + (i * 2));
        } else if (obj instanceof Double d) {
            System.out.printf("  Double, formatted: %.4f%n", d);
        } else if (obj instanceof List<?> list) {
            System.out.println("  List with " + list.size() + " elements");
        } else {
            System.out.println("  Unknown type or null: " + obj);
        }
    }


    // =========================================================================
    // SECTION 5: SEALED CLASSES — Java 17 (final, LTS)
    // =========================================================================
    /**
     * CONCEPT:
     *   A sealed class (or interface) restricts which other classes can extend/implement it.
     *   The `permits` clause lists the ONLY allowed subclasses.
     *
     * MOTIVATION:
     *   Before sealed classes, you couldn't express "this type has exactly these subtypes"
     *   without enums. But enums can't have different fields per variant.
     *   Sealed classes fill this gap: a closed type hierarchy where each subtype
     *   can have its own fields, methods, and behavior.
     *
     * THE THREE SUBTYPE MODIFIERS (all must appear on subclasses):
     *   1. final     — cannot be extended further (most common for leaf types)
     *   2. sealed    — can be extended, but only by its own permits list
     *   3. non-sealed — reopens the hierarchy; anyone can extend (use sparingly)
     *
     * EXHAUSTIVENESS — the killer feature:
     *   A sealed hierarchy gives the compiler complete knowledge of all subtypes.
     *   A switch expression over a sealed type can be EXHAUSTIVE without `default`.
     *   If you add a new subtype, the compiler flags all non-exhaustive switches.
     *   This makes refactoring safer.
     *
     * INTERVIEW ANGLE:
     *   Q: "Why sealed classes instead of enums?"
     *   A: Enums have one instance per variant with no per-instance state differences.
     *      Sealed classes allow different fields per subtype. A Circle has a radius;
     *      a Rectangle has width and height. You can't model that cleanly with enums.
     *
     *   Q: "What is the relationship between sealed classes and pattern matching?"
     *   A: They're designed together. Sealed classes enable exhaustive pattern matching
     *      in switch expressions — the compiler knows all possible subtypes.
     *
     *   Q: "Where must the subclasses be defined?"
     *   A: In Java 17, they must be in the SAME package (or same module if using modules).
     *      They can be in different files but the package must match.
     *      If they're in the same source file (unnamed/named), no permits clause needed
     *      — the compiler infers it.
     *
     *   Q: "What did Java 17 remove alongside sealed classes?"
     *   A: The --illegal-access flag (strong encapsulation of JDK internals),
     *      removal of the Applet API, deprecation of the Security Manager.
     *      Java 17 is the LTS where the JDK cleanup happened.
     *
     * GOTCHA:
     *   non-sealed reopens the hierarchy — it's a conscious design choice to say
     *   "this branch can be extended by anyone." If you non-sealed a subclass,
     *   exhaustiveness checking is lost for that branch.
     *
     * COMPLEXITY:
     *   Dispatch over a sealed hierarchy in a switch = O(1) for the compiler;
     *   same as method dispatch. No performance difference from inheritance.
     */

    // Sealed interface — Shape is the root of a closed hierarchy
    sealed interface Shape permits Circle, Rectangle, Triangle {
        double area(); // contract all shapes must fulfill

        // Default method — works fine with sealed
        default String kind() {
            // Pattern matching switch (Java 21) — sealed permits Circle/Rectangle/Triangle.
            // BUT Triangle is non-sealed (anyone can subclass it), so the compiler
            // cannot guarantee exhaustiveness — a default arm IS required.
            return switch (this) {
                case Circle c    -> "circle";
                case Rectangle r -> "rectangle";
                case Triangle t  -> "triangle";
                default          -> "unknown"; // required because Triangle is non-sealed
            };
        }
    }

    // final — Circle is a leaf, no further subclassing
    record Circle(double radius) implements Shape {
        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }

    // final — Rectangle is a leaf
    record Rectangle(double width, double height) implements Shape {
        @Override
        public double area() {
            return width * height;
        }
    }

    // non-sealed — anyone can extend Triangle (opens the hierarchy back up)
    // This means exhaustiveness is lost for Triangle in switch expressions
    static non-sealed class Triangle implements Shape {
        private final double base, height;

        Triangle(double base, double height) {
            this.base = base;
            this.height = height;
        }

        @Override
        public double area() {
            return 0.5 * base * height;
        }
    }

    // Nested sealed hierarchy — common in domain modeling
    sealed interface Result<T> permits Result.Success, Result.Failure {
        record Success<T>(T value) implements Result<T> {}
        record Failure<T>(String error, Exception cause) implements Result<T> {}
    }

    static void demonstrateSealedClasses() {
        System.out.println("\n--- SEALED CLASSES (Java 17) ---");

        List<Shape> shapes = List.of(
                new Circle(5),
                new Rectangle(4, 6),
                new Triangle(3, 8)
        );

        for (Shape shape : shapes) {
            // Pattern matching switch — exhaustive over sealed hierarchy
            String description = switch (shape) {
                case Circle c    -> String.format("Circle(r=%.1f, area=%.2f)", c.radius(), c.area());
                case Rectangle r -> String.format("Rectangle(%.1fx%.1f, area=%.2f)", r.width(), r.height(), r.area());
                case Triangle t  -> String.format("Triangle(area=%.2f)", t.area());
                // default IS required here because Triangle is non-sealed — unknown subtypes possible
                default          -> "Unknown shape subtype";
            };
            System.out.println(description);
        }

        // Sealed Result type — functional error handling pattern
        Result<Integer> success = new Result.Success<>(42);
        Result<Integer> failure = new Result.Failure<>("Not found", new RuntimeException("404"));

        processResult(success);
        processResult(failure);

        // kind() using sealed + pattern matching in default method
        System.out.println("Shape kinds: " + shapes.stream()
                .map(Shape::kind)
                .toList());
    }

    static void processResult(Result<?> result) {
        // Exhaustive switch over sealed Result — no default needed for Success/Failure
        switch (result) {
            case Result.Success<?> s -> System.out.println("Success: " + s.value());
            case Result.Failure<?> f -> System.out.println("Failure: " + f.error());
        }
    }


    // =========================================================================
    // SECTION 6: VIRTUAL THREADS — Java 21 (final, Project Loom)
    // =========================================================================
    /**
     * CONCEPT:
     *   Virtual threads are lightweight threads managed by the JVM, not the OS.
     *   They're "virtual" because many of them are multiplexed onto a small pool
     *   of OS threads called "carrier threads."
     *
     * THE PROBLEM WITH PLATFORM THREADS:
     *   Each platform thread = one OS thread = ~1MB stack = expensive.
     *   A typical server can handle ~thousands of platform threads.
     *   I/O-bound tasks spend 99% of their time BLOCKING — wasting OS threads.
     *   This is the C10K problem: serving 10,000 concurrent connections is hard.
     *
     * HOW VIRTUAL THREADS SOLVE IT:
     *   When a virtual thread blocks on I/O, the JVM UNMOUNTS it from the carrier
     *   thread. The carrier thread is free to run another virtual thread.
     *   When the I/O completes, the VT is REMOUNTED on any available carrier thread.
     *   You can have MILLIONS of virtual threads — one per request is fine.
     *
     * THREAD-PER-REQUEST MODEL:
     *   The entire point of virtual threads is to make the thread-per-request model
     *   viable again. Instead of reactive/async code, write simple blocking code
     *   but with virtual threads. The JVM handles the concurrency.
     *
     * INTERVIEW ANGLE:
     *   Q: "How do virtual threads differ from platform threads?"
     *   A: Platform threads are 1:1 with OS threads (expensive, limited ~thousands).
     *      Virtual threads are M:N — millions of VTs on a small carrier pool.
     *      VTs are cheap (small stack, ~kB), platform threads are expensive (~MB).
     *
     *   Q: "Do virtual threads replace platform threads?"
     *   A: NO. CPU-bound work should still use platform threads.
     *      VTs are designed for I/O-bound, blocking workloads.
     *      For CPU-bound tasks, ForkJoinPool (platform threads) is still correct.
     *
     *   Q: "What is pinning and why is it a problem?"
     *   A: Pinning = a virtual thread gets STUCK on its carrier thread and cannot
     *      be unmounted. This defeats the purpose of virtual threads.
     *      Causes of pinning:
     *        1. synchronized blocks/methods (most common)
     *        2. native methods (JNI)
     *      Fix: Replace synchronized with ReentrantLock (VT-friendly).
     *      Java 24 is improving this, but for Java 21 it's still an issue.
     *
     *   Q: "What frameworks support virtual threads?"
     *   A: Spring Boot 3.2+ (set spring.threads.virtual.enabled=true),
     *      Tomcat 10.1+, Helidon, Quarkus. Most modern frameworks adapted.
     *
     *   Q: "Is ThreadLocal safe with virtual threads?"
     *   A: It works but can cause memory leaks. VTs are created/destroyed rapidly.
     *      ThreadLocal values won't be GC'd until the VT is GC'd.
     *      Use ScopedValues instead (Java 21 preview → Java 23 final).
     *
     * GOTCHA:
     *   Don't use virtual threads for CPU-bound work — they run on carrier threads
     *   anyway, so you'd get the same parallelism but with more overhead.
     *   Virtual threads don't make code faster — they make it more SCALABLE
     *   by reducing thread blocking waste.
     *
     * GOTCHA 2:
     *   Thread.sleep() inside a virtual thread is fine and cheap — it unmounts.
     *   synchronized block inside a virtual thread pins it — use ReentrantLock.
     */
    static void demonstrateVirtualThreads() throws Exception {
        System.out.println("\n--- VIRTUAL THREADS (Java 21) ---");

        // === Creating Virtual Threads ===

        // Method 1: Thread.ofVirtual().start() — simple one-off VT
        Thread vt1 = Thread.ofVirtual().start(() ->
                System.out.println("VT1 running on: " + Thread.currentThread()));
        vt1.join();

        // Method 2: Thread.ofVirtual().name() — named VT for debugging
        Thread vt2 = Thread.ofVirtual()
                .name("my-vt-", 0) // auto-numbering: my-vt-0, my-vt-1, etc.
                .start(() -> System.out.println("VT2 name: " + Thread.currentThread().getName()));
        vt2.join();

        // Method 3: Thread.startVirtualThread() — shortest form
        Thread vt3 = Thread.startVirtualThread(() ->
                System.out.println("VT3 isVirtual: " + Thread.currentThread().isVirtual()));
        vt3.join();

        // Method 4: ExecutorService — best for batches of tasks
        // newVirtualThreadPerTaskExecutor creates one VT per submitted task
        // Unlike thread pools, there is no pool — VTs are cheap enough to not pool
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                futures.add(executor.submit(() -> {
                    // Simulate I/O — Thread.sleep unmounts VT from carrier
                    Thread.sleep(10);
                    return "Task " + taskId + " on " + Thread.currentThread().getName();
                }));
            }

            for (Future<String> f : futures) {
                System.out.println("  " + f.get());
            }
        } // executor.close() blocks until all tasks finish — part of structured model

        // === Demonstrating Scale ===
        // Creating 10,000 virtual threads — would be impractical with platform threads
        int taskCount = 10_000;
        CountDownLatch latch = new CountDownLatch(taskCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    Thread.sleep(1); // simulated I/O
                    latch.countDown();
                    return null;
                });
            }
        }
        latch.await();
        System.out.println("Completed " + taskCount + " virtual thread tasks");

        // === VT-Safe Lock: ReentrantLock instead of synchronized ===
        // synchronized PINS a virtual thread to its carrier — bad for throughput
        // ReentrantLock does NOT pin — VT can unmount while waiting for the lock

        ReentrantLock lock = new ReentrantLock();
        int[] counter = {0}; // array trick for effectively-final in lambda

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                futures.add(executor.submit(() -> {
                    lock.lock(); // VT-friendly: can unmount while waiting
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) f.get();
        }
        System.out.println("Counter (VT-safe lock): " + counter[0]); // 100

        // === Thread characteristics ===
        Thread.ofVirtual().start(() -> {
            Thread t = Thread.currentThread();
            System.out.println("isVirtual: " + t.isVirtual());       // true
            System.out.println("isDaemon: " + t.isDaemon());         // always true for VTs
            // VTs are always daemon threads — JVM won't wait for them to finish
        }).join();
    }


    // =========================================================================
    // SECTION 7: RECORD PATTERNS — Java 21 (final)
    // =========================================================================
    /**
     * CONCEPT:
     *   Extends pattern matching to DECONSTRUCT records.
     *   Instead of extracting components manually after matching, the pattern
     *   extracts them in one step.
     *
     * EVOLUTION:
     *   Java 16: instanceof String s         — type pattern (binds the object)
     *   Java 21: instanceof Point(int x, int y) — record pattern (deconstructs)
     *
     * INTERVIEW ANGLE:
     *   Q: "What's the difference between a type pattern and a record pattern?"
     *   A: Type pattern: `obj instanceof String s` — s is bound to obj cast to String.
     *      Record pattern: `obj instanceof Point(int x, int y)` — x and y are extracted
     *      directly from the record's components. No need to call p.x() and p.y().
     *
     *   Q: "Can you nest record patterns?"
     *   A: Yes. `obj instanceof Line(Point(int x1, int y1), Point(int x2, int y2))`
     *      This is powerful for deeply nested data structures (ADTs, ASTs).
     *
     *   Q: "What happens if the record doesn't match?"
     *   A: The pattern fails — like any instanceof. The binding variables are not
     *      in scope. No exception is thrown.
     *
     * GOTCHA:
     *   Record patterns require the object to be a record. They work with sealed
     *   hierarchies of records beautifully (see Section 5 + Section 8 together).
     */

    record Line(Point start, Point end) {}

    record ColoredPoint(Point point, String color) {}

    static void demonstrateRecordPatterns() {
        System.out.println("\n--- RECORD PATTERNS (Java 21) ---");

        Object obj = new Point(3, 4);

        // Old style: instanceof check + accessor calls
        if (obj instanceof Point p) {
            System.out.println("Old: " + p.x() + ", " + p.y()); // need p.x() call
        }

        // New: record pattern — deconstructs in one shot
        if (obj instanceof Point(int x, int y)) {
            System.out.println("Record pattern: " + x + ", " + y); // x and y directly bound
        }

        // NESTED record patterns — deconstruct all the way down
        Object lineObj = new Line(new Point(0, 0), new Point(3, 4));
        if (lineObj instanceof Line(Point(int x1, int y1), Point(int x2, int y2))) {
            double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            System.out.printf("Line length: %.2f%n", length); // 5.00
        }

        // Record patterns in switch (Java 21 pattern switch)
        List<Object> items = List.of(
                new Point(1, 2),
                new ColoredPoint(new Point(3, 4), "red"),
                new Line(new Point(0, 0), new Point(1, 1)),
                "not a record"
        );

        for (Object item : items) {
            String desc = switch (item) {
                case Point(int x, int y)
                    -> "Point at (" + x + "," + y + ")";
                case ColoredPoint(Point(int x, int y), String color)
                    -> color + " point at (" + x + "," + y + ")";  // nested deconstruction
                case Line(Point(int x1, int y1), Point(int x2, int y2))
                    -> "Line from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
                default -> "unknown: " + item;
            };
            System.out.println("  " + desc);
        }

        // Underscore / var patterns — use var when type is obvious
        if (obj instanceof Point(var px, var py)) {
            // var infers int — useful when type is long or obvious from context
            System.out.println("With var: " + px + ", " + py);
        }
    }


    // =========================================================================
    // SECTION 8: PATTERN MATCHING FOR SWITCH — Java 21 (final)
    // =========================================================================
    /**
     * CONCEPT:
     *   Extends switch to work with arbitrary types (not just int/String/enum),
     *   supports pattern matching in case labels, guarded patterns with `when`,
     *   and explicit null handling.
     *
     * EVOLUTION:
     *   Java 14: switch expressions (values, no fall-through with ->)
     *   Java 17: switch with sealed classes (exhaustiveness)
     *   Java 21: switch with full pattern matching (types, guards, null, deconstruction)
     *
     * KEY FEATURES:
     *   1. Type patterns: case Integer i ->
     *   2. Record patterns: case Point(int x, int y) ->
     *   3. Guarded patterns: case String s when s.length() > 5 ->
     *   4. Null handling: case null ->
     *   5. Dominance rules: more specific patterns MUST come before general ones
     *
     * DOMINANCE (interview trap):
     *   A pattern P1 dominates P2 if every value matching P2 also matches P1.
     *   Example: `case Object o` dominates `case String s` because every String
     *   is an Object. The compiler REJECTS code with dominated (unreachable) patterns.
     *   Order matters: put specific (narrow) patterns BEFORE general (wide) ones.
     *
     * INTERVIEW ANGLE:
     *   Q: "What is a guarded pattern?"
     *   A: A pattern with an additional boolean condition: `case String s when s.isEmpty()`.
     *      The `when` clause is evaluated only if the type pattern matches.
     *      Old syntax was `&&` in the condition, but `when` is cleaner and official.
     *
     *   Q: "Can switch handle null in Java 21?"
     *   A: Yes. `case null ->` is now valid. Without it, a null value still throws
     *      NullPointerException. You must explicitly add `case null` to handle it.
     *      You can also combine: `case null, default ->`.
     *
     *   Q: "What does exhaustive mean in pattern switch?"
     *   A: For a switch over a sealed type, all subtypes must be covered.
     *      If not, the compiler gives an error — no silent NPE at runtime.
     *      For non-sealed types (Object, etc.), a `default` or catch-all is required.
     *
     * GOTCHA:
     *   switch(null) — passing null as the selector throws NPE regardless of
     *   whether you have a `case null` arm. The null check is on the LABEL, not
     *   the selector expression itself... actually wait: in Java 21, if you have
     *   `case null` in the switch, the null selector IS handled. Without it, NPE.
     */
    static void demonstratePatternSwitch() {
        System.out.println("\n--- PATTERN MATCHING FOR SWITCH (Java 21) ---");

        // Basic pattern switch over Object
        List<Object> values = List.of(42, "hello", 3.14, true, "hi", List.of(1,2,3));

        for (Object val : values) {
            String result = switch (val) {
                case Integer i when i > 100 -> "big integer: " + i;   // guarded pattern
                case Integer i              -> "small integer: " + i;  // more general, must come after
                case String s when s.length() <= 2 -> "short string: " + s;
                case String s               -> "long string: " + s;
                case Double d               -> String.format("double: %.2f", d);
                case Boolean b              -> "boolean: " + b;
                case List<?> list           -> "list of size " + list.size();
                default                    -> "unknown: " + val; // required because Object is not sealed
            };
            System.out.println("  " + result);
        }

        // NULL HANDLING — explicit case null
        processWithNull("value");
        processWithNull(null);
        processWithNull(42);

        // SEALED HIERARCHY — exhaustive, no default needed
        List<Shape> shapes = List.of(new Circle(3), new Rectangle(4, 5), new Triangle(2, 6));
        for (Shape shape : shapes) {
            double perimeter = switch (shape) {
                case Circle c    -> 2 * Math.PI * c.radius();
                case Rectangle r -> 2 * (r.width() + r.height());
                case Triangle t  -> shape.area() * 2 / 3; // approximation
                // default IS required because Triangle is non-sealed — compiler cannot
                // guarantee Circle/Rectangle/Triangle are the only concrete subtypes of Shape
                default          -> 0.0;
            };
            System.out.printf("  %s perimeter ≈ %.2f%n", shape.kind(), perimeter);
        }

        // RECORD PATTERNS in switch
        List<Object> points = List.of(
                new Point(0, 0),
                new Point(3, 0),
                new Point(0, 4),
                new Point(3, 4)
        );
        for (Object p : points) {
            String location = switch (p) {
                case Point(int x, int y) when x == 0 && y == 0 -> "origin";
                case Point(int x, int y) when x == 0           -> "on Y-axis at y=" + y;
                case Point(int x, int y) when y == 0           -> "on X-axis at x=" + x;
                case Point(int x, int y)                       -> "at (" + x + "," + y + ")";
                default                                        -> "not a point";
            };
            System.out.println("  Point location: " + location);
        }
    }

    static void processWithNull(Object obj) {
        String result = switch (obj) {
            case null       -> "null input";     // explicit null handling
            case String s   -> "string: " + s;
            case Integer i  -> "int: " + i;
            default         -> "other: " + obj;
        };
        System.out.println("  processWithNull: " + result);
    }


    // =========================================================================
    // SECTION 9: SEQUENCED COLLECTIONS — Java 21 (final)
    // =========================================================================
    /**
     * CONCEPT:
     *   Java 21 added three new interfaces to the collections hierarchy:
     *     SequencedCollection<E>  — ordered collection with defined first/last
     *     SequencedSet<E>         — extends SequencedCollection, no duplicates
     *     SequencedMap<K,V>       — map with defined first/last entry order
     *
     *   These fill a gap: there was no uniform way to access the first or last
     *   element of a collection without knowing its concrete type.
     *
     * THE GAP (interview hook):
     *   Before Java 21:
     *     list.get(0)                    // first element
     *     list.get(list.size() - 1)      // last element — ugly
     *     deque.peekFirst()              // Deque only
     *     sortedSet.first()              // SortedSet only
     *     linkedHashMap.entrySet().iterator().next() // first entry of LinkedHashMap — horrible
     *
     *   After Java 21:
     *     collection.getFirst()          // works on List, Deque, LinkedHashSet...
     *     collection.getLast()           // uniform across all sequenced collections
     *
     * NEW METHODS on SequencedCollection:
     *   getFirst()  / getLast()      — access without index arithmetic
     *   addFirst(e) / addLast(e)     — add at ends (not for all implementations)
     *   removeFirst() / removeLast() — remove from ends
     *   reversed()                   — returns a VIEW in reverse order (no copy!)
     *
     * HIERARCHY ADDITIONS:
     *   List       now implements SequencedCollection
     *   Deque      now implements SequencedCollection
     *   LinkedHashSet now implements SequencedSet
     *   LinkedHashMap now implements SequencedMap
     *   SortedSet  now implements SequencedSet
     *   SortedMap  now implements SequencedMap
     *
     * INTERVIEW ANGLE:
     *   Q: "What problem do SequencedCollections solve?"
     *   A: Uniform API for first/last access across collection types. Before Java 21,
     *      every collection type had a different idiom. Now it's uniform.
     *
     *   Q: "Does reversed() copy the collection?"
     *   A: NO. It returns a VIEW. Modifications to the view reflect in the original
     *      and vice versa. It's O(1) to create the reversed view.
     *
     *   Q: "What about ArrayList.getFirst() vs ArrayList.get(0)?"
     *   A: Equivalent result, but getFirst() throws NoSuchElementException if empty,
     *      while get(0) throws IndexOutOfBoundsException. Semantics differ slightly.
     *
     * GOTCHA:
     *   addFirst()/addLast() on ArrayList is O(n) because it shifts elements.
     *   Use LinkedList/ArrayDeque if you need efficient head/tail insertions.
     *   reversed() on a list returns a non-modifiable view... except for List,
     *   which returns a modifiable reversed view backed by the original.
     */
    static void demonstrateSequencedCollections() {
        System.out.println("\n--- SEQUENCED COLLECTIONS (Java 21) ---");

        // === SequencedCollection via List ===
        List<String> list = new ArrayList<>(List.of("banana", "apple", "cherry", "date"));
        System.out.println("Original: " + list);

        // NEW uniform API — no more list.get(list.size() - 1)
        System.out.println("getFirst(): " + list.getFirst()); // banana
        System.out.println("getLast():  " + list.getLast());  // date

        // addFirst/addLast
        list.addFirst("avocado");
        list.addLast("elderberry");
        System.out.println("After addFirst/Last: " + list);

        // removeFirst/removeLast
        String first = list.removeFirst();
        String last  = list.removeLast();
        System.out.println("Removed first: " + first + ", last: " + last);
        System.out.println("After remove: " + list);

        // reversed() — O(1) view, no copy
        List<String> reversed = list.reversed();
        System.out.println("Reversed view: " + reversed);

        // Reversed view is backed by original — modifications reflect both ways
        list.set(0, "MODIFIED");
        System.out.println("After modifying original, reversed: " + reversed); // first element changed

        // === SequencedSet via LinkedHashSet ===
        // LinkedHashSet maintains insertion order — now has first/last access
        SequencedCollection<Integer> linkedSet = new LinkedHashSet<>(List.of(3, 1, 4, 1, 5, 9));
        System.out.println("\nLinkedHashSet: " + linkedSet);
        System.out.println("Set getFirst(): " + linkedSet.getFirst()); // 3
        System.out.println("Set getLast():  " + linkedSet.getLast());  // 9

        // === SequencedMap via LinkedHashMap ===
        SequencedMap<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        scores.put("Carol", 92);

        System.out.println("\nLinkedHashMap: " + scores);
        System.out.println("First entry: " + scores.firstEntry());  // Alice=95
        System.out.println("Last entry:  " + scores.lastEntry());   // Carol=92
        System.out.println("First key:   " + scores.firstEntry().getKey());

        // reversed() on SequencedMap
        System.out.println("Reversed map: " + scores.reversed());

        // === Deque is also a SequencedCollection ===
        Deque<String> deque = new ArrayDeque<>(List.of("x", "y", "z"));
        System.out.println("\nDeque getFirst(): " + deque.getFirst()); // x
        System.out.println("Deque getLast():  " + deque.getLast());   // z
    }


    // =========================================================================
    // SECTION 10: SCOPED VALUES — Java 21 (Preview)
    // =========================================================================
    /**
     * CONCEPT:
     *   ScopedValue is an immutable, bounded alternative to ThreadLocal.
     *   A scoped value is set for a specific scope (a lambda or Runnable),
     *   readable anywhere in that scope's call tree, and automatically cleaned up.
     *
     * THE PROBLEM WITH ThreadLocal + Virtual Threads:
     *   ThreadLocal was designed for platform threads. With virtual threads:
     *   1. Memory leaks: VTs are created/destroyed constantly. Each carries its
     *      ThreadLocal map. If you create millions of VTs with ThreadLocals,
     *      GC pressure increases significantly.
     *   2. Inheritance: InheritableThreadLocal copies the parent's map to the child
     *      thread — expensive when spinning up millions of VTs.
     *   3. Mutability: ThreadLocal is mutable, leading to subtle bugs in complex
     *      call graphs where multiple callers can set conflicting values.
     *
     * HOW SCOPED VALUES ARE BETTER:
     *   1. Immutable: once bound, cannot be changed (only re-bound in a nested scope)
     *   2. Bounded lifetime: value exists only within the scope that bound it
     *   3. No mutation bugs: callers can't accidentally overwrite each other's values
     *   4. Efficient with VTs: no per-thread map — just scope-chain lookup
     *
     * INTERVIEW ANGLE:
     *   Q: "When would you use ScopedValue over ThreadLocal?"
     *   A: When using virtual threads, when you need context propagation (user auth,
     *      request ID, transaction) that should be read-only within a request scope,
     *      and when you want the value to be automatically cleaned up.
     *
     *   Q: "Can you rebind a ScopedValue in a nested scope?"
     *   A: Yes. ScopedValue.where(VALUE, newValue).run(() -> { ... }) creates a
     *      nested binding. Inside the nested scope, VALUE.get() returns newValue.
     *      Outside the nested scope, the outer value is restored.
     *
     *   Q: "Status in Java 21?"
     *   A: Preview in Java 21 (JEP 446). Final in Java 23+.
     *      Use --enable-preview to run in Java 21.
     *
     * NOTE: ScopedValues require --enable-preview in Java 21.
     * The code below shows the API pattern as comments for study purposes.
     */
    static void demonstrateScopedValues() {
        System.out.println("\n--- SCOPED VALUES (Java 21 — Preview) ---");
        System.out.println("NOTE: ScopedValues require --enable-preview in Java 21.");
        System.out.println("Shown as code comments for study purposes.\n");

        /*
         * SCOPED VALUE PATTERN (would run with --enable-preview):
         *
         * // Static constant — the ScopedValue itself is just a key, not a value
         * static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
         * static final ScopedValue<User>   CURRENT_USER = ScopedValue.newInstance();
         *
         * // BINDING: set the value for a scope
         * ScopedValue.where(REQUEST_ID, "req-12345")
         *            .where(CURRENT_USER, currentUser)
         *            .run(() -> {
         *                handleRequest(); // REQUEST_ID and CURRENT_USER available here
         *            });
         *
         * // READING: anywhere inside the bound scope's call tree
         * void handleRequest() {
         *     String reqId = REQUEST_ID.get();       // "req-12345"
         *     User user = CURRENT_USER.get();        // currentUser
         *     log("Handling " + reqId + " for " + user.name());
         *     processData(); // can also call REQUEST_ID.get() here
         * }
         *
         * // NESTED RE-BINDING: temporarily override in a sub-scope
         * ScopedValue.where(REQUEST_ID, "sub-request")
         *            .run(() -> {
         *                // REQUEST_ID.get() == "sub-request" here
         *                handleSubRequest();
         *            });
         * // Back here: REQUEST_ID.get() == "req-12345" again
         *
         * // CALL() vs RUN() — call() returns a value, run() is void:
         * String result = ScopedValue.where(REQUEST_ID, "req-abc")
         *                            .call(() -> processAndReturn());
         *
         * // CHECKING if bound:
         * if (REQUEST_ID.isBound()) {
         *     String id = REQUEST_ID.get();
         * }
         *
         * // COMPARISON to ThreadLocal:
         * // ThreadLocal version:
         * static final ThreadLocal<String> TL_REQUEST_ID = new ThreadLocal<>();
         * TL_REQUEST_ID.set("req-12345");
         * handleRequest();       // reads TL_REQUEST_ID.get()
         * TL_REQUEST_ID.remove(); // must manually clean up or leak!
         *
         * // ScopedValue version:
         * ScopedValue.where(REQUEST_ID, "req-12345").run(() -> handleRequest());
         * // Automatic cleanup — no explicit remove() needed
         */

        // Demonstrate ThreadLocal for comparison (compiles normally)
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("request-context-data");

        try {
            System.out.println("ThreadLocal value: " + threadLocal.get());
            System.out.println("Problem: must manually call remove() or risk leak in VT workloads");
        } finally {
            threadLocal.remove(); // manual cleanup — easy to forget, especially with VTs
        }

        System.out.println("ScopedValue advantage: automatic cleanup, immutable, VT-safe");
        System.out.println("API: ScopedValue.where(KEY, value).run(runnable)");
        System.out.println("API: ScopedValue.where(KEY, value).call(callable)");
    }


    // =========================================================================
    // SECTION 11: STRUCTURED CONCURRENCY — Java 21 (Preview)
    // =========================================================================
    /**
     * CONCEPT:
     *   Structured Concurrency treats a group of related tasks as a single unit.
     *   Just as structured control flow (if/for/try) has clear entry and exit points,
     *   structured concurrency ensures that subtasks don't outlive their parent scope.
     *
     * THE PROBLEM WITH UNSTRUCTURED CONCURRENCY:
     *   With ExecutorService, you can submit tasks that outlive the method that
     *   submitted them. A task leak = a thread running after its logical owner exits.
     *   Debugging is hard: stack traces don't show the logical parent-child relationship.
     *   Cancellation is manual and error-prone.
     *
     * STRUCTURED CONCURRENCY GUARANTEES:
     *   1. Subtask lifetimes ⊆ scope lifetime (no leaks)
     *   2. Errors in subtasks are propagated to the scope (no silent failures)
     *   3. If one subtask fails, others can be automatically cancelled (ShutdownOnFailure)
     *   4. If one subtask succeeds, others can be cancelled (ShutdownOnSuccess)
     *
     * INTERVIEW ANGLE:
     *   Q: "How does StructuredTaskScope differ from CompletableFuture?"
     *   A: CompletableFuture is unstructured — futures can outlive their creator.
     *      StructuredTaskScope enforces that all subtasks complete (or are cancelled)
     *      before the scope exits. It also preserves the logical call tree for debugging.
     *
     *   Q: "What does ShutdownOnFailure do?"
     *   A: If ANY subtask throws, the scope shuts down — signals other subtasks to stop.
     *      After scope.join(), call scope.throwIfFailed() to propagate the exception.
     *
     *   Q: "Status in Java 21?"
     *   A: Preview (JEP 453). Becoming final in Java 25 (finalized in Java 24/25 path).
     *      For Java 21, requires --enable-preview.
     *
     * NOTE: Structured Concurrency requires --enable-preview in Java 21.
     * Shown as comments for study.
     */
    static void demonstrateStructuredConcurrency() {
        System.out.println("\n--- STRUCTURED CONCURRENCY (Java 21 — Preview) ---");
        System.out.println("NOTE: StructuredTaskScope requires --enable-preview in Java 21.");
        System.out.println("Shown as code comments for study purposes.\n");

        /*
         * STRUCTURED CONCURRENCY PATTERN (requires --enable-preview):
         *
         * // Scenario: fetch user data and order data in parallel
         * // If EITHER fails, cancel the other and propagate the error
         *
         * String fetchUser(int id) throws InterruptedException {
         *     Thread.sleep(100); // simulate I/O
         *     return "User-" + id;
         * }
         *
         * List<String> fetchOrders(int userId) throws InterruptedException {
         *     Thread.sleep(150); // simulate I/O
         *     return List.of("Order1", "Order2");
         * }
         *
         * // SHUTDOWN ON FAILURE — fail fast: if any task fails, cancel all others
         * try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
         *     Subtask<String>       userTask   = scope.fork(() -> fetchUser(42));
         *     Subtask<List<String>> ordersTask = scope.fork(() -> fetchOrders(42));
         *
         *     scope.join();           // wait for all subtasks to complete/fail
         *     scope.throwIfFailed();  // if any threw, rethrow here
         *
         *     // Both succeeded — safe to call .get()
         *     String user = userTask.get();
         *     List<String> orders = ordersTask.get();
         *     System.out.println(user + " has orders: " + orders);
         * }
         * // Scope exit guarantees all subtasks are done — no leaks
         *
         * // SHUTDOWN ON SUCCESS — first to succeed wins (like Promise.any())
         * try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
         *     scope.fork(() -> fetchFromPrimary());
         *     scope.fork(() -> fetchFromBackup());
         *
         *     scope.join();
         *     String result = scope.result(); // result of first successful task
         *     System.out.println("Got: " + result);
         * }
         *
         * // TIMEOUT on the entire scope
         * try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
         *     Subtask<String> task = scope.fork(() -> slowOperation());
         *     scope.joinUntil(Instant.now().plusSeconds(5)); // timeout after 5s
         *     scope.throwIfFailed();
         *     String result = task.get();
         * }
         *
         * // ERROR HANDLING PATTERNS:
         * try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
         *     Subtask<String> t1 = scope.fork(task1);
         *     Subtask<String> t2 = scope.fork(task2);
         *     scope.join().throwIfFailed(e -> new RuntimeException("Task failed", e));
         *     // throwIfFailed with mapper to wrap in custom exception type
         * }
         *
         * // WHY THIS IS BETTER than CompletableFuture:
         * // CompletableFuture version (unstructured — task can leak):
         * CompletableFuture<String> f1 = CompletableFuture.supplyAsync(task1);
         * CompletableFuture<String> f2 = CompletableFuture.supplyAsync(task2);
         * // If f1 fails, f2 might still be running — leaked task
         * // If method returns early, both futures are orphaned
         * // Stack traces don't show logical parent-child relationship
         */

        // Demonstrate the unstructured alternative (what NOT to do with VTs)
        System.out.println("Unstructured alternative for comparison:");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> f1 = executor.submit(() -> { Thread.sleep(50); return "result1"; });
            Future<String> f2 = executor.submit(() -> { Thread.sleep(30); return "result2"; });
            System.out.println("  " + f1.get() + ", " + f2.get());
            System.out.println("  Problem: no built-in failure propagation or cancellation");
            System.out.println("  StructuredTaskScope solves this with ShutdownOnFailure/Success");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // =========================================================================
    // SECTION 12: STRING TEMPLATES — Java 21 (Preview)
    // =========================================================================
    /**
     * CONCEPT:
     *   String Templates provide safe string interpolation — embedding expressions
     *   directly into string literals, similar to JavaScript's `${...}`.
     *   The KEY difference from other languages: templates are processed through
     *   a "template processor" that can validate, sanitize, or transform the result.
     *
     * WHY NOT JUST STRING INTERPOLATION?
     *   Naive interpolation is a SQL injection / XSS vector:
     *     String query = "SELECT * FROM users WHERE name = '" + userInput + "'";
     *   Template processors allow safe handling:
     *     String query = SQL."SELECT * FROM users WHERE name = \{userInput}";
     *     // SQL processor can parameterize the query automatically
     *
     * BUILT-IN PROCESSORS:
     *   STR  — basic interpolation, returns String
     *   FMT  — like printf formatting + interpolation
     *   RAW  — returns StringTemplate object (not yet a String)
     *
     * STATUS: Preview in Java 21 (JEP 430). Preview in Java 22 again. REMOVED in Java 23
     * due to design concerns. Will return in a future version with a revised design.
     * For interviews: know what it was, and WHY it was pulled (design not settled).
     *
     * INTERVIEW ANGLE:
     *   Q: "What happened to String Templates in Java 23?"
     *   A: They were REMOVED (JEP Withdrawn). The core interpolation worked, but the
     *      template processor design needed rethinking. It will return in a future JEP.
     *      This shows Java's commitment to getting the design right vs. rushing features.
     *
     *   Q: "How is STR different from String.format or text blocks?"
     *   A: STR."Hello \{name}" is inline — no positional format specifiers, no
     *      separate argument list. The expression is directly at the substitution site.
     *
     *   Q: "What is the security benefit of template processors?"
     *   A: Processors can intercept the template before it becomes a String.
     *      A SQL processor can parameterize inputs; an HTML processor can escape values.
     *      This is impossible with + concatenation or String.format.
     *
     * NOTE: Requires --enable-preview in Java 21. Shown as comments.
     */
    static void demonstrateStringTemplates() {
        System.out.println("\n--- STRING TEMPLATES (Java 21 Preview — Withdrawn in Java 23) ---");
        System.out.println("NOTE: String Templates require --enable-preview in Java 21.");
        System.out.println("      Feature was WITHDRAWN in Java 23 for redesign.");
        System.out.println("Shown as code comments for study purposes.\n");

        /*
         * STRING TEMPLATES PATTERN (Java 21 with --enable-preview):
         *
         * String name = "Alice";
         * int score = 95;
         *
         * // STR processor — basic interpolation
         * String greeting = STR."Hello, \{name}! Your score is \{score}.";
         * // Result: "Hello, Alice! Your score is 95."
         *
         * // Expressions inside \{} — any valid Java expression
         * String msg = STR."Result: \{score > 90 ? "A" : "B"}";
         * String computed = STR."2 + 2 = \{2 + 2}";
         * String upper = STR."Upper: \{name.toUpperCase()}";
         *
         * // Multi-line with text block
         * String json = STR."""
         *         {
         *           "name": "\{name}",
         *           "score": \{score}
         *         }
         *         """;
         *
         * // FMT processor — format specifiers + interpolation
         * String report = FMT."Name: %-10s\{name} Score: %5.1f\{(double)score}";
         *
         * // RAW processor — returns StringTemplate, not String
         * // Useful for building custom processors
         * StringTemplate template = RAW."Hello \{name}";
         * // template.fragments() -> ["Hello ", ""]
         * // template.values()    -> [name value]
         *
         * // CUSTOM PROCESSOR — the real power:
         * // A SQL processor that prevents injection:
         * interface SQLTemplate {
         *     static StringTemplate.Processor<PreparedStatement, SQLException> SQL = st -> {
         *         // fragments = SQL parts, values = user inputs
         *         String query = String.join("?", st.fragments());
         *         PreparedStatement ps = connection.prepareStatement(query);
         *         int i = 1;
         *         for (Object value : st.values()) {
         *             ps.setObject(i++, value); // parameterized, never concatenated
         *         }
         *         return ps;
         *     };
         * }
         * String userInput = "'; DROP TABLE users; --"; // SQL injection attempt
         * PreparedStatement ps = SQL."SELECT * FROM users WHERE name = \{userInput}";
         * // Safely parameterized — injection attempt neutralized
         */

        // Current alternative without String Templates (all compile normally)
        String name = "Alice";
        int score = 95;
        System.out.println("Current alternatives while String Templates is in redesign:");
        System.out.println("  String.format: " + String.format("Hello, %s! Score: %d.", name, score));
        System.out.println("  formatted():   " + "Hello, %s! Score: %d.".formatted(name, score));
        System.out.println("  Concatenation: " + "Hello, " + name + "! Score: " + score + ".");
        System.out.println("  (String Templates API: STR.\"Hello, \\{name}!\")");
    }


    // =========================================================================
    // BONUS: OTHER JAVA 17-21 IMPROVEMENTS (quick reference for interviews)
    // =========================================================================
    /**
     * JAVA 17 ADDITIONAL CHANGES:
     *   - Strong encapsulation of JDK internals: --illegal-access flag REMOVED.
     *     Reflective access to JDK internals (sun.misc.Unsafe) requires explicit
     *     --add-opens. Many older libraries broke on Java 17+ for this reason.
     *   - Deprecation of Security Manager (JEP 411) — slated for removal.
     *   - Random number generators: RandomGenerator interface, new algorithms
     *     (LXM family): RandomGeneratorFactory, SplittableRandom improvements.
     *   - Context-Specific Deserialization Filters (JEP 415) — security improvement.
     *   - Restore Always-Strict Floating-Point Semantics (JEP 306).
     *   - New macOS rendering pipeline (Metal), macOS/AArch64 port.
     *
     * JAVA 18 (2022):
     *   - UTF-8 by default (JEP 400) — Charset.defaultCharset() = UTF-8 always.
     *     Previously platform-dependent (Windows could be Cp1252). HUGE for portability.
     *   - Simple Web Server (JEP 408): `java -m jdk.httpserver` for static files.
     *   - Code Snippets in Javadoc (JEP 413): @snippet tag.
     *   - Deprecation of finalize() for removal (JEP 421) — use Cleaner or try-with-resources.
     *
     * JAVA 19 (2022):
     *   - Virtual Threads PREVIEW (JEP 425).
     *   - Structured Concurrency INCUBATOR (JEP 428).
     *   - Record Patterns PREVIEW (JEP 405).
     *   - Pattern Matching for switch PREVIEW 3 (JEP 427).
     *
     * JAVA 20 (2023):
     *   - All previews from 19 continue as previews (2nd/3rd preview rounds).
     *   - Scoped Values INCUBATOR → PREVIEW.
     *
     * JAVA 21 ADDITIONAL:
     *   - Generational ZGC (JEP 439): ZGC now has young/old generations.
     *     Better throughput for most workloads. Enable with -XX:+UseZGC.
     *   - Deprecation of 32-bit Windows builds (JEP 449).
     *   - Prepare to disallow dynamic loading of agents (JEP 451) — security.
     *   - Key Encapsulation Mechanism API (JEP 452) — cryptography.
     *   - Unnamed Classes and Instance Main Methods PREVIEW (JEP 445):
     *     void main() { ... } // no class declaration, no public, no String[] args
     *     Makes Java beginner-friendly (HelloWorld without boilerplate). Java 23 final path.
     *   - Unnamed Patterns and Variables PREVIEW (JEP 443):
     *     case Point(int x, _) -> ...  // underscore ignores components
     *     This is now "_" (not var) — final in Java 22.
     */
    static void demonstrateBonusFeatures() {
        System.out.println("\n--- BONUS: OTHER NOTABLE CHANGES ---");

        // UTF-8 as default (Java 18+)
        System.out.println("Default charset: " + java.nio.charset.Charset.defaultCharset()); // UTF-8

        // Improved Random (Java 17)
        java.util.random.RandomGenerator rng = java.util.random.RandomGenerator.of("L64X128MixRandom");
        System.out.println("New RandomGenerator: " + rng.nextInt(100));

        // RandomGeneratorFactory for choosing algorithm
        java.util.random.RandomGeneratorFactory.all()
                .filter(f -> f.isJumpable())
                .map(java.util.random.RandomGeneratorFactory::name)
                .sorted()
                .limit(3)
                .forEach(name -> System.out.println("  Jumpable RNG: " + name));

        // instanceof with generics (Java 16)
        Object list = List.of(1, 2, 3);
        if (list instanceof List<?> l && !l.isEmpty()) {
            System.out.println("List with elements, size: " + l.size());
        }

        // Unnamed patterns (Java 21 preview concept — using _ for unused bindings)
        // In Java 21 preview: case Point(int x, _) -> x  (ignore y)
        // Shown via regular pattern:
        Object point = new Point(7, 0);
        if (point instanceof Point(int x, int ignored)) { // 'ignored' = conceptual _
            System.out.println("X coordinate only: " + x);
        }

        // finalize() deprecation — correct cleanup pattern
        System.out.println("Use try-with-resources instead of finalize():");
        try (var resource = new java.io.StringReader("test")) {
            System.out.println("  Resource used: " + resource.read());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // =========================================================================
    // SUMMARY TABLE
    // =========================================================================
    static void printSummaryTable() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("JAVA 14-21 FEATURE SUMMARY TABLE");
        System.out.println("=".repeat(70));
        System.out.printf("%-8s %-42s %-12s %-10s%n", "Version", "Feature", "Status", "Preview Start");
        System.out.println("-".repeat(70));

        String[][] table = {
                {"14",  "Switch Expressions",                    "Final",   "12"},
                {"15",  "Text Blocks",                           "Final",   "13"},
                {"16",  "Records",                               "Final",   "14"},
                {"16",  "Pattern Matching instanceof",           "Final",   "14"},
                {"17",  "Sealed Classes",                        "Final",   "15"},
                {"17",  "Strong JDK Encapsulation",              "Final",   "16"},
                {"18",  "UTF-8 by Default",                      "Final",   "N/A"},
                {"18",  "Simple Web Server",                     "Final",   "N/A"},
                {"21",  "Virtual Threads (Project Loom)",        "Final",   "19"},
                {"21",  "Record Patterns",                       "Final",   "19"},
                {"21",  "Pattern Matching for switch",           "Final",   "17"},
                {"21",  "Sequenced Collections",                 "Final",   "N/A"},
                {"21",  "Generational ZGC",                      "Final",   "N/A"},
                {"21",  "Scoped Values",                         "Preview", "20"},
                {"21",  "Structured Concurrency",               "Preview", "19"},
                {"21",  "Unnamed Classes & Instance main()",    "Preview", "21"},
                {"21",  "Unnamed Patterns (_)",                  "Preview", "21"},
                {"21",  "String Templates (STR.\"...\")",        "Preview", "21"},
                {"23",  "String Templates",                      "REMOVED", "21"},
                {"23+", "String Templates (redesigned)",         "TBD",     "TBD"},
        };

        for (String[] row : table) {
            System.out.printf("%-8s %-42s %-12s %-10s%n", row[0], row[1], row[2], row[3]);
        }

        System.out.println("=".repeat(70));
        System.out.println("\nLTS RELEASES: Java 8, 11, 17, 21 (every 3 years since Java 17)");
        System.out.println("Support: Java 21 LTS = 5+ years extended support (enterprise standard)");
        System.out.println("=".repeat(70));
    }


    // =========================================================================
    // INTERVIEW QUICK-FIRE Q&A REFERENCE
    // =========================================================================
    /**
     * TOP INTERVIEW QUESTIONS AND ANSWERS:
     *
     * Q1: "What's the difference between a record and a class?"
     * A:  Records are implicitly final, extend java.lang.Record, auto-generate
     *     constructor/equals/hashCode/toString/accessors from declared components.
     *     Cannot extend other classes. Accessor names are x(), not getX().
     *     Cannot add instance fields beyond components.
     *     Use when: immutable data carrier / DTO / value object.
     *
     * Q2: "Explain virtual threads. When would you NOT use them?"
     * A:  VTs are JVM-managed threads multiplexed on OS carrier threads.
     *     Ideal for I/O-bound blocking code (database, HTTP, file I/O).
     *     NOT for CPU-bound work — VTs don't add CPU parallelism.
     *     NOT in synchronized blocks without care — causes pinning.
     *     NOT as a drop-in for platform threads in all cases (e.g., ThreadLocal issues).
     *
     * Q3: "What is pinning in the context of virtual threads?"
     * A:  A VT is pinned when it cannot unmount from its carrier thread.
     *     Caused by: synchronized blocks/methods, native frames (JNI).
     *     Fix: Replace synchronized with ReentrantLock.
     *     JDK 24 is improving synchronized to reduce pinning.
     *
     * Q4: "What problem do sealed classes solve? Why not just use enums?"
     * A:  Sealed = closed type hierarchy. Enums = single instance per variant, same fields.
     *     Sealed classes = each subtype can have DIFFERENT fields, methods, behaviors.
     *     Enables exhaustive pattern matching. Compiler catches missing cases.
     *
     * Q5: "What is the difference between yield and return in Java?"
     * A:  return exits a METHOD. yield exits a BLOCK within a switch expression.
     *     yield cannot appear outside a switch expression. return cannot appear
     *     inside a switch expression to "return a value from the expression" —
     *     you must use yield for that.
     *
     * Q6: "How do text blocks handle indentation?"
     * A:  stripIndent() removes common leading whitespace (incidental indent).
     *     Amount removed = column position of the closing """.
     *     Essential indentation (spaces that are PART of the string value) is preserved.
     *
     * Q7: "What is a guarded pattern in Java 21?"
     * A:  case String s when s.length() > 5 — the `when` clause adds a boolean guard.
     *     The pattern matches only if BOTH the type check passes AND the when expression
     *     is true. Previously spelled using && in the condition; `when` is the final form.
     *
     * Q8: "Can a record implement an interface?"
     * A:  Yes. Records can implement any interface.
     *     Records CANNOT extend classes (they implicitly extend java.lang.Record).
     *     Records are implicitly final — they cannot be extended by anything.
     *
     * Q9: "What is SequencedCollection and why was it added?"
     * A:  New Java 21 interface giving uniform getFirst()/getLast()/reversed() access.
     *     Previously: list.get(list.size()-1) for last element — inconsistent across types.
     *     Now all ordered collections (List, Deque, LinkedHashSet, etc.) share one API.
     *
     * Q10: "ScopedValue vs ThreadLocal — key differences?"
     * A:  ThreadLocal: mutable, manual cleanup (remove()), inherited by child threads
     *     (with InheritableThreadLocal), problematic with VTs (memory leak risk).
     *     ScopedValue: immutable, automatic cleanup on scope exit, bounded lifetime,
     *     designed for virtual threads, cannot be set after binding.
     *
     * Q11: "What is structured concurrency? Compare to CompletableFuture."
     * A:  StructuredTaskScope ensures subtask lifetimes are bounded by the scope.
     *     No leaked tasks, built-in cancellation (ShutdownOnFailure/Success), logical
     *     call hierarchy preserved for debugging.
     *     CompletableFuture: unstructured, tasks can outlive creators, no automatic
     *     cancellation on failure, complex error handling chains.
     *
     * Q12: "What was the significance of Java 17 beyond sealed classes?"
     * A:  Java 17 = LTS. Strong encapsulation: --illegal-access REMOVED, reflective
     *     access to JDK internals blocked by default. Many libraries (Spring, Hibernate
     *     older versions) broke on Java 17. Forced ecosystem to modernize.
     *     Also: deprecation of Security Manager, new Random API, macOS Metal renderer.
     */


    // =========================================================================
    // MAIN — runs all demonstrations
    // =========================================================================
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("Java 14-21 Features — Complete Study Demonstration");
        System.out.println("=".repeat(60));

        // Java 14
        demonstrateSwitchExpressions();

        // Java 15
        demonstrateTextBlocks();

        // Java 16
        demonstrateRecords();
        demonstratePatternMatchingInstanceof();

        // Java 17 (LTS)
        demonstrateSealedClasses();

        // Java 21 (LTS)
        demonstrateVirtualThreads();
        demonstrateRecordPatterns();
        demonstratePatternSwitch();
        demonstrateSequencedCollections();

        // Java 21 Preview (shown as comments — would need --enable-preview)
        demonstrateScopedValues();
        demonstrateStructuredConcurrency();
        demonstrateStringTemplates();

        // Bonus: other changes across versions
        demonstrateBonusFeatures();

        // Summary reference
        printSummaryTable();

        System.out.println("\nStudy complete. Key LTS upgrade: Java 11 -> Java 17 -> Java 21");
    }
}
