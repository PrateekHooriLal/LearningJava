package com.java.spring;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

/**
 * ============================================================
 * HOW SPRING RECOGNIZES AND RESOLVES DEPENDENCIES
 * ============================================================
 *
 * CONCEPT:
 *   When an interviewer asks "how does Spring recognize dependencies?" they want
 *   to know the MECHANICS — not just "@Autowired does it".
 *
 *   The answer spans 3 phases:
 *
 *   PHASE 1 — DISCOVERY: How Spring finds beans in the first place
 *     ClassPathBeanDefinitionScanner scans the classpath for @Component classes.
 *     @Bean methods in @Configuration classes are also registered.
 *     Each found class becomes a BeanDefinition (metadata only, no instance yet).
 *
 *   PHASE 2 — DEPENDENCY IDENTIFICATION: How Spring knows WHAT each bean needs
 *     AutowiredAnnotationBeanPostProcessor.postProcessMergedBeanDefinition() runs
 *     BEFORE the bean is instantiated. It uses reflection to scan:
 *       - All fields annotated with @Autowired
 *       - All methods annotated with @Autowired (setter injection)
 *       - All constructors (constructor injection)
 *     The result is stored as InjectionMetadata in a cache (reflection cost = ONCE).
 *
 *   PHASE 3 — DEPENDENCY RESOLUTION: How Spring finds the RIGHT bean to inject
 *     DefaultListableBeanFactory.resolveDependency() runs the resolution algorithm:
 *       1. Find ALL beans matching the required TYPE
 *       2. If exactly 1 match → inject it
 *       3. If 0 matches → NoSuchBeanDefinitionException (unless required=false)
 *       4. If multiple matches → disambiguation:
 *          a. Does any candidate have @Primary? → use it
 *          b. Does any candidate have @Priority? → use highest
 *          c. Does the field/param name match a bean name? → use it
 *          d. Is @Qualifier specified? → filter by qualifier name
 *          e. Still ambiguous → NoUniqueBeanDefinitionException
 *
 * THE FULL ANSWER (memorize this for interviews):
 *   "Spring uses ClassPathBeanDefinitionScanner to find @Component classes and
 *    register BeanDefinitions. Then AutowiredAnnotationBeanPostProcessor uses
 *    reflection to find all @Autowired injection points and caches that metadata.
 *    When a bean is being created, resolveDependency() looks up all beans of the
 *    required type, then disambiguates via @Primary, @Qualifier, or name matching."
 *
 * INTERVIEW ANGLE:
 *   Q: "How does Spring know what to inject into a field?"
 *   Q: "What happens when two beans implement the same interface?"
 *   Q: "What is AutowiredAnnotationBeanPostProcessor?"
 *   Q: "How does @Qualifier work internally?"
 *   Q: "What's the injection order — by type or by name?"
 *
 * REAL-WORLD USE:
 *   Every Spring application — understanding this prevents:
 *   - NoUniqueBeanDefinitionException (multiple beans, forgot @Primary/@Qualifier)
 *   - NoSuchBeanDefinitionException (wrong type, missing @Component)
 *   - Unexpected bean injected (name-based fallback surprised you)
 */
public class SpringDependencyResolution {

    public static void main(String[] args) throws Exception {
        System.out.println("=== PHASE 1: Bean Discovery ===");
        demonstrateBeanDiscovery();

        System.out.println("\n=== PHASE 2: Dependency Identification via Reflection ===");
        demonstrateDependencyIdentification();

        System.out.println("\n=== PHASE 3: Resolution Algorithm ===");
        demonstrateResolutionAlgorithm();

        System.out.println("\n=== DISAMBIGUATION: @Primary, @Qualifier, Name Matching ===");
        demonstrateDisambiguation();

        System.out.println("\n=== CONSTRUCTOR INJECTION DETECTION ===");
        demonstrateConstructorDetection();

        System.out.println("\n=== DEPENDENCY ORDERING (Topological Sort) ===");
        demonstrateDependencyOrdering();
    }

