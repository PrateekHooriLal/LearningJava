package com.java.java21;

/**
 * =====================================================================
 * JAVA 21 FEATURE: SEALED CLASSES + PATTERN MATCHING (instanceof)
 * =====================================================================
 *
 * WHAT ARE SEALED CLASSES? (Java 17 — stable; Java 21 LTS)
 * -------------------------------------------------------
 * A sealed class/interface RESTRICTS which classes can extend/implement it.
 * You declare the permitted subtypes using the `permits` keyword.
 *
 * WHY WAS THIS ADDED?
 * Before sealed classes, if you had:
 *   abstract class Shape {}
 *   class Circle extends Shape {}
 *   class Rectangle extends Shape {}
 *
 * Any class in any package could extend Shape. You couldn't enforce a closed hierarchy.
 * This meant switch statements on Shape couldn't be exhaustive — someone could always add
 * a new subtype you didn't account for.
 *
 * Sealed classes say: "The only subtypes of Shape that EVER EXIST are Circle, Rectangle, Triangle."
 * The compiler can now VERIFY exhaustiveness — a switch that covers all 3 is complete.
 *
 * REAL-WORLD ANALOGY:
 * Think of it like a JSON value type — a JSON value is EXACTLY one of:
 *   JsonNull, JsonBoolean, JsonNumber, JsonString, JsonArray, JsonObject.
 * There are no other kinds. Sealed class enforces this in Java.
 *
 * RULES FOR SEALED CLASSES:
 * 1. The `permits` clause lists ALL allowed direct subtypes.
 * 2. Every permitted subtype must be in the same package (or module).
 * 3. Every permitted subtype must be declared as ONE of:
 *    - `final`     → cannot be further extended (leaf node)
 *    - `sealed`    → can be extended but with its own restricted set
 *    - `non-sealed`→ opens the hierarchy again (defeats the purpose — rarely used)
 *
 * PATTERN MATCHING FOR instanceof (Java 16 — stable):
 * -------------------------------------------------------
 * Old way (pre Java 16):
 *   if (shape instanceof Circle) {
 *       Circle c = (Circle) shape;   // explicit cast — redundant and error-prone
 *       doSomething(c.radius);
 *   }
 *
 * New way (Java 16+):
 *   if (shape instanceof Circle c) {   // declares binding variable c — auto-cast!
 *       doSomething(c.radius());        // c is already typed as Circle in this block
 *   }
 *
 * The "binding variable" (c) is only in scope where the pattern is guaranteed to match.
 *
 * INTERVIEW MUST-KNOW QUESTIONS:
 *   Q: "What problem do sealed classes solve?"
 *   A: They allow exhaustive type checking at compile time. Combined with switch expressions,
 *      the compiler verifies all cases are handled — no runtime surprises.
 *
 *   Q: "How is sealed different from an enum?"
 *   A: Enum = instances of the SAME type (all instances are Color, just different values).
 *      Sealed = DIFFERENT types that share a common base (Circle, Rectangle are different classes).
 *      Sealed subtypes can carry different data per type.
 *
 *   Q: "Can a sealed class be abstract?"
 *   A: Yes. Usually it IS abstract since you don't want to instantiate the base type directly.
 *
 *   Q: "What is non-sealed?"
 *   A: A permitted subtype that RE-OPENS the hierarchy. Rarely used — it means
 *      any class can extend that non-sealed type, bypassing the closed hierarchy.
 *
 * =====================================================================
 */
public class SealedClassesDemo {

    // =====================================================================
    // 1. SEALED INTERFACE — Shape hierarchy (classic example)
    // =====================================================================
    // `permits` lists all allowed direct implementors — nothing else can implement Shape
    sealed interface Shape permits Circle, Rectangle, Triangle {
        // All shapes must provide their area
        double area();
        // All shapes must provide their perimeter
        double perimeter();
    }

    // `final` — Circle cannot be further subclassed
    // This is the most common choice for leaf types
    record Circle(double radius) implements Shape {
        // Compact constructor validation
        Circle {
            if (radius <= 0) throw new IllegalArgumentException("Radius must be positive");
        }
        @Override public double area()      { return Math.PI * radius * radius; }
        @Override public double perimeter() { return 2 * Math.PI * radius; }
    }

    // `final` — Rectangle is a leaf type
    record Rectangle(double width, double height) implements Shape {
        Rectangle {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Dimensions must be positive");
        }
        @Override public double area()      { return width * height; }
        @Override public double perimeter() { return 2 * (width + height); }
    }

    // `final` — Triangle
    record Triangle(double base, double height, double sideA, double sideB, double sideC) implements Shape {
        @Override public double area()      { return 0.5 * base * height; }
        @Override public double perimeter() { return sideA + sideB + sideC; }
    }

    // =====================================================================
    // 2. SEALED CLASS for payment types (real-world fintech example)
    // =====================================================================
    // Using abstract class (not interface) when you need shared state/behavior
    sealed abstract class Payment permits CreditCard, BankTransfer, UPI {
        private final double amount;
        private final String currency;

        Payment(double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        double getAmount()   { return amount; }
        String getCurrency() { return currency; }

        // Common behavior — subclasses override for payment-type-specific logic
        abstract String processPayment();
    }

    final class CreditCard extends Payment {
        private final String cardNumber; // last 4 digits only for security
        CreditCard(double amount, String currency, String cardNumber) {
            super(amount, currency);
            this.cardNumber = cardNumber;
        }
        @Override public String processPayment() {
            return String.format("Processing %.2f %s via Credit Card ending %s",
                    getAmount(), getCurrency(), cardNumber);
        }
    }

