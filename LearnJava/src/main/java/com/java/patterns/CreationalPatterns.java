package com.java.patterns;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * ============================================================
 * CREATIONAL PATTERNS — Advanced GoF + Modern Java Patterns
 * ============================================================
 *
 * CONCEPT:
 *   Creational patterns deal with OBJECT CREATION mechanisms, aiming to create
 *   objects in a manner suitable to the situation. They abstract the instantiation
 *   process, making the system independent of how objects are created.
 *
 * PATTERNS COVERED:
 *   1. Abstract Factory — factory of factories
 *   2. Prototype — clone instead of new
 *   3. Object Pool — reuse expensive objects
 *   4. Dependency Injection — manual (no framework)
 *   5. Service Locator — antipattern (why to avoid)
 *
 * NOTE: Singleton, Builder, Factory Method are in separate files.
 *
 * INTERVIEW ANGLE:
 *   - "Abstract Factory vs Factory Method": AF creates families of related objects;
 *     FM creates ONE product via a template method (subclass decides which type).
 *   - "Prototype vs copy constructor": Copy constructor is safer in Java —
 *     no Cloneable pitfalls, no CloneNotSupportedException, no shallow copy bugs.
 *   - "DI vs Service Locator": DI pushes dependencies IN (explicit, testable);
 *     SL pulls dependencies OUT (hidden, hard to test — antipattern).
 *
 * REAL-WORLD USE:
 *   Abstract Factory: javax.xml.parsers.DocumentBuilderFactory, cross-platform UI
 *   Prototype: Object.clone(), Spring's prototype scope
 *   Object Pool: HikariCP, Apache Commons Pool, thread pools
 *   DI: Spring Framework, Guice, Dagger
 */
public class CreationalPatterns {

    // =========================================================
    // 1. ABSTRACT FACTORY PATTERN
    // =========================================================
    //
    // CONCEPT: Provides an interface for creating FAMILIES of related or
    //   dependent objects without specifying their concrete classes.
    //   "Factory of Factories" — but that's a simplification.
    //   More precisely: a factory that guarantees CONSISTENCY between products.
    //
    // PROBLEM SOLVED:
    //   Without Abstract Factory, you might create a LightButton with a DarkTextField —
    //   inconsistent theme. Abstract Factory enforces that all components come from
    //   the same family (all Light OR all Dark).
    //
    // WHEN TO USE:
    //   - System must be independent of how its products are created
    //   - System needs to work with multiple FAMILIES of products
    //   - You want to enforce product family consistency
    //
    // ABSTRACT FACTORY vs FACTORY METHOD:
    //   Factory Method: creates ONE product. Abstract method in creator.
    //   Abstract Factory: creates a FAMILY of products. Separate factory object.
    //
    // REAL JAVA EXAMPLE:
    //   javax.xml.parsers.DocumentBuilderFactory (different XML parsers)
    //   java.sql.Connection (creates Statement, PreparedStatement, CallableStatement)
    //   DriverManager.getConnection() — returns Connection (factory for DB objects)
    //
    // GOTCHA: Adding a new product type to the family requires changing the
    //   Abstract Factory interface AND all concrete factories — expensive change.
    //   Mitigation: parameterize the factory method (but loses type safety).

    // ABSTRACT PRODUCTS
    interface Button {
        void render();
        void onClick();
    }

    interface TextField {
        void render();
        String getValue();
    }

    interface Checkbox {
        void render();
        boolean isChecked();
    }

    // CONCRETE PRODUCTS — Light Theme family
    static class LightButton implements Button {
        @Override public void render() { System.out.println("    [LightButton] ☐ Render white btn"); }
        @Override public void onClick() { System.out.println("    [LightButton] Clicked (light style)"); }
    }
    static class LightTextField implements TextField {
        @Override public void render() { System.out.println("    [LightTextField] Render white input"); }
        @Override public String getValue() { return "light input value"; }
    }
    static class LightCheckbox implements Checkbox {
        private boolean checked = false;
        @Override public void render() { System.out.println("    [LightCheckbox] □ Render light check"); }
        @Override public boolean isChecked() { return checked; }
    }

