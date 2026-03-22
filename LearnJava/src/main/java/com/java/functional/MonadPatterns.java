package com.java.functional;

/**
 * CONCEPT: Monad Patterns in Java
 *
 * CORE IDEA: A monad is a design pattern for chaining operations on wrapped values
 * while automatically handling context (null, failure, async, list, etc.).
 *
 * PRAGMATIC DEFINITION (for interviews):
 *   A monad is a wrapper type M<T> with two operations:
 *     1. unit (wrap): T → M<T>          (e.g., Optional.of(x), CompletableFuture.completedFuture(x))
 *     2. flatMap:     M<T> → (T → M<U>) → M<U>  (chain operations, flatten nesting)
 *   Plus map:         M<T> → (T → U) → M<U>    (transform value, keep wrapper)
 *
 * THREE MONAD LAWS (for interview bonus points):
 *   1. Left Identity:   unit(a).flatMap(f) == f(a)
 *      "Wrapping then immediately flatMapping is the same as just calling f(a)"
 *   2. Right Identity:  m.flatMap(unit) == m
 *      "flatMapping with the wrap function gives back the same monad"
 *   3. Associativity:   m.flatMap(f).flatMap(g) == m.flatMap(x -> f(x).flatMap(g))
 *      "Chaining order doesn't matter — grouping of flatMaps is associative"
 *
 * WHEN TO USE:
 *   - Optional: null-safe computation chains (replace null checks)
 *   - Result<T>: error-handling chains (replace try-catch spaghetti)
 *   - CompletableFuture: async computation chains (replace callback hell)
 *   - Stream: data transformation chains (lazy, composable)
 *
 * INTERVIEW ANGLE:
 *   Q: Is Optional a monad?
 *   A: Yes — it has unit (Optional.of), flatMap (Optional.flatMap), map (Optional.map).
 *      And it satisfies the three monad laws.
 *      CAVEAT: Optional.map wraps the result back in Optional.
 *              Optional.flatMap expects the function to return Optional (no double wrapping).
 *
 *   Q: What is the difference between map and flatMap on Optional?
 *   A: map:     Optional<T> → (T → U) → Optional<U>
 *               The function returns U; map wraps it in Optional automatically.
 *      flatMap: Optional<T> → (T → Optional<U>) → Optional<U>
 *               The function itself returns Optional<U>; flatMap doesn't double-wrap.
 *      If you use map with a function that returns Optional<U>, you get Optional<Optional<U>>. BAD.
 *      Use flatMap when the transformation itself might fail (returns Optional).
 *
 *   Q: What is the difference between thenApply and thenCompose on CompletableFuture?
 *   A: thenApply:   CF<T> → (T → U) → CF<U>               [like map]
 *      thenCompose: CF<T> → (T → CF<U>) → CF<U>            [like flatMap]
 *      Same rule: use thenCompose when the transformation returns a CompletableFuture.
 *
 * REAL-WORLD USE:
 *   - Optional chain: parse → validate → lookup → transform (each step might return empty)
 *   - Result chain: validate → persist → notify (each step might fail)
 *   - CF chain: fetchUser → fetchOrders → enrichOrders (async pipeline)
 *
 * GOTCHA:
 *   - Optional.get() without isPresent(): throws NoSuchElementException. NEVER do this.
 *   - Optional.orElse() vs orElseGet(): orElse always evaluates its argument (eager).
 *   - Optional is for RETURN TYPES only. Do NOT use as field type (increases object count).
 *   - Do NOT create Optional<Optional<T>> — use flatMap to avoid nesting.
 *   - CompletableFuture.get() blocks the thread — defeats async purpose. Use join() or thenApply.
 */

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.*;

public class MonadPatterns {

    // =========================================================================
    // SECTION 1: Optional as a Monad — Comprehensive Guide
    // =========================================================================