    // =========================================================
    // SIMULATED ANNOTATIONS
    // =========================================================

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    @interface Component { String value() default ""; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
    @interface Autowired { boolean required() default true; }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
    @interface Qualifier { String value(); }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface Primary {}

    // =========================================================
    // PHASE 1: BEAN DISCOVERY — ClassPathBeanDefinitionScanner
    // =========================================================
    //
    // In real Spring, ApplicationContext startup triggers:
    //   ConfigurationClassPostProcessor (a BeanFactoryPostProcessor)
    //     → processes @ComponentScan annotation
    //     → creates ClassPathBeanDefinitionScanner
    //     → scans all classes in the base package(s)
    //     → for each class with @Component (or @Service, @Repository, @Controller,
    //        @RestController — all are @Component stereotypes via meta-annotation):
    //         → reads the class metadata WITHOUT loading the class (uses ASM bytecode reader)
    //         → creates a ScannedGenericBeanDefinition
    //         → registers it in the BeanDefinitionRegistry
    //
    // @Bean methods:
    //   ConfigurationClassParser processes @Configuration classes.
    //   Each @Bean method → ConfigurationClassBeanDefinition registered.
    //   The method IS the factory — Spring will call it reflectively to create the bean.
    //
    // Bean naming rules:
    //   @Component("myService") → name = "myService"
    //   @Component             → name = class simple name, first letter lowercase
    //   @Bean                  → name = method name
    //   @Bean(name="foo")      → name = "foo"
    //
    // INTERVIEW Q: "Does Spring load every class in the classpath?"
    // A: NO. It uses ASM (bytecode analysis library) to READ class file metadata
    //    WITHOUT actually loading classes via ClassLoader.
    //    Only classes that match the component filter are loaded.
    //    This is why Spring startup is not as slow as "loading everything" would imply.

    static void demonstrateBeanDiscovery() {
        // Simulate the scanner finding @Component classes
        Class<?>[] allClasses = {
            NotificationService.class, EmailNotifier.class, SmsNotifier.class,
            PaymentService.class, OrderService.class
        };

        Map<String, Class<?>> registry = new LinkedHashMap<>();

        for (Class<?> cls : allClasses) {
            Component annotation = cls.getAnnotation(Component.class);
            if (annotation != null) {
                // Real Spring: uses AnnotationBeanNameGenerator to derive the bean name
                String beanName = annotation.value().isEmpty()
                    ? Character.toLowerCase(cls.getSimpleName().charAt(0))
                        + cls.getSimpleName().substring(1)  // "EmailNotifier" → "emailNotifier"
                    : annotation.value();
                registry.put(beanName, cls);
                System.out.println("  [Scanner] Found: " + beanName + " → " + cls.getSimpleName());
            }
        }

        System.out.println("  Total beans registered: " + registry.size());

        // KEY INSIGHT: At this point, NO INSTANCES exist yet.
        // We only have names → class mappings (BeanDefinitions).
        // Instantiation happens later when the context calls finishBeanFactoryInitialization().
    }

    // =========================================================
    // PHASE 2: DEPENDENCY IDENTIFICATION
    // =========================================================
    //
    // AutowiredAnnotationBeanPostProcessor.postProcessMergedBeanDefinition() runs
    // for EACH bean BEFORE it is instantiated. It:
    //
    //   1. Calls findAutowiringMetadata(beanClass)
    //   2. Reflects over ALL declared fields → collects @Autowired fields
    //   3. Reflects over ALL declared methods → collects @Autowired methods (setters)
    //   4. Reflects over ALL constructors → finds @Autowired constructor (or single ctor)
    //   5. Stores as InjectionMetadata in a ConcurrentHashMap cache
    //      (key = beanClassName, value = InjectionMetadata)
    //
    // WHY CACHE? Reflection is expensive. For a singleton, injection happens once.
    //   For a prototype, injection happens every time — caching the METADATA (not the
    //   result) avoids re-scanning the class on every instance creation.
    //
    // WHAT IS InjectionMetadata?
    //   A list of InjectionMetadata.InjectedElement objects.
    //   Each element knows: which field/method, is it required?, what type to look for.
    //   At injection time, each element calls inject(bean, beanName, pvs).
    //
    // INTERVIEW Q: "When does Spring scan for @Autowired fields?"
    // A: Before the bean is created, via postProcessMergedBeanDefinition().
    //    The class is scanned ONCE (metadata cached). Injection happens via
    //    postProcessProperties() when the bean instance exists.

    // Simulated InjectionMetadata
    record InjectionPoint(Field field, boolean required, String qualifierValue) {}

    static List<InjectionPoint> findInjectionPoints(Class<?> beanClass) {
        List<InjectionPoint> points = new ArrayList<>();

        // Scan declared fields (including private — reflection ignores access modifiers)
        for (Field field : beanClass.getDeclaredFields()) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                String qualVal = qualifier != null ? qualifier.value() : null;
                points.add(new InjectionPoint(field, autowired.required(), qualVal));
                System.out.println("  [Scan] Found @Autowired field: "
                    + beanClass.getSimpleName() + "." + field.getName()
                    + " : " + field.getType().getSimpleName()
                    + (qualVal != null ? " @Qualifier(\"" + qualVal + "\")" : ""));
            }
        }