    // CONCRETE PRODUCTS — Dark Theme family
    static class DarkButton implements Button {
        @Override public void render() { System.out.println("    [DarkButton] ■ Render dark btn"); }
        @Override public void onClick() { System.out.println("    [DarkButton] Clicked (dark style)"); }
    }
    static class DarkTextField implements TextField {
        @Override public void render() { System.out.println("    [DarkTextField] Render dark input"); }
        @Override public String getValue() { return "dark input value"; }
    }
    static class DarkCheckbox implements Checkbox {
        private boolean checked = true;
        @Override public void render() { System.out.println("    [DarkCheckbox] ✓ Render dark check"); }
        @Override public boolean isChecked() { return checked; }
    }

    // ABSTRACT FACTORY: defines the interface for creating a PRODUCT FAMILY
    interface UIComponentFactory {
        Button createButton();
        TextField createTextField();
        Checkbox createCheckbox();
    }

    // CONCRETE FACTORIES: each creates one consistent product family
    static class LightThemeFactory implements UIComponentFactory {
        @Override public Button createButton() { return new LightButton(); }
        @Override public TextField createTextField() { return new LightTextField(); }
        @Override public Checkbox createCheckbox() { return new LightCheckbox(); }
    }

    static class DarkThemeFactory implements UIComponentFactory {
        @Override public Button createButton() { return new DarkButton(); }
        @Override public TextField createTextField() { return new DarkTextField(); }
        @Override public Checkbox createCheckbox() { return new DarkCheckbox(); }
    }

    // CLIENT: works with Abstract Factory — doesn't know which theme
    // KEY INSIGHT: The client is completely decoupled from concrete product classes.
    //   Change the factory, change the entire theme. Zero client code changes.
    static class LoginForm {
        private final Button loginButton;
        private final TextField usernameField;
        private final Checkbox rememberMe;

        LoginForm(UIComponentFactory factory) {
            // Client creates components using ONLY the abstract factory interface
            this.loginButton = factory.createButton();
            this.usernameField = factory.createTextField();
            this.rememberMe = factory.createCheckbox();
        }

        void render() {
            usernameField.render();
            rememberMe.render();
            loginButton.render();
        }
    }

    // =========================================================
    // 2. PROTOTYPE PATTERN
    // =========================================================
    //
    // CONCEPT: Specify the kinds of objects to create using a prototypical
    //   instance, and create new objects by CLONING this prototype.
    //   Use when creation cost is high but copying is cheap.
    //
    // JAVA'S Cloneable INTERFACE — PITFALLS:
    //   1. Cloneable is a MARKER interface with no clone() method — confusing design.
    //   2. Object.clone() does SHALLOW copy by default — reference fields share
    //      the same object. Deep copy requires manual override.
    //   3. clone() returns Object, requires cast.
    //   4. CloneNotSupportedException is checked — adds try-catch noise.
    //   5. Final fields can't be reassigned in clone() — breaks with final fields.
    //
    // PREFER: Copy constructor or static factory method for cloning.
    //   The copy constructor approach: new Shape(original) — explicit, safe, no casts.
    //
    // WHEN TO USE:
    //   - Object creation is expensive (DB read, network call, heavy computation)
    //   - Instances differ only in state — start from a prototype and modify
    //   - Avoid subclassing: clone() instead of new
    //
    // REAL JAVA EXAMPLE:
    //   Object.clone() (but use carefully)
    //   Spring @Scope("prototype"): each injection gets a new instance
    //   java.lang.String (immutable, but conceptually each substring is a "clone")
    //
    // GOTCHA: Deep vs Shallow copy.
    //   Shallow: primitives are copied, references are shared.
    //   Deep: all referenced objects are also cloned.
    //   For mutable nested objects, shallow clone creates subtle sharing bugs.

    // Using COPY CONSTRUCTOR approach (preferred over Cloneable)
    static class ShapePrototype {
        private String color;
        private List<Integer> coordinates; // mutable — requires deep copy

