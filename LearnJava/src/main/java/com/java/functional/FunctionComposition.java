package com.java.functional;

/**
 * CONCEPT: Function Composition and Functional Programming in Java
 *
 * CORE IDEA: Functions are first-class values in Java (via functional interfaces).
 * You can compose, chain, curry, and partially apply functions just like data.
 * This enables building complex behavior from simple, reusable building blocks.
 *
 * WHEN TO USE:
 *   - Pipeline processing: input → parse → validate → transform → output
 *   - Decorating behavior: logging, auditing, retry wrapping a function
 *   - Lazy evaluation: defer expensive computation until actually needed
 *   - Reusable predicates: build complex filter conditions from simple ones
 *   - Currying: create specialized functions from general ones by partial application
 *
 * TIME/SPACE: Depends on composed functions. The composition wrapper itself is O(1).
 *
 * INTERVIEW ANGLE:
 *   Q: What is a pure function?
 *   A: Two properties:
 *      1. Same input ALWAYS produces same output (deterministic)
 *      2. No side effects (no I/O, no mutation of external state, no exceptions)
 *      WHY: pure functions are testable (no mocking needed), memoizable
 *      (cache results), thread-safe (no shared state), and composable.
 *
 *   Q: What is the difference between andThen and compose?
 *   A: f.andThen(g) = g(f(x)) — apply f first, then g. Left-to-right pipeline.
 *      f.compose(g) = f(g(x)) — apply g first, then f. Mathematical f∘g notation.
 *      MNEMONIC: andThen = "then" suggests after (pipeline order).
 *                compose = "compose" suggests mathematical composition (right-to-left).
 *
 *   Q: What are the 4 types of method references?
 *   A: 1. Static:             Math::abs
 *      2. Instance (specific): "hello"::toUpperCase
 *      3. Instance (arbitrary): String::toUpperCase  (first arg is the receiver)
 *      4. Constructor:         ArrayList::new
 *
 *   Q: What is currying?
 *   A: Transforming a function f(a, b) → c into f(a) → (g: b → c).
 *      You apply arguments one at a time, getting back a function for the rest.
 *      Enables partial application: fix one argument, get a specialized function.
 *
 * REAL-WORLD USE:
 *   - Spring's filter chains, interceptors
 *   - Builder pattern for validation pipelines
 *   - Middleware in web frameworks (Vert.x, Micronaut handlers)
 *   - Data transformation pipelines (ETL)
 *
 * GOTCHA:
 *   - Function.andThen/compose: only for Function<T,R>. UnaryOperator is a Function<T,T>.
 *   - Predicate.and() vs &&: Predicate.and() short-circuits; but unlike &&, both sides
 *     are Predicate objects (each may have side effects — which you're avoiding in FP!).
 *   - orElse vs orElseGet in Optional: orElse always evaluates its argument!
 *     Even if Optional has a value. Use orElseGet(() -> ...) for lazy evaluation.
 *   - Currying: Java doesn't have syntax sugar for currying. Must write nested lambdas.
 */

import java.util.*;
import java.util.function.*;

public class FunctionComposition {

    // =========================================================================
    // SECTION 1: andThen vs compose
    // =========================================================================

    /**
     * KEY INSIGHT: The naming is about DATA FLOW DIRECTION.
     *
     *   f.andThen(g): data flows left-to-right → f first, then g
     *     result = g(f(x))
     *     Think: "apply f, AND THEN apply g"
     *     Reads naturally: step1.andThen(step2).andThen(step3)
     *
     *   f.compose(g): data flows right-to-left → g first, then f
     *     result = f(g(x))
     *     Think: mathematical composition f∘g (outer ∘ inner)
     *     Less intuitive for pipelines; more useful in math/FP contexts.
     *
     * MENTAL MODEL (diagram):
     *   andThen: x → [f] → [g] → result    (pipeline: left to right)
     *   compose: x → [g] → [f] → result    (pipeline: right to left)
     *
     * REAL-WORLD: andThen is almost always what you want for data pipelines.
     *   compose is used in Comparator (thenComparing is compose-like).
     */
    public static void demonstrateAndThenVsCompose() {
        System.out.println("=== andThen vs compose ===");

        Function<Integer, Integer> doubler     = x -> x * 2;
        Function<Integer, Integer> addTen      = x -> x + 10;
        Function<Integer, String>  toStr       = Object::toString;

        // andThen: doubler first, then addTen
        Function<Integer, Integer> doubleAndAddTen = doubler.andThen(addTen);
        System.out.println("  andThen: double then add10 on 5 → " + doubleAndAddTen.apply(5));
        // 5*2=10, 10+10=20 → 20

        // compose: addTen first, then doubler
        Function<Integer, Integer> addTenThenDouble = doubler.compose(addTen);
        System.out.println("  compose: (add10 then double) on 5 → " + addTenThenDouble.apply(5));
        // 5+10=15, 15*2=30 → 30

        // Pipeline: chain multiple andThens (reads naturally left-to-right)
        Function<String, String> pipeline = ((Function<String, String>) String::trim)
            .andThen(String::toLowerCase)
            .andThen(s -> s.replace(" ", "_"));
        System.out.println("  Pipeline '  Hello World  ' → " + pipeline.apply("  Hello World  "));
        // → "hello_world"

        // Type-changing composition: Integer → Integer → String
        Function<Integer, String> intToHex = doubler.andThen(toStr);
        System.out.println("  double(5) then toString: " + intToHex.apply(5));  // "10"
    }

