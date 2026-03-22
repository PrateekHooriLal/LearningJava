package com.java.java21;

/**
 * =====================================================================
 * JAVA 21 FEATURE: RECORD PATTERNS (Deconstruction Patterns)
 * =====================================================================
 *
 * WHAT IS IT? (Java 21 — full release; previewed in Java 19, 20)
 * -------------------------------------------------------
 * Record patterns let you DECONSTRUCT a record inside instanceof or switch.
 * Instead of:
 *   if (obj instanceof Point p) {
 *       int x = p.x();     // still need to call accessor methods
 *       int y = p.y();
 *   }
 *
 * You can write:
 *   if (obj instanceof Point(int x, int y)) {
 *       // x and y are directly in scope — no accessor calls needed
 *   }
 *
 * The record's components are extracted (deconstructed) directly into variables.
 *
 * WHY IS THIS USEFUL?
 * -------------------------------------------------------
 * 1. LESS BOILERPLATE: No need to call accessor methods after matching.
 * 2. NESTED PATTERNS: You can deconstruct nested records in one step.
 *    e.g., if a Line record contains two Point records, you can extract
 *    all 4 coordinates in one pattern.
 * 3. PAIRS WITH SWITCH: Combined with switch, makes complex data processing
 *    very clean and readable.
 *
 * HOW IT CONNECTS TO PREVIOUS CONCEPTS:
 * - Records (Week 1): You need records to use record patterns
 * - Pattern matching instanceof (Week 2): Record patterns extend instanceof matching
 * - Switch patterns (Week 3): Record patterns work in switch cases too
 *
 * INTERVIEW MUST-KNOW QUESTIONS:
 *   Q: "What is a record pattern?"
 *   A: Syntax to deconstruct a record's components directly in instanceof or switch.
 *      `instanceof Point(int x, int y)` extracts x and y without accessor calls.
 *
 *   Q: "What is a nested record pattern?"
 *   A: Deconstructing a record whose components are themselves records.
 *      `instanceof Line(Point(int x1, int y1), Point(int x2, int y2))` — extracts 4 values at once.
 *
 *   Q: "Can you use wildcards in record patterns?"
 *   A: Yes! Use `_` (underscore) to ignore a component you don't care about.
 *      `instanceof Point(int x, _)` — extracts only x, ignores y.
 *      (The unnamed pattern variable `_` is finalized in Java 22.)
 *
 * =====================================================================
 */
public class RecordPatternsDemo {

    // Basic records for demonstration
    record Point(int x, int y) {}
    record Line(Point start, Point end) {}
    record ColoredPoint(Point point, String color) {}
    record Rectangle(Point topLeft, Point bottomRight) {}

    // =====================================================================
    // 1. BASIC RECORD PATTERN in instanceof
    // =====================================================================
    static void demo1_BasicRecordPattern() {
        System.out.println("=== 1. Basic Record Pattern (instanceof) ===");

        Object obj = new Point(3, 4);

        // OLD WAY (Java 16 instanceof pattern, but still needs accessor calls):
        if (obj instanceof Point p) {
            int x = p.x();  // still need accessor method
            int y = p.y();
            System.out.println("  Old way: Point at (" + x + ", " + y + ")");
        }

        // NEW WAY (Java 21 record pattern — deconstruct directly):
        if (obj instanceof Point(int x, int y)) {
            // x and y are directly available — no accessor calls
            System.out.println("  New way: Point at (" + x + ", " + y + ")");
            System.out.println("  Distance from origin: " + Math.sqrt(x * x + y * y));
        }

        // Works with null check automatically — if obj is null, the pattern doesn't match
        Object nullObj = null;
        System.out.println("  Null check: " + (nullObj instanceof Point(int x, int y))); // false — no NPE
    }