        // Regular constructor
        ShapePrototype(String color, List<Integer> coordinates) {
            this.color = color;
            this.coordinates = coordinates;
        }

        // COPY CONSTRUCTOR: explicit deep copy — safer than Cloneable
        // KEY INSIGHT: Every mutable field gets its own new instance.
        ShapePrototype(ShapePrototype original) {
            this.color = original.color; // String is immutable — safe to share
            this.coordinates = new ArrayList<>(original.coordinates); // deep copy list
        }

        public void setColor(String color) { this.color = color; }
        public void addCoordinate(int c) { coordinates.add(c); }
        public String getColor() { return color; }
        public List<Integer> getCoordinates() { return Collections.unmodifiableList(coordinates); }

        @Override public String toString() {
            return "Shape{color=" + color + ", coords=" + coordinates + "}";
        }
    }

    // Cloneable example (showing the pitfalls, then correct deep clone)
    static class CloneableShape implements Cloneable {
        String name;
        int[] points; // array — requires manual deep copy

        CloneableShape(String name, int[] points) {
            this.name = name;
            this.points = points;
        }

        @Override
        public CloneableShape clone() {
            try {
                CloneableShape cloned = (CloneableShape) super.clone(); // shallow copy
                // MUST manually deep copy mutable reference fields:
                cloned.points = Arrays.copyOf(this.points, this.points.length);
                // String is immutable — shallow copy is fine
                return cloned;
            } catch (CloneNotSupportedException e) {
                // Can't happen since we implement Cloneable, but forced by checked exception
                throw new AssertionError("Should not happen", e);
            }
        }
    }

    // =========================================================
    // 3. OBJECT POOL PATTERN
    // =========================================================
    //
    // CONCEPT: Reuse a pool of pre-initialized, expensive-to-create objects
    //   rather than creating and destroying them on demand.
    //   Think HikariCP: reuse DB connections instead of creating new ones.
    //
    // WHEN TO USE:
    //   - Object creation is expensive (DB connections, thread pools, parsers)
    //   - Objects are used frequently but held for short durations
    //   - Limit on number of simultaneously existing objects is needed
    //
    // THREAD SAFETY: Use BlockingQueue for safe concurrent acquire/release.
    //   LinkedBlockingQueue: unbounded or bounded, FIFO order.
    //   ArrayBlockingQueue: bounded, fixed capacity — use for strict limits.
    //
    // REAL JAVA EXAMPLE:
    //   java.util.concurrent.ThreadPoolExecutor (pool of threads)
    //   HikariCP, c3p0, DBCP (pool of database connections)
    //   Apache Commons Pool2
    //
    // GOTCHA:
    //   1. Poisoned objects: if a borrowed object is left in bad state before return,
    //      next borrower gets a broken object. Add validation on return.
    //   2. Deadlock: if pool is empty and all borrowers wait — add timeout.
    //   3. Memory leak: never releasing objects back to pool.
    //   4. Max pool size vs min pool size: tune based on actual load.

    // Represents an expensive resource
    static class DatabaseConnection {
        private static final AtomicInteger idGen = new AtomicInteger(0);
        private final int id;
        private boolean valid = true;

        DatabaseConnection() {
            this.id = idGen.incrementAndGet();
            System.out.println("    [DB] Created connection #" + id + " (expensive!)");
        }

        public String executeQuery(String sql) {
            if (!valid) throw new IllegalStateException("Connection #" + id + " is invalid");
            return "Result of '" + sql + "' from conn#" + id;
        }

        public boolean isValid() { return valid; }
        public void invalidate() { this.valid = false; }
        public int getId() { return id; }
    }

    // Generic Object Pool using BlockingQueue for thread safety
    static class ObjectPool<T> {
        // KEY INSIGHT: BlockingQueue.poll(timeout) + offer() give us thread-safe
        //   acquire/release without explicit synchronization.
        private final BlockingQueue<T> pool;
        private final Supplier<T> factory;   // creates new objects
        private final Predicate<T> validator; // validates before reuse
        private final int maxSize;

