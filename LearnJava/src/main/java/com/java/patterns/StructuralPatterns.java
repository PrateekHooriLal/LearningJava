package com.java.patterns;

import java.util.*;
import java.util.concurrent.*;
import java.lang.ref.*;

/**
 * ============================================================
 * STRUCTURAL PATTERNS — All 7 GoF Structural Design Patterns
 * ============================================================
 *
 * CONCEPT:
 *   Structural patterns deal with object COMPOSITION — how classes and objects
 *   are assembled into larger structures. They use inheritance and composition
 *   to form new structures without recreating them from scratch.
 *
 * WHEN TO USE:
 *   - When you need to compose objects into tree structures (Composite)
 *   - When you need to add behavior to objects without subclassing (Decorator)
 *   - When you need to simplify a complex subsystem (Facade)
 *   - When you need to translate one interface to another (Adapter)
 *   - When you need to decouple abstraction from implementation (Bridge)
 *   - When you need to share fine-grained objects efficiently (Flyweight)
 *   - When you need to control access to an object (Proxy)
 *
 * TIME/SPACE:
 *   Pattern overhead is generally O(1) per method call (one level of indirection).
 *   Flyweight: O(unique states) memory vs O(all objects) without it.
 *
 * INTERVIEW ANGLE:
 *   - "Adapter vs Facade": Adapter changes the interface of ONE object;
 *     Facade provides a NEW simplified interface over MANY objects.
 *   - "Decorator vs Proxy": Decorator ADDS behavior, Proxy CONTROLS access.
 *     Both wrap the object, but intent differs.
 *   - "Composite vs Decorator": Composite has MULTIPLE children (tree);
 *     Decorator wraps ONE object (chain).
 *
 * REAL-WORLD USE:
 *   - java.io.* streams: Decorator pattern all the way down
 *   - Collections.synchronizedList(): Proxy pattern
 *   - java.awt.Component hierarchy: Composite pattern
 *   - String interning in JVM: Flyweight pattern
 *   - JDBC Driver: Adapter (vendor code -> JDBC interface)
 */
public class StructuralPatterns {

    // =========================================================
    // 1. ADAPTER PATTERN
    // =========================================================
    //
    // CONCEPT: Convert the interface of a class into another interface
    //   that clients expect. Lets incompatible interfaces work together.
    //
    // WHEN TO USE:
    //   - Integrating legacy code with a new system
    //   - Using third-party libraries with different interfaces
    //   - "We can't modify the existing class, but we need it to fit our interface"
    //
    // TWO FORMS:
    //   1. Class Adapter: extends the adaptee (Java has no multiple inheritance,
    //      so you can only extend ONE class — limited applicability)
    //   2. Object Adapter: wraps the adaptee via composition — PREFER THIS.
    //      More flexible (works with subclasses of adaptee too).
    //
    // REAL JAVA EXAMPLE:
    //   java.util.Arrays#asList() — adapts an array to List interface
    //   java.io.InputStreamReader — adapts InputStream (byte-based) to Reader (char-based)
    //   JDBC DriverManager — each driver is an adapter from vendor API to JDBC
    //
    // GOTCHA: Don't use Adapter when you can simply refactor. It adds a layer
    //   of indirection that can make the call chain hard to trace.

    // The NEW interface our system expects
    interface PaymentGateway {
        boolean processPayment(String currency, double amount);
        String getTransactionId();
    }

    // The LEGACY class we can't modify (third-party / old codebase)
    static class LegacyPaymentProcessor {
        // Legacy method: uses different parameter types and naming
        public int executeCharge(double amt, String currencyCode) {
            System.out.println("  [Legacy] Charging " + amt + " " + currencyCode);
            return 42; // legacy int transaction id
        }
        public String fetchStatus(int txId) { return "SUCCESS_" + txId; }
    }

    // OBJECT ADAPTER (composition — preferred)
    // KEY INSIGHT: We hold a reference to the adaptee, not inherit from it.
    //   This means we can adapt ANY subclass of LegacyPaymentProcessor,
    //   and we aren't locked into the inheritance hierarchy.
    static class PaymentAdapter implements PaymentGateway {
        private final LegacyPaymentProcessor legacy; // composition
        private int lastTxId;

