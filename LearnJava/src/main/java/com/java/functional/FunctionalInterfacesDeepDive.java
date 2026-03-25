package com.java.functional;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * ============================================================
 * FUNCTIONAL INTERFACES DEEP DIVE — java.util.function Package
 * ============================================================
 *
 * CONCEPT:
 *   A Functional Interface has EXACTLY ONE abstract method (SAM = Single Abstract Method).
 *   @FunctionalInterface annotation is OPTIONAL but recommended — the compiler
 *   enforces the SAM rule if you add it.
 *   A lambda expression = anonymous implementation of a functional interface.
 *
 * THE 8 CORE FUNCTIONAL INTERFACES (java.util.function):
 *   Function<T,R>        → T in, R out           (transform)
 *   Consumer<T>          → T in, void out         (side effect)
 *   Supplier<T>          → nothing in, T out      (produce / lazy value)
 *   Predicate<T>         → T in, boolean out      (test / filter)
 *   BiFunction<T,U,R>    → T+U in, R out          (two-arg transform)
 *   BiConsumer<T,U>      → T+U in, void out       (two-arg side effect)
 *   UnaryOperator<T>     → T in, T out            (special Function<T,T>)
 *   BinaryOperator<T>    → T+T in, T out          (special BiFunction<T,T,T>)
 *
 * INTERVIEW ANGLE:
 *   Interviewers test whether you know: composition (andThen/compose),
 *   lazy evaluation (Supplier vs direct value), method reference types,
 *   and the subtle orElse vs orElseGet difference.
 *
 * REAL-WORLD USE:
 *   Every Stream operation uses these: filter(Predicate), map(Function),
 *   forEach(Consumer), reduce(BinaryOperator), collect(Collector).
 *   Also: Optional.orElseGet(Supplier), Map.forEach(BiConsumer).
 */
public class FunctionalInterfacesDeepDive {

    // Sample data class — using record (Java 16+)
    record User(String name, int age, boolean active) {}

    public static void main(String[] args) {
        System.out.println("=== 1. FUNCTION<T,R> ===");
        demonstrateFunction();

        System.out.println("\n=== 2. CONSUMER<T> & BICONSUMER<T,U> ===");
        demonstrateConsumer();

        System.out.println("\n=== 3. SUPPLIER<T> — Lazy Evaluation ===");
        demonstrateSupplier();

        System.out.println("\n=== 4. PREDICATE<T> — Composition ===");
        demonstratePredicate();

        System.out.println("\n=== 5. BIFUNCTION<T,U,R> & BINARYOPERATOR ===");
        demonstrateBiFunction();

        System.out.println("\n=== 6. UNARYOPERATOR<T> ===");
        demonstrateUnaryOperator();

        System.out.println("\n=== 7. METHOD REFERENCES (4 Types) ===");
        demonstrateMethodReferences();

        System.out.println("\n=== 8. FUNCTION COMPOSITION DEEP DIVE ===");
        demonstrateComposition();

        System.out.println("\n=== 9. FULL PIPELINE — All 8 FIs Together ===");
        demonstrateFullPipeline();

        System.out.println("\n=== 10. INTERVIEW GOTCHAS ===");
        demonstrateGotchas();
    }

    // =========================================================
    // 1. FUNCTION<T, R> — Transform: T → R
    // =========================================================
    //
    // SAM method: R apply(T t)
    // Composition:
    //   andThen(Function after)  → apply THIS first, then after:  g(f(x))
    //   compose(Function before) → apply before first, then THIS: f(g(x))
    //
    // INTERVIEW: andThen vs compose — which is which?
    //   f.andThen(g)  → f first, then g  → g(f(x))  [read left to right]
    //   f.compose(g)  → g first, then f  → f(g(x))  [read right to left — like math]
    //   Mnemonic: andThen = "do f, THEN g". compose = "g feeds INTO f".
    //
    // REAL-WORLD: Spring's Converter<S,T>, Jackson ObjectMapper transformations,
    //             Stream.map(function), CompletableFuture.thenApply(function)

