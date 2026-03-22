package com.java.java21;

/**
 * =====================================================================
 * JAVA 21 FEATURE: PATTERN MATCHING FOR SWITCH
 * =====================================================================
 *
 * WHAT IS IT? (Java 21 — full release; previewed since Java 17)
 * -------------------------------------------------------
 * Pattern matching extends the `switch` statement/expression to match
 * on the TYPE of the value, not just its literal value.
 * Combined with sealed classes, the compiler enforces EXHAUSTIVENESS —
 * you MUST handle every possible type.
 *
 * EVOLUTION OF switch IN JAVA:
 * -------------------------------------------------------
 * Java 1:   switch only on int, char, String — primitive switch
 * Java 14:  switch EXPRESSION (returns a value) — `yield` keyword
 * Java 16:  instanceof pattern matching (if-else only)
 * Java 17:  switch pattern matching (preview)
 * Java 21:  Pattern matching for switch — FULL RELEASE
 *
 * THE KEY NEW FEATURES IN JAVA 21:
 * 1. TYPE PATTERNS in switch: `case Circle c -> ...`
 * 2. GUARDED PATTERNS (when clause): `case Circle c when c.radius() > 10 -> "large"`
 * 3. NULL HANDLING: `case null -> "null input"` (previously switch threw NPE on null)
 * 4. EXHAUSTIVENESS CHECK with sealed types: if you miss a subtype → COMPILE ERROR
 *
 * INTERVIEW MUST-KNOW QUESTIONS:
 *   Q: "What happens if you have a sealed type and don't cover all cases in a switch?"
 *   A: COMPILE ERROR. The compiler statically verifies exhaustiveness for sealed types.
 *      This is one of the biggest benefits — you can't accidentally forget a case.
 *
 *   Q: "What is a guarded pattern?"
 *   A: A type pattern with an additional boolean `when` condition.
 *      `case Circle c when c.radius() > 10` — matches Circle AND radius > 10.
 *      If the `when` condition is false, the case is skipped and fall-through continues.
 *
 *   Q: "How does null work in switch expressions before Java 21?"
 *   A: It throws NullPointerException! In Java 21 you can handle it explicitly with `case null`.
 *
 *   Q: "What is the difference between switch statement and switch expression?"
 *   A: - Switch STATEMENT: doesn't return a value, uses break, traditional
 *      - Switch EXPRESSION (Java 14+): returns a value, uses -> or yield, no fall-through by default
 *      - Pattern matching switch is always a switch EXPRESSION (or can be used as statement)
 *
 *   Q: "Why use switch over if-else chain for type checking?"
 *   A: 1. Exhaustiveness guarantee (compiler error if you miss a case with sealed types)
 *      2. Better readability for many cases
 *      3. Potential compiler optimization (jump tables vs sequential if-else)
 *      4. Returns a value (expression form) — cleaner code
 *
 * =====================================================================
 */
public class PatternMatchingDemo {