        PaymentAdapter(LegacyPaymentProcessor legacy) {
            this.legacy = legacy;
        }

        @Override
        public boolean processPayment(String currency, double amount) {
            // ADAPTER WORK: translate from new interface to old interface
            this.lastTxId = legacy.executeCharge(amount, currency);
            return lastTxId > 0;
        }

        @Override
        public String getTransactionId() {
            return legacy.fetchStatus(lastTxId);
        }
    }

    // =========================================================
    // 2. BRIDGE PATTERN
    // =========================================================
    //
    // CONCEPT: Decouple an abstraction from its implementation so that
    //   both can vary independently. Uses COMPOSITION over inheritance
    //   to avoid the "class explosion" problem.
    //
    // PROBLEM IT SOLVES (the N×M explosion):
    //   Without Bridge: Shape(Circle, Square, Triangle) × Renderer(Vector, Raster, OpenGL)
    //   = 3×3 = 9 classes needed (CircleVectorRenderer, CircleRasterRenderer, ...)
    //   With Bridge: 3 + 3 = 6 classes. Shape holds reference to Renderer.
    //
    // WHEN TO USE:
    //   - When you want to avoid permanent binding between abstraction & implementation
    //   - When both should be extensible via subclassing independently
    //   - Common in cross-platform UI toolkits
    //
    // REAL JAVA EXAMPLE:
    //   java.sql.DriverManager + java.sql.Driver — platform-independent abstraction
    //   AWT Peer interface (java.awt.peer.*) bridges AWT to native OS
    //
    // GOTCHA: Don't confuse with Adapter.
    //   Bridge: designed upfront to let both sides vary.
    //   Adapter: retrofitted when two incompatible things need to work together.

    // IMPLEMENTATION hierarchy (can vary independently)
    interface Renderer {
        void renderCircle(double radius);
        void renderSquare(double side);
    }

    static class VectorRenderer implements Renderer {
        @Override public void renderCircle(double r) {
            System.out.println("  [Vector] Drawing circle, r=" + r);
        }
        @Override public void renderSquare(double s) {
            System.out.println("  [Vector] Drawing square, side=" + s);
        }
    }

    static class RasterRenderer implements Renderer {
        @Override public void renderCircle(double r) {
            System.out.println("  [Raster] Rendering circle pixels, r=" + r);
        }
        @Override public void renderSquare(double s) {
            System.out.println("  [Raster] Rendering square pixels, side=" + s);
        }
    }

    // ABSTRACTION hierarchy (can vary independently)
    static abstract class Shape {
        // KEY INSIGHT: The "bridge" is this reference to Renderer.
        //   Shape doesn't know which renderer is used at compile time.
        protected final Renderer renderer;

        Shape(Renderer renderer) { this.renderer = renderer; }
        abstract void draw();
        abstract void resize(double factor);
    }

    static class BridgeCircle extends Shape {
        private double radius;
        BridgeCircle(Renderer renderer, double radius) {
            super(renderer);
            this.radius = radius;
        }
        @Override public void draw() { renderer.renderCircle(radius); }
        @Override public void resize(double factor) { radius *= factor; }
    }

    static class BridgeSquare extends Shape {
        private double side;
        BridgeSquare(Renderer renderer, double side) {
            super(renderer);
            this.side = side;
        }
        @Override public void draw() { renderer.renderSquare(side); }
        @Override public void resize(double factor) { side *= factor; }
    }

    // =========================================================
    // 3. COMPOSITE PATTERN
    // =========================================================
    //
    // CONCEPT: Compose objects into tree structures to represent part-whole
    //   hierarchies. Composite lets clients treat individual objects (Leaf)
    //   and compositions of objects (Composite) uniformly.
    //
    // WHEN TO USE:
    //   - Tree structures: filesystems, org charts, UI component hierarchies
    //   - When clients should be able to ignore the difference between
    //     single objects and compositions of objects
    //
    // REAL JAVA EXAMPLE:
    //   java.awt.Component (leaf) and Container (composite)
    //   javax.swing.JComponent
    //   org.w3c.dom.Node (XML tree)
    //
    // GOTCHA: The "common interface" forces awkward methods onto Leaf objects.
    //   E.g., add()/remove() on a File node makes no semantic sense.
    //   Solution: Either accept this (transparent composite) or split the
    //   interface into node + container (safe composite, but loses uniformity).