        ObjectPool(int maxSize, Supplier<T> factory, Predicate<T> validator) {
            this.maxSize = maxSize;
            this.factory = factory;
            this.validator = validator;
            this.pool = new LinkedBlockingQueue<>(maxSize);
            // Pre-populate the pool
            for (int i = 0; i < maxSize; i++) {
                pool.offer(factory.get());
            }
        }

        // ACQUIRE: get from pool or create new (with timeout to prevent deadlock)
        public T acquire(long timeoutMs) throws InterruptedException {
            T obj = pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (obj == null) {
                throw new RuntimeException("Pool exhausted after " + timeoutMs + "ms");
            }
            // Validate: if stale/broken, create a fresh one
            if (!validator.test(obj)) {
                System.out.println("    [Pool] Object invalid, creating replacement");
                obj = factory.get();
            }
            return obj;
        }

        // RELEASE: return to pool (validate first to prevent poisoning)
        public void release(T obj) {
            if (validator.test(obj)) {
                if (!pool.offer(obj)) {
                    System.out.println("    [Pool] Pool full, discarding object");
                }
            } else {
                System.out.println("    [Pool] Invalid object discarded, creating replacement");
                pool.offer(factory.get()); // replace with fresh object
            }
        }

        public int available() { return pool.size(); }
    }

    // =========================================================
    // 4. DEPENDENCY INJECTION (manual, no framework)
    // =========================================================
    //
    // CONCEPT: Invert control of dependencies — instead of an object creating
    //   its own dependencies (violating SRP), dependencies are provided externally.
    //
    // THREE INJECTION TYPES:
    //   1. CONSTRUCTOR INJECTION (best): dependencies provided at construction.
    //      - Makes dependencies EXPLICIT (required params in constructor)
    //      - Enables IMMUTABILITY (final fields)
    //      - Makes testing easy (just pass mock in constructor)
    //      - Fails fast: NullPointerException at construction, not at use time
    //   2. SETTER INJECTION: inject via setters after construction.
    //      - Allows optional dependencies
    //      - Object can be in inconsistent state between construction and injection
    //   3. FIELD INJECTION (worst, avoid in production code):
    //      - @Autowired on field (Spring)
    //      - Makes testing hard (need reflection to inject mocks)
    //      - Hides dependencies (not in constructor = not obvious)
    //
    // HOW SPRING WORKS:
    //   Spring's IoC container: scan @Component classes, call constructors,
    //   resolve dependencies recursively, store in ApplicationContext (bean registry).
    //   @Autowired = "please inject this dependency from the container".
    //   Fundamentally: DI with a factory/registry.
    //
    // WHEN TO USE: Always. DI is a fundamental practice, not just a pattern.
    //
    // REAL JAVA EXAMPLE: Spring @Autowired, Guice @Inject, Dagger
    //
    // GOTCHA: Circular dependencies with constructor injection are detected at
    //   startup (good!). With field injection, they may not be detected until runtime.

    interface NotificationService {
        void send(String recipient, String message);
    }

    interface UserRepository {
        String findEmailByUserId(String userId);
    }

    // Concrete implementations
    static class EmailNotificationService implements NotificationService {
        @Override public void send(String recipient, String message) {
            System.out.println("    [Email] Sending to " + recipient + ": " + message);
        }
    }

    static class InMemoryUserRepository implements UserRepository {
        private final Map<String, String> users = Map.of(
            "u1", "alice@example.com",
            "u2", "bob@example.com"
        );
        @Override public String findEmailByUserId(String userId) {
            return users.getOrDefault(userId, "unknown@example.com");
        }
    }

    // Constructor Injection (BEST practice)
    // KEY INSIGHT: UserService declares what it NEEDS via constructor params.
    //   It doesn't know or care which implementation is provided.
    //   Testing: pass a mock NotificationService to test UserService in isolation.
    static class UserService {
        private final UserRepository userRepo;           // final = immutable
        private final NotificationService notifService;  // final = immutable