    // Reusing sealed Shape hierarchy from SealedClassesDemo
    sealed interface Shape permits PatternMatchingDemo.Circle, PatternMatchingDemo.Rectangle, PatternMatchingDemo.Triangle {}
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double base, double height) implements Shape {}

    // =====================================================================
    // 1. BASIC TYPE PATTERN IN SWITCH (switch expression)
    // =====================================================================
    // `case Circle c ->` matches when the shape is a Circle AND binds it to variable c
    // The switch EXPRESSION returns a value (note: no break, no fall-through)
    static double calculateArea(Shape shape) {
        // switch EXPRESSION — returns a double
        // The compiler verifies exhaustiveness because Shape is sealed!
        // If you remove a case (e.g., Triangle), you get: "switch expression does not cover all input values"
        return switch (shape) {
            case Circle c    -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Triangle t  -> 0.5 * t.base() * t.height();
            // No `default` needed — the compiler KNOWS these 3 cover all Shape subtypes
        };
    }

    // =====================================================================
    // 2. GUARDED PATTERNS (when clause) — Java 21
    // =====================================================================
    // `case Circle c when c.radius() > 10` — type match + boolean guard
    // If the type matches but the `when` condition is false, the case is SKIPPED
    // and evaluation continues to the next case.
    //
    // ORDER MATTERS: More specific guards must come BEFORE less specific ones.
    // (Same rule as if-else chains — put specific cases first)
    static String classifyShape(Shape shape) {
        return switch (shape) {
            // Guard: only matches Circle with radius > 10
            case Circle c when c.radius() > 10  -> "Large circle (r=" + c.radius() + ")";

            // Guard: only matches Circle with radius > 5 (and ≤ 10 because above case was already checked)
            case Circle c when c.radius() > 5   -> "Medium circle (r=" + c.radius() + ")";

            // Matches any remaining Circle (radius ≤ 5)
            case Circle c                        -> "Small circle (r=" + c.radius() + ")";

            // Square: a rectangle where width == height
            case Rectangle r when r.width() == r.height() -> "Square (side=" + r.width() + ")";

            // Any remaining rectangle
            case Rectangle r -> String.format("Rectangle (%.1f x %.1f)", r.width(), r.height());

            // Triangle — no guard needed
            case Triangle t  -> "Triangle (base=" + t.base() + ")";
        };
    }

    // =====================================================================
    // 3. NULL HANDLING IN SWITCH (Java 21)
    // =====================================================================
    // Before Java 21: switch(null) → NullPointerException
    // Java 21: you can add `case null` to handle it gracefully
    static String handleNull(Shape shape) {
        return switch (shape) {
            // Handle null FIRST (must be the first or early case)
            case null        -> "No shape provided (null)";
            case Circle c    -> "Got a circle";
            case Rectangle r -> "Got a rectangle";
            case Triangle t  -> "Got a triangle";
        };
    }

    // =====================================================================
    // 4. COMBINING null AND DEFAULT
    // =====================================================================
    // You can also combine `case null, default` in one arm
    static String withCombinedNullDefault(Object obj) {
        return switch (obj) {
            case Integer i   -> "Integer: " + i;
            case String s    -> "String: '" + s + "'";
            case Double d    -> "Double: " + d;
            // Handles null AND any other type not matched above
            case null, default -> "Something else or null: " + obj;
        };
    }

    // =====================================================================
    // 5. PATTERN MATCHING AS STATEMENT (not expression) — yield keyword
    // =====================================================================
    // When switch is used as a STATEMENT (not returning a value), or when
    // you need multiple lines of logic per case, use `yield` to produce the value
    static String describeWithYield(Shape shape) {
        return switch (shape) {
            case Circle c -> {
                // Multi-line case block — use yield to return from this block
                double area = Math.PI * c.radius() * c.radius();
                double perimeter = 2 * Math.PI * c.radius();
                yield String.format("Circle: area=%.2f, perimeter=%.2f", area, perimeter);
                // Interview: "What is yield?" → It's like return but for switch expressions.
                //   Regular `return` would exit the METHOD. `yield` exits just this case block.
            }
            case Rectangle r -> {
                double area = r.width() * r.height();
                yield String.format("Rectangle: %.1f x %.1f = area %.2f", r.width(), r.height(), area);
            }
            case Triangle t -> "Triangle (area = " + (0.5 * t.base() * t.height()) + ")";
        };
    }

    // =====================================================================
    // 6. PATTERN MATCHING WITH NON-SEALED TYPES (uses default)
    // =====================================================================
    // When the type is NOT sealed, the compiler can't verify exhaustiveness.
    // You MUST provide a `default` case (or cover Object's subtypes exhaustively — impossible).
    static String describeObject(Object obj) {
        return switch (obj) {
            case Integer i when i < 0  -> "Negative integer: " + i;
            case Integer i             -> "Positive/zero integer: " + i;
            case String s when s.isEmpty() -> "Empty string";
            case String s              -> "String: '" + s + "'";
            case null                  -> "null";
            default                    -> "Unknown type: " + obj.getClass().getSimpleName();
            // `default` is REQUIRED here because Object is not sealed
        };
    }

    // =====================================================================
    // MAIN — Run all demos
    // =====================================================================
    public static void main(String[] args) {

        Shape circle10 = new Circle(10.0);
        Shape circle3  = new Circle(3.0);
        Shape circle7  = new Circle(7.0);
        Shape square   = new Rectangle(5.0, 5.0);
        Shape rect     = new Rectangle(3.0, 7.0);
        Shape tri      = new Triangle(6.0, 4.0);

        System.out.println("=== 1. Basic Type Pattern — Area Calculation ===");
        System.out.printf("Circle(10) area: %.2f%n", calculateArea(circle10));
        System.out.printf("Rectangle(3,7) area: %.2f%n", calculateArea(rect));
        System.out.printf("Triangle(6,4) area: %.2f%n", calculateArea(tri));

        System.out.println("\n=== 2. Guarded Patterns ===");
        System.out.println(classifyShape(new Circle(15.0)));  // Large
        System.out.println(classifyShape(circle7));           // Medium
        System.out.println(classifyShape(circle3));           // Small
        System.out.println(classifyShape(square));            // Square
        System.out.println(classifyShape(rect));              // Rectangle
        System.out.println(classifyShape(tri));               // Triangle

        System.out.println("\n=== 3. Null Handling ===");
        System.out.println(handleNull(circle10));  // Got a circle
        System.out.println(handleNull(null));       // No shape provided (null)

        System.out.println("\n=== 4. Combined null + default ===");
        System.out.println(withCombinedNullDefault(42));         // Integer: 42
        System.out.println(withCombinedNullDefault("hello"));    // String: 'hello'
        System.out.println(withCombinedNullDefault(3.14));       // Double: 3.14
        System.out.println(withCombinedNullDefault(null));       // Something else or null: null
        System.out.println(withCombinedNullDefault(true));       // Something else (Boolean)

        System.out.println("\n=== 5. Multi-line case with yield ===");
        System.out.println(describeWithYield(circle10));
        System.out.println(describeWithYield(rect));

        System.out.println("\n=== 6. Non-sealed Object pattern ===");
        System.out.println(describeObject(-5));
        System.out.println(describeObject(42));
        System.out.println(describeObject("hello"));
        System.out.println(describeObject(""));
        System.out.println(describeObject(null));
        System.out.println(describeObject(3.14));
    }
}