    // COMPONENT: common interface for both Leaf and Composite
    interface FileSystemNode {
        String getName();
        long getSize();         // recursive for directories
        void print(String indent);
    }

    // LEAF: no children
    static class FileNode implements FileSystemNode {
        private final String name;
        private final long size;
        FileNode(String name, long size) { this.name = name; this.size = size; }

        @Override public String getName() { return name; }
        @Override public long getSize() { return size; }
        @Override public void print(String indent) {
            System.out.println(indent + "📄 " + name + " (" + size + " bytes)");
        }
    }

    // COMPOSITE: has children, delegates operations recursively
    static class DirectoryNode implements FileSystemNode {
        private final String name;
        // KEY INSIGHT: Children are stored as the COMPONENT interface,
        //   so a directory can contain both files and other directories.
        private final List<FileSystemNode> children = new ArrayList<>();

        DirectoryNode(String name) { this.name = name; }

        public void add(FileSystemNode node) { children.add(node); }
        public void remove(FileSystemNode node) { children.remove(node); }

        @Override public String getName() { return name; }

        // Recursive aggregation — this is the power of Composite
        @Override public long getSize() {
            return children.stream().mapToLong(FileSystemNode::getSize).sum();
        }

        @Override public void print(String indent) {
            System.out.println(indent + "📁 " + name + "/ (" + getSize() + " bytes)");
            children.forEach(c -> c.print(indent + "  "));
        }
    }

    // =========================================================
    // 4. DECORATOR PATTERN
    // =========================================================
    //
    // CONCEPT: Attach additional responsibilities to an object DYNAMICALLY.
    //   Provides a flexible alternative to subclassing for extending functionality.
    //   Wraps the component and delegates while adding behavior before/after.
    //
    // WHEN TO USE:
    //   - When you want to add behavior to individual objects, not the whole class
    //   - When subclassing is impractical (sealed class, or too many combinations)
    //   - When behavior should be addable and removable at runtime
    //
    // REAL JAVA EXAMPLE: java.io is entirely Decorator:
    //   new BufferedInputStream(new GZIPInputStream(new FileInputStream("file.gz")))
    //   Each wrapper adds one behavior: buffering, decompression, file reading.
    //   Collections.unmodifiableList() is a Decorator.
    //   javax.servlet.http.HttpServletRequestWrapper is a Decorator.
    //
    // GOTCHA: Don't confuse with Inheritance. Decorator is COMPOSITION.
    //   With inheritance, you'd need BufferedGZIPFileInputStream etc.
    //   With Decorator, you stack wrappers at runtime.
    //   ALSO: Decorators break object identity — decorated.equals(original) is false.

    interface TextProcessor {
        String process(String text);
    }

    // CONCRETE COMPONENT (the thing being decorated)
    static class PlainTextProcessor implements TextProcessor {
        @Override public String process(String text) { return text; }
    }

    // BASE DECORATOR: implements the interface, holds a reference to another
    // KEY INSIGHT: By making BaseDecorator abstract and having it implement
    //   TextProcessor, all decorators only override what they care about.
    static abstract class TextDecorator implements TextProcessor {
        protected final TextProcessor wrapped; // the "bridge" to inner object
        TextDecorator(TextProcessor wrapped) { this.wrapped = wrapped; }
        @Override public String process(String text) { return wrapped.process(text); }
    }

    // CONCRETE DECORATORS: each adds ONE behavior
    static class UpperCaseDecorator extends TextDecorator {
        UpperCaseDecorator(TextProcessor wrapped) { super(wrapped); }
        @Override public String process(String text) {
            return super.process(text).toUpperCase(); // delegate THEN add behavior
        }
    }

    static class TrimDecorator extends TextDecorator {
        TrimDecorator(TextProcessor wrapped) { super(wrapped); }
        @Override public String process(String text) {
            return super.process(text.trim()); // add behavior THEN delegate
        }
    }