    /**
     * KEY INSIGHT: Optional is Java's null-safe monad.
     * It eliminates the need for null checks by making "might be absent" explicit.
     *
     * MENTAL MODEL: Optional<T> = box that contains either one value or nothing.
     *   map and flatMap "open the box", apply a function, and "close it" again.
     *   If the box is empty, all operations are skipped → no NullPointerException.
     *
     * OPTIONAL CREATION:
     *   Optional.of(value)           — value MUST be non-null (else NullPointerException)
     *   Optional.ofNullable(value)   — value can be null (returns empty if null)
     *   Optional.empty()             — explicitly absent
     *
     * ANTI-PATTERNS (mention in interview):
     *   1. optional.get() without isPresent()  → NoSuchElementException (worse than NPE!)
     *   2. Optional<T> as field type           → extra object per instance, bad for GC
     *   3. Optional<T> as method parameter     → use method overloading instead
     *   4. Optional.of(null)                   → NullPointerException at construction!
     */
    public static void demonstrateOptionalAsMonad() {
        System.out.println("=== Optional as Monad ===");

        // map: T → U, Optional handles wrapping
        Optional<String> name = Optional.of("  alice  ");
        Optional<String> trimmed = name.map(String::trim);
        Optional<Integer> length = trimmed.map(String::length);
        System.out.println("  map chain: " + length);  // Optional[5]

        // flatMap: T → Optional<U>, no double wrapping
        // Scenario: lookup a user's city
        Optional<String> user   = Optional.of("alice");
        Optional<String> city   = user.flatMap(MonadPatterns::lookupCity);
        Optional<String> weather = city.flatMap(MonadPatterns::lookupWeather);
        System.out.println("  flatMap chain: alice's weather = " + weather);

        // Chained on one line (monad chain):
        Optional<String> result = Optional.of("bob")
            .flatMap(MonadPatterns::lookupCity)
            .flatMap(MonadPatterns::lookupWeather);
        System.out.println("  flatMap chain: bob's weather = " + result);  // empty

        // WRONG: map with Optional-returning function = nested Optional<Optional<T>>
        Optional<Optional<String>> nested = user.map(MonadPatterns::lookupCity); // BAD!
        System.out.println("  map with Optional fn = nested: " + nested.getClass().getSimpleName() + " wrapping Optional");
        // Always use flatMap when the function returns Optional!
    }

    private static Optional<String> lookupCity(String username) {
        Map<String, String> db = Map.of("alice", "New York", "carol", "London");
        return Optional.ofNullable(db.get(username));
    }

    private static Optional<String> lookupWeather(String city) {
        Map<String, String> db = Map.of("New York", "Sunny", "London", "Rainy");
        return Optional.ofNullable(db.get(city));
    }