    static void demonstrateFunction() {
        // Basic usage
        Function<String, Integer> strLen = String::length;       // method ref
        Function<String, Integer> parse  = Integer::parseInt;    // static method ref
        System.out.println("strLen('hello') = " + strLen.apply("hello")); // 5
        System.out.println("parse('42') = "    + parse.apply("42"));      // 42

        // andThen: strLen first, then multiply by 2
        // strLen.andThen(double) = double(strLen(s))
        Function<String, Integer> strLenDoubled = strLen.andThen(n -> n * 2);
        System.out.println("strLenDoubled('hello') = " + strLenDoubled.apply("hello")); // 10

        // compose: trim first, THEN strLen (reverse order from andThen)
        // strLen.compose(trim) = strLen(trim(s))
        Function<String, Integer> trimThenLen = strLen.compose(String::trim);
        System.out.println("trimThenLen('  hi  ') = " + trimThenLen.apply("  hi  ")); // 2

        // Practical pipeline: "  42  " → trim → parse → square
        Function<String, Integer> pipeline = parse.compose(String::trim)
                                                   .andThen(n -> n * n);
        System.out.println("pipeline('  5  ') = " + pipeline.apply("  5  ")); // 25

        // Q: What's the difference between Function and UnaryOperator?
        // A: UnaryOperator<T> extends Function<T,T> — input and output are the SAME type.
        //    Use UnaryOperator when T in = T out for clarity.
    }

    // =========================================================
    // 2. CONSUMER<T> — Side Effect: T → void
    // =========================================================
    //
    // SAM method: void accept(T t)
    // Composition: andThen(Consumer after) — chains two consumers sequentially
    //
    // KEY INSIGHT: Consumer is for SIDE EFFECTS ONLY — logging, saving, printing.
    //   It returns nothing, so you CANNOT chain it like Function.
    //
    // BiConsumer<T,U>: same idea but takes two arguments.
    //   Used in: Map.forEach(BiConsumer<K,V>)
    //            Map.replaceAll(BiFunction — actually BiConsumer for just acting)
    //
    // INTERVIEW: When do you use Consumer vs Function?
    //   Consumer: side effects (print, save, log) — no output needed
    //   Function: transformation — output is needed downstream
    //
    // REAL-WORLD: Stream.forEach(consumer), List.forEach(consumer), Map.forEach(BiConsumer)

    static void demonstrateConsumer() {
        Consumer<String> print  = System.out::println;
        Consumer<String> log    = s -> System.out.println("[LOG] " + s);

        // andThen: run print THEN log
        Consumer<String> printAndLog = print.andThen(log);
        printAndLog.accept("Hello Consumer");
        // Output:
        //   Hello Consumer
        //   [LOG] Hello Consumer

        // BiConsumer — two arguments
        BiConsumer<String, Integer> printNameAge = (name, age) ->
            System.out.println(name + " is " + age + " years old");
        printNameAge.accept("Alice", 30);

        // BiConsumer composition
        BiConsumer<String, Integer> logNameAge = (name, age) ->
            System.out.println("[LOG] User created: " + name);
        BiConsumer<String, Integer> saveAndLog = printNameAge.andThen(logNameAge);
        saveAndLog.accept("Bob", 25);

        // Map.forEach uses BiConsumer<K, V>
        Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 87);
        scores.forEach((name, score) ->         // BiConsumer<String, Integer>
            System.out.println(name + ": " + score));