        // Real Spring also scans superclass fields recursively up the hierarchy
        return points;
    }

    static void demonstrateDependencyIdentification() {
        System.out.println("  Scanning NotificationService for @Autowired injection points:");
        List<InjectionPoint> points = findInjectionPoints(NotificationService.class);
        System.out.println("  → Found " + points.size() + " injection point(s)");

        System.out.println("\n  Scanning PaymentService for @Autowired injection points:");
        List<InjectionPoint> paymentPoints = findInjectionPoints(PaymentService.class);
        System.out.println("  → Found " + paymentPoints.size() + " injection point(s)");
    }

    // =========================================================
    // PHASE 3: THE RESOLUTION ALGORITHM
    // =========================================================
    //
    // DefaultListableBeanFactory.resolveDependency(DependencyDescriptor desc, ...)
    //
    // STEP 1: Get the required TYPE from the injection point
    //   e.g., field "Notifier notifier" → required type = Notifier.class
    //
    // STEP 2: findAutowireCandidates(beanName, requiredType, descriptor)
    //   → searches BeanDefinitionRegistry for all beans ASSIGNABLE to requiredType
    //   → "assignable" = exact type OR subclass OR implementation of interface
    //   → returns Map<String, Object> candidateBeans
    //
    // STEP 3: If exactly 1 candidate → inject it. Done.
    //
    // STEP 4: If 0 candidates:
    //   → required=true  → throw NoSuchBeanDefinitionException
    //   → required=false → leave field null (optional dependency)
    //   → Optional<T>    → inject Optional.empty()
    //
    // STEP 5: If multiple candidates → determineAutowireCandidate():
    //   a. Look for @Primary among candidates → if exactly 1 primary → use it
    //   b. Look for @Priority (javax/jakarta) → use highest priority value
    //   c. Check if any candidate name == field/param name → use it (NAME FALLBACK)
    //   d. None of the above → throw NoUniqueBeanDefinitionException
    //
    // THE NAME FALLBACK is a common surprise:
    //   If you have two Notifier beans named "emailNotifier" and "smsNotifier",
    //   and your field is named "emailNotifier", Spring uses emailNotifier WITHOUT
    //   you needing @Qualifier. This is implicit and can cause subtle bugs.
    //
    // INTERVIEW Q: "What exception do you get with two beans of the same type?"
    // A: NoUniqueBeanDefinitionException — Spring found multiple candidates and
    //    couldn't determine which one to use. Fix: add @Primary to one, or
    //    use @Qualifier at the injection point to specify which one.

    static void demonstrateResolutionAlgorithm() {
        // Simulate the registry
        Map<String, Object> beanRegistry = new LinkedHashMap<>();
        beanRegistry.put("emailNotifier", new EmailNotifier());
        beanRegistry.put("smsNotifier", new SmsNotifier());
        PaymentService ps = new PaymentService();
        NotificationService ns = new NotificationService();
        beanRegistry.put("paymentService", ps);
        beanRegistry.put("orderService", new OrderService(ps, ns));

        System.out.println("  Resolving: PaymentService needs PaymentGateway...");
        Object resolved = resolveByType(beanRegistry, PaymentGateway.class, "paymentGateway", null);
        System.out.println("  → Resolved: " + (resolved != null ? resolved.getClass().getSimpleName() : "null"));

        System.out.println("\n  Resolving: OrderService needs Notifier (multiple candidates)...");
        // Two beans implement Notifier — this would be ambiguous without qualifiers
        Object resolved2 = resolveByType(beanRegistry, Notifier.class, "notifier", null);
        // resolved2 is null (ambiguous) — would throw NoUniqueBeanDefinitionException in Spring
    }

    // Simulates DefaultListableBeanFactory.resolveDependency()
    static Object resolveByType(Map<String, Object> registry, Class<?> type,
                                 String fieldName, String qualifier) {
        // STEP 2: Find all candidates by type
        Map<String, Object> candidates = registry.entrySet().stream()
            .filter(e -> type.isAssignableFrom(e.getValue().getClass()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> a, LinkedHashMap::new));

        System.out.println("    Candidates for " + type.getSimpleName() + ": " + candidates.keySet());

        // STEP 3: Exactly one → done
        if (candidates.size() == 1) {
            return candidates.values().iterator().next();
        }

        // STEP 4: Zero → NoSuchBeanDefinitionException
        if (candidates.isEmpty()) {
            System.out.println("    → NoSuchBeanDefinitionException: no bean of type " + type.getSimpleName());
            return null;
        }

        // STEP 5a: Check @Primary
        Optional<Map.Entry<String, Object>> primary = candidates.entrySet().stream()
            .filter(e -> e.getValue().getClass().isAnnotationPresent(Primary.class))
            .findFirst();
        if (primary.isPresent()) {
            System.out.println("    → @Primary wins: " + primary.get().getKey());
            return primary.get().getValue();
        }

        // STEP 5b: Check @Qualifier match
        if (qualifier != null) {
            Object qualified = candidates.get(qualifier);
            if (qualified != null) {
                System.out.println("    → @Qualifier(\"" + qualifier + "\") wins: " + qualified.getClass().getSimpleName());
                return qualified;
            }
        }

        // STEP 5c: Name fallback — does field name match a bean name?
        if (candidates.containsKey(fieldName)) {
            System.out.println("    → Name fallback: field name '" + fieldName + "' matches a bean name");
            return candidates.get(fieldName);
        }

        // STEP 5d: Still ambiguous
        System.out.println("    → NoUniqueBeanDefinitionException: "
            + candidates.size() + " candidates, no way to disambiguate: " + candidates.keySet());
        return null;
    }

    // =========================================================
    // DISAMBIGUATION — @Primary, @Qualifier, Name Fallback
    // =========================================================
    //
    // @Primary:
    //   Marks one bean as the DEFAULT when multiple candidates exist.
    //   Only ONE bean of a type should be @Primary (two @Primary = still ambiguous).
    //   Use case: you have a real and a test implementation — @Primary on the real one.
    //
    // @Qualifier("name"):
    //   AT THE INJECTION POINT (field/parameter), specifies WHICH bean by name.
    //   More explicit than @Primary. Use when you have multiple beans of same type
    //   and different injection points need different ones.
    //   Note: @Qualifier at bean definition side creates a custom qualifier,
    //         @Qualifier at injection point selects by that qualifier.
    //
    // NAME FALLBACK (implicit — be careful!):
    //   If field is named "emailNotifier" and a bean named "emailNotifier" exists,
    //   Spring uses it WITHOUT any annotation. Silent behavior that surprises people.
    //
    // INTERVIEW Q: "@Primary vs @Qualifier — which wins?"
    // A: @Qualifier wins. @Qualifier at the injection point is more specific
    //    than @Primary at the bean definition. Resolution order:
    //    @Qualifier → @Primary → @Priority → name fallback → exception.
    //
    // INTERVIEW Q: "Can I have two @Primary beans of the same type?"
    // A: Yes, you CAN define them — but Spring will still throw
    //    NoUniqueBeanDefinitionException because it finds two primary candidates.
    //    Only one @Primary per type is meaningful.

    static void demonstrateDisambiguation() throws Exception {
        Map<String, Object> registry = new LinkedHashMap<>();
        registry.put("emailNotifier", new EmailNotifier()); // @Primary
        registry.put("smsNotifier",   new SmsNotifier());

        System.out.println("  @Primary — field 'notifier' with two candidates:");
        Object r1 = resolveByType(registry, Notifier.class, "notifier", null);
        System.out.println("  Result: " + (r1 != null ? r1.getClass().getSimpleName() : "ambiguous!"));

        System.out.println("\n  @Qualifier('smsNotifier') — explicit qualifier overrides @Primary:");
        Object r2 = resolveByType(registry, Notifier.class, "notifier", "smsNotifier");
        System.out.println("  Result: " + (r2 != null ? r2.getClass().getSimpleName() : "ambiguous!"));

        System.out.println("\n  Name fallback — field named 'smsNotifier' (no annotation):");
        // If the field is named "smsNotifier" — Spring checks if that name is a candidate
        Object r3 = resolveByType(registry, Notifier.class, "smsNotifier", null);
        System.out.println("  Result: " + (r3 != null ? r3.getClass().getSimpleName() : "ambiguous!"));

        // REAL-WORLD TIP: Always use @Qualifier explicitly when you have multiple
        // beans of the same type. Don't rely on name fallback — it's implicit and
        // fragile (rename the field → injection breaks silently at runtime).
    }

    // =========================================================
    // CONSTRUCTOR INJECTION DETECTION
    // =========================================================
    //
    // Spring resolves constructor injection differently from field injection.
    //
    // RULE 1: Single constructor, no @Autowired needed (Spring 4.3+)
    //   Spring finds exactly 1 constructor → uses it automatically.
    //   This is why @Service classes with one constructor don't need @Autowired.
    //
    // RULE 2: Multiple constructors — must annotate one with @Autowired
    //   Spring doesn't know which to use → requires @Autowired to pick one.
    //   If multiple have @Autowired → error (unless required=false fallback).
    //
    // RULE 3: @Autowired(required=false) on multiple constructors
    //   Spring tries each and uses the one whose dependencies are all available.
    //   (rare, used for optional feature injection)
    //
    // MECHANICS: AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors()
    //   This is the method that picks the constructor before instantiation.
    //   ConstructorResolver then resolves each constructor parameter like a field:
    //     → same resolveDependency() algorithm per parameter
    //     → @Qualifier on constructor params works the same way as on fields
    //
    // INTERVIEW Q: "Does @Autowired work on constructors without writing it?"
    // A: YES — if the class has exactly ONE constructor (Spring 4.3+),
    //    Spring auto-detects it for injection. No @Autowired annotation needed.
    //    This is why modern Spring Boot code often has no @Autowired at all.

    static void demonstrateConstructorDetection() {
        // Simulate: how does Spring choose a constructor?
        Class<OrderService> cls = OrderService.class;
        Constructor<?>[] constructors = cls.getDeclaredConstructors();

        System.out.println("  OrderService constructors: " + constructors.length);
        for (Constructor<?> ctor : constructors) {
            boolean hasAutowired = ctor.isAnnotationPresent(Autowired.class);
            System.out.println("    " + ctor.toGenericString()
                + (hasAutowired ? " @Autowired" : ""));
        }

        if (constructors.length == 1) {
            System.out.println("  → Single constructor: Spring uses it automatically (no @Autowired needed)");
            System.out.println("  → Each parameter resolved via resolveDependency()");
        }

        // Simulate constructor parameter resolution
        Constructor<?> chosen = constructors[0];
        System.out.println("  → Resolving " + chosen.getParameterCount() + " constructor params:");
        for (Parameter param : chosen.getParameters()) {
            System.out.println("    Param: " + param.getType().getSimpleName()
                + " → look up by type in registry");
        }
    }

    // =========================================================
    // DEPENDENCY ORDERING — Topological Sort
    // =========================================================
    //
    // Spring must instantiate beans in DEPENDENCY ORDER.
    // If A depends on B, Spring must create B first.
    //
    // How Spring determines order:
    //   1. InjectionMetadata for each bean lists its dependencies (by type).
    //   2. Spring builds a dependency graph: A → B means "A needs B".
    //   3. It performs a TOPOLOGICAL SORT (depth-first, post-order).
    //      → Beans with no dependencies are created first.
    //      → Then beans whose dependencies are all ready.
    //
    // @DependsOn("beanName"):
    //   Forces explicit ordering without a direct injection dependency.
    //   Example: a DatabaseMigrationBean must run before UserService,
    //   but UserService doesn't inject it — use @DependsOn("migrationBean").
    //
    // Circular dependency detection:
    //   During the topological sort, if Spring finds a cycle in constructor deps
    //   (A → B → A), it throws BeanCurrentlyInCreationException.
    //   For setter/field injection cycles, the 3-level cache (see SpringBeanLifecycle.java)
    //   breaks the cycle by providing early references.
    //
    // INTERVIEW Q: "How does Spring decide which bean to create first?"
    // A: It builds a dependency graph from the BeanDefinitions and @Autowired metadata,
    //    then does a topological sort. Beans with no dependencies come first.
    //    @DependsOn provides explicit ordering when there's no direct injection link.

    static void demonstrateDependencyOrdering() {
        // Simulate the dependency graph
        // NotificationService → EmailNotifier, SmsNotifier
        // PaymentService      → PaymentGateway
        // OrderService        → PaymentService, NotificationService
        Map<String, List<String>> depGraph = new LinkedHashMap<>();
        depGraph.put("emailNotifier",      List.of());
        depGraph.put("smsNotifier",        List.of());
        depGraph.put("notificationService", List.of("emailNotifier", "smsNotifier"));
        depGraph.put("paymentGateway",     List.of());
        depGraph.put("paymentService",     List.of("paymentGateway"));
        depGraph.put("orderService",       List.of("paymentService", "notificationService"));

        System.out.println("  Dependency graph:");
        depGraph.forEach((bean, deps) ->
            System.out.println("    " + bean + " → " + (deps.isEmpty() ? "(none)" : deps)));

        System.out.println("\n  Topological instantiation order:");
        List<String> order = topologicalSort(depGraph);
        for (int i = 0; i < order.size(); i++) {
            System.out.println("    " + (i + 1) + ". " + order.get(i));
        }
    }

    // Simulates Spring's dependency-order resolution (Kahn's algorithm)
    static List<String> topologicalSort(Map<String, List<String>> graph) {
        // Count in-degrees (how many beans depend on each bean)
        Map<String, Integer> inDegree = new HashMap<>();
        graph.forEach((bean, deps) -> {
            inDegree.putIfAbsent(bean, 0);
            deps.forEach(dep -> inDegree.merge(dep, 0, Integer::sum)); // ensure deps exist
        });
        // Beans with no dependents (leaf nodes) go first
        // Actually in-degree here means: how many dependencies does this bean have
        Map<String, Long> depCount = graph.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        List<String> result = new ArrayList<>();
        Queue<String> ready = new LinkedList<>();
        depCount.forEach((k, v) -> { if (v == 0) ready.add(k); });

        while (!ready.isEmpty()) {
            String bean = ready.poll();
            result.add(bean);
            // Find beans that depended on this one — they might now be ready
            graph.forEach((b, deps) -> {
                if (deps.contains(bean)) {
                    depCount.merge(b, -1L, Long::sum);
                    if (depCount.get(b) == 0) ready.add(b);
                }
            });
        }
        return result;
    }

    // =========================================================
    // SAMPLE BEANS used in simulations above
    // =========================================================

    interface Notifier { void notify(String msg); }
    interface PaymentGateway { void charge(double amount); }

    @Component
    @Primary  // EmailNotifier is the default Notifier when multiple exist
    static class EmailNotifier implements Notifier {
        public void notify(String msg) { System.out.println("[Email] " + msg); }
    }

    @Component
    static class SmsNotifier implements Notifier {
        public void notify(String msg) { System.out.println("[SMS] " + msg); }
    }

    @Component
    static class StripeGateway implements PaymentGateway {
        public void charge(double amount) { System.out.println("[Stripe] charged: $" + amount); }
    }

    // NotificationService with field injection — two @Autowired fields
    @Component
    static class NotificationService {
        @Autowired
        @Qualifier("emailNotifier")    // explicit — don't rely on name fallback
        Notifier primaryNotifier;

        @Autowired(required = false)   // optional — won't fail if no PushNotifier exists
        Notifier pushNotifier;         // will be null if no bean found

        void send(String msg) {
            primaryNotifier.notify(msg);
        }
    }

    @Component
    static class PaymentService {
        @Autowired
        PaymentGateway paymentGateway; // one candidate — resolved trivially by type
    }

    // OrderService with constructor injection — single constructor, no @Autowired needed
    @Component
    static class OrderService {
        private final PaymentService paymentService;
        private final NotificationService notificationService;

        // Spring 4.3+: single constructor → auto-detected for injection
        // No @Autowired annotation required!
        OrderService(PaymentService paymentService, NotificationService notificationService) {
            this.paymentService = paymentService;
            this.notificationService = notificationService;
        }

        void placeOrder(String item) {
            notificationService.send("Order placed: " + item);
        }
    }
}