    static class HtmlEscapeDecorator extends TextDecorator {
        HtmlEscapeDecorator(TextProcessor wrapped) { super(wrapped); }
        @Override public String process(String text) {
            return super.process(text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        }
    }

    // =========================================================
    // 5. FACADE PATTERN
    // =========================================================
    //
    // CONCEPT: Provide a unified, simplified interface to a set of interfaces
    //   in a subsystem. Facade defines a higher-level interface that makes
    //   the subsystem easier to use.
    //
    // WHEN TO USE:
    //   - When you want to provide a simple interface to a complex body of code
    //   - When you want to layer your subsystems (each layer uses facade of lower)
    //   - When there are many dependencies between clients and implementation classes
    //
    // REAL JAVA EXAMPLE:
    //   javax.faces.context.FacesContext (JSF facade over HTTP/Servlet)
    //   SLF4J as facade over Log4j/Logback/JUL
    //   Spring's JdbcTemplate (facade over raw JDBC)
    //
    // GOTCHA: Facade can become a "God Object" if it takes on too much logic.
    //   A Facade should ORCHESTRATE subsystems, not contain business logic itself.
    //   Also: Facade doesn't HIDE the subsystem — clients can still use it directly.

    // Complex subsystem classes (clients shouldn't need to know these)
    static class Projector {
        void turnOn() { System.out.println("  [Projector] ON"); }
        void setInput(String src) { System.out.println("  [Projector] Input: " + src); }
        void turnOff() { System.out.println("  [Projector] OFF"); }
    }
    static class SoundSystem {
        void turnOn() { System.out.println("  [Sound] ON"); }
        void setVolume(int vol) { System.out.println("  [Sound] Volume: " + vol); }
        void turnOff() { System.out.println("  [Sound] OFF"); }
    }
    static class SmartLights {
        void dim(int pct) { System.out.println("  [Lights] Dimmed to " + pct + "%"); }
        void on() { System.out.println("  [Lights] Full brightness"); }
    }
    static class StreamingDevice {
        void on() { System.out.println("  [Streaming] ON"); }
        void playMovie(String name) { System.out.println("  [Streaming] Playing: " + name); }
        void stop() { System.out.println("  [Streaming] Stopped"); }
    }

    // FACADE: simple interface over complex orchestration
    static class HomeTheaterFacade {
        private final Projector projector;
        private final SoundSystem sound;
        private final SmartLights lights;
        private final StreamingDevice streamer;

        HomeTheaterFacade() {
            this.projector = new Projector();
            this.sound = new SoundSystem();
            this.lights = new SmartLights();
            this.streamer = new StreamingDevice();
        }

        // KEY INSIGHT: watchMovie() orchestrates 7 steps the client doesn't care about.
        //   The client doesn't need to know the correct startup sequence.
        public void watchMovie(String movie) {
            System.out.println("  [Facade] Getting ready to watch: " + movie);
            lights.dim(20);
            projector.turnOn();
            projector.setInput("HDMI-1");
            sound.turnOn();
            sound.setVolume(70);
            streamer.on();
            streamer.playMovie(movie);
        }

        public void endMovie() {
            System.out.println("  [Facade] Shutting down theater");
            streamer.stop();
            sound.turnOff();
            projector.turnOff();
            lights.on();
        }
    }

    // =========================================================
    // 6. FLYWEIGHT PATTERN
    // =========================================================
    //
    // CONCEPT: Use sharing to support large numbers of fine-grained objects
    //   efficiently. Split object state into INTRINSIC (shared, immutable,
    //   stored in flyweight) and EXTRINSIC (unique per usage, passed by client).
    //
    // WHEN TO USE:
    //   - Application uses a HUGE number of similar objects (thousands+)
    //   - Object state can be made mostly external
    //   - Object identity doesn't matter (equals() by value, not reference)
    //
    // MEMORY SAVINGS EXAMPLE:
    //   Text editor with 1 million characters:
    //   WITHOUT Flyweight: 1,000,000 × (font 8B + size 4B + bold 1B + color 4B + pos 16B) = ~33 MB
    //   WITH Flyweight: 5 unique fonts × 17B + 1,000,000 × 16B (just position) = ~16 MB
    //   Real savings depend on how many unique font styles exist.
    //
    // REAL JAVA EXAMPLE:
    //   String pool / String interning (JVM intern pool is a Flyweight factory)
    //   Integer.valueOf(-128 to 127): JVM caches and reuses these objects
    //   java.awt.Font objects
    //
    // GOTCHA: Flyweight makes the code complex. Only use when memory IS the bottleneck.
    //   Flyweight objects must be IMMUTABLE (intrinsic state never changes).
    //   Thread safety: flyweights are naturally thread-safe if immutable.