    final class BankTransfer extends Payment {
        private final String accountNumber;
        BankTransfer(double amount, String currency, String accountNumber) {
            super(amount, currency);
            this.accountNumber = accountNumber;
        }
        @Override public String processPayment() {
            return String.format("Processing %.2f %s via Bank Transfer to account %s",
                    getAmount(), getCurrency(), accountNumber);
        }
    }

    final class UPI extends Payment {
        private final String upiId;
        UPI(double amount, String currency, String upiId) {
            super(amount, currency);
            this.upiId = upiId;
        }
        @Override public String processPayment() {
            return String.format("Processing %.2f %s via UPI to %s",
                    getAmount(), getCurrency(), upiId);
        }
    }

    // =====================================================================
    // 3. PATTERN MATCHING for instanceof
    // =====================================================================
    // The OLD way — requires explicit cast (verbose and error-prone)
    static String describeShapeOldWay(Shape shape) {
        if (shape instanceof Circle) {
            Circle c = (Circle) shape;  // explicit cast — we KNOW it's a Circle but Java still needs this
            return String.format("Old way: Circle with radius %.1f", c.radius());
        } else if (shape instanceof Rectangle) {
            Rectangle r = (Rectangle) shape;
            return String.format("Old way: Rectangle %s x %s", r.width(), r.height());
        }
        return "Old way: Unknown shape";
    }

    // The NEW way — pattern matching eliminates the cast (Java 16+)
    static String describeShapeNewWay(Shape shape) {
        // `instanceof Circle c` declares a binding variable c — auto-cast, no explicit cast needed
        // c is ONLY in scope inside this if block (where the pattern is guaranteed true)
        if (shape instanceof Circle c) {
            return String.format("New way: Circle with radius %.1f, area = %.2f", c.radius(), c.area());
        } else if (shape instanceof Rectangle r) {
            // The binding variable name (r) is scoped only to this else-if block
            return String.format("New way: Rectangle %.1f x %.1f, area = %.2f", r.width(), r.height(), r.area());
        } else if (shape instanceof Triangle t) {
            return String.format("New way: Triangle base=%.1f height=%.1f", t.base(), t.height());
        }
        return "Unknown";  // This line is now UNREACHABLE because Shape is sealed!
        // The compiler knows all Shape subtypes are Circle, Rectangle, Triangle.
        // In Module 3 (PatternMatchingDemo), we'll use switch which enforces exhaustiveness.
    }

    // Pattern matching with GUARDS (condition on the binding variable)
    // Java 16+: you can add && conditions to narrow the match further
    static String classifyShape(Shape shape) {
        if (shape instanceof Circle c && c.radius() > 10) {
            return "Large circle (radius > 10)";
        } else if (shape instanceof Circle c) {
            // c is in scope here too — a circle with radius <= 10
            return "Small circle (radius <= 10)";
        } else if (shape instanceof Rectangle r && r.width() == r.height()) {
            return "Square (equal sides)";
        } else if (shape instanceof Rectangle r) {
            return "Rectangle (non-square)";
        }
        return "Other shape";
    }

    // =====================================================================
    // MAIN — Run all demos
    // =====================================================================
    public static void main(String[] args) {

        System.out.println("=== 1. Basic Sealed Interface ===");
        Shape circle = new Circle(5.0);
        Shape rect = new Rectangle(4.0, 6.0);
        Shape triangle = new Triangle(3.0, 4.0, 3.0, 4.0, 5.0);

        System.out.printf("Circle area: %.2f, perimeter: %.2f%n", circle.area(), circle.perimeter());
        System.out.printf("Rectangle area: %.2f, perimeter: %.2f%n", rect.area(), rect.perimeter());
        System.out.printf("Triangle area: %.2f, perimeter: %.2f%n", triangle.area(), triangle.perimeter());

        System.out.println("\n=== 2. instanceof — Old Way vs New Way ===");
        System.out.println(describeShapeOldWay(circle));
        System.out.println(describeShapeNewWay(circle));
        System.out.println(describeShapeNewWay(rect));
        System.out.println(describeShapeNewWay(triangle));

        System.out.println("\n=== 3. Pattern Matching with Guards ===");
        System.out.println(classifyShape(new Circle(15.0)));  // Large circle
        System.out.println(classifyShape(new Circle(3.0)));   // Small circle
        System.out.println(classifyShape(new Rectangle(5.0, 5.0)));  // Square
        System.out.println(classifyShape(new Rectangle(3.0, 7.0)));  // Rectangle

        System.out.println("\n=== 4. Sealed Class Compile-Time Safety ===");
        // Try this: create a new class extending Shape outside this file.
        // Result: COMPILE ERROR — "class is not allowed to extend sealed class"
        // This is the KEY benefit of sealed classes — the hierarchy is locked.
        System.out.println("Sealed hierarchy is locked. No new Shape types possible outside permits list.");

        System.out.println("\n=== 5. Processing a list of shapes ===");
        java.util.List<Shape> shapes = java.util.List.of(
            new Circle(5.0),
            new Rectangle(3.0, 4.0),
            new Circle(1.0),
            new Rectangle(7.0, 7.0),
            new Triangle(6.0, 4.0, 5.0, 5.0, 6.0)
        );

        double totalArea = shapes.stream()
            .mapToDouble(Shape::area)
            .sum();
        System.out.printf("Total area of all shapes: %.2f%n", totalArea);

        // Count circles vs rectangles
        long circleCount = shapes.stream().filter(s -> s instanceof Circle).count();
        long rectCount   = shapes.stream().filter(s -> s instanceof Rectangle).count();
        System.out.println("Circles: " + circleCount + ", Rectangles: " + rectCount);
    }
}