/*
 * ============================================================
 * INTERVIEW CHEAT SHEET — SPRING DEPENDENCY RESOLUTION
 * ============================================================
 *
 * THE FULL ANSWER in 4 sentences:
 *   "Spring scans the classpath using ClassPathBeanDefinitionScanner — it reads
 *    bytecode metadata (without loading classes) to find @Component classes and
 *    registers them as BeanDefinitions. Then AutowiredAnnotationBeanPostProcessor
 *    uses reflection to find @Autowired fields, setters, and constructors in each
 *    bean class, caching the InjectionMetadata so reflection only happens once.
 *    When a bean is instantiated, resolveDependency() finds all beans assignable
 *    to the required type, then disambiguates via @Primary, @Qualifier, or name
 *    matching — throwing NoUniqueBeanDefinitionException if it can't decide.
 *    Finally, Spring instantiates beans in topological order (dependencies first),
 *    using a 3-level cache to resolve setter/field circular dependencies."
 *
 * RESOLUTION PRIORITY ORDER:
 *   1. @Qualifier (most specific — explicit name)
 *   2. @Primary   (default bean of that type)
 *   3. @Priority  (javax/jakarta annotation)
 *   4. Name fallback (field/param name matches a bean name)
 *   5. → NoUniqueBeanDefinitionException
 *
 * KEY CLASSES (real Spring):
 *   ClassPathBeanDefinitionScanner        → discovers @Component beans
 *   ConfigurationClassPostProcessor       → processes @Configuration / @ComponentScan
 *   AutowiredAnnotationBeanPostProcessor  → finds @Autowired points, injects deps
 *   DefaultListableBeanFactory            → holds registry, runs resolveDependency()
 *   InjectionMetadata                     → cached reflection results per class
 *   ConstructorResolver                   → handles constructor injection
 *
 * COMMON ERRORS AND CAUSES:
 *   NoSuchBeanDefinitionException     → required=true but no bean of that type found
 *   NoUniqueBeanDefinitionException   → multiple candidates, no @Primary/@Qualifier
 *   BeanCurrentlyInCreationException  → circular dependency in constructor injection
 *   NullPointerException (field null) → @Autowired(required=false) and no bean found
 *
 * CONSTRUCTOR INJECTION RULES:
 *   1 constructor           → Spring uses it automatically (Spring 4.3+)
 *   Multiple constructors   → must annotate one with @Autowired
 *   Each constructor param  → resolved same as @Autowired field
 */