    // INTRINSIC state: shared, immutable, context-independent
    static final class CharacterFlyweight {
        // KEY INSIGHT: These fields are INTRINSIC — they define the "kind" of character
        //   and are shared across all instances of the same kind.
        private final char character;
        private final String fontFamily;
        private final int fontSize;
        private final boolean bold;

        CharacterFlyweight(char character, String fontFamily, int fontSize, boolean bold) {
            this.character = character;
            this.fontFamily = fontFamily;
            this.fontSize = fontSize;
            this.bold = bold;
        }

        // EXTRINSIC state (position) is passed IN at render time, not stored here
        void render(int x, int y, String color) {
            System.out.printf("  Char '%c' [%s %d%s] at (%d,%d) color=%s%n",
                character, fontFamily, fontSize, bold ? " bold" : "", x, y, color);
        }

        @Override public String toString() {
            return character + "/" + fontFamily + "/" + fontSize + "/" + bold;
        }
    }

    // FLYWEIGHT FACTORY: creates/caches flyweights
    static class CharacterFlyweightFactory {
        // KEY INSIGHT: The cache is the heart of Flyweight.
        //   Key = intrinsic state. Same intrinsic = same flyweight object.
        private static final Map<String, CharacterFlyweight> cache = new HashMap<>();
        private static int cacheHits = 0, cacheMisses = 0;

        static CharacterFlyweight get(char c, String font, int size, boolean bold) {
            String key = c + "|" + font + "|" + size + "|" + bold;
            CharacterFlyweight fw = cache.computeIfAbsent(key, k -> {
                cacheMisses++;
                return new CharacterFlyweight(c, font, size, bold);
            });
            if (cache.containsKey(key) && cacheMisses > 0) cacheHits++;
            return fw;
        }

        static void printStats() {
            System.out.println("  Flyweight cache size: " + cache.size() +
                " objects (vs potentially thousands without sharing)");
        }
    }

    // CLIENT context: holds EXTRINSIC state + reference to flyweight
    static class TextCharacter {
        private final CharacterFlyweight flyweight; // shared
        private final int x, y;                     // extrinsic: unique position
        private final String color;                  // extrinsic: unique color

        TextCharacter(char c, String font, int size, boolean bold, int x, int y, String color) {
            this.flyweight = CharacterFlyweightFactory.get(c, font, size, bold);
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void render() { flyweight.render(x, y, color); }
    }

    // =========================================================
    // 7. PROXY PATTERN
    // =========================================================
    //
    // CONCEPT: Provide a surrogate or placeholder for another object
    //   to control access to it.
    //
    // THREE TYPES:
    //   1. VIRTUAL PROXY: delays expensive object creation until needed (lazy init)
    //   2. PROTECTION PROXY: controls access based on permissions
    //   3. REMOTE PROXY: represents an object in a different address space
    //      (Java RMI stub is a classic remote proxy)
    //
    // WHEN TO USE:
    //   - Lazy initialization of expensive objects (Virtual)
    //   - Access control (Protection)
    //   - Logging/caching/monitoring without modifying the target (Decorator-like)
    //   - Remote object representation (Remote)
    //
    // REAL JAVA EXAMPLES:
    //   Collections.synchronizedList() — Protection Proxy (thread-safe wrapper)
    //   java.lang.reflect.Proxy — dynamic proxy (used by Spring AOP, Hibernate)
    //   Hibernate lazy-loaded entities — Virtual Proxy
    //   Java RMI stubs — Remote Proxy
    //
    // DECORATOR vs PROXY:
    //   DECORATOR: adds behavior (the client KNOWS it's a decorator)
    //   PROXY: controls access (the client thinks it's the real object)
    //   Both implement the same interface. The difference is INTENT.

