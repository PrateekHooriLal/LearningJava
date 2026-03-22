package com.java.jvm;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ============================================================
 * JVM CLASS LOADING — Architecture, Phases, Custom Loaders
 * ============================================================
 *
 * CONCEPT:
 *   The JVM doesn't load all classes at startup. Classes are loaded
 *   ON DEMAND — when first referenced. Class loading is the process by which
 *   the JVM reads a .class file and creates a Class<?> object in memory.
 *
 * WHY IT MATTERS:
 *   - Security: each classloader defines a trust boundary
 *   - Hot reload: load new versions of classes at runtime (frameworks, OSGi)
 *   - Plugin systems: load user-supplied code dynamically
 *   - Bytecode instrumentation: transform classes at load time (Java agents, AOP)
 *   - Sandboxing: restrict what third-party code can access
 *
 * INTERVIEW ANGLE:
 *   - "ClassNotFoundException vs NoClassDefFoundError" — crucial distinction
 *   - "How does Spring hot-reload work?" → Custom classloader per deployment unit
 *   - "How does Java prevent java.lang.String from being replaced?"
 *     → Bootstrap classloader always loads java.* first (parent delegation)
 *   - "What triggers class initialization?" → First active use
 *
 * REAL-WORLD USE:
 *   - Tomcat: separate classloader per webapp (isolation between deployments)
 *   - OSGi: each bundle has own classloader with explicit module dependencies
 *   - JUnit 5: custom classloader for test isolation
 *   - Instrumentation: -javaagent transforms classes at load time (JProfiler, Jacoco)
 *
 * TIME/SPACE:
 *   Class loading: one-time cost. Thread-safe (JVM guarantees class initialization is atomic).
 *   Classloader hierarchy traversal: O(depth) where depth is typically 3.
 */
public class ClassLoadingDemo {

    // =========================================================
    // 1. CLASSLOADER HIERARCHY
    // =========================================================
    //
    // THREE BUILT-IN CLASSLOADERS:
    //
    // BOOTSTRAP CLASSLOADER (C/C++, not a Java class):
    //   Loads: java.lang.*, java.util.*, rt.jar (Java 8) or java.base module (Java 9+)
    //   Parent: none (it IS the root)
    //   Security: trusted. Classes loaded here bypass security checks.
    //   In Java: ClassLoader#getParent() returns null for bootstrap parent.
    //
    // EXTENSION/PLATFORM CLASSLOADER (Java 9+: Platform ClassLoader):
    //   Loads: javax.*, jdk.* modules, $JAVA_HOME/lib/ext/ (Java 8)
    //   Parent: Bootstrap
    //
    // APPLICATION/SYSTEM CLASSLOADER:
    //   Loads: your classpath, -cp entries, target/classes, .jar files in classpath
    //   Parent: Platform ClassLoader
    //   This is the classloader for your application code.
    //
    // PARENT DELEGATION MODEL:
    //   When asked to load class X:
    //   1. Check local cache (already loaded?) → return if found
    //   2. Ask PARENT to load X → return if parent succeeds
    //   3. If parent fails: load X yourself from own source
    //
    //   Why parent-first?
    //   Security: prevents user code from replacing java.lang.String (Bootstrap loads it first).
    //   Consistency: same Class object across different loaders in the hierarchy.
    //
    // BREAKING PARENT DELEGATION:
    //   OSGi and some frameworks REVERSE delegation for plugin code:
    //   load from local bundle first, only ask parent for platform classes.
    //   This enables: each plugin can have its own version of a library.
    //   Risk: multiple incompatible Class objects for "same" class in different loaders.

    static void demonstrateClassLoaderHierarchy() {
        System.out.println("--- ClassLoader Hierarchy ---");

        // Application classloader: loads your code
        ClassLoader appLoader = ClassLoadingDemo.class.getClassLoader();
        System.out.println("App ClassLoader: " + appLoader);

        // Platform classloader (Java 9+): loads javax.* and platform modules
        ClassLoader platformLoader = appLoader.getParent();
        System.out.println("Platform ClassLoader: " + platformLoader);

        // Bootstrap classloader: returns null (it's native, not a Java object)
        ClassLoader bootstrapLoader = platformLoader != null ? platformLoader.getParent() : null;
        System.out.println("Bootstrap ClassLoader: " + bootstrapLoader + " (null = C++ bootstrap)");

        // Verifying: java.lang.String is loaded by bootstrap (trusted code)
        ClassLoader stringLoader = String.class.getClassLoader();
        System.out.println("String.class classloader: " + stringLoader + " (null = bootstrap)");

        // Your class is loaded by app loader
        ClassLoader ourLoader = ClassLoadingDemo.class.getClassLoader();
        System.out.println("ClassLoadingDemo classloader: " + ourLoader);
    }