    // =========================================================================
    // SECTION 2: Predicate Composition
    // =========================================================================

    /**
     * KEY INSIGHT: Predicate.and(), or(), negate() let you build complex conditions
     * from simple, testable predicates — each piece individually testable.
     *
     * COMPARISON:
     *   // Without Predicate composition (hard to reuse, test, name):
     *   list.stream().filter(s -> s.length() > 3 && !s.startsWith("X") && s.contains("a"))
     *
     *   // With Predicate composition (each part named, testable, reusable):
     *   Predicate<String> longEnough  = s -> s.length() > 3;
     *   Predicate<String> notStartsX  = Predicate.not(s -> s.startsWith("X"));
     *   Predicate<String> containsA   = s -> s.contains("a");
     *   list.stream().filter(longEnough.and(notStartsX).and(containsA))
     *
     * Predicate.not() (Java 11+): static factory for negate, works as method reference:
     *   list.stream().filter(Predicate.not(String::isEmpty))  // not empty strings
     *
     * SHORT-CIRCUIT: Predicate.and() short-circuits like &&. If the first predicate
     *   is false, the second is NOT evaluated.
     */
    public static void demonstratePredicateComposition() {
        System.out.println("\n=== Predicate Composition ===");

        Predicate<String> longEnough  = s -> s.length() > 3;
        Predicate<String> startsWithA = s -> s.startsWith("A");
        Predicate<String> notEmpty    = Predicate.not(String::isEmpty);

        List<String> words = List.of("Alice", "Bob", "Ant", "Alligator", "Ape", "");

        // and: both conditions
        List<String> longAndStartsA = words.stream()
            .filter(longEnough.and(startsWithA))
            .toList();
        System.out.println("  Long AND starts with A: " + longAndStartsA);  // [Alice, Alligator]

        // or: either condition
        List<String> longOrStartsA = words.stream()
            .filter(longEnough.or(startsWithA))
            .toList();
        System.out.println("  Long OR starts with A: " + longOrStartsA);  // [Alice, Ant, Alligator, Ape]

        // negate: opposite
        List<String> notLong = words.stream()
            .filter(longEnough.negate().and(notEmpty))
            .toList();
        System.out.println("  NOT long AND not empty: " + notLong);  // [Bob, Ant, Ape]

        // Predicate.not as method reference (cleaner syntax)
        List<String> nonEmpty = words.stream()
            .filter(Predicate.not(String::isEmpty))
            .toList();
        System.out.println("  Non-empty: " + nonEmpty);
    }

    // =========================================================================
    // SECTION 3: Currying and Partial Application
    // =========================================================================

    /**
     * KEY INSIGHT: Currying transforms a multi-argument function into a chain
     * of single-argument functions. This enables PARTIAL APPLICATION.
     *
     * CURRYING:
     *   f(a, b) → c  becomes  curry(f)(a)(b) → c
     *   In Java types: BiFunction<A,B,C> becomes Function<A, Function<B,C>>
     *
     * PARTIAL APPLICATION:
     *   Fix one argument, get back a function waiting for the rest.
     *   add(a, b) → a+b  becomes  add5 = partial(add, 5) → b → 5+b
     *
     * USE CASES:
     *   - Configurators: buildRequest(method)(url)(headers)
     *   - Validators: validate(rule)(input) — fix rule, apply to many inputs
     *   - Mappers: String.format("pattern") → partial on format string
     *
     * INTERVIEW: "Does Java support currying natively?"
     *   A: No, Java has no syntax sugar for currying (unlike Haskell where all functions
     *      are curried by default). You must manually write nested lambdas.
     *      But the type system supports it: Function<A, Function<B, C>>.
     */

