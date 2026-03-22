// Run with: --enable-preview --source 21
package com.java.java21;

/**
 * CONCEPT: Scoped Values (Java 21 Preview — JEP 446)
 *
 * CORE IDEA: An immutable, per-scope alternative to ThreadLocal.
 * A ScopedValue<T> is bound to a value for the duration of a specific scope
 * (a Runnable or Callable), then automatically unbound. Child threads created
 * within the scope inherit the binding automatically.
 *
 * WHEN TO USE:
 *   - Request-scoped context: propagate userId, traceId, locale, security
 *     principal through a call chain without passing as method parameters
 *   - Replace ThreadLocal in virtual-thread-heavy code (ThreadLocal has
 *     issues with virtual threads — see GOTCHA section)
 *   - Any "ambient context" that should flow through a scope automatically
 *
 * TIME/SPACE: O(1) read (no HashMap lookup — JVM knows the binding directly)
 *   ThreadLocal uses a per-thread HashMap; ScopedValue lookup is O(1) via
 *   a JVM-maintained binding stack.
 *
 * INTERVIEW ANGLE:
 *   Q: What's wrong with ThreadLocal in Java 21?
 *   A: Three problems:
 *      1. MUTABLE — anyone can call set() at any time, leading to bugs
 *      2. MEMORY LEAK — must call remove() explicitly; forgetting leaks memory
 *         especially in thread pools where threads are reused
 *      3. VIRTUAL THREAD MISMATCH — virtual threads are pinned to carrier
 *         threads during blocking; ThreadLocal is per-carrier-thread, not
 *         per-virtual-thread, causing subtle bugs with thread pools + VT
 *
 *   Q: How does ScopedValue improve on ThreadLocal?
 *   A: 1. IMMUTABLE within scope — once bound, cannot be changed (only re-shadowed)
 *      2. AUTOMATIC CLEANUP — binding is removed when scope exits, no remove() needed
 *      3. VIRTUAL THREAD CORRECT — bound per virtual thread, inherited by children
 *      4. FASTER READ — O(1) direct access vs ThreadLocal's O(1) amortized HashMap
 *
 *   Q: How does ScopedValue propagate to child tasks in StructuredTaskScope?
 *   A: Automatically. When you fork() a task inside a StructuredTaskScope while
 *      a ScopedValue is bound, the child task inherits the binding. This is
 *      "implicit propagation" — no explicit passing needed.
 *
 * REAL-WORLD USE:
 *   - Web framework: bind Request object for duration of request handling
 *   - Security: bind authenticated Principal, readable anywhere in call stack
 *   - Distributed tracing: bind TraceContext, automatically included in all logs
 *   - Localization: bind Locale for i18n without passing to every method
 *
 * GOTCHA:
 *   - ScopedValue is PREVIEW in Java 21. API may change in future releases.
 *   - ScopedValue.get() throws NoSuchElementException if called outside a binding scope!
 *     Always use isBound() check or orElse() for safe access.
 *   - You CANNOT call set() after binding — immutable. Use rebinding (nested where()) instead.
 *   - Rebinding creates a new scope; the outer binding resumes after inner scope exits.
 */

import java.util.concurrent.*;
import java.util.*;

public class ScopedValuesDemo {

    // -------------------------------------------------------------------------
    // SECTION 1: Declare ScopedValues as public static final constants
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: ScopedValues are declared as static final.
     * They're not values themselves — they're KEYS used to look up bound values.
     * Think of them as typed slots that can hold a value for a given scope.
     *
     * Convention: SCREAMING_SNAKE_CASE, typically in a "Context" or constants class.
     *
     * ANALOGY: Like a safe deposit box key. The key is always the same object,
     * but each scope has its own deposit (binding) in that box.
     */

    // These would be ScopedValue<T> in preview-enabled code:
    // public static final ScopedValue<String> USER_ID    = ScopedValue.newInstance();
    // public static final ScopedValue<String> TRACE_ID   = ScopedValue.newInstance();
    // public static final ScopedValue<Locale> LOCALE     = ScopedValue.newInstance();
    // public static final ScopedValue<String> TENANT     = ScopedValue.newInstance();

    // For this demo without preview, we simulate using ThreadLocal to show contrast:
    private static final ThreadLocal<String> TL_USER_ID  = new ThreadLocal<>();
    private static final ThreadLocal<String> TL_TRACE_ID = new ThreadLocal<>();

