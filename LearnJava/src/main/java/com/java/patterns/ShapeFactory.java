package com.java.patterns.factory;

/**
 * FACTORY METHOD + ABSTRACT FACTORY PATTERNS
 *
 * TWO PATTERNS COVERED:
 *
 *   A) FACTORY METHOD:
 *      A static factory method (ShapeFactory.create) that returns different Shape
 *      implementations based on a type string.
 *      Decouples the client from concrete class names.
 *
 *   B) ABSTRACT FACTORY:
 *      A factory-of-factories. ThemeFactory creates families of related objects
 *      (Button + Checkbox) that belong together (LightTheme or DarkTheme).
 *      Ensures consistency: you can't accidentally mix LightButton with DarkCheckbox.
 *
 * DIFFERENCE BETWEEN THEM:
 *   Factory Method: one product, multiple implementations. "Give me A Shape."
 *   Abstract Factory: families of related products. "Give me a CONSISTENT set of UI components."
 *
 * REAL-WORLD USES:
 *   Factory Method:
 *     - JDBC DriverManager.getConnection() → returns a Connection for MySQL/Postgres/etc.
 *     - Spring BeanFactory.getBean() → returns the appropriate bean
 *     - DocumentBuilderFactory.newDocumentBuilder() → SAX/DOM parsers
 *   Abstract Factory:
 *     - Look-and-feel toolkits (Swing UIManager, JavaFX skins)
 *     - Cross-platform UI libraries
 *     - Test doubles: MockThemeFactory vs RealThemeFactory
 *
 * INTERVIEW FREQUENCY: Medium-high. Lead rounds ask "when would you use Factory vs Abstract Factory?"
 *
 * KEY INTERVIEW TALKING POINTS:
 *   1. "What's the difference between Factory Method and Abstract Factory?"
 *      Factory Method: single product. Abstract Factory: families of products.
 *   2. "How does factory relate to Open/Closed Principle?"
 *      Adding a new shape doesn't modify ShapeFactory's existing code — just add a new case.
 *   3. "What is the downside of Abstract Factory?"
 *      Adding a new product type (e.g., ScrollBar) requires changing ALL factory interfaces and classes.
 */
public class ShapeFactory {

    // =========================================================================
    // PART A: Factory Method Pattern
    // =========================================================================

    /** Shape interface — the product abstraction */
    interface Shape {
        void draw();
        double area();
    }

    /** Concrete product: Circle */
    static class Circle implements Shape {
        private final double radius;

        Circle(double radius) { this.radius = radius; }

        @Override
        public void draw() {
            System.out.printf("  Drawing Circle (radius=%.1f)%n", radius);
        }

        @Override
        public double area() {
            return Math.PI * radius * radius; // πr²
        }
    }

    /** Concrete product: Rectangle */
    static class Rectangle implements Shape {
        private final double width, height;

        Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void draw() {
            System.out.printf("  Drawing Rectangle (%.1f × %.1f)%n", width, height);
        }

        @Override
        public double area() {
            return width * height;
        }
    }

    /** Concrete product: Triangle */
    static class Triangle implements Shape {
        private final double base, height;

        Triangle(double base, double height) {
            this.base = base;
            this.height = height;
        }

        @Override
        public void draw() {
            System.out.printf("  Drawing Triangle (base=%.1f, height=%.1f)%n", base, height);
        }

        @Override
        public double area() {
            return 0.5 * base * height; // ½ × base × height
        }
    }

    /**
     * Static Factory Method — central creation point for Shape objects.
     *
     * WHY THIS MATTERS:
     *   - Client code doesn't need to know "Circle", "Rectangle", etc.
     *   - Changing a class name or adding a new subtype is localized here.
     *   - Follows Open/Closed Principle: extend by adding new cases, don't modify callers.
     *
     * In production, you'd use an enum or registration map instead of String comparison.
     */
    public static Shape create(String type, double... params) {
        switch (type.toLowerCase()) {
            case "circle":
                if (params.length < 1) throw new IllegalArgumentException("Circle needs radius");
                return new Circle(params[0]);

            case "rectangle":
                if (params.length < 2) throw new IllegalArgumentException("Rectangle needs width, height");
                return new Rectangle(params[0], params[1]);

            case "triangle":
                if (params.length < 2) throw new IllegalArgumentException("Triangle needs base, height");
                return new Triangle(params[0], params[1]);

            default:
                // Using IllegalArgumentException (RuntimeException) rather than checked exception
                // because unknown type is a programming error, not a recoverable runtime condition
                throw new IllegalArgumentException("Unknown shape: " + type);
        }
    }