        // GOTCHA: Consumer.andThen always calls BOTH — even if first one throws!
        // If you need short-circuit behavior, use Predicate or manual if-else.
    }

    // =========================================================
    // 3. SUPPLIER<T> — Lazy Produce: () → T
    // =========================================================
    //
    // SAM method: T get()
    // No composition methods (nothing to compose with — no input).
    //
    // KEY INSIGHT: Supplier enables LAZY EVALUATION.
    //   The value is computed ONLY when .get() is called.
    //   Use this when computation is expensive and might not be needed.
    //
    // THE MOST IMPORTANT INTERVIEW QUESTION ABOUT SUPPLIER:
    //   Q: What's the difference between Optional.orElse(T) and Optional.orElseGet(Supplier<T>)?
    //   A: orElse(value)         → value is ALWAYS evaluated, even if Optional is present!
    //      orElseGet(supplier)   → supplier is called ONLY if Optional is empty (lazy)
    //
    //   Example where this matters:
    //     orElse(expensiveDbCall())        → DB call happens ALWAYS
    //     orElseGet(() -> expensiveDbCall()) → DB call only if empty
    //
    // REAL-WORLD: Optional.orElseGet, CompletableFuture.supplyAsync(Supplier),
    //             Hibernate lazy loading, @Lazy beans, ObjectFactory<T>

    static void demonstrateSupplier() {
        // Basic supplier
        Supplier<List<String>> listFactory = ArrayList::new; // new list each time
        List<String> list1 = listFactory.get();
        List<String> list2 = listFactory.get();
        System.out.println("Same instance? " + (list1 == list2)); // false — new each time

        // Lazy evaluation — computation deferred until needed
        Supplier<String> expensive = () -> {
            System.out.println("  [computing expensive value...]");
            return "result";
        };

        Optional<String> present = Optional.of("existing");
        Optional<String> empty   = Optional.empty();

        // orElse: ALWAYS evaluates the argument (eager)
        System.out.println("orElse on present:");
        String r1 = present.orElse(computeExpensive());   // computeExpensive() CALLED even though value exists!
        System.out.println("  result: " + r1);

        // orElseGet: LAZY — only calls supplier if Optional is empty
        System.out.println("orElseGet on present:");
        String r2 = present.orElseGet(expensive);   // expensive.get() NOT called
        System.out.println("  result: " + r2);

        System.out.println("orElseGet on empty:");
        String r3 = empty.orElseGet(expensive);     // expensive.get() IS called
        System.out.println("  result: " + r3);

        // Supplier as ObjectFactory — Spring uses this for prototype beans in singletons
        Supplier<User> userFactory = () -> new User("Guest", 0, false);
        User u1 = userFactory.get(); // new instance
        User u2 = userFactory.get(); // another new instance
        System.out.println("u1 == u2: " + (u1 == u2)); // false

        // Q: "Supplier is like a Callable but with no checked exception?"
        // A: Exactly. Callable<V> can throw Exception; Supplier<T> cannot.
        //    Use Callable for thread-submitted tasks; Supplier for lazy values.
    }

    static String computeExpensive() {
        System.out.println("  [orElse: computing even though not needed!]");
        return "fallback";
    }

    // =========================================================
    // 4. PREDICATE<T> — Test: T → boolean
    // =========================================================
    //
    // SAM method: boolean test(T t)
    // Composition:
    //   and(Predicate other)    → this AND other (short-circuits on false)
    //   or(Predicate other)     → this OR other  (short-circuits on true)
    //   negate()                → NOT this
    //   Predicate.not(pred)     → static NOT (Java 11)
    //
    // KEY INSIGHT: Predicates compose like boolean expressions.
    //   isActive.and(isAdult).or(isAdmin).negate()
    //   = !(( isActive && isAdult ) || isAdmin)
    //
    // INTERVIEW: How do you combine multiple filter conditions?
    //   Stream.filter(p1.and(p2).and(p3))  vs  multiple .filter() calls
    //   Both are equivalent — multiple filters are short-circuiting too.
    //   Predicate composition is cleaner and reusable.
    //
    // REAL-WORLD: Stream.filter, removeIf, Optional.filter

    static void demonstratePredicate() {
        List<User> users = List.of(
            new User("Alice", 30, true),
            new User("Bob",   17, true),
            new User("Carol", 25, false),
            new User("Dave",  35, true)
        );

        // Basic predicates
        Predicate<User> isAdult  = u -> u.age() >= 18;
        Predicate<User> isActive = u -> u.active();
        Predicate<User> isVIP    = u -> u.age() > 30;

        // Composition: active AND adult
        Predicate<User> activeAdult = isActive.and(isAdult);

        // active AND (adult OR VIP)
        Predicate<User> activeAndAdultOrVIP = isActive.and(isAdult.or(isVIP));

        // negate: NOT active
        Predicate<User> inactive = isActive.negate();
        // Java 11: Predicate.not(isActive)  ← same as negate(), useful in method refs
        Predicate<User> inactiveV2 = Predicate.not(isActive);

        System.out.println("Active adults:");
        users.stream().filter(activeAdult).map(User::name).forEach(System.out::println);

        System.out.println("Inactive users:");
        users.stream().filter(inactive).map(User::name).forEach(System.out::println);

        // BiPredicate<T, U>
        BiPredicate<String, Integer> nameLongerThan = (name, len) -> name.length() > len;
        System.out.println("Name > 4 chars: " + nameLongerThan.test("Alice", 4)); // true

        // removeIf uses Predicate
        List<User> mutableList = new ArrayList<>(users);
        mutableList.removeIf(Predicate.not(isActive)); // remove inactive users
        System.out.println("After removeIf inactive: " +
            mutableList.stream().map(User::name).collect(Collectors.joining(", ")));

        // Q: "Can you use method references as Predicates?"
        // A: Yes — if the method signature matches T → boolean
        //    String::isEmpty  →  Predicate<String>
        //    User::active     →  Predicate<User> (if active() returns boolean)
        Predicate<String> isEmpty = String::isEmpty;
        System.out.println("isEmpty(''): " + isEmpty.test("")); // true
    }

    // =========================================================
    // 5. BIFUNCTION<T,U,R> — Two inputs, one output
    // =========================================================
    //
    // SAM method: R apply(T t, U u)
    // Composition: andThen(Function after) — applies after to the result
    //
    // SPECIAL CASES (important for interviews):
    //   BinaryOperator<T>  extends BiFunction<T,T,T>  — both inputs AND output same type
    //   Used in: Stream.reduce(identity, BinaryOperator)
    //            Collections.sort with Comparator (Comparator is a functional interface too!)
    //
    // REAL-WORLD: Map.merge(key, value, BiFunction<oldVal, newVal, mergedVal>)
    //             Stream.reduce(BinaryOperator)

    static void demonstrateBiFunction() {
        // BiFunction: combine name + age into a formatted string
        BiFunction<String, Integer, String> formatUser =
            (name, age) -> name + " (age " + age + ")";
        System.out.println(formatUser.apply("Alice", 30)); // Alice (age 30)

        // andThen on BiFunction
        BiFunction<String, Integer, Integer> nameHashPlusAge =
            ((BiFunction<String, Integer, String>) (n, a) -> n + a)
                .andThen(String::length);
        // Simpler:
        BiFunction<String, Integer, String> greet = (name, age) -> "Hi " + name;
        Function<String, String> shout = String::toUpperCase;
        BiFunction<String, Integer, String> greetLoud = greet.andThen(shout);
        System.out.println(greetLoud.apply("Alice", 30)); // HI ALICE

        // BinaryOperator<T> — same type in and out
        BinaryOperator<Integer> sum = Integer::sum;
        BinaryOperator<String> concat = String::concat;
        System.out.println("sum(3,4) = " + sum.apply(3, 4));       // 7
        System.out.println("concat = " + concat.apply("Hello", "World")); // HelloWorld

        // Stream.reduce uses BinaryOperator
        int total = IntStream.rangeClosed(1, 5).boxed()
            .reduce(0, Integer::sum);          // 0 + 1 + 2 + 3 + 4 + 5
        System.out.println("reduce sum 1-5 = " + total); // 15

        // Map.merge — BiFunction for merging values on key collision
        Map<String, Integer> wordCount = new HashMap<>();
        String[] words = {"apple", "banana", "apple", "cherry", "banana", "apple"};
        for (String w : words) {
            wordCount.merge(w, 1, Integer::sum); // on collision: oldVal + newVal
        }
        System.out.println("wordCount: " + wordCount); // {apple=3, banana=2, cherry=1}
    }

    // =========================================================
    // 6. UNARYOPERATOR<T> — Same type in and out: T → T
    // =========================================================
    //
    // Extends Function<T,T> — specialization when input and output are the same type.
    // SAM method: T apply(T t)   (inherited from Function)
    // Has: andThen, compose (inherited from Function)
    // Static: UnaryOperator.identity() → returns input unchanged (useful as no-op)
    //
    // REAL-WORLD: List.replaceAll(UnaryOperator<E>), String operations

    static void demonstrateUnaryOperator() {
        UnaryOperator<String> trim       = String::trim;
        UnaryOperator<String> toUpper    = String::toUpperCase;
        UnaryOperator<String> addBracket = s -> "[" + s + "]";

        // Chain with andThen (same as Function)
        UnaryOperator<String> normalize = trim.andThen(toUpper)::apply;
        // Note: andThen returns Function<T,R> not UnaryOperator<T> — cast needed if you
        // need a UnaryOperator reference. Usually fine to use as Function<String,String>.

        Function<String, String> pipeline = trim.andThen(toUpper).andThen(addBracket);
        System.out.println(pipeline.apply("  hello  ")); // [HELLO]

        // List.replaceAll uses UnaryOperator<E>
        List<String> names = new ArrayList<>(List.of("  alice  ", "  BOB  ", " carol "));
        names.replaceAll(s -> s.trim().toLowerCase());
        System.out.println(names); // [alice, bob, carol]

        // identity() — useful as a no-op placeholder
        UnaryOperator<String> noOp = UnaryOperator.identity();
        System.out.println(noOp.apply("unchanged")); // unchanged

        // Q: When to use UnaryOperator vs Function<T,T>?
        // A: Semantically identical. UnaryOperator<T> is cleaner and signals
        //    "input and output are the same type" — better for readability.
    }

    // =========================================================
    // 7. METHOD REFERENCES — 4 Types
    // =========================================================
    //
    // A method reference is shorthand for a lambda that just calls a method.
    // Format: ClassName::methodName or instance::methodName
    //
    // TYPE 1: Static method reference — ClassName::staticMethod
    //   Maps to: parameters match the method's static parameters
    //   Lambda equivalent: (args) -> ClassName.staticMethod(args)
    //
    // TYPE 2: Instance method on a specific instance — instance::method
    //   Maps to: instance is captured, method called on it
    //   Lambda equivalent: (args) -> capturedInstance.method(args)
    //
    // TYPE 3: Instance method on an arbitrary instance of a type — ClassName::instanceMethod
    //   Maps to: FIRST parameter becomes `this`, rest are method args
    //   Lambda equivalent: (obj, args) -> obj.method(args)
    //   THIS IS THE TRICKY ONE — the type provides the first argument!
    //
    // TYPE 4: Constructor reference — ClassName::new
    //   Maps to: Supplier (no args) or Function (with args)
    //   Lambda equivalent: () -> new ClassName() or (arg) -> new ClassName(arg)
    //
    // INTERVIEW: "What functional interface does String::toUpperCase map to?"
    //   It's Type 3 (instance method on arbitrary instance).
    //   String::toUpperCase = (String s) -> s.toUpperCase()
    //   → maps to Function<String, String> or UnaryOperator<String>

    static void demonstrateMethodReferences() {
        // TYPE 1: Static method reference
        // Integer::parseInt = (String s) -> Integer.parseInt(s) → Function<String, Integer>
        Function<String, Integer>  parseIntRef  = Integer::parseInt;
        Function<Double, Double>   sqrtRef      = Math::sqrt;
        BinaryOperator<Integer>    maxRef       = Integer::max;
        System.out.println("parseInt('100') = " + parseIntRef.apply("100"));   // 100
        System.out.println("sqrt(16.0) = "     + sqrtRef.apply(16.0));          // 4.0
        System.out.println("max(3,7) = "        + maxRef.apply(3, 7));           // 7

        // TYPE 2: Instance method on a specific (captured) instance
        String prefix = "Hello, ";
        Function<String, String> greet = prefix::concat; // prefix is captured
        // = (name) -> prefix.concat(name)
        System.out.println(greet.apply("Alice")); // Hello, Alice

        List<String> log = new ArrayList<>();
        Consumer<String> addToLog = log::add;  // log instance is captured
        addToLog.accept("event1");
        addToLog.accept("event2");
        System.out.println("log: " + log); // [event1, event2]

        // TYPE 3: Instance method on an arbitrary instance of a type (TRICKY)
        // The FIRST parameter of the functional interface becomes `this`
        // String::toUpperCase = (String s) -> s.toUpperCase()
        Function<String, String>   upperRef  = String::toUpperCase;  // 1 param → this
        Function<String, Integer>  lenRef    = String::length;       // 1 param → this
        BiFunction<String,String,String> concatRef = String::concat; // 2 params → this + arg
        System.out.println(upperRef.apply("hello"));           // HELLO
        System.out.println(lenRef.apply("world"));             // 5
        System.out.println(concatRef.apply("Hello", "World")); // HelloWorld

        // String::compareTo = (String s1, String s2) -> s1.compareTo(s2)
        // → maps to BiFunction<String,String,Integer> or Comparator<String>!
        Comparator<String> alphaOrder = String::compareTo; // Type 3 as Comparator
        List<String> names = new ArrayList<>(List.of("Charlie", "Alice", "Bob"));
        names.sort(alphaOrder);
        System.out.println("sorted: " + names); // [Alice, Bob, Charlie]

        // TYPE 4: Constructor reference
        Supplier<ArrayList<String>>          listSupplier = ArrayList::new; // no-arg ctor
        Function<String, StringBuilder>      sbBuilder    = StringBuilder::new; // 1-arg ctor
        BiFunction<String, Integer, Object>  userMaker =    // 2-arg ctor (would need a class)
            (name, age) -> new User(name, age, true);
        System.out.println(sbBuilder.apply("hello")); // hello

        // INTERVIEW TRAP: Which type is String::toUpperCase?
        // A: Type 3 — instance method on arbitrary instance.
        //    The String object is the FIRST (and only) parameter.
        //    Function<String, String>: apply(String s) → s.toUpperCase()
    }

    // =========================================================
    // 8. FUNCTION COMPOSITION DEEP DIVE
    // =========================================================
    //
    // andThen vs compose — the most common interview confusion:
    //
    //   f.andThen(g)  → g(f(x))  — f first, THEN g   [left to right]
    //   f.compose(g)  → f(g(x))  — g first, then f   [right to left, like math]
    //
    // Mnemonic: andThen = sequential steps (1st this, then that)
    //           compose = mathematical composition f∘g (apply g first, feed to f)
    //
    // Predicate composition:
    //   p1.and(p2)   → p1 && p2  (short-circuit: if p1 false, p2 not evaluated)
    //   p1.or(p2)    → p1 || p2  (short-circuit: if p1 true, p2 not evaluated)
    //   p1.negate()  → !p1
    //
    // Consumer composition:
    //   c1.andThen(c2) → c1 first, then c2 (always both, no short-circuit)
    //
    // REAL-WORLD PATTERN: build a validation/transformation pipeline

    static void demonstrateComposition() {
        // Function pipeline: raw CSV → parsed User → formatted output
        Function<String, String[]>  splitCSV  = line -> line.split(",");
        Function<String[], User>    parseUser = parts ->
            new User(parts[0].trim(), Integer.parseInt(parts[1].trim()), Boolean.parseBoolean(parts[2].trim()));
        Function<User, String>      formatUser = u ->
            String.format("%s (age %d, %s)", u.name(), u.age(), u.active() ? "active" : "inactive");

        // Compose into a single pipeline
        Function<String, String> csvToDisplay = splitCSV.andThen(parseUser).andThen(formatUser);
        System.out.println(csvToDisplay.apply("Alice, 30, true"));  // Alice (age 30, active)
        System.out.println(csvToDisplay.apply("Bob, 17, false"));   // Bob (age 17, inactive)

        // andThen vs compose — side by side
        Function<Integer, Integer> times2  = x -> x * 2;
        Function<Integer, Integer> plus10  = x -> x + 10;

        Function<Integer, Integer> times2ThenPlus10 = times2.andThen(plus10); // (x*2)+10
        Function<Integer, Integer> plus10ThenTimes2 = times2.compose(plus10); // (x+10)*2

        System.out.println("andThen(5): " + times2ThenPlus10.apply(5)); // (5*2)+10 = 20
        System.out.println("compose(5): " + plus10ThenTimes2.apply(5)); // (5+10)*2 = 30

        // Predicate composition for validation
        Predicate<String> notNull    = s -> s != null;
        Predicate<String> notEmpty   = s -> !s.isEmpty();
        Predicate<String> isEmail    = s -> s.contains("@");
        Predicate<String> validEmail = notNull.and(notEmpty).and(isEmail);

        System.out.println("validEmail('a@b.com'): " + validEmail.test("a@b.com")); // true
        System.out.println("validEmail(''): "         + validEmail.test(""));        // false
        System.out.println("validEmail('notEmail'): " + validEmail.test("notEmail")); // false

        // Consumer composition for multi-step side effects
        Consumer<User> saveToDb   = u -> System.out.println("  [DB] Saving: " + u.name());
        Consumer<User> sendEmail  = u -> System.out.println("  [Email] Welcome: " + u.name());
        Consumer<User> auditLog   = u -> System.out.println("  [Audit] Created: " + u.name());

        Consumer<User> onUserCreated = saveToDb.andThen(sendEmail).andThen(auditLog);
        onUserCreated.accept(new User("NewUser", 25, true));
    }

    // =========================================================
    // 9. FULL PIPELINE — All 8 Functional Interfaces Together
    // =========================================================

    static void demonstrateFullPipeline() {
        List<String> rawData = List.of(
            "  Alice,30,true  ",
            "  Bob,17,false  ",
            "  Carol,25,true  ",
            "  Dave,35,false  ",
            "  Eve,28,true  "
        );

        // Supplier<T>: provide initial configuration / defaults
        Supplier<Integer> minAge = () -> 18;

        // UnaryOperator<T>: normalize raw input
        UnaryOperator<String> normalize = String::trim;

        // Function<T,R>: parse CSV → User
        Function<String, User> parseUser = line -> {
            String[] parts = line.split(",");
            return new User(parts[0].trim(), Integer.parseInt(parts[1].trim()), Boolean.parseBoolean(parts[2].trim()));
        };

        // Predicate<T>: filter conditions
        Predicate<User> isAdult  = u -> u.age() >= minAge.get(); // uses Supplier
        Predicate<User> isActive = User::active;

        // BiFunction<T,U,R>: format user with rank
        BiFunction<User, Integer, String> formatWithRank =
            (u, rank) -> rank + ". " + u.name() + " (age " + u.age() + ")";

        // BinaryOperator<T>: combine strings (used in reduce)
        BinaryOperator<String> joinLines = (a, b) -> a + "\n  " + b;

        // Consumer<T>: final action
        Consumer<String> printResult = s -> System.out.println("  " + s);

        // BiConsumer<T,U>: log with extra context
        BiConsumer<User, String> auditLog =
            (u, action) -> System.out.println("  [AUDIT] " + action + ": " + u.name());

        // THE FULL PIPELINE:
        System.out.println("Active adults from raw CSV data:");
        List<User> validUsers = rawData.stream()
            .map(normalize)         // UnaryOperator: trim whitespace
            .map(parseUser)         // Function: CSV → User
            .filter(isAdult)        // Predicate: age >= 18
            .filter(isActive)       // Predicate: must be active
            .collect(Collectors.toList());

        // Format with rank using BiFunction
        for (int i = 0; i < validUsers.size(); i++) {
            String formatted = formatWithRank.apply(validUsers.get(i), i + 1);
            printResult.accept(formatted);         // Consumer: print
            auditLog.accept(validUsers.get(i), "PROCESSED"); // BiConsumer: audit
        }
    }

    // =========================================================
    // 10. INTERVIEW GOTCHAS
    // =========================================================

    static void demonstrateGotchas() {
        // GOTCHA 1: Lambda captures must be effectively final
        // This DOES NOT compile:
        //   int count = 0;
        //   Runnable r = () -> count++;  // ERROR: count must be effectively final
        // WHY: Lambdas capture the VARIABLE REFERENCE, not a copy.
        //      If the variable could change, the behavior would be unpredictable.
        //      Workaround: use int[] count = {0}; or AtomicInteger.
        System.out.println("Gotcha 1: Lambda variables must be effectively final");
        int[] counter = {0};
        Runnable r = () -> counter[0]++;  // array reference is final, contents are not
        r.run(); r.run();
        System.out.println("  counter = " + counter[0]); // 2

        // GOTCHA 2: orElse vs orElseGet
        System.out.println("Gotcha 2: orElse always evaluates, orElseGet is lazy");
        Optional<String> opt = Optional.of("present");
        // orElse:    computeFallback() is called EVEN THOUGH opt has a value!
        // orElseGet: computeFallback() is NOT called when opt has a value
        // Rule: if fallback computation is expensive → always use orElseGet

        // GOTCHA 3: andThen in Consumer always runs both sides (no short-circuit)
        System.out.println("Gotcha 3: Consumer.andThen always runs BOTH sides");
        Consumer<String> c1 = s -> { System.out.println("  c1: " + s); throw new RuntimeException("fail"); };
        Consumer<String> c2 = s -> System.out.println("  c2: " + s);
        // c1.andThen(c2).accept("test"); // c2 would NOT run because c1 throws
        // Unlike Predicate.and() which short-circuits

        // GOTCHA 4: Function.compose vs andThen — order matters
        System.out.println("Gotcha 4: compose vs andThen order");
        Function<Integer, Integer> f = x -> { System.out.println("  f(" + x + ")"); return x * 2; };
        Function<Integer, Integer> g = x -> { System.out.println("  g(" + x + ")"); return x + 10; };
        System.out.println("  f.andThen(g) with 5 → f first then g:");
        f.andThen(g).apply(5);  // f(5)=10, g(10)=20
        System.out.println("  f.compose(g) with 5 → g first then f:");
        f.compose(g).apply(5);  // g(5)=15, f(15)=30

        // GOTCHA 5: Method reference type 3 vs type 2 confusion
        System.out.println("Gotcha 5: String::length is Type 3, not Type 2");
        // String::length → Function<String, Integer>  (the String IS the parameter)
        // someString::length → Supplier<Integer>      (someString is captured)
        Function<String, Integer> t3 = String::length;     // Type 3: arbitrary instance
        String s = "hello";
        Supplier<Integer>         t2 = s::length;          // Type 2: specific instance captured
        System.out.println("  t3('world') = " + t3.apply("world")); // 5
        System.out.println("  t2() = " + t2.get());                 // 5 (always 'hello'.length)
    }
}