    /**
     * KEY INSIGHT: orElse vs orElseGet is a CRITICAL performance distinction.
     *
     * orElse(T other):
     *   'other' is ALWAYS evaluated (it's a regular method argument).
     *   Even if Optional has a value, the default expression runs.
     *   Use only for CHEAP defaults (literals, already-computed values).
     *
     * orElseGet(Supplier<T> supplier):
     *   Supplier is called ONLY if Optional is empty. LAZY evaluation.
     *   Use for EXPENSIVE defaults (DB query, file read, object creation).
     *
     * EXAMPLE BUG:
     *   Optional.of("value").orElse(readFromDatabase());  // reads DB even though value present!
     *   Optional.of("value").orElseGet(() -> readFromDatabase());  // DB not touched
     */
    public static void demonstrateOrElseVsOrElseGet() {
        System.out.println("\n=== orElse vs orElseGet (Performance Trap) ===");

        int[] callCount = {0};

        Optional<String> present = Optional.of("actual-value");

        // orElse: ALWAYS runs the argument
        present.orElse("default " + (++callCount[0])); // default evaluated!
        System.out.println("  orElse ran even with present Optional: callCount=" + callCount[0]); // 1

        // orElseGet: LAZY — only runs if Optional is empty
        callCount[0] = 0;
        present.orElseGet(() -> "default " + (++callCount[0])); // NOT called
        System.out.println("  orElseGet SKIPPED with present Optional: callCount=" + callCount[0]); // 0

        // With empty Optional: both run
        callCount[0] = 0;
        Optional<String> empty = Optional.empty();
        String r1 = empty.orElse("default");
        System.out.println("  empty.orElse: " + r1);

        String r2 = empty.orElseGet(() -> "computed-default");
        System.out.println("  empty.orElseGet: " + r2);

        // orElseThrow: throw if empty
        try {
            empty.orElseThrow(() -> new IllegalStateException("Expected a value!"));
        } catch (IllegalStateException e) {
            System.out.println("  orElseThrow: " + e.getMessage());
        }

        // ifPresent vs ifPresentOrElse
        Optional<String> opt = Optional.of("hello");
        opt.ifPresent(v -> System.out.println("  ifPresent: " + v));
        opt.ifPresentOrElse(
            v -> System.out.println("  ifPresentOrElse (present): " + v),
            () -> System.out.println("  ifPresentOrElse (absent)")
        );
        empty.ifPresentOrElse(
            v -> System.out.println("  should not print"),
            () -> System.out.println("  ifPresentOrElse (empty branch)")
        );
    }

    /**
     * KEY INSIGHT: Optional.stream() (Java 9+) bridges Optional and Stream.
     * Lets you use Optional in stream pipelines.
     *
     * USE CASES:
     *   - flatMap a Stream<Optional<T>> to Stream<T> (flatten and drop empties)
     *   - Use Stream operations on Optional (already in a stream context)
     *
     * EXAMPLE:
     *   List<Optional<String>> optionals = List.of(Optional.of("a"), Optional.empty(), Optional.of("c"));
     *   List<String> values = optionals.stream()
     *       .flatMap(Optional::stream)  // Optional.stream() returns Stream of 0 or 1 elements
     *       .toList();
     *   // → ["a", "c"]  (empties dropped!)
     */
    public static void demonstrateOptionalStream() {
        System.out.println("\n=== Optional.stream() — Bridge to Stream API ===");

        List<Optional<String>> mixed = List.of(
            Optional.of("alice"),
            Optional.empty(),
            Optional.of("carol"),
            Optional.empty(),
            Optional.of("eve")
        );

        // Without stream(): have to check each manually
        // With stream(): flatten all to Stream<String>, empties become empty stream → dropped
        List<String> present = mixed.stream()
            .flatMap(Optional::stream)  // KEY: Optional.stream() gives 0 or 1 element stream
            .toList();
        System.out.println("  Flattened (empties dropped): " + present);  // [alice, carol, eve]

        // Combine with other operations:
        List<String> upperPresent = mixed.stream()
            .flatMap(Optional::stream)
            .map(String::toUpperCase)
            .toList();
        System.out.println("  Flattened + uppercase: " + upperPresent);

        // Optional.stream() useful for conditional inclusion in stream:
        String searchTerm = "alice";
        Optional<String> maybeUser = lookupCity(searchTerm);
        // Include city in a list only if present:
        List<String> cities = maybeUser.stream().toList(); // 0 or 1 elements
        System.out.println("  City of alice (as list): " + cities);  // [New York]
    }

    // =========================================================================
    // SECTION 2: Custom Result<T,E> Monad — Sealed Interface
    // =========================================================================