    // =========================================================================
    // PART B: Abstract Factory Pattern — LightTheme / DarkTheme
    // =========================================================================

    /** Button product interface */
    interface Button {
        void render();
        void onClick();
    }

    /** Checkbox product interface */
    interface Checkbox {
        void render();
        void onToggle();
    }

    /**
     * Abstract Factory interface — creates a family of related UI widgets.
     * The client uses this interface and never knows which concrete theme it's using.
     */
    interface ThemeFactory {
        Button createButton();
        Checkbox createCheckbox();
    }

    // --- Light Theme products ---

    static class LightButton implements Button {
        @Override public void render()  { System.out.println("  [Light] Rendering bright white button"); }
        @Override public void onClick() { System.out.println("  [Light] Button clicked (light ripple effect)"); }
    }

    static class LightCheckbox implements Checkbox {
        @Override public void render()   { System.out.println("  [Light] Rendering light gray checkbox"); }
        @Override public void onToggle() { System.out.println("  [Light] Checkbox toggled (light animation)"); }
    }

    /** Concrete Factory for Light Theme — creates light-themed widget family */
    static class LightThemeFactory implements ThemeFactory {
        @Override public Button createButton()     { return new LightButton(); }
        @Override public Checkbox createCheckbox() { return new LightCheckbox(); }
    }

    // --- Dark Theme products ---

    static class DarkButton implements Button {
        @Override public void render()  { System.out.println("  [Dark] Rendering dark charcoal button"); }
        @Override public void onClick() { System.out.println("  [Dark] Button clicked (dark glow effect)"); }
    }

    static class DarkCheckbox implements Checkbox {
        @Override public void render()   { System.out.println("  [Dark] Rendering dark checkbox"); }
        @Override public void onToggle() { System.out.println("  [Dark] Checkbox toggled (dark animation)"); }
    }

    /** Concrete Factory for Dark Theme */
    static class DarkThemeFactory implements ThemeFactory {
        @Override public Button createButton()     { return new DarkButton(); }
        @Override public Checkbox createCheckbox() { return new DarkCheckbox(); }
    }

    /**
     * Application class that uses the Abstract Factory.
     * It only depends on ThemeFactory interface — completely decoupled from concrete classes.
     * Swapping themes requires zero changes to Application code.
     */
    static class Application {
        private final Button button;
        private final Checkbox checkbox;

        // Dependency Injection: factory is passed in (not created internally)
        // This also enables easy testing: pass a MockThemeFactory for tests
        Application(ThemeFactory factory) {
            // The factory creates a consistent family — can't accidentally mix Light/Dark
            this.button = factory.createButton();
            this.checkbox = factory.createCheckbox();
        }

        void renderUI() {
            button.render();
            checkbox.render();
        }

        void interact() {
            button.onClick();
            checkbox.onToggle();
        }
    }

    // =========================================================================
    // main() — demonstrate both patterns
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("=== Part A: Factory Method ===");
        Shape circle    = ShapeFactory.create("circle", 5.0);
        Shape rectangle = ShapeFactory.create("rectangle", 4.0, 6.0);
        Shape triangle  = ShapeFactory.create("triangle", 3.0, 8.0);

        circle.draw();
        System.out.printf("  Area: %.2f%n", circle.area());

        rectangle.draw();
        System.out.printf("  Area: %.2f%n", rectangle.area());

        triangle.draw();
        System.out.printf("  Area: %.2f%n", triangle.area());

        // Polymorphism: same interface, different behavior
        System.out.println("\n  Polymorphic loop:");
        Shape[] shapes = {circle, rectangle, triangle};
        for (Shape s : shapes) {
            s.draw(); // Runtime dispatch to the correct draw()
        }

        // Error case
        try {
            ShapeFactory.create("hexagon", 5.0);
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught expected error: " + e.getMessage());
        }

        System.out.println("\n=== Part B: Abstract Factory — Light Theme ===");
        Application lightApp = new Application(new LightThemeFactory());
        lightApp.renderUI();
        lightApp.interact();

        System.out.println("\n=== Part B: Abstract Factory — Dark Theme ===");
        // Switching theme: only one line changes (the factory) — Application code unchanged
        Application darkApp = new Application(new DarkThemeFactory());
        darkApp.renderUI();
        darkApp.interact();

        System.out.println("\n--- Key Insight ---");
        System.out.println("Application is 100% decoupled from LightButton/DarkButton.");
        System.out.println("To add HighContrastTheme: create new factory + products, no Application changes.");
    }
}