    // Curry a BiFunction: (a,b) → c becomes a → (b → c)
    public static <A, B, C> Function<A, Function<B, C>> curry(BiFunction<A, B, C> f) {
        return a -> b -> f.apply(a, b);
    }

    // Partial application: fix first argument of a BiFunction
    public static <A, B, C> Function<B, C> partial(BiFunction<A, B, C> f, A fixedA) {
        return b -> f.apply(fixedA, b);
    }

    public static void demonstrateCurrying() {
        System.out.println("\n=== Currying and Partial Application ===");

        // BiFunction: (a, b) → a + b
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;

        // Curry it
        Function<Integer, Function<Integer, Integer>> curriedAdd = curry(add);
        System.out.println("  curriedAdd(3)(4) = " + curriedAdd.apply(3).apply(4));  // 7

        // Partial application: fix first argument
        Function<Integer, Integer> add5 = partial(add, 5);  // add5 = n → 5 + n
        System.out.println("  add5(10) = " + add5.apply(10));  // 15
        System.out.println("  add5(20) = " + add5.apply(20));  // 25

        // Practical: generate greeting messages
        BiFunction<String, String, String> greet = (greeting, name) -> greeting + ", " + name + "!";
        Function<String, String> hello = partial(greet, "Hello");
        Function<String, String> goodMorning = partial(greet, "Good morning");

        List<String> names = List.of("Alice", "Bob", "Carol");
        names.forEach(name -> System.out.println("  " + hello.apply(name)));
        System.out.println("  " + goodMorning.apply("Team"));

        // Curried string builder: method → url → body → request
        Function<String, Function<String, String>> buildRequest =
            method -> url -> method + " " + url;
        Function<String, String> getRequest = buildRequest.apply("GET");
        System.out.println("  " + getRequest.apply("https://api.example.com/users"));
        System.out.println("  " + buildRequest.apply("POST").apply("https://api.example.com/users"));
    }

    // =========================================================================
    // SECTION 4: Function Composition Pipeline
    // =========================================================================

    /**
     * KEY INSIGHT: Compose an entire processing pipeline from pure functions.
     * Each step is a Function<T, T> (or with type change: Function<A, B>).
     * The pipeline is itself a Function — testable, reusable, composable.
     *
     * REAL-WORLD: ETL pipeline
     *   extract(rawInput) → parse(String) → validate(ParsedDTO) → transform(ValidDTO) → persist(FinalDTO)
     *
     * ADVANTAGE over imperative pipeline:
     *   - Each step individually testable (unit test each Function)
     *   - Easy to add/remove/reorder steps
     *   - The entire pipeline is a value — store, pass, clone it
     */
    public static void demonstratePipeline() {
        System.out.println("\n=== Function Composition Pipeline ===");

        // Step functions — each pure and individually testable
        Function<String, String> trim      = String::trim;
        Function<String, String> lowercase = String::toLowerCase;
        Function<String, String> removeSpaces = s -> s.replace(" ", "-");
        Function<String, String> addPrefix  = s -> "slug-" + s;

        // Compose pipeline: trim → lowercase → replace spaces → add prefix
        Function<String, String> slugify = trim
            .andThen(lowercase)
            .andThen(removeSpaces)
            .andThen(addPrefix);

        List<String> titles = List.of("  Hello World  ", "Java 21 Features", "UPPER CASE TITLE");
        System.out.println("  Slug pipeline results:");
        titles.forEach(t -> System.out.println("    '" + t + "' → '" + slugify.apply(t) + "'"));

        // Pipeline with type changes
        Function<String, Integer> parseAndDouble =
            ((Function<String, String>) String::trim)
            .andThen(Integer::parseInt)     // String → Integer
            .andThen(n -> n * 2);           // Integer → Integer

        System.out.println("  parseAndDouble('  21  ') = " + parseAndDouble.apply("  21  "));  // 42
    }

    // =========================================================================
    // SECTION 5: Method References — 4 Types
    // =========================================================================