        // Constructor injection: explicit, immutable, testable
        UserService(UserRepository userRepo, NotificationService notifService) {
            // Fail fast: if null injected, blow up at construction time
            this.userRepo = Objects.requireNonNull(userRepo, "userRepo required");
            this.notifService = Objects.requireNonNull(notifService, "notifService required");
        }

        public void notifyUser(String userId, String message) {
            String email = userRepo.findEmailByUserId(userId);
            notifService.send(email, message);
        }
    }

    // Simple DIY DI Container (illustrates what Spring does)
    static class SimpleContainer {
        private final Map<Class<?>, Object> registry = new HashMap<>();

        public <T> void register(Class<T> type, T instance) {
            registry.put(type, instance);
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Class<T> type) {
            return (T) registry.getOrDefault(type,
                null /* in Spring: throw NoSuchBeanDefinitionException */);
        }
    }

    // =========================================================
    // 5. SERVICE LOCATOR — ANTIPATTERN (know why to avoid it)
    // =========================================================
    //
    // CONCEPT: A registry that components query to obtain their dependencies.
    //   Objects pull their own dependencies from a central locator.
    //
    // WHY IT'S AN ANTIPATTERN:
    //   1. HIDDEN DEPENDENCIES: ServiceLocator.get(X) anywhere in code — you can't
    //      tell a class's dependencies from its constructor/interface alone.
    //   2. HARD TO TEST: Can't easily substitute test doubles — must configure
    //      the global locator for every test.
    //   3. RUNTIME FAILURE: Missing dependency detected at runtime, not compile time.
    //   4. TIGHT COUPLING to the locator itself — now dependent on the locator.
    //
    // WHEN IS IT ACCEPTABLE:
    //   - Legacy code migration (stepping stone toward DI)
    //   - Plugin systems where the caller is unknown at compile time
    //   - Java's ServiceLoader (java.util.ServiceLoader) is a form of SL
    //
    // COMPARE: DI (preferred) vs SL (antipattern)
    //   DI: UserService(repo, notif) — dependencies VISIBLE in constructor
    //   SL: ServiceLocator.get(UserRepo.class) inside UserService — HIDDEN
    //
    // INTERVIEW Q: "Have you used Service Locator?"
    //   A: "Yes, I'm familiar with it. It's generally considered an antipattern
    //      because it hides dependencies and makes testing difficult. I prefer
    //      constructor injection with a DI container like Spring."

    static class ServiceLocator {
        // Static mutable global state — the root of all evil for testing
        private static final Map<Class<?>, Object> services = new HashMap<>();

        public static <T> void register(Class<T> type, T service) {
            services.put(type, service);
        }

        @SuppressWarnings("unchecked")
        public static <T> T get(Class<T> type) {
            Object svc = services.get(type);
            if (svc == null) throw new RuntimeException("No service for: " + type.getSimpleName());
            return (T) svc;
        }
    }

    // Anti-pattern: UserServiceBad uses Service Locator internally
    // Problem: you can't tell it needs UserRepository/NotificationService
    // without reading the source code!
    static class UserServiceBad {
        public void notifyUser(String userId, String message) {
            // Hidden dependencies — fetched at runtime from global state
            UserRepository repo = ServiceLocator.get(UserRepository.class);
            NotificationService notif = ServiceLocator.get(NotificationService.class);
            String email = repo.findEmailByUserId(userId);
            notif.send(email, message);
        }
    }

    // =========================================================
    // MAIN: Demo all patterns
    // =========================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. ABSTRACT FACTORY PATTERN ===");
        {
            System.out.println("-- Light Theme --");
            UIComponentFactory lightFactory = new LightThemeFactory();
            new LoginForm(lightFactory).render();

            System.out.println("-- Dark Theme --");
            UIComponentFactory darkFactory = new DarkThemeFactory();
            new LoginForm(darkFactory).render();
            // KEY: Same LoginForm code, completely different component family
        }