    interface Image {
        void display();
        String getName();
    }

    // REAL subject (expensive to load)
    static class RealImage implements Image {
        private final String filename;
        private byte[] imageData; // simulate expensive resource

        RealImage(String filename) {
            this.filename = filename;
            loadFromDisk(); // expensive operation in constructor
        }

        private void loadFromDisk() {
            System.out.println("  [RealImage] Loading from disk: " + filename + " (expensive!)");
            // simulate loading
            this.imageData = new byte[1024];
        }

        @Override public void display() {
            System.out.println("  [RealImage] Displaying: " + filename);
        }
        @Override public String getName() { return filename; }
    }

    // VIRTUAL PROXY: lazy loads the real object on first use
    // KEY INSIGHT: The proxy and real object implement the SAME interface.
    //   Client code works with the Image interface and never knows if it's
    //   talking to the proxy or the real object.
    static class VirtualImageProxy implements Image {
        private final String filename;
        private RealImage realImage; // null until first use

        VirtualImageProxy(String filename) {
            this.filename = filename;
            System.out.println("  [Proxy] Created proxy for: " + filename + " (no disk I/O yet)");
        }

        @Override public void display() {
            if (realImage == null) {
                realImage = new RealImage(filename); // lazy initialization
            }
            realImage.display();
        }
        @Override public String getName() { return filename; }
    }

    // PROTECTION PROXY: access control
    enum UserRole { ADMIN, VIEWER }

    static class ProtectedImageProxy implements Image {
        private final RealImage realImage;
        private final UserRole currentUserRole;

        ProtectedImageProxy(String filename, UserRole role) {
            this.realImage = new RealImage(filename);
            this.currentUserRole = role;
        }

        @Override public void display() {
            // KEY INSIGHT: Check access BEFORE delegating to real object
            if (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.VIEWER) {
                realImage.display();
            } else {
                throw new SecurityException("Access denied: insufficient permissions");
            }
        }
        @Override public String getName() { return realImage.getName(); }
    }

    // LOGGING PROXY (AOP-style): adds cross-cutting concerns
    static class LoggingImageProxy implements Image {
        private final Image wrapped;
        private int callCount = 0;

        LoggingImageProxy(Image wrapped) { this.wrapped = wrapped; }

        @Override public void display() {
            callCount++;
            System.out.println("  [LogProxy] display() called (#" + callCount + ")");
            long start = System.nanoTime();
            wrapped.display();
            long elapsed = System.nanoTime() - start;
            System.out.printf("  [LogProxy] display() completed in %d ns%n", elapsed);
        }
        @Override public String getName() { return wrapped.getName(); }
    }