    // -------------------------------------------------------------------------
    // SECTION 2: Basic Binding and Reading
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: ScopedValue.where(KEY, value).run(action) is the primary API.
     * The binding is active only during the execution of the action lambda.
     * After the lambda completes (or throws), the binding is automatically removed.
     *
     * PREVIEW CODE (what this would look like with --enable-preview):
     *
     *   // Step 1: Bind the value for the scope
     *   ScopedValue.where(USER_ID, "user-123")
     *              .where(TRACE_ID, "trace-abc")   // chain multiple bindings
     *              .run(() -> {
     *                  // Step 2: Any method in the call stack can read it
     *                  processRequest();
     *              });
     *   // Step 3: After run() returns, bindings are gone. No cleanup needed.
     *
     *   private void processRequest() {
     *       String userId = USER_ID.get();       // reads the bound value
     *       String traceId = TRACE_ID.get();     // reads the bound value
     *       log("Processing for " + userId + " trace=" + traceId);
     *   }
     */
    public static void demonstrateBasicBinding() {
        System.out.println("=== Basic Binding Demo ===");

        // ThreadLocal equivalent (for comparison):
        try {
            TL_USER_ID.set("user-123");
            TL_TRACE_ID.set("trace-abc");
            processRequestWithThreadLocal();
        } finally {
            // PROBLEM: Must manually clean up. Forgetting this = memory leak in thread pools!
            TL_USER_ID.remove();
            TL_TRACE_ID.remove();
        }

        /*
         * ScopedValue equivalent (preview):
         *
         * ScopedValue.where(USER_ID, "user-123")
         *            .where(TRACE_ID, "trace-abc")
         *            .run(() -> processRequest());
         * // AUTO-CLEANUP: no remove() needed. Binding gone when run() exits.
         * // IMMUTABLE: nobody inside can call USER_ID.set("hacker") — no such method!
         */

        System.out.println("ThreadLocal binding complete (required manual cleanup)");
        System.out.println("ScopedValue would auto-clean at scope exit");
    }

