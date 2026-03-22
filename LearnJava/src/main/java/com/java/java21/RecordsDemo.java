package com.java.java21;

import java.io.Serializable;
import java.util.List;

/**
 * =====================================================================
 * JAVA 21 FEATURE: RECORDS + TEXT BLOCKS
 * =====================================================================
 *
 * WHAT ARE RECORDS? (Java 16 — stable; Java 21 LTS)
 * -------------------------------------------------------
 * A "record" is a special kind of class designed for immutable data carriers.
 * You declare the fields (called "components") in the header, and the
 * compiler auto-generates:
 *   1. A canonical constructor (takes all components as parameters)
 *   2. Accessor methods: point.x(), point.y()  (NOT getX() — note no "get")
 *   3. equals() — compares all components by value
 *   4. hashCode() — based on all components
 *   5. toString() — "Point[x=3, y=4]"
 *
 * WHY WAS THIS ADDED?
 * Before records, creating a simple "value class" (like a coordinate or a DTO)
 * required ~50 lines of boilerplate: fields, constructor, getters, equals, hashCode, toString.
 * With records: 1 line. And it's IMPOSSIBLE to forget equals/hashCode (a common bug).
 *
 * RECORD vs LOMBOK @Value:
 *   - @Value (Lombok) = generated at compile time via annotation processor → needs Lombok dependency
 *   - record = built into the Java language → no extra dependency, IDE understands it natively
 *   - Both produce immutable value classes, but record is the standard approach going forward
 *
 * RECORD vs PLAIN CLASS:
 *   - record fields are FINAL — you cannot change them after construction (immutable)
 *   - A record CANNOT extend another class (it implicitly extends java.lang.Record)
 *   - A record CAN implement interfaces
 *   - A record CAN have static fields, static methods, and instance methods
 *   - A record CANNOT have instance fields other than its components
 *
 * INTERVIEW MUST-KNOW QUESTIONS:
 *   Q: "Can a record extend a class?"
 *   A: No. Records implicitly extend java.lang.Record (like enums extend Enum). No double inheritance.
 *
 *   Q: "Can a record implement an interface?"
 *   A: Yes! This is how you attach behavior to records.
 *
 *   Q: "Are records serializable?"
 *   A: Only if you add `implements Serializable`. They don't auto-serialize.
 *
 *   Q: "Are records thread-safe?"
 *   A: Yes — all fields are final, so they are safely published and visible across threads.
 *
 *   Q: "What is a compact constructor?"
 *   A: A constructor inside the record that runs BEFORE the fields are assigned.
 *      Used for VALIDATION. You don't re-declare parameters — they're auto-assigned after your code runs.
 *
 * WHAT ARE TEXT BLOCKS? (Java 15 — stable; Java 21 LTS)
 * -------------------------------------------------------
 * Multi-line string literals delimited by triple quotes """ ... """.
 * Java handles indentation stripping automatically — no need for manual \n concatenation.
 * Great for JSON, SQL, HTML, XML snippets in code.
 *
 * =====================================================================
 */
public class RecordsDemo {

    // =====================================================================
    // 1. BASIC RECORD DECLARATION
    // =====================================================================
    // This one line replaces ~50 lines of boilerplate.
    // Components: x and y are the record's fields (final, package-private accessor methods).
    record Point(int x, int y) {}

    // A record that implements an interface
    interface Printable {
        void print();
    }

    record NamedPoint(String name, int x, int y) implements Printable {
        // Custom instance method — records can have behavior too
        @Override
        public void print() {
            System.out.println("Point '" + name + "' at (" + x + ", " + y + ")");
        }

        // Static factory method — often cleaner than calling constructor directly
        static NamedPoint origin(String name) {
            return new NamedPoint(name, 0, 0);
        }
    }

    // =====================================================================
    // 2. COMPACT CONSTRUCTOR (Validation)
    // =====================================================================
    // A compact constructor has no parameter list — the components are in scope
    // and will be auto-assigned AFTER this block runs.
    // This is where you put VALIDATION logic.
    //
    // In a regular constructor you'd write:
    //   public Circle(double radius) { if (radius <= 0) throw...; this.radius = radius; }
    // In a compact constructor:
    //   record Circle(double radius) { Circle { if (radius <= 0) throw...; } }
    //   The `this.radius = radius` assignment is IMPLICIT after the block.
    record Circle(double radius) {
        // Compact constructor — runs before auto-assignment of fields
        Circle {
            if (radius <= 0) {
                // Interview: "Where should validation go in a record?"
                // → In the compact constructor — it's the only place to intercept construction
                throw new IllegalArgumentException("Radius must be positive, got: " + radius);
            }
            // Optional: normalize data here (e.g., round to 2 decimal places)
            // radius = Math.round(radius * 100.0) / 100.0;  // you CAN reassign components here
        }

        // Custom method — compute area using the immutable radius field
        double area() {
            // Math.PI is a double constant — records can use any instance fields freely
            return Math.PI * radius * radius;
        }
    }