    /**
     * KEY INSIGHT: Method references are syntactic sugar for lambdas.
     * They're more readable when the lambda just calls one method.
     *
     * 4 TYPES:
     *   1. Static:                ClassName::staticMethod
     *      lambda: (args) → ClassName.staticMethod(args)
     *      example: Math::abs, Integer::parseInt, String::valueOf
     *
     *   2. Instance (specific):   instance::instanceMethod
     *      lambda: (args) → instance.instanceMethod(args)
     *      example: "hello"::toUpperCase, System.out::println
     *      NOTE: 'instance' is captured from enclosing scope — like a closure
     *
     *   3. Instance (arbitrary):  ClassName::instanceMethod
     *      lambda: (obj, args) → obj.instanceMethod(args)  [obj is first parameter!]
     *      example: String::toLowerCase (takes a String as receiver)
     *      Used for: Comparator.comparing(String::length)
     *
     *   4. Constructor:          ClassName::new
     *      lambda: (args) → new ClassName(args)
     *      example: ArrayList::new, Person::new
     *
     * GOTCHA: Type 2 vs Type 3 can be confusing:
     *   "hello"::contains  → instance ref (captures "hello", takes CharSequence)
     *   String::contains   → arbitrary instance ref (takes String + CharSequence)
     *   The function signature tells you which it is: arity differs by 1.
     */
    public static void demonstrateMethodReferences() {
        System.out.println("\n=== 4 Types of Method References ===");

        // Type 1: Static method reference
        Function<Integer, Integer> abs = Math::abs;  // x → Math.abs(x)
        Function<String, Integer>  parseInt = Integer::parseInt;  // s → Integer.parseInt(s)
        System.out.println("  Static Math::abs: " + abs.apply(-42));      // 42
        System.out.println("  Static Integer::parseInt: " + parseInt.apply("99"));  // 99

        // Type 2: Instance on particular object (captures the object)
        String prefix = "Hello-";
        Function<String, String> addPrefix = prefix::concat;  // s → prefix.concat(s)
        System.out.println("  Instance (specific) concat: " + addPrefix.apply("World"));  // "Hello-World"
        Consumer<String> print = System.out::println;  // s → System.out.println(s)
        print.accept("  Printed via method ref");

        // Type 3: Instance on arbitrary object (first arg IS the receiver)
        Function<String, String> toLower = String::toLowerCase;    // s → s.toLowerCase()
        Function<String, Integer> length = String::length;         // s → s.length()
        Comparator<String> byLength = Comparator.comparing(String::length); // uses arbitrary instance ref
        List<String> words = new ArrayList<>(List.of("banana", "apple", "cherry", "date"));
        words.sort(byLength);
        System.out.println("  Arbitrary instance String::toLowerCase: " + toLower.apply("JAVA"));
        System.out.println("  Sorted by length: " + words);

        // Type 4: Constructor reference
        Supplier<List<String>> listFactory = ArrayList::new;    // () → new ArrayList<>()
        Function<Integer, int[]> arrFactory = int[]::new;       // n → new int[n]
        List<String> newList = listFactory.get();
        newList.add("created via constructor ref");
        System.out.println("  Constructor ref: " + newList);
        System.out.println("  Array constructor ref length: " + arrFactory.apply(5).length);
    }

    // =========================================================================
    // SECTION 6: Lazy Evaluation with Supplier<T>
    // =========================================================================

    /**
     * KEY INSIGHT: Supplier<T> represents a deferred computation.
     * The computation is NOT run until .get() is called.
     *
     * WHY LAZY EVALUATION:
     *   1. Performance: skip expensive computation if result not needed
     *   2. Side effects: avoid I/O until you're sure you need the result
     *   3. Initialization: delay object creation until first use (lazy singleton)
     *   4. Optional.orElseGet: avoid computing default value if Optional has a value
     *
     * GOTCHA: orElse vs orElseGet
     *   Optional.orElse(expensiveCall())         → ALWAYS evaluates expensiveCall()!
     *   Optional.orElseGet(() -> expensiveCall()) → ONLY evaluates if Optional is empty
     *   Always prefer orElseGet for expensive defaults.
     */
    public static void demonstrateLazyEvaluation() {
        System.out.println("\n=== Lazy Evaluation with Supplier ===");

        // Supplier defers computation
        Supplier<List<Integer>> expensive = () -> {
            System.out.println("  [expensive computation running!]");
            return List.of(1, 2, 3, 4, 5);
        };

        System.out.println("  Supplier created — no computation yet");
        List<Integer> result = expensive.get();  // NOW it runs
        System.out.println("  After .get(): " + result);

        // Practical: orElse vs orElseGet
        Optional<String> present = Optional.of("value");
        Optional<String> empty   = Optional.empty();

        // orElse ALWAYS evaluates its argument (even if Optional has value)
        System.out.println("\n  orElse vs orElseGet:");
        String[] computeLog = {""};
        Supplier<String> computeDefault = () -> {
            computeLog[0] += "computed ";
            return "default";
        };

        present.orElse(computeDefault.get());   // ALWAYS calls computeDefault.get()!
        System.out.println("  present.orElse ran? " + !computeLog[0].isEmpty()); // true — BAD
        computeLog[0] = "";
        present.orElseGet(computeDefault);      // SKIPS computeDefault (present has value)
        System.out.println("  present.orElseGet ran? " + !computeLog[0].isEmpty()); // false — GOOD

        // Lazy logger (common pattern in logging frameworks):
        boolean debugEnabled = false;
        // BAD: always computes the message even if debug is disabled
        // if (debugEnabled) log("debug: " + expensiveToString());

        // GOOD: only compute message if needed
        Supplier<String> lazyMsg = () -> "debug: " + List.of(1,2,3).stream().mapToInt(i->i).sum();
        if (debugEnabled) System.out.println(lazyMsg.get()); // never runs (debugEnabled = false)
        System.out.println("  Lazy log message not computed (debug disabled)");
    }