        System.out.println("\n=== 2. PROTOTYPE PATTERN ===");
        {
            System.out.println("-- Copy Constructor (preferred) --");
            ShapePrototype original = new ShapePrototype("red", new ArrayList<>(List.of(0, 1, 2)));
            ShapePrototype clone = new ShapePrototype(original); // deep copy

            clone.setColor("blue");
            clone.addCoordinate(99);

            System.out.println("    Original: " + original);
            System.out.println("    Clone:    " + clone);
            // KEY: original not affected — deep copy confirmed

            System.out.println("-- Cloneable (with pitfall demo) --");
            CloneableShape s1 = new CloneableShape("circle", new int[]{1, 2, 3});
            CloneableShape s2 = s1.clone();
            s2.points[0] = 99; // would corrupt s1 without deep copy!
            System.out.println("    s1.points[0]=" + s1.points[0] + " (should still be 1, not 99)");
        }

        System.out.println("\n=== 3. OBJECT POOL PATTERN ===");
        {
            ObjectPool<DatabaseConnection> pool = new ObjectPool<>(
                3,
                DatabaseConnection::new,
                DatabaseConnection::isValid
            );

            System.out.println("    Pool size: " + pool.available());
            DatabaseConnection conn1 = pool.acquire(1000);
            DatabaseConnection conn2 = pool.acquire(1000);
            System.out.println("    Pool size after 2 acquires: " + pool.available());
            System.out.println("    " + conn1.executeQuery("SELECT * FROM users"));
            pool.release(conn1);
            pool.release(conn2);
            System.out.println("    Pool size after release: " + pool.available());

            // Test invalidation
            DatabaseConnection conn3 = pool.acquire(1000);
            conn3.invalidate(); // simulate broken connection
            pool.release(conn3); // should be discarded and replaced
            System.out.println("    Pool size after invalid release: " + pool.available());
        }

        System.out.println("\n=== 4. DEPENDENCY INJECTION (manual) ===");
        {
            System.out.println("-- Constructor Injection (best practice) --");
            UserRepository repo = new InMemoryUserRepository();
            NotificationService notif = new EmailNotificationService();
            UserService service = new UserService(repo, notif); // inject dependencies
            service.notifyUser("u1", "Your account was updated");

            System.out.println("\n-- DIY DI Container --");
            SimpleContainer container = new SimpleContainer();
            container.register(UserRepository.class, new InMemoryUserRepository());
            container.register(NotificationService.class, new EmailNotificationService());
            // Wire manually (Spring does this via reflection + @Autowired)
            UserService wiredService = new UserService(
                container.get(UserRepository.class),
                container.get(NotificationService.class)
            );
            wiredService.notifyUser("u2", "Welcome back!");
        }

        System.out.println("\n=== 5. SERVICE LOCATOR (antipattern — know why to avoid) ===");
        {
            // Register services in global state
            ServiceLocator.register(UserRepository.class, new InMemoryUserRepository());
            ServiceLocator.register(NotificationService.class, new EmailNotificationService());

            UserServiceBad badService = new UserServiceBad();
            badService.notifyUser("u1", "This works but is hard to test");

            System.out.println("\n  WHY ANTIPATTERN:");
            System.out.println("  - UserServiceBad constructor: UserServiceBad() — no hint of dependencies");
            System.out.println("  - To test, must configure global ServiceLocator (not isolated)");
            System.out.println("  - Missing registration = RuntimeException, not compile error");
        }

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: Abstract Factory vs Factory Method?");
        System.out.println("A: Factory Method = one product, subclass decides type.");
        System.out.println("   Abstract Factory = FAMILY of related products, enforces consistency.");
        System.out.println();
        System.out.println("Q: Why prefer copy constructor over Cloneable?");
        System.out.println("A: Cloneable is a broken design: marker interface, shallow copy by default,");
        System.out.println("   CloneNotSupportedException checked, can't use final fields.");
        System.out.println("   Copy constructor: explicit, type-safe, deep by design, no special interface.");
        System.out.println();
        System.out.println("Q: Object Pool vs new every time?");
        System.out.println("A: Pool: amortized creation cost, bounded resource usage, predictable latency.");
        System.out.println("   New: simple, no state sharing bugs. Use pool when creation is expensive");
        System.out.println("   (DB connection ~100ms vs pool acquire ~0.1ms).");
    }
}