    // =========================================================
    // MAIN: Demo all patterns
    // =========================================================
    public static void main(String[] args) {
        System.out.println("=== 1. ADAPTER PATTERN ===");
        {
            LegacyPaymentProcessor legacy = new LegacyPaymentProcessor();
            PaymentGateway gateway = new PaymentAdapter(legacy); // wraps legacy
            boolean ok = gateway.processPayment("USD", 99.99);
            System.out.println("  Payment ok: " + ok + ", txId: " + gateway.getTransactionId());
            // KEY: Client uses PaymentGateway interface, never touches LegacyPaymentProcessor directly
        }

        System.out.println("\n=== 2. BRIDGE PATTERN ===");
        {
            Renderer vector = new VectorRenderer();
            Renderer raster = new RasterRenderer();

            // Mix and match freely — no N×M explosion
            Shape c1 = new BridgeCircle(vector, 5.0);
            Shape c2 = new BridgeCircle(raster, 3.0);
            Shape s1 = new BridgeSquare(vector, 4.0);

            c1.draw(); c2.draw(); s1.draw();
            c1.resize(2); c1.draw(); // after resize
        }

        System.out.println("\n=== 3. COMPOSITE PATTERN ===");
        {
            // Build a filesystem tree
            FileNode readme = new FileNode("README.md", 1024);
            FileNode main = new FileNode("Main.java", 5120);
            FileNode test = new FileNode("Test.java", 3072);

            DirectoryNode src = new DirectoryNode("src");
            src.add(main); src.add(test);

            DirectoryNode root = new DirectoryNode("project");
            root.add(readme); root.add(src);

            root.print(""); // recursive print
            System.out.println("  Total size: " + root.getSize() + " bytes");
            // KEY: root.getSize() works whether node is file or directory — uniformity
        }

        System.out.println("\n=== 4. DECORATOR PATTERN ===");
        {
            // Stack decorators at runtime — order matters!
            TextProcessor plain = new PlainTextProcessor();
            TextProcessor trimmed = new TrimDecorator(plain);
            TextProcessor escaped = new HtmlEscapeDecorator(trimmed);
            TextProcessor upper = new UpperCaseDecorator(escaped);

            String input = "  hello <world>  ";
            System.out.println("  Input:    '" + input + "'");
            System.out.println("  Trimmed:  '" + trimmed.process(input) + "'");
            System.out.println("  Escaped:  '" + escaped.process(input) + "'");
            System.out.println("  All:      '" + upper.process(input) + "'");

            // Java IO equivalent:
            // InputStream is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(f)));
        }

        System.out.println("\n=== 5. FACADE PATTERN ===");
        {
            HomeTheaterFacade theater = new HomeTheaterFacade();
            theater.watchMovie("Inception");
            System.out.println("  ... enjoying movie ...");
            theater.endMovie();
        }

        System.out.println("\n=== 6. FLYWEIGHT PATTERN ===");
        {
            // Simulate 10 characters in a text editor (most share font/style)
            List<TextCharacter> text = new ArrayList<>();
            String content = "Hello!"; // 6 unique characters
            for (int i = 0; i < content.length(); i++) {
                // Multiple chars share the same flyweight (same font/size/bold)
                text.add(new TextCharacter(content.charAt(i), "Arial", 12, false,
                    i * 10, 100, "black"));
            }
            // Add repeated characters (will reuse flyweights)
            text.add(new TextCharacter('l', "Arial", 12, false, 100, 100, "red"));
            text.add(new TextCharacter('l', "Arial", 12, false, 110, 100, "blue"));

            text.forEach(TextCharacter::render);
            CharacterFlyweightFactory.printStats();
        }

        System.out.println("\n=== 7. PROXY PATTERN ===");
        {
            System.out.println("-- Virtual Proxy (lazy load) --");
            Image proxy = new VirtualImageProxy("photo.jpg");
            System.out.println("  Proxy created, no disk I/O yet. Name: " + proxy.getName());
            proxy.display(); // loads on first call
            proxy.display(); // uses cached real image

            System.out.println("\n-- Logging Proxy (AOP style) --");
            Image logged = new LoggingImageProxy(new VirtualImageProxy("banner.png"));
            logged.display();
            logged.display();

            // INTERVIEW Q: How does Collections.synchronizedList work?
            // A: It's a Protection Proxy that wraps every method in synchronized(mutex){}.
            //    It prevents concurrent modification but does NOT make iteration safe —
            //    you still need synchronized(list) around iteration code.
        }

        // INTERVIEW Q&A BLOCK
        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: Decorator vs Proxy?");
        System.out.println("A: Both wrap object implementing same interface. INTENT differs:");
        System.out.println("   Decorator ADDS/enhances behavior. Proxy CONTROLS access.");
        System.out.println("   Proxy often doesn't add new behavior, just gates access.");
        System.out.println();
        System.out.println("Q: Adapter vs Facade?");
        System.out.println("A: Adapter changes interface of ONE object.");
        System.out.println("   Facade provides simplified interface over MULTIPLE subsystem objects.");
        System.out.println();
        System.out.println("Q: When would you NOT use Flyweight?");
        System.out.println("A: When objects are not numerous (< thousands), or when extrinsic state");
        System.out.println("   is large, or when object identity matters. Complexity cost > memory benefit.");
    }
}