    /**
     * KEY INSIGHT: Result<T> is Optional<T> with the reason for absence.
     *
     * PROBLEM with exceptions for expected failures:
     *   parseAge("abc") could throw NumberFormatException.
     *   If every method throws, you get try-catch chains (spaghetti code).
     *   You can accidentally swallow exceptions (catch(Exception e) { /* ignore */ }).
     *
     * SOLUTION with Result<T>:
     *   parseAge("abc") returns Result.failure(new NumberFormatException(...)).
     *   The caller MUST handle both Success and Failure — compiler enforces it.
     *   Failure chains through map/flatMap automatically — no need for try-catch at each step.
     *
     * MONAD LAWS CHECK:
     *   Left identity:   Result.success(x).flatMap(f) == f(x)      ✓
     *   Right identity:  result.flatMap(Result::success) == result   ✓
     *   Associativity:   result.flatMap(f).flatMap(g) ==
     *                    result.flatMap(x -> f(x).flatMap(g))        ✓
     *
     * SEALED INTERFACE + PERMITS (Java 17+):
     *   sealed ensures ONLY Success<T> and Failure<T> can implement Result<T>.
     *   switch on sealed type → compiler enforces exhaustiveness (no default needed).
     */
    public sealed interface Result<T> permits MonadPatterns.Ok, MonadPatterns.Err {
        boolean isOk();
        T value();                // throws if Err
        String errorMessage();    // throws if Ok

        // map: transform the value if Ok, propagate Err unchanged
        <U> Result<U> map(Function<T, U> fn);

        // flatMap: chain operations that might fail
        <U> Result<U> flatMap(Function<T, Result<U>> fn);

        // recover: provide a fallback value on Err
        T recover(Function<String, T> fallback);

        // fold: collapse both cases into one value (always produces a result)
        <U> U fold(Function<T, U> onOk, Function<String, U> onErr);

        // Factory methods:
        static <T> Result<T> ok(T value) { return new Ok<>(value); }
        static <T> Result<T> err(String message) { return new Err<>(message); }
        static <T> Result<T> tryOf(java.util.concurrent.Callable<T> action) {
            try { return ok(action.call()); }
            catch (Exception e) { return err(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()); }
        }
    }

    public record Ok<T>(T value) implements Result<T> {
        @Override public boolean isOk() { return true; }
        @Override public String errorMessage() { throw new NoSuchElementException("Ok has no error"); }
        @Override public <U> Result<U> map(Function<T, U> fn) { return Result.tryOf(() -> fn.apply(value)); }
        @Override public <U> Result<U> flatMap(Function<T, Result<U>> fn) { return fn.apply(value); }
        @Override public T recover(Function<String, T> fallback) { return value; }
        @Override public <U> U fold(Function<T, U> onOk, Function<String, U> onErr) { return onOk.apply(value); }
        @Override public String toString() { return "Ok(" + value + ")"; }
    }

    public record Err<T>(String errorMessage) implements Result<T> {
        @Override public boolean isOk() { return false; }
        @Override public T value() { throw new NoSuchElementException("Err has no value: " + errorMessage); }
        @SuppressWarnings("unchecked")
        @Override public <U> Result<U> map(Function<T, U> fn) { return (Result<U>) this; }
        @SuppressWarnings("unchecked")
        @Override public <U> Result<U> flatMap(Function<T, Result<U>> fn) { return (Result<U>) this; }
        @Override public T recover(Function<String, T> fallback) { return fallback.apply(errorMessage); }
        @Override public <U> U fold(Function<T, U> onOk, Function<String, U> onErr) { return onErr.apply(errorMessage); }
        @Override public String toString() { return "Err(" + errorMessage + ")"; }
    }

    // Chain of operations where any can fail — no try-catch at each step:
    public static Result<String> processUserInput(String input) {
        return Result.tryOf(() -> input.trim())                    // step 1: trim (can't fail, but consistent)
            .flatMap(s -> s.isEmpty()
                ? Result.err("Input is empty")
                : Result.ok(s))                                     // step 2: validate not empty
            .flatMap(s -> Result.tryOf(() -> Integer.parseInt(s))) // step 3: parse int
            .map(n -> n * 2)                                        // step 4: double it (simple transform)
            .map(n -> "Result: " + n);                             // step 5: to string
    }

    public static void demonstrateResultMonad() {
        System.out.println("\n=== Custom Result<T> Monad ===");

        // Happy path
        Result<String> success = processUserInput("  21  ");
        System.out.println("  '  21  ' → " + success);

        // Empty input fails at step 2
        Result<String> emptyFail = processUserInput("   ");
        System.out.println("  '   ' → " + emptyFail);

        // Non-numeric fails at step 3
        Result<String> parseFail = processUserInput("abc");
        System.out.println("  'abc' → " + parseFail);

        // Failure propagates through map/flatMap (no NullPointerException, no swallowed exception):
        Result<String> afterMoreOps = emptyFail
            .map(s -> s.toUpperCase())         // skipped (Err propagates)
            .flatMap(s -> Result.ok("wrapped")) // skipped (Err propagates)
            .map(s -> s + " !!!");             // skipped (Err propagates)
        System.out.println("  Error propagated: " + afterMoreOps);

        // recover: provide fallback on error
        String recovered = parseFail.recover(err -> "default-value-on-error");
        System.out.println("  Recovered: " + recovered);

        // fold: always produces a value (collapse both branches)
        String folded = success.fold(
            v -> "SUCCESS: " + v,
            e -> "FAILED: " + e
        );
        System.out.println("  Folded success: " + folded);

        String foldedErr = parseFail.fold(
            v -> "SUCCESS: " + v,
            e -> "FAILED: " + e
        );
        System.out.println("  Folded error: " + foldedErr);

        // Pattern match on sealed type (Java 21):
        String matched = switch (success) {
            case Ok<String>  ok  -> "Matched Ok: " + ok.value();
            case Err<String> err -> "Matched Err: " + err.errorMessage();
        };
        System.out.println("  Pattern matched: " + matched);
    }

    // =========================================================================
    // SECTION 3: Stream as a Monad
    // =========================================================================

    /**
     * KEY INSIGHT: Stream<T> is a monad for lazy sequences.
     *
     *   unit:     Stream.of(x)     — wrap one element
     *   map:      stream.map(f)    — transform each element
     *   flatMap:  stream.flatMap(f) — expand each element to a stream, flatten
     *
     * LAZINESS: Stream operations are lazy — no computation until a terminal op.
     * Each intermediate op creates a new "stage" in a lazy pipeline.
     * The terminal op (collect, forEach, sum) triggers the pipeline.
     *
     * WHY STREAM IS A MONAD:
     *   Left identity:  Stream.of(x).flatMap(f) == f(x)  (for single-element streams)
     *   Right identity: stream.flatMap(Stream::of) == stream  (wrapping is identity)
     *   Associativity:  see monad law 3
     *
     * STREAM LAZINESS EXAMPLE:
     *   Stream.iterate(0, n -> n + 1)  // infinite stream — not computed yet
     *         .filter(n -> n % 2 == 0)  // still lazy
     *         .map(n -> n * n)          // still lazy
     *         .limit(5)                 // still lazy
     *         .toList();               // NOW it runs: 0, 4, 16, 36, 64
     *   Only 10 integers generated (0-9 checked, 5 even found), not infinite.
     */
    public static void demonstrateStreamAsMonad() {
        System.out.println("\n=== Stream as Monad ===");

        // flatMap: each element → stream, then flatten
        List<String> sentences = List.of("hello world", "java streams are cool");
        List<String> words = sentences.stream()
            .flatMap(s -> Arrays.stream(s.split(" ")))  // each sentence → word stream
            .toList();
        System.out.println("  Words via flatMap: " + words);

        // Stream monad for number expansion:
        // Each number n → Stream of [n, n*n]
        List<Integer> expanded = Stream.of(1, 2, 3, 4, 5)
            .flatMap(n -> Stream.of(n, n * n))
            .toList();
        System.out.println("  Expanded (n, n²): " + expanded);

        // Stream laziness demo:
        System.out.println("\n  Laziness demo:");
        long count = Stream.iterate(0, n -> n + 1)
            .peek(n -> {}) // invisible — only runs for consumed elements
            .filter(n -> n % 2 == 0)
            .map(n -> n * n)
            .limit(5)
            .peek(n -> System.out.print("  computed: " + n + " "))
            .count();
        System.out.println("\n  Count of elements: " + count);
        // Only 0, 4, 16, 36, 64 are computed — stream stops at limit(5)
    }

    // =========================================================================
    // SECTION 4: CompletableFuture as Async Monad
    // =========================================================================

    /**
     * KEY INSIGHT: CompletableFuture<T> is the async monad.
     *   map    = thenApply(Function<T,U>)         — transform result (sync)
     *   flatMap = thenCompose(Function<T,CF<U>>)   — chain async operations
     *
     * MONAD OPERATIONS:
     *   unit:        CompletableFuture.completedFuture(x)
     *   thenApply:   CF<T> → (T → U) → CF<U>
     *   thenCompose: CF<T> → (T → CF<U>) → CF<U>  (FLATMAP for async)
     *
     * GOTCHA: thenApply vs thenCompose (exactly like map vs flatMap on Optional):
     *   If your transformation function ALREADY returns CF<U>, use thenCompose.
     *   Using thenApply would give CF<CF<U>> — a nested future!
     *
     * ASYNC PIPELINE:
     *   fetchUser(id)         → CF<User>
     *   .thenApply(user → user.getName())   → CF<String>  (sync transform)
     *   .thenCompose(name → fetchOrders(name)) → CF<List<Order>>  (async chain)
     *   .thenApply(orders → orders.size())   → CF<Integer>
     *
     * EXCEPTION HANDLING:
     *   .exceptionally(ex → fallbackValue) — recover from exception
     *   .handle((result, ex) → ...)        — handle both success and failure
     *   .whenComplete((result, ex) → ...)  — side effect, doesn't change result
     *
     * COMBINING:
     *   allOf(cf1, cf2, cf3)  — wait for all (returns CF<Void>)
     *   anyOf(cf1, cf2, cf3)  — wait for first (returns CF<Object>)
     *   thenCombine(cf2, fn)  — combine two CFs when both complete
     */
    public static void demonstrateCompletableFutureAsMonad() {
        System.out.println("\n=== CompletableFuture as Async Monad ===");

        // Async pipeline: fetch user → fetch their orders → count orders
        CompletableFuture<Integer> orderCount =
            fetchUserAsync(42)                           // CF<String>
            .thenApply(user -> "user:" + user)          // thenApply = map (sync transform)
            .thenCompose(u -> fetchOrdersAsync(u))       // thenCompose = flatMap (async chain)
            .thenApply(List::size);                      // thenApply = map (sync transform)

        System.out.println("  Order count: " + orderCount.join());  // blocks for result

        // Exception handling chain:
        CompletableFuture<String> withFallback =
            CompletableFuture.supplyAsync(() -> { throw new RuntimeException("Service down"); })
            .thenApply(r -> "result: " + r)
            .exceptionally(ex -> "fallback: " + ex.getMessage());  // recover from exception
        System.out.println("  With fallback: " + withFallback.join());

        // handle: both success and failure paths
        CompletableFuture<String> handled =
            CompletableFuture.supplyAsync(() -> "success-value")
            .handle((result, ex) -> {
                if (ex != null) return "Error: " + ex.getMessage();
                return "OK: " + result;
            });
        System.out.println("  Handled (success): " + handled.join());

        // Combining two futures (thenCombine = both must succeed)
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> " World");
        CompletableFuture<String> combined = cf1.thenCombine(cf2, String::concat);
        System.out.println("  Combined: " + combined.join());

        // allOf: wait for all
        CompletableFuture<Void> all = CompletableFuture.allOf(cf1, cf2);
        all.join(); // waits
        System.out.println("  allOf completed. cf1=" + cf1.join() + ", cf2=" + cf2.join());

        // DIFFERENCE MAP vs FLATMAP:
        // WRONG: thenApply with CF-returning function = CF<CF<Integer>>
        CompletableFuture<CompletableFuture<List<String>>> nested =
            fetchUserAsync(1).thenApply(u -> fetchOrdersAsync(u)); // returns CF<CF<...>>!
        System.out.println("  Nested CF class: " + nested.getClass().getSimpleName()); // still CF

        // CORRECT: thenCompose flattens
        CompletableFuture<List<String>> flat =
            fetchUserAsync(1).thenCompose(u -> fetchOrdersAsync(u)); // CF<List<String>>
        System.out.println("  Flat CF orders: " + flat.join().size());
    }

    // Simulated async service calls
    private static CompletableFuture<String> fetchUserAsync(int id) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "user" + id;
        });
    }

    private static CompletableFuture<List<String>> fetchOrdersAsync(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return List.of("order1", "order2", "order3");
        });
    }

    // =========================================================================
    // SECTION 5: Monad Laws Verification
    // =========================================================================

    /**
     * KEY INSIGHT: Verifying the three monad laws proves that Optional and Result
     * are "correct" monads. This is the theoretical backing for interview discussions.
     *
     * WHY LAWS MATTER:
     *   If a type follows monad laws, you can reason about flatMap chains without
     *   worrying about evaluation order or unexpected behavior.
     *   Laws guarantee: refactoring doesn't change semantics.
     */
    public static void verifyMonadLaws() {
        System.out.println("\n=== Monad Laws Verification (Optional) ===");

        Function<Integer, Optional<Integer>> f = n -> n > 0 ? Optional.of(n * 2) : Optional.empty();
        Function<Integer, Optional<Integer>> g = n -> n < 100 ? Optional.of(n + 1) : Optional.empty();

        int a = 5;
        Optional<Integer> m = Optional.of(10);

        // Law 1: Left Identity: unit(a).flatMap(f) == f(a)
        boolean law1 = Optional.of(a).flatMap(f).equals(f.apply(a));
        System.out.println("  Law 1 (Left Identity):  " + law1);  // true

        // Law 2: Right Identity: m.flatMap(unit) == m
        boolean law2 = m.flatMap(Optional::of).equals(m);
        System.out.println("  Law 2 (Right Identity): " + law2);  // true

        // Law 3: Associativity: m.flatMap(f).flatMap(g) == m.flatMap(x -> f(x).flatMap(g))
        Optional<Integer> left  = m.flatMap(f).flatMap(g);
        Optional<Integer> right = m.flatMap(x -> f.apply(x).flatMap(g));
        boolean law3 = left.equals(right);
        System.out.println("  Law 3 (Associativity):  " + law3);  // true

        System.out.println("  All monad laws satisfied for Optional ✓");
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Monad Patterns Demo ===\n");

        demonstrateOptionalAsMonad();
        demonstrateOrElseVsOrElseGet();
        demonstrateOptionalStream();
        demonstrateResultMonad();
        demonstrateStreamAsMonad();
        demonstrateCompletableFutureAsMonad();
        verifyMonadLaws();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("Monad = wrapper with unit (wrap), map (transform), flatMap (chain + flatten)");
        System.out.println("3 Monad Laws: Left Identity, Right Identity, Associativity");
        System.out.println("Optional.map: fn returns U → Optional wraps it");
        System.out.println("Optional.flatMap: fn returns Optional<U> → no double wrapping");
        System.out.println("CRITICAL: orElse is EAGER, orElseGet is LAZY — use orElseGet for expensive defaults");
        System.out.println("Optional.stream(): bridge to Stream API — flatMap drops empties");
        System.out.println("Result<T>: sealed Success|Failure — no swallowed exceptions, compiler-forced handling");
        System.out.println("CF.thenApply = map (sync transform); CF.thenCompose = flatMap (async chain)");
        System.out.println("ANTI-PATTERNS: Optional.get() without check, Optional as field, Optional<Optional<T>>");
    }
}