    // =========================================================
    // 2. CLASS LOADING PHASES
    // =========================================================
    //
    // PHASE 1 — LOADING:
    //   Find the .class file (on classpath, in jar, from network, from byte array).
    //   Create a Class<?> object in the method area (Metaspace).
    //   Classloader's findClass() / defineClass() is called here.
    //
    // PHASE 2 — LINKING (three sub-phases):
    //   VERIFICATION: bytecode is valid? Doesn't violate JVM constraints?
    //     - Type checking, stack overflow checks, branch target validation
    //     - Can be skipped with -Xverify:none (DANGEROUS, only for trusted code)
    //   PREPARATION: allocate memory for class-level (static) fields, set to DEFAULT values.
    //     - int static fields = 0, boolean = false, Object refs = null
    //     - NOT running initializer code yet!
    //   RESOLUTION: resolve symbolic references (class names → Class objects,
    //     method names → MethodRef). Lazy or eager depending on JVM.
    //
    // PHASE 3 — INITIALIZATION:
    //   Run static initializers: static blocks + static field initializers IN ORDER.
    //   JVM guarantees: initialization is THREAD-SAFE and happens exactly ONCE.
    //   This is what makes "initialization-on-demand holder" singleton thread-safe.
    //
    // TRIGGERS FOR INITIALIZATION (active use):
    //   - First instantiation: new MyClass()
    //   - First static field access: MyClass.FIELD (except compile-time constants)
    //   - First static method invocation: MyClass.staticMethod()
    //   - Class.forName("MyClass") (unless initialize=false argument)
    //   - subclass initialization triggers superclass first
    //   NOTE: array creation (new MyClass[10]) does NOT initialize MyClass!
    //
    // PASSIVE USES (no initialization):
    //   - Accessing compile-time constant (static final int X = 5): inlined by javac
    //   - Array type reference without instantiation
    //   - ClassLoader.loadClass() (see below)

    // Static initializer order demonstration
    static class Parent {
        static final int COMPILE_TIME_CONST = 42; // compile-time constant: inlined, no class init!
        static int runtimeStatic;

        static {
            System.out.println("  Parent static initializer runs FIRST");
            runtimeStatic = 100;
        }

        static int getRuntimeStatic() { return runtimeStatic; }
    }

    static class Child extends Parent {
        static int childStatic;

        static {
            System.out.println("  Child static initializer runs AFTER parent");
            // KEY: Parent is already initialized by the time we get here.
            // runtimeStatic is already 100.
            childStatic = runtimeStatic * 2; // 200
        }
    }

    static void demonstrateStaticInit() {
        System.out.println("\n--- Static Initializer Order ---");
        System.out.println("Accessing Child.childStatic: " + Child.childStatic);
        // Output order: Parent init → Child init → value printed
    }

    // =========================================================
    // 3. Class.forName() vs ClassLoader.loadClass()
    // =========================================================
    //
    // Class.forName(name):
    //   → Loads AND INITIALIZES the class (runs static initializers)
    //   → Uses the calling class's classloader by default
    //   → Used in: JDBC (Class.forName("com.mysql.cj.jdbc.Driver") triggers static block
    //     that registers the driver with DriverManager)
    //   → Throws ClassNotFoundException if not found (CHECKED exception)
    //
    // ClassLoader.loadClass(name):
    //   → Loads the class but does NOT initialize it (no static initializers!)
    //   → The link/initialize phases happen lazily or explicitly
    //   → Used in: custom classloaders, frameworks deferring initialization
    //   → Throws ClassNotFoundException (CHECKED)
    //
    // WHICH TO USE:
    //   If you need the class's static initializer to run (e.g., JDBC driver registration):
    //     → Class.forName()
    //   If you're writing a custom classloader or plugin framework:
    //     → loadClass() for control over initialization timing

    static void demonstrateForNameVsLoadClass() throws ClassNotFoundException {
        System.out.println("\n--- Class.forName() vs loadClass() ---");

        // Class.forName: loads AND initializes
        Class<?> clazz1 = Class.forName("java.util.ArrayList");
        System.out.println("forName: " + clazz1.getName() + " (initialized)");

        // loadClass: loads but defers initialization
        ClassLoader loader = ClassLoadingDemo.class.getClassLoader();
        Class<?> clazz2 = loader.loadClass("java.util.LinkedList");
        System.out.println("loadClass: " + clazz2.getName() + " (not yet initialized)");

        // Same Class object? YES — class identity is (classloader, className) pair
        Class<?> clazz3 = loader.loadClass("java.util.ArrayList");
        System.out.println("Same Class object for ArrayList: " + (clazz1 == clazz3)); // true
    }