    // =========================================================================
    // SECTION 7: BiFunction, UnaryOperator, BinaryOperator
    // =========================================================================

    /**
     * KEY INSIGHT: These are specializations of Function<T,R> for common cases.
     *
     *   BiFunction<A,B,C>:     apply(A a, B b) → C      (two inputs, one output)
     *   UnaryOperator<T>:      apply(T t) → T            (Function<T,T>; same type in/out)
     *   BinaryOperator<T>:     apply(T t1, T t2) → T     (BiFunction<T,T,T>; same types)
     *
     * WHEN TO USE:
     *   UnaryOperator<T>: transformations that don't change type (uppercase, trim, negate)
     *   BinaryOperator<T>: reductions (sum, max, concat, merge)
     *   These are used extensively in Stream.reduce(), Stream.map(), Collection.replaceAll().
     */
    public static void demonstrateFunctionalInterfaces() {
        System.out.println("\n=== BiFunction, UnaryOperator, BinaryOperator ===");

        // UnaryOperator<T>: T → T (same type)
        UnaryOperator<String> trim = String::trim;
        UnaryOperator<Integer> doubler = n -> n * 2;

        List<String> strings = new ArrayList<>(List.of("  hello  ", "  world  "));
        strings.replaceAll(trim);  // replaceAll takes UnaryOperator<E>
        System.out.println("  replaceAll(trim): " + strings);

        // BinaryOperator<T>: (T, T) → T (same types in and out)
        BinaryOperator<Integer> sum    = Integer::sum;
        BinaryOperator<String>  concat = (a, b) -> a + b;

        // Used in Stream.reduce():
        List<Integer> nums = List.of(1, 2, 3, 4, 5);
        int total = nums.stream().reduce(0, sum);  // BinaryOperator
        System.out.println("  reduce sum: " + total);  // 15

        // BiFunction: (A, B) → C
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        System.out.println("  BiFunction repeat('abc', 3): " + repeat.apply("abc", 3));  // abcabcabc

        // Consumer.andThen: chain side effects
        Consumer<String> log   = msg -> System.out.println("  [LOG] " + msg);
        Consumer<String> audit = msg -> System.out.println("  [AUDIT] " + msg);
        Consumer<String> pipeline = log.andThen(audit);
        pipeline.accept("User created");  // both run in order
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Function Composition Demo ===\n");

        demonstrateAndThenVsCompose();
        demonstratePredicateComposition();
        demonstrateCurrying();
        demonstratePipeline();
        demonstrateMethodReferences();
        demonstrateLazyEvaluation();
        demonstrateFunctionalInterfaces();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("andThen: f then g  (g∘f, pipeline order)");
        System.out.println("compose: g then f  (f∘g, math order)");
        System.out.println("Pure function: same input → same output, no side effects");
        System.out.println("Currying: f(a,b) → c becomes f(a) → (b → c) via Function<A,Function<B,C>>");
        System.out.println("Partial: fix one arg of BiFunction, get Function for remaining args");
        System.out.println("4 method ref types: static, instance-specific, instance-arbitrary, constructor");
        System.out.println("Supplier: lazy evaluation — defer computation until .get()");
        System.out.println("CRITICAL: orElse always evaluates; orElseGet is lazy (prefer for expensive defaults)");
    }
}