    // -------------------------------------------------------------------------
    // SECTION 3: Inheritance in StructuredTaskScope
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: When you fork() tasks inside StructuredTaskScope while a
     * ScopedValue is bound, ALL child tasks inherit the binding automatically.
     * This is the killer feature for microservice/request-scoped context propagation.
     *
     * PREVIEW CODE:
     *
     *   // Outer scope binds USER_ID
     *   ScopedValue.where(USER_ID, "user-42").run(() -> {
     *       try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
     *
     *           // These child tasks can read USER_ID without being passed it!
     *           var t1 = scope.fork(() -> {
     *               String id = USER_ID.get(); // "user-42" — inherited!
     *               return fetchOrders(id);
     *           });
     *
     *           var t2 = scope.fork(() -> {
     *               String id = USER_ID.get(); // "user-42" — inherited!
     *               return fetchProfile(id);
     *           });
     *
     *           scope.join().throwIfFailed();
     *           return new Dashboard(t1.get(), t2.get());
     *       }
     *   });
     *
     * CONTRAST with ThreadLocal:
     *   - New threads do NOT inherit ThreadLocal by default (InheritableThreadLocal does,
     *     but it copies the value at thread creation time — not dynamic, not VT-safe)
     *   - ScopedValue inheritance is automatic AND works correctly with virtual threads
     */
    public static void demonstrateInheritanceInStructuredScope() {
        System.out.println("\n=== Inheritance in Structured Scope ===");

        /*
         * Pseudocode for the pattern:
         *
         * HTTP Request arrives → framework does:
         *   ScopedValue.where(USER_ID,  extractUserId(request))
         *              .where(TRACE_ID, extractTraceId(request))
         *              .where(LOCALE,   extractLocale(request))
         *              .run(() -> {
         *                  // Your entire request handling code runs here.
         *                  // Any method, any depth, any forked virtual thread
         *                  // can access USER_ID.get(), TRACE_ID.get(), etc.
         *                  handleRequest(request);
         *              });
         * // When run() exits: all bindings removed, all resources released.
         */

        // Simulating with ThreadLocal + explicit propagation (the pain point SV solves):
        String userId = "user-42";
        String traceId = "trace-xyz";

        // With ThreadLocal in a thread pool, you'd need to:
        // 1. Set before submitting task
        // 2. Propagate to any child threads manually
        // 3. Clean up after task completes
        // This is error-prone, especially with nested parallelism.

        System.out.println("ScopedValue inheritance pattern explained.");
        System.out.println("Child tasks in StructuredTaskScope inherit all ScopedValue bindings.");
        System.out.println("No manual propagation needed. No cleanup needed.");
    }

    // -------------------------------------------------------------------------
    // SECTION 4: Rebinding — shadow outer value in inner scope
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: Rebinding creates a nested scope that shadows the outer binding.
     * When the inner scope exits, the outer binding is RESTORED.
     * This is like a stack frame for values — exactly like lexical scoping in FP.
     *
     * USE CASE: A request handler runs as user "alice", but needs to execute
     * a sub-operation on behalf of user "bob" temporarily (e.g., permission check
     * from bob's perspective), then resume as alice.
     *
     * PREVIEW CODE:
     *
     *   ScopedValue.where(USER_ID, "alice").run(() -> {
     *       System.out.println(USER_ID.get()); // "alice"
     *
     *       // Rebind for inner scope — shadows "alice"
     *       ScopedValue.where(USER_ID, "bob").run(() -> {
     *           System.out.println(USER_ID.get()); // "bob"
     *           checkBobsPermissions();
     *       });
     *
     *       // Outer binding restored automatically
     *       System.out.println(USER_ID.get()); // "alice" again
     *   });
     *
     * IMPORTANT: Rebinding does NOT mutate the ScopedValue.
     * It creates a new binding frame on the scope stack. This is unlike
     * ThreadLocal.set() which overwrites the value (mutable, error-prone).
     */
    public static void demonstrateRebinding() {
        System.out.println("\n=== Rebinding Demo ===");

        /*
         * The stack of bindings (preview pseudocode):
         *
         * ┌─────────────────────────────────────────────────┐
         * │ Scope 1: USER_ID = "alice"                      │
         * │   ┌─────────────────────────────────────────────┤
         * │   │ Scope 2: USER_ID = "bob" (shadows scope 1)  │
         * │   │   USER_ID.get() → "bob"                     │
         * │   └─────────────────────────────────────────────┤
         * │   USER_ID.get() → "alice" (scope 2 exited)      │
         * └─────────────────────────────────────────────────┘
         */

        System.out.println("Rebinding creates new binding frame, outer frame restored on exit.");
        System.out.println("This is functional, immutable scoping — no accidental mutation.");
    }

    // -------------------------------------------------------------------------
    // SECTION 5: Safe Access Patterns
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: Always handle the case where ScopedValue might not be bound.
     *
     * GOTCHA: ScopedValue.get() throws NoSuchElementException if not in a binding scope.
     * This happens if you call a method that uses ScopedValue from outside any binding
     * (e.g., in a unit test, a scheduled job, or main thread without setup).
     *
     * SAFE PATTERNS (preview):
     *   // Option 1: Check isBound() first
     *   if (USER_ID.isBound()) {
     *       log("User: " + USER_ID.get());
     *   }
     *
     *   // Option 2: orElse with default
     *   String userId = USER_ID.orElse("anonymous");
     *
     *   // Option 3: orElseThrow with meaningful message
     *   String userId = USER_ID.orElseThrow(() ->
     *       new IllegalStateException("This method must be called within a request scope"));
     */
    public static String safeGetUserId_Simulated(String defaultValue) {
        // Simulated safe access pattern using ThreadLocal:
        String value = TL_USER_ID.get();
        return value != null ? value : defaultValue;

        /*
         * ScopedValue equivalent (preview):
         * return USER_ID.orElse(defaultValue);
         */
    }

    // -------------------------------------------------------------------------
    // SECTION 6: ScopedValue vs ThreadLocal side-by-side comparison
    // -------------------------------------------------------------------------

    /**
     * COMPREHENSIVE COMPARISON for interview:
     *
     * ┌─────────────────────┬──────────────────────────┬──────────────────────────┐
     * │ Feature             │ ThreadLocal<T>           │ ScopedValue<T>           │
     * ├─────────────────────┼──────────────────────────┼──────────────────────────┤
     * │ Mutability          │ Mutable (set anytime)    │ Immutable within scope   │
     * │ Lifetime            │ Until remove() called    │ Tied to scope (auto)     │
     * │ Memory leak risk    │ High (pool thread reuse) │ None (auto cleanup)      │
     * │ Virtual thread safe │ Issues with pinning      │ Designed for VT          │
     * │ Child thread inherit│ InheritableThreadLocal   │ Automatic in SC          │
     * │ Read performance    │ O(1) HashMap lookup      │ O(1) direct JVM binding  │
     * │ Rebinding           │ set() overwrites         │ Nested scope, auto-restore│
     * │ API complexity      │ get/set/remove           │ where().run()/call()     │
     * │ Preview?            │ No (stable)              │ Yes (Java 21 preview)    │
     * └─────────────────────┴──────────────────────────┴──────────────────────────┘
     *
     * WHEN TO STILL USE ThreadLocal:
     *   - Mutable per-thread state (e.g., SimpleDateFormat caching per thread)
     *   - Accumulating per-thread data across multiple method calls
     *   - Code that doesn't use virtual threads or structured concurrency
     *
     * WHEN TO PREFER ScopedValue:
     *   - Request-scoped context in virtual-thread-based servers
     *   - Any scenario with StructuredTaskScope (automatic child inheritance)
     *   - When immutability is desired (prevents accidental mutation bugs)
     */
    public static void printComparisonTable() {
        System.out.println("\n=== ThreadLocal vs ScopedValue Comparison ===");
        System.out.println("ThreadLocal: mutable, manual cleanup, memory-leak-prone");
        System.out.println("ScopedValue: immutable, auto-cleanup, VT-correct, SC-aware");
    }

    // -------------------------------------------------------------------------
    // SECTION 7: Practical Pattern — Request Context in a Web Framework
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: This is the canonical use case that motivated ScopedValue.
     * Before ScopedValue, frameworks like Spring used ThreadLocal for request context
     * (RequestContextHolder). This pattern breaks with virtual threads.
     *
     * PREVIEW CODE for a web-framework-style request handler:
     *
     *   // In the framework's dispatch method:
     *   public void dispatch(HttpRequest request, HttpResponse response) {
     *       String userId  = authenticator.getUserId(request);
     *       String traceId = tracer.getOrCreateTraceId(request);
     *       Locale locale  = i18n.detectLocale(request);
     *
     *       ScopedValue.where(USER_ID,  userId)
     *                  .where(TRACE_ID, traceId)
     *                  .where(LOCALE,   locale)
     *                  .run(() -> controller.handle(request, response));
     *       // After run(): all context auto-cleaned, no risk of cross-request pollution
     *   }
     *
     *   // In your business logic (deep in the call stack):
     *   public class OrderService {
     *       public Order createOrder(OrderRequest req) {
     *           // No dependency injection of context — just read from scope
     *           String userId  = USER_ID.get();    // injected by framework
     *           String traceId = TRACE_ID.get();   // for distributed tracing
     *           Locale locale  = LOCALE.get();     // for i18n in messages
     *
     *           auditLog("Order created by " + userId + " [" + traceId + "]");
     *           return orderRepo.save(new Order(req, userId));
     *       }
     *   }
     */
    public static void demonstrateWebFrameworkPattern() {
        System.out.println("\n=== Web Framework Pattern Demo ===");
        System.out.println("Framework binds: userId, traceId, locale at request entry");
        System.out.println("Business logic reads bound values anywhere in call stack");
        System.out.println("On request completion: all bindings auto-removed");
        System.out.println("Virtual thread safety: no cross-request pollution");
    }

    // -------------------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------------------

    private static void processRequestWithThreadLocal() {
        // Deep in the call stack — reads from ThreadLocal
        String userId  = TL_USER_ID.get();
        String traceId = TL_TRACE_ID.get();
        System.out.println("Processing request for userId=" + userId + " traceId=" + traceId);
        // Problem: if this code forked a new thread, ThreadLocal would NOT propagate
        // unless InheritableThreadLocal was used AND a new thread was created per request.
    }

    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        System.out.println("=== Scoped Values Demo ===");
        System.out.println("(Note: ScopedValue API requires --enable-preview in Java 21)");
        System.out.println();

        demonstrateBasicBinding();
        demonstrateInheritanceInStructuredScope();
        demonstrateRebinding();

        System.out.println("\n--- Safe Access Pattern ---");
        // Without binding, safe access returns default
        String userId = safeGetUserId_Simulated("anonymous");
        System.out.println("Safe userId (no binding): " + userId); // "anonymous"

        // With binding (ThreadLocal simulation):
        TL_USER_ID.set("user-99");
        userId = safeGetUserId_Simulated("anonymous");
        System.out.println("Safe userId (with binding): " + userId); // "user-99"
        TL_USER_ID.remove();

        demonstrateWebFrameworkPattern();
        printComparisonTable();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("ScopedValue = immutable ThreadLocal with automatic lifetime management");
        System.out.println("Binding API: ScopedValue.where(KEY, val).run(() -> { KEY.get() })");
        System.out.println("Inheritance: child SC tasks automatically inherit parent bindings");
        System.out.println("Rebinding: nested where() creates shadow binding, outer restored on exit");
        System.out.println("Key win: eliminates memory leaks, mutation bugs, and VT-compatibility issues");
    }
}