    // =====================================================================
    // 3. RECORD AS A DTO (Data Transfer Object)
    // =====================================================================
    // This is the most common use of records in real Spring Boot apps.
    // Instead of a Lombok @Value class, use a record for API request/response objects.
    //
    // Interview: "How would you use a record in a REST API?"
    // → As request/response DTOs. Immutable, no setters, Jackson can deserialize into them.
    //   (Spring Boot 3+ with Jackson 2.12+ supports record deserialization natively.)
    record TransferRequestDTO(Long fromWalletId, Long toWalletId, double amount) {
        // Compact constructor for validation
        TransferRequestDTO {
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
            if (fromWalletId.equals(toWalletId)) throw new IllegalArgumentException("Cannot transfer to same wallet");
        }
    }

    // =====================================================================
    // 4. SERIALIZABLE RECORD
    // =====================================================================
    // Records are NOT serializable by default.
    // Add `implements Serializable` explicitly when you need Java serialization.
    record SerializablePoint(int x, int y) implements Serializable {
        // serialVersionUID is optional for records but good practice
    }

    // =====================================================================
    // 5. GENERIC RECORD
    // =====================================================================
    // Records can be generic — useful for pair/tuple use cases
    //
    // Interview: "Can records be generic?" → Yes.
    record Pair<A, B>(A first, B second) {
        // Swap the pair — returns a new record (records are immutable)
        Pair<B, A> swap() {
            return new Pair<>(second, first);
        }
    }

    // =====================================================================
    // MAIN — Run all demos with labeled test cases
    // =====================================================================
    public static void main(String[] args) {

        System.out.println("=== 1. Basic Record ===");
        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        Point p3 = new Point(1, 2);

        // Accessor methods: x() and y() — NOT getX() / getY()
        System.out.println("p1.x() = " + p1.x() + ", p1.y() = " + p1.y());

        // equals() is auto-generated — compares by VALUE of all components
        System.out.println("p1.equals(p2) = " + p1.equals(p2));  // true (same values)
        System.out.println("p1.equals(p3) = " + p1.equals(p3));  // false

        // toString() auto-generated: "Point[x=3, y=4]"
        System.out.println("p1.toString() = " + p1);

        // hashCode() auto-generated — consistent with equals()
        System.out.println("p1.hashCode() == p2.hashCode(): " + (p1.hashCode() == p2.hashCode())); // true

        System.out.println("\n=== 2. Record Implementing Interface ===");
        NamedPoint np = new NamedPoint("Home", 10, 20);
        np.print();  // uses the custom print() method
        NamedPoint origin = NamedPoint.origin("Center");
        origin.print();

        System.out.println("\n=== 3. Compact Constructor Validation ===");
        try {
            Circle valid = new Circle(5.0);
            System.out.printf("Valid circle area: %.2f%n", valid.area());

            Circle invalid = new Circle(-1.0);  // should throw
        } catch (IllegalArgumentException e) {
            System.out.println("Caught expected: " + e.getMessage());
        }

        System.out.println("\n=== 4. Record as DTO ===");
        TransferRequestDTO dto = new TransferRequestDTO(1L, 2L, 500.0);
        System.out.println("DTO: " + dto);
        // Fields accessed via accessor methods: dto.fromWalletId(), dto.amount()
        System.out.println("Amount: " + dto.amount());

        try {
            // Same wallet — should fail
            new TransferRequestDTO(1L, 1L, 100.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println("\n=== 5. Generic Record ===");
        Pair<String, Integer> pair = new Pair<>("Alice", 30);
        System.out.println("Pair: " + pair);
        Pair<Integer, String> swapped = pair.swap();
        System.out.println("Swapped: " + swapped);

        System.out.println("\n=== 6. Records in Collections ===");
        // Records work perfectly in collections because equals/hashCode are correct
        List<Point> points = List.of(new Point(1, 2), new Point(3, 4), new Point(1, 2));
        long distinctCount = points.stream().distinct().count();
        System.out.println("Total: " + points.size() + ", Distinct: " + distinctCount); // 2 distinct

        // =====================================================================
        // 7. TEXT BLOCKS
        // =====================================================================
        // Before (Java 8 style): ugly concatenation
        String oldJson = "{\n" +
                "  \"name\": \"Alice\",\n" +
                "  \"age\": 30\n" +
                "}";

        // After (Java 15+ Text Block): clean, readable, indentation-stripped automatically
        // The JVM strips common leading whitespace based on where the closing """ is.
        String jsonTextBlock = """
                {
                  "name": "Alice",
                  "age": 30
                }
                """;

        System.out.println("\n=== 7. Text Blocks ===");
        System.out.println("JSON (old way):\n" + oldJson);
        System.out.println("JSON (text block):\n" + jsonTextBlock);

        // Text blocks are great for SQL
        String sql = """
                SELECT u.name, w.balance
                FROM wallets w
                JOIN users u ON u.id = w.user_id
                WHERE w.currency = 'USD'
                ORDER BY w.balance DESC
                """;
        System.out.println("SQL:\n" + sql);

        // Interview: "Can you use String methods on a text block?"
        // A: Yes! A text block IS a String. All String methods work.
        System.out.println("SQL length: " + sql.length());
        System.out.println("SQL contains 'USD': " + sql.contains("USD"));
    }
}