/*
 * ============================================================
 * QUICK REFERENCE — FUNCTIONAL INTERFACES CHEAT SHEET
 * ============================================================
 *
 * INTERFACE          SAM METHOD          IN → OUT        COMPOSITION
 * Function<T,R>      apply(T)            T → R           andThen, compose
 * Consumer<T>        accept(T)           T → void        andThen
 * Supplier<T>        get()               () → T          (none)
 * Predicate<T>       test(T)             T → boolean     and, or, negate
 * BiFunction<T,U,R>  apply(T,U)          T,U → R         andThen
 * BiConsumer<T,U>    accept(T,U)         T,U → void      andThen
 * UnaryOperator<T>   apply(T)            T → T           andThen, compose
 * BinaryOperator<T>  apply(T,T)          T,T → T         (none extra)
 *
 * andThen vs compose:
 *   f.andThen(g)  = g(f(x))   ← f first (left to right)
 *   f.compose(g)  = f(g(x))   ← g first (right to left, math style)
 *
 * METHOD REFERENCE TYPES:
 *   Type 1: ClassName::staticMethod       → args match static method params
 *   Type 2: instance::instanceMethod      → instance captured, args passed
 *   Type 3: ClassName::instanceMethod     → first param = `this`, rest = args
 *   Type 4: ClassName::new                → Supplier or Function (constructor)
 *
 * KEY INTERVIEW POINTS:
 *   - orElse(val) is EAGER; orElseGet(supplier) is LAZY → prefer orElseGet for expensive ops
 *   - Lambda captures must be EFFECTIVELY FINAL
 *   - Consumer.andThen does NOT short-circuit (both always run unless exception)
 *   - Predicate.and/or DOES short-circuit (like && and ||)
 *   - String::toUpperCase is Type 3 (maps to Function<String,String>)
 *   - "hello"::toUpperCase is Type 2 (maps to Supplier<String>)
 */