    // =========================================================
    // 4. CUSTOM CLASSLOADER — Load from Byte Array
    // =========================================================
    //
    // USE CASES:
    //   - Plugin systems: load .class files from network or database
    //   - Bytecode instrumentation: transform class bytes before defining
    //   - Hot reload: reload a class without restarting JVM (limited by JVM constraints)
    //   - Sandboxing: define a security context for untrusted code
    //   - Encryption: store encrypted .class files, decrypt at load time
    //
    // IMPLEMENTATION PATTERN:
    //   Override findClass() — called by loadClass() when parent delegation fails.
    //   Call defineClass() with the raw bytecode bytes.
    //   defineClass() turns bytes → Class<?> object.
    //
    // LIMITATIONS:
    //   - A class loaded by classloader A cannot see classes in classloader B's hierarchy
    //     (unless explicitly exported). ClassCastException if you try to cast across loaders.
    //   - Can't unload individual classes — entire classloader must be GC'd.
    //   - Can't redefine a class that's already defined in the SAME classloader.
    //     Hot-reload requires a NEW classloader instance.

    static class ByteArrayClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytesMap = new HashMap<>();

        // Register a class's bytecode before loading
        void registerClass(String className, byte[] classBytes) {
            classBytesMap.put(className, classBytes);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytesMap.get(name);
            if (bytes == null) {
                throw new ClassNotFoundException("No bytes registered for: " + name);
            }
            System.out.println("  [ByteArrayClassLoader] Defining class: " + name);
            // KEY: defineClass() turns raw bytecode bytes into a Class<?> object
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    // Utility: get a class's bytecode from current classloader (for demo)
    static byte[] getClassBytes(String className) {
        String resourceName = className.replace('.', '/') + ".class";
        try (var stream = ClassLoader.getSystemResourceAsStream(resourceName)) {
            if (stream == null) return new byte[0];
            return stream.readAllBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    static void demonstrateCustomClassLoader() {
        System.out.println("\n--- Custom ClassLoader (load from bytes) ---");
        ByteArrayClassLoader customLoader = new ByteArrayClassLoader();

        // Get Stack's bytecode and register it with custom loader
        String className = "java.util.Stack";
        byte[] bytes = getClassBytes(className);

        if (bytes.length > 0) {
            // Note: for bootstrap classes like java.util.Stack,
            // parent delegation will kick in and load it from bootstrap.
            // To truly test custom loading, we'd need a user-defined class.
            System.out.println("  Class bytes length: " + bytes.length + " bytes");
        }

        // Demonstrate: custom loader for a user class
        // In a real plugin system, you'd read bytes from a jar or network
        System.out.println("  Custom ClassLoader created. Would load user-supplied bytecode.");
        System.out.println("  Key method: defineClass(name, bytes, offset, length)");
        System.out.println("  Use case: hot-reload, plugins, sandboxing, decryption");
    }

    // =========================================================
    // 5. ClassNotFoundException vs NoClassDefFoundError
    // =========================================================
    //
    // ClassNotFoundException (CHECKED EXCEPTION):
    //   WHEN: Explicitly loading a class that doesn't exist on classpath.
    //   HOW: Class.forName("com.missing.Clazz"), ClassLoader.loadClass()
    //   MEANING: Class simply doesn't exist on the classpath.
    //   ACTION: Catch it and handle gracefully (optional dependency, plugin not installed).
    //
    // NoClassDefFoundError (UNCHECKED ERROR):
    //   WHEN: Class was present at COMPILE time but MISSING at runtime.
    //   OR: Class was found but its STATIC INITIALIZER threw an exception.
    //   MEANING: JVM found the class but can't complete loading/linking.
    //   COMMON CAUSE 1: Jar file missing from runtime classpath (present during compile).
    //   COMMON CAUSE 2: Static initializer threw an exception → class in "erroneous state".
    //     All subsequent attempts to use the class throw NoClassDefFoundError.
    //   CAUSE 3: ClassLoader issues — class visible to one loader but not another.
    //   ACTION: Usually a deployment/build configuration error. Check classpath.
    //
    // INTERVIEW GOTCHA: "I deployed but get NoClassDefFoundError even though
    //   ClassNotFoundException never happens." → Check: (1) is the jar in runtime classpath?
    //   (2) Did a static initializer fail? Enable -XX:+PrintClassLoaderStatistics for diagnosis.

    static void demonstrateClassNotFound() {
        System.out.println("\n--- ClassNotFoundException vs NoClassDefFoundError ---");

        // ClassNotFoundException: explicit load of missing class
        try {
            Class.forName("com.nonexistent.SomeClass");
        } catch (ClassNotFoundException e) {
            System.out.println("  ClassNotFoundException caught: " + e.getMessage());
            System.out.println("  → Class simply not on classpath. Handle gracefully.");
        }

        // NoClassDefFoundError demo: simulate via static initializer failure
        System.out.println("  NoClassDefFoundError: class found but static init failed");
        System.out.println("  → Check: missing runtime jar? Failed static initializer?");
        System.out.println("  → Unlike ClassNotFoundException, this is NOT expected and");
        System.out.println("    usually indicates a configuration/deployment error.");
    }

    // =========================================================
    // 6. JAVA MODULE SYSTEM (JPMS — Java 9+)
    // =========================================================
    //
    // CONCEPT: Java Platform Module System adds a module layer on top of classloaders.
    //   Each module declares:
    //   - requires: which modules it depends on
    //   - exports: which packages are visible to other modules
    //   - opens: which packages can be reflected upon
    //
    // KEY DIFFERENCES FROM CLASSPATH:
    //   Classpath: flat, no isolation. Any class can access any other class.
    //   JPMS: strong encapsulation. module-info.java controls visibility.
    //   Improves: security (no access to internal APIs), reliability (explicit deps),
    //             performance (smaller runtime images with jlink).
    //
    // CLASSLOADER IN JPMS:
    //   Each module has its own classloader (actually: Boot, Platform, App layers).
    //   Modules can define custom module layers with custom classloaders.
    //
    // COMMON INTERVIEW Q: "Why does --add-opens work?"
    //   A: It overrides the module's exports/opens declarations at JVM startup.
    //      Used when you need reflection access to internal JDK APIs
    //      (e.g., Spring, Hibernate accessing java.lang.reflect internals).
    //      Production code should use proper APIs, not --add-opens hacks.

    static void demonstrateModuleSystem() {
        System.out.println("\n--- Java Module System (JPMS) ---");

        // Get module info for this class
        Module thisModule = ClassLoadingDemo.class.getModule();
        System.out.println("  This class's module: " + thisModule.getName());
        System.out.println("  Is named module: " + thisModule.isNamed());

        // java.lang.String is in java.base module
        Module javaBase = String.class.getModule();
        System.out.println("  String's module: " + javaBase.getName());

        // Check if a module exports a package
        System.out.println("  java.base exports java.lang: " +
            javaBase.isExported("java.lang"));
        System.out.println("  java.base exports sun.misc: " +
            javaBase.isExported("sun.misc")); // false — internal, not exported
    }

    // =========================================================
    // MAIN
    // =========================================================
    public static void main(String[] args) throws Exception {
        System.out.println("=== JVM CLASS LOADING DEMO ===");

        demonstrateClassLoaderHierarchy();
        demonstrateStaticInit();
        demonstrateForNameVsLoadClass();
        demonstrateCustomClassLoader();
        demonstrateClassNotFound();
        demonstrateModuleSystem();

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: ClassNotFoundException vs NoClassDefFoundError?");
        System.out.println("A: ClassNotFoundException: checked, thrown when explicitly loading");
        System.out.println("   a non-existent class (Class.forName). Expected, handle gracefully.");
        System.out.println("   NoClassDefFoundError: unchecked Error, class existed at compile time");
        System.out.println("   but missing at runtime, or static initializer failed. Config bug.");
        System.out.println();
        System.out.println("Q: Why parent delegation? Couldn't it cause issues?");
        System.out.println("A: Security: prevents replacing java.lang.String with malicious version.");
        System.out.println("   Consistency: same Class object for same class in hierarchy.");
        System.out.println("   Issue: if parent loads a class that needs a class from child's scope,");
        System.out.println("   it fails (classic: JNDI uses App loader, implementation uses Thread context loader).");
        System.out.println("   Fix: Thread.currentThread().getContextClassLoader() for framework code.");
        System.out.println();
        System.out.println("Q: How does hot-reload work (like Spring DevTools)?");
        System.out.println("A: DevTools creates a child classloader for application classes.");
        System.out.println("   On file change: DISCARD the child classloader, create a new one,");
        System.out.println("   reload all application classes. Parent loader (for spring/lib classes)");
        System.out.println("   stays the same → fast reload. Can't hot-reload library changes.");
        System.out.println();
        System.out.println("Q: What triggers class initialization?");
        System.out.println("A: First ACTIVE USE: new Instance(), static field access (non-const),");
        System.out.println("   static method call, Class.forName(), subclass init.");
        System.out.println("   NOT: array creation, compile-time constants, classloader.loadClass().");
    }
}