    // =====================================================================
    // 2. NESTED RECORD PATTERNS — The key differentiator
    // =====================================================================
    static void demo2_NestedPatterns() {
        System.out.println("\n=== 2. Nested Record Patterns ===");

        Object line = new Line(new Point(0, 0), new Point(3, 4));

        // WITHOUT nested patterns — verbose:
        if (line instanceof Line l) {
            int x1 = l.start().x();
            int y1 = l.start().y();
            int x2 = l.end().x();
            int y2 = l.end().y();
            System.out.println("  Without nesting: (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
        }

        // WITH nested record patterns — all 4 values extracted in one shot:
        if (line instanceof Line(Point(int x1, int y1), Point(int x2, int y2))) {
            System.out.println("  With nesting: (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
            double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            System.out.printf("  Line length: %.2f%n", length);
        }
    }

    // =====================================================================
    // 3. RECORD PATTERNS IN SWITCH
    // =====================================================================
    sealed interface Shape permits RecordPatternsDemo.ShapeCircle, RecordPatternsDemo.ShapeRect {}
    record ShapeCircle(Point center, double radius) implements Shape {}
    record ShapeRect(Point topLeft, Point bottomRight) implements Shape {}

    static String describeShape(Shape shape) {
        return switch (shape) {
            // Deconstruct ShapeCircle — extract center components AND radius in one pattern
            case ShapeCircle(Point(int cx, int cy), double r) ->
                String.format("Circle at center (%d,%d) with radius %.1f", cx, cy, r);

            // Deconstruct ShapeRect — extract both corner coordinates
            case ShapeRect(Point(int x1, int y1), Point(int x2, int y2)) ->
                String.format("Rect from (%d,%d) to (%d,%d), size %dx%d",
                        x1, y1, x2, y2, Math.abs(x2 - x1), Math.abs(y2 - y1));
        };
    }

    // =====================================================================
    // 4. GUARDED RECORD PATTERNS — Combine deconstruction with when clause
    // =====================================================================
    static String classifyPoint(Object obj) {
        return switch (obj) {
            // Deconstruct AND add a guard condition
            case Point(int x, int y) when x == 0 && y == 0 -> "Origin (0, 0)";
            case Point(int x, int y) when x == 0            -> "On Y-axis: (0, " + y + ")";
            case Point(int x, int y) when y == 0            -> "On X-axis: (" + x + ", 0)";
            case Point(int x, int y) when x > 0 && y > 0   -> "Quadrant I: (" + x + ", " + y + ")";
            case Point(int x, int y) when x < 0 && y > 0   -> "Quadrant II: (" + x + ", " + y + ")";
            case Point(int x, int y)                         -> "Other quadrant: (" + x + ", " + y + ")";
            default -> "Not a point: " + obj;
        };
    }

    // =====================================================================
    // 5. COLORED POINT — Shows how record patterns compose
    // =====================================================================
    static String describeColoredPoint(Object obj) {
        return switch (obj) {
            // Nested: deconstruct ColoredPoint, then deconstruct the inner Point
            case ColoredPoint(Point(int x, int y), String color) when color.equals("red") ->
                "Red point at (" + x + ", " + y + ") — danger zone!";

            case ColoredPoint(Point(int x, int y), String color) ->
                color + " point at (" + x + ", " + y + ")";

            default -> "Not a colored point";
        };
    }

    // =====================================================================
    // MAIN — Run all demos
    // =====================================================================
    public static void main(String[] args) {
        demo1_BasicRecordPattern();
        demo2_NestedPatterns();

        System.out.println("\n=== 3. Record Patterns in Switch ===");
        Shape c = new ShapeCircle(new Point(5, 5), 10.0);
        Shape r = new ShapeRect(new Point(0, 0), new Point(6, 4));
        System.out.println("  " + describeShape(c));
        System.out.println("  " + describeShape(r));

        System.out.println("\n=== 4. Guarded Record Patterns ===");
        System.out.println("  " + classifyPoint(new Point(0, 0)));
        System.out.println("  " + classifyPoint(new Point(0, 5)));
        System.out.println("  " + classifyPoint(new Point(3, 0)));
        System.out.println("  " + classifyPoint(new Point(3, 4)));
        System.out.println("  " + classifyPoint(new Point(-2, 3)));
        System.out.println("  " + classifyPoint("not a point"));

        System.out.println("\n=== 5. Nested ColoredPoint Patterns ===");
        System.out.println("  " + describeColoredPoint(new ColoredPoint(new Point(1, 2), "red")));
        System.out.println("  " + describeColoredPoint(new ColoredPoint(new Point(3, 4), "blue")));
        System.out.println("  " + describeColoredPoint("other"));

        System.out.println("\n=== Summary ===");
        System.out.println("  Record patterns = instanceof/switch + automatic deconstruction of record components");
        System.out.println("  Most powerful when: nested records, combining with guards");
        System.out.println("  Replaces: verbose p.component() accessor chains");
    }
}
