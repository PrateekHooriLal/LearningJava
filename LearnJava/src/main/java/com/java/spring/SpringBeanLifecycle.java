package com.java.spring;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/**
 * ============================================================
 * SPRING BEAN LIFECYCLE & APPLICATION CONTEXT STARTUP
 * ============================================================
 *
 * CONCEPT:
 *   Spring's IoC (Inversion of Control) container manages the entire
 *   lifecycle of your beans — from discovery to destruction.
 *   This file simulates Spring's internal startup sequence using
 *   ONLY java.lang.reflect.* — no Spring dependency.
 *
 * THE 10-STEP STARTUP SEQUENCE (memorize this for interviews):
 *   1.  Parse configuration (@ComponentScan / @Configuration / XML)
 *   2.  Register BeanDefinitions (metadata, not beans yet)
 *   3.  Run BeanFactoryPostProcessors (modify definitions before creation)
 *   4.  Instantiate singleton beans (in dependency order)
 *   5.  Populate properties / inject dependencies (@Autowired)
 *   6.  BeanPostProcessor.postProcessBeforeInitialization()
 *   7.  Call init methods (@PostConstruct / afterPropertiesSet / init-method)
 *   8.  BeanPostProcessor.postProcessAfterInitialization() ← AOP PROXY CREATED HERE
 *   9.  Bean is ready in the context (stored in singletonObjects map)
 *   10. On shutdown: @PreDestroy → DisposableBean.destroy() → destroy-method
 *
 * INTERVIEW ANGLE:
 *   Q: "How does Spring start up?"
 *   A: Walk through the 10 steps above. Key insight: Spring separates
 *      METADATA registration (BeanDefinition) from INSTANTIATION.
 *      This allows BeanFactoryPostProcessors to modify beans before they're created.
 *
 *   Q: "When is a @Transactional proxy created?"
 *   A: Step 8 — postProcessAfterInitialization. AbstractAutoProxyCreator
 *      (a BeanPostProcessor) wraps the bean in a CGLIB proxy.
 *
 *   Q: "Why can't constructor injection have circular dependencies?"
 *   A: Spring needs to fully construct A to inject into B, but needs B to
 *      construct A — deadlock. Setter injection uses the 3-level cache to
 *      break the cycle by exposing partially-constructed beans.
 *
 * REAL-WORLD USE:
 *   - Every Spring Boot app goes through this sequence on startup
 *   - Understanding this explains @Transactional bugs, @Async pitfalls,
 *     and N+1 Hibernate issues (all proxy-related)
 */
public class SpringBeanLifecycle {

    public static void main(String[] args) throws Exception {
        System.out.println("=== SPRING BEAN LIFECYCLE SIMULATION ===\n");

        SimulatedApplicationContext ctx = new SimulatedApplicationContext();

        // Register beans (simulates @ComponentScan finding @Component classes)
        ctx.register("userService", UserService.class);
        ctx.register("orderService", OrderService.class);
        ctx.register("emailService", EmailService.class);

        // Start the context — triggers the full startup sequence
        ctx.refresh();

        System.out.println("\n=== CONTEXT READY — USING BEANS ===\n");
        UserService userService = (UserService) ctx.getBean("userService");
        userService.createUser("Alice");

        System.out.println("\n=== SHUTDOWN ===\n");
        ctx.close();

        System.out.println("\n=== SCOPE DEMO ===\n");
        demonstrateScopes();

        System.out.println("\n=== CIRCULAR DEPENDENCY DEMO ===\n");
        demonstrateCircularDependencyResolution();

        System.out.println("\n=== INJECTION TYPES DEMO ===\n");
        demonstrateInjectionTypes();
    }

    // =========================================================
    // SIMULATED ANNOTATIONS (mirrors Spring's actual annotations)
    // =========================================================

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    @interface PostConstruct {}     // called after injection, before ready

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    @interface PreDestroy {}        // called before bean is removed from context

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    @interface Autowired {}         // field injection marker

    // =========================================================
    // STEP 1 & 2: BEANDEFINITION — The Heart of the IoC Container
    // =========================================================
    //
    // KEY INSIGHT: Spring does NOT create beans immediately on startup.
    // It first registers BeanDefinition objects (pure metadata).
    // This separation allows BeanFactoryPostProcessors to modify definitions
    // before any bean is instantiated.
    //
    // Real Spring BeanDefinition holds:
    //   - beanClass          → the actual Class object
    //   - scope              → singleton / prototype / request / session
    //   - lazyInit           → create on first access or at startup?
    //   - initMethodName     → @PostConstruct method name
    //   - destroyMethodName  → @PreDestroy method name
    //   - propertyValues     → setter-injected dependencies
    //   - constructorArgs    → constructor-injected dependencies
    //   - dependsOn          → explicit ordering hint
    //   - primary            → @Primary (used in ambiguous autowiring)
    //
    // INTERVIEW Q: What is a BeanDefinition?
    // A: The recipe/blueprint for a bean. Spring reads @Component, @Bean, XML
    //    and creates BeanDefinition objects. Beans are created FROM these definitions.

    static class BeanDefinition {
        final String beanName;
        final Class<?> beanClass;
        String scope = "singleton";   // singleton | prototype
        boolean lazyInit = false;
        Object singletonInstance;     // null until instantiated (singleton cache)

        BeanDefinition(String name, Class<?> cls) {
            this.beanName = name;
            this.beanClass = cls;
        }
    }

    // =========================================================
    // STEP 3: BEANFACTORYPOSTPROCESSOR
    // =========================================================
    //
    // Runs BEFORE any beans are created.
    // Can modify BeanDefinitions — e.g., change scope, add properties.
    //
    // Real-world examples in Spring:
    //   - PropertySourcesPlaceholderConfigurer: resolves @Value("${...}") placeholders
    //   - ConfigurationClassPostProcessor: processes @Configuration classes
    //
    // CONTRAST with BeanPostProcessor (Step 6 & 8):
    //   BeanFactoryPostProcessor → operates on DEFINITIONS (before instantiation)
    //   BeanPostProcessor        → operates on INSTANCES (after instantiation)
    //
    // Q: "When does @Value get resolved?"
    // A: By PropertySourcesPlaceholderConfigurer (a BeanFactoryPostProcessor),
    //    which runs BEFORE beans are created. It modifies the BeanDefinition to
    //    replace "${db.url}" with the actual value from application.properties.

    interface BeanFactoryPostProcessor {
        void postProcessBeanFactory(Map<String, BeanDefinition> definitions);
    }

    // =========================================================
    // STEPS 6 & 8: BEANPOSTPROCESSOR
    // =========================================================
    //
    // Wraps EVERY bean as it's being initialized.
    // This is how Spring implements AOP, @Transactional, @Async, @Cacheable.
    //
    // postProcessBeforeInitialization → called BEFORE @PostConstruct
    // postProcessAfterInitialization  → called AFTER @PostConstruct
    //                                   ← AOP PROXY IS CREATED HERE
    //
    // Real Spring BeanPostProcessors:
    //   AutowiredAnnotationBeanPostProcessor  → processes @Autowired
    //   CommonAnnotationBeanPostProcessor     → processes @PostConstruct, @PreDestroy
    //   AbstractAutoProxyCreator              → creates CGLIB/JDK proxies for AOP
    //
    // INTERVIEW Q: "How does @Transactional work internally?"
    // A: AbstractAutoProxyCreator (BeanPostProcessor) detects @Transactional in
    //    postProcessAfterInitialization(), creates a CGLIB subclass proxy.
    //    The proxy overrides your methods: begin-tx → call real method → commit/rollback.

    interface BeanPostProcessor {
        // Can return a DIFFERENT object (e.g., a proxy) — this is how AOP works!
        default Object postProcessBeforeInitialization(Object bean, String beanName) {
            return bean; // default: return unchanged
        }
        default Object postProcessAfterInitialization(Object bean, String beanName) {
            return bean; // default: return unchanged
        }
    }

    // Simulated logging BeanPostProcessor (like Spring's CommonAnnotationBeanPostProcessor)
    static class LoggingBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            System.out.println("  [BPP-Before] Processing bean: " + beanName);
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            System.out.println("  [BPP-After]  Bean ready: " + beanName
                + " ← AOP proxy would be created here if needed");
            return bean; // In real Spring: might return a CGLIB proxy instead!
        }
    }

    // =========================================================
    // THE SIMULATED APPLICATION CONTEXT
    // =========================================================
    //
    // This simulates AbstractApplicationContext.refresh() — the single most
    // important method in Spring. Every Spring Boot app calls this on startup.
    //
    // Real Spring refresh() steps (from AbstractApplicationContext.java):
    //   1.  prepareRefresh()
    //   2.  obtainFreshBeanFactory()
    //   3.  prepareBeanFactory()
    //   4.  postProcessBeanFactory()          ← subclass hook
    //   5.  invokeBeanFactoryPostProcessors() ← Step 3 in our model
    //   6.  registerBeanPostProcessors()
    //   7.  initMessageSource()
    //   8.  initApplicationEventMulticaster()
    //   9.  onRefresh()                       ← subclass hook
    //   10. registerListeners()
    //   11. finishBeanFactoryInitialization() ← instantiates all singletons
    //   12. finishRefresh()                   ← publish ContextRefreshedEvent

    static class SimulatedApplicationContext {
        private final Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
        // singleton cache = singletonObjects in DefaultSingletonBeanRegistry
        private final Map<String, Object> singletonObjects = new HashMap<>();
        private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
        private final List<BeanFactoryPostProcessor> bfPostProcessors = new ArrayList<>();

        void register(String name, Class<?> cls) {
            definitions.put(name, new BeanDefinition(name, cls));
            System.out.println("[Register] BeanDefinition: " + name + " → " + cls.getSimpleName());
        }

        void refresh() throws Exception {
            System.out.println("\n--- refresh() called ---");

            // Step 3: Run BeanFactoryPostProcessors
            System.out.println("\n[Step 3] BeanFactoryPostProcessors:");
            for (BeanFactoryPostProcessor bfpp : bfPostProcessors) {
                bfpp.postProcessBeanFactory(definitions);
            }

            // Register internal BeanPostProcessors (like Spring registers its own)
            beanPostProcessors.add(new LoggingBeanPostProcessor());

            // Steps 4-9: Instantiate all singleton beans (in registration order)
            System.out.println("\n[Steps 4-9] Instantiating singleton beans:");
            for (BeanDefinition def : definitions.values()) {
                if ("singleton".equals(def.scope) && !def.lazyInit) {
                    getOrCreateBean(def.beanName);
                }
            }
        }

        Object getBean(String name) throws Exception {
            BeanDefinition def = definitions.get(name);
            if (def == null) throw new RuntimeException("No bean: " + name);
            if ("prototype".equals(def.scope)) {
                return createBean(def); // always new instance for prototype
            }
            return singletonObjects.get(name);
        }

        private Object getOrCreateBean(String name) throws Exception {
            if (singletonObjects.containsKey(name)) return singletonObjects.get(name);
            BeanDefinition def = definitions.get(name);
            Object bean = createBean(def);
            singletonObjects.put(name, bean);
            return bean;
        }

        private Object createBean(BeanDefinition def) throws Exception {
            System.out.println("  [Step 4] Instantiating: " + def.beanName);

            // Step 4: Instantiation via no-arg constructor (or @Autowired constructor)
            Object bean = def.beanClass.getDeclaredConstructor().newInstance();

            // Step 5: Property/field injection (@Autowired fields)
            injectDependencies(bean, def.beanName);

            // Step 6: BeanPostProcessor.postProcessBeforeInitialization
            for (BeanPostProcessor bpp : beanPostProcessors) {
                bean = bpp.postProcessBeforeInitialization(bean, def.beanName);
            }

            // Step 7: Call @PostConstruct init methods
            callPostConstruct(bean, def.beanName);

            // Step 8: BeanPostProcessor.postProcessAfterInitialization (AOP proxy here!)
            for (BeanPostProcessor bpp : beanPostProcessors) {
                bean = bpp.postProcessAfterInitialization(bean, def.beanName);
            }

            System.out.println("  [Step 9] Bean ready: " + def.beanName);
            return bean;
        }

        // Simulates AutowiredAnnotationBeanPostProcessor
        private void injectDependencies(Object bean, String beanName) throws Exception {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    String depName = field.getName(); // by name (simplified)
                    if (definitions.containsKey(depName)) {
                        System.out.println("  [Step 5] @Autowired " + beanName + "." + depName);
                        field.setAccessible(true);
                        field.set(bean, getOrCreateBean(depName));
                    }
                }
            }
        }

        // Simulates CommonAnnotationBeanPostProcessor handling @PostConstruct
        private void callPostConstruct(Object bean, String beanName) throws Exception {
            for (Method m : bean.getClass().getDeclaredMethods()) {
                if (m.isAnnotationPresent(PostConstruct.class)) {
                    System.out.println("  [Step 7] @PostConstruct: " + beanName + "." + m.getName() + "()");
                    m.setAccessible(true);
                    m.invoke(bean);
                }
            }
        }

        void close() throws Exception {
            System.out.println("[Shutdown] Running @PreDestroy on all beans...");
            for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
                for (Method m : entry.getValue().getClass().getDeclaredMethods()) {
                    if (m.isAnnotationPresent(PreDestroy.class)) {
                        System.out.println("  [@PreDestroy] " + entry.getKey() + "." + m.getName() + "()");
                        m.setAccessible(true);
                        m.invoke(entry.getValue());
                    }
                }
            }
        }
    }

    // =========================================================
    // SAMPLE BEANS (simulate @Component classes)
    // =========================================================

    static class EmailService {
        @PostConstruct
        void init() { System.out.println("    EmailService: connecting to SMTP server..."); }

        @PreDestroy
        void cleanup() { System.out.println("    EmailService: closing SMTP connection"); }

        void sendEmail(String to, String msg) {
            System.out.println("    [Email] → " + to + ": " + msg);
        }
    }

    static class OrderService {
        @PostConstruct
        void init() { System.out.println("    OrderService: warming up order cache..."); }

        void placeOrder(String user) {
            System.out.println("    [Order] placing order for: " + user);
        }
    }

    static class UserService {
        // Field injection (simulates @Autowired)
        // GOTCHA: Field injection is discouraged — not testable without Spring,
        // hides dependencies, makes classes harder to reason about.
        // Use constructor injection in production code!
        @Autowired EmailService emailService;
        @Autowired OrderService orderService;

        @PostConstruct
        void init() { System.out.println("    UserService: initialized (all deps injected)"); }

        @PreDestroy
        void shutdown() { System.out.println("    UserService: cleaning up user sessions"); }

        void createUser(String name) {
            System.out.println("    [User] Creating user: " + name);
            orderService.placeOrder(name);
            emailService.sendEmail(name + "@example.com", "Welcome!");
        }
    }

    // =========================================================
    // BEAN SCOPES
    // =========================================================
    //
    // SINGLETON (default):
    //   - One instance per ApplicationContext
    //   - Shared across all injection points
    //   - GOTCHA: Stateful singleton = threading bug waiting to happen!
    //     (instance variables shared across all requests)
    //
    // PROTOTYPE:
    //   - New instance every time getBean() is called
    //   - Spring does NOT manage lifecycle after creation (no @PreDestroy!)
    //   - Use for stateful beans (e.g., command objects, wizard steps)
    //
    // REQUEST (web-aware):
    //   - One instance per HTTP request
    //   - Requires WebApplicationContext
    //
    // SESSION (web-aware):
    //   - One instance per HTTP session
    //
    // CRITICAL GOTCHA — Injecting prototype into singleton:
    //   @Singleton ServiceA {
    //     @Autowired PrototypeB b;  // b is injected ONCE at startup → still only one instance!
    //   }
    //   Fix: Inject ApplicationContext and call ctx.getBean(PrototypeB.class) each time.
    //   Or use @Lookup annotation (Spring overrides the method via CGLIB).
    //   Or inject ObjectFactory<PrototypeB> and call factory.getObject() each time.
    //
    // Q: "How many instances of a @Service class exist at runtime?"
    // A: By default, exactly ONE — it's a singleton.
    //    So @Service methods must be STATELESS (no mutable instance fields storing request data).

    static void demonstrateScopes() {
        System.out.println("Singleton scope: same instance returned every time");
        System.out.println("Prototype scope: new instance every call");
        System.out.println("REQUEST/SESSION: one per HTTP request/session (web context only)");
        System.out.println();
        System.out.println("GOTCHA: @Autowired PrototypeBean b in a Singleton → b is created ONCE.");
        System.out.println("Fix: Use ObjectFactory<B> or ApplicationContext.getBean(B.class)");

        // Simulate the problem
        // Singleton holds a reference to a "prototype" — but it's the same object forever
        List<String> prototypeInstances = new ArrayList<>();
        Supplier<String> prototypeFactory = () -> "PrototypeBean@" + System.nanoTime();

        // Wrong way (like field injection into singleton):
        String injectedOnce = prototypeFactory.get();
        System.out.println("Wrong (field injection): always → " + injectedOnce);

        // Right way (ObjectFactory):
        System.out.println("Right (ObjectFactory): each call → " + prototypeFactory.get());
        System.out.println("Right (ObjectFactory): each call → " + prototypeFactory.get());
    }

    // =========================================================
    // CIRCULAR DEPENDENCY — The 3-Level Cache
    // =========================================================
    //
    // SCENARIO: A depends on B, B depends on A.
    //
    // WHY CONSTRUCTOR INJECTION FAILS:
    //   To create A: need B → to create B: need A → deadlock!
    //   Spring detects this and throws BeanCurrentlyInCreationException.
    //   This is actually GOOD — it forces you to redesign the dependency.
    //
    // HOW SETTER/FIELD INJECTION CAN WORK (3-level cache):
    //   Spring's DefaultSingletonBeanRegistry uses THREE caches:
    //
    //   Level 1: singletonObjects        → fully initialized beans (the "ready" cache)
    //   Level 2: earlySingletonObjects   → partially constructed, exposed for circular refs
    //   Level 3: singletonFactories      → ObjectFactory that can create an early reference
    //
    //   Creation of A (with field injection):
    //   1. Start creating A → put ObjectFactory<A> in Level 3 cache
    //   2. Inject A's dependencies → need B
    //   3. Start creating B → put ObjectFactory<B> in Level 3 cache
    //   4. Inject B's dependencies → need A
    //   5. Check caches → find A's factory in Level 3 → get EARLY REFERENCE to A
    //   6. Inject early A into B → B is fully initialized → move to Level 1
    //   7. Back to A: B is now available → inject into A → A fully initialized → Level 1
    //
    // INTERVIEW Q: "Can Spring always resolve circular dependencies?"
    // A: NO. Only for SINGLETON beans with setter/field injection.
    //    Constructor injection → always fails (Spring can't create a partial instance).
    //    Prototype circular deps → always fail (no caching for prototypes).
    //
    // REAL ADVICE: If you have a circular dependency, it's a design smell.
    //    Refactor: extract a third service, or rethink the dependency direction.

    static void demonstrateCircularDependencyResolution() {
        System.out.println("Constructor injection circular dep → BeanCurrentlyInCreationException");
        System.out.println("Setter/field injection circular dep → resolved via 3-level cache:");
        System.out.println("  Level 1: singletonObjects      (fully ready)");
        System.out.println("  Level 2: earlySingletonObjects (partial, for circular refs)");
        System.out.println("  Level 3: singletonFactories    (ObjectFactory → early reference)");
        System.out.println();
        System.out.println("Best practice: circular deps = design smell. Refactor!");
        System.out.println("Introduce a 3rd service or flip dependency direction.");
    }

    // =========================================================
    // INJECTION TYPES — CONSTRUCTOR vs FIELD vs SETTER
    // =========================================================
    //
    // CONSTRUCTOR INJECTION (recommended):
    //   + All dependencies are explicit (visible in constructor signature)
    //   + Bean is IMMUTABLE — deps can be final
    //   + Testable without Spring (just call new Service(mockDep))
    //   + Circular deps fail at startup (good — fail fast)
    //   - Verbose for many dependencies
    //
    // FIELD INJECTION (common but bad):
    //   + Concise
    //   - Dependencies are hidden (no constructor signature)
    //   - Cannot be final — mutability risk
    //   - Not testable without Spring (can't inject mocks without reflection)
    //   - Circular deps silently resolved (hides design problems)
    //
    // SETTER INJECTION (optional dependencies):
    //   + Good for optional/changeable dependencies
    //   - Dependencies not required (can be null at runtime)
    //   - Circular deps resolved (same concern as field injection)
    //
    // Q: "Which injection type does Spring recommend?"
    // A: Constructor injection (as of Spring 4+, this is the recommended approach).
    //    Spring's own docs and @SpringBootTest work best with constructor injection.

    static void demonstrateInjectionTypes() throws Exception {
        System.out.println("=== Constructor Injection (RECOMMENDED) ===");

        // Simulates: @Service class OrderProcessor { private final EmailService emailService; }
        // Dependencies are REQUIRED and IMMUTABLE — set once via constructor
        EmailService emailSvc = new EmailService();

        // Reflection simulation of constructor injection
        Constructor<OrderProcessor> ctor = OrderProcessor.class.getDeclaredConstructor(EmailService.class);
        OrderProcessor processor = ctor.newInstance(emailSvc);
        processor.process("order-123");

        System.out.println("\n=== Field Injection (AVOID — shown for contrast) ===");
        // Simulates: @Autowired EmailService emailService; (field on class)
        OrderProcessorFieldInjected fp = new OrderProcessorFieldInjected();
        Field f = OrderProcessorFieldInjected.class.getDeclaredField("emailService");
        f.setAccessible(true);
        f.set(fp, emailSvc);  // Spring does this via reflection — you can't test without it
        fp.process("order-456");
    }

    // Constructor injection bean — note: dependencies are FINAL
    static class OrderProcessor {
        private final EmailService emailService; // immutable — can't accidentally null it

        // Spring detects single constructor and uses it automatically (no @Autowired needed in Spring 4.3+)
        OrderProcessor(EmailService emailService) {
            this.emailService = Objects.requireNonNull(emailService, "emailService required");
        }

        void process(String orderId) {
            System.out.println("Processing: " + orderId);
            emailService.sendEmail("customer@example.com", "Order " + orderId + " confirmed");
        }
    }

    // Field injection bean — dependencies are NOT final, hidden, hard to test
    static class OrderProcessorFieldInjected {
        @Autowired
        EmailService emailService; // not final, not visible in constructor, Spring-dependent

        void process(String orderId) {
            System.out.println("Processing: " + orderId);
            emailService.sendEmail("customer@example.com", "Order " + orderId + " confirmed");
        }
    }
}

/*
 * ============================================================
 * QUICK REFERENCE — INTERVIEW CHEAT SHEET
 * ============================================================
 *
 * SPRING STARTUP SEQUENCE (10 steps):
 *   1. Parse config → 2. Register BeanDefinitions → 3. BeanFactoryPostProcessors
 *   4. Instantiate → 5. Inject deps → 6. BPP.beforeInit → 7. @PostConstruct
 *   8. BPP.afterInit (AOP proxy!) → 9. Bean ready → 10. @PreDestroy on shutdown
 *
 * KEY CLASSES:
 *   BeanDefinition            → recipe for a bean (not the bean itself)
 *   BeanFactory               → basic container (getBean only)
 *   ApplicationContext        → BeanFactory + events + i18n + AOP
 *   BeanFactoryPostProcessor  → modifies definitions BEFORE creation
 *   BeanPostProcessor         → wraps instances AFTER creation (AOP here)
 *
 * SCOPES:
 *   singleton  → 1 per ApplicationContext (default)
 *   prototype  → new per getBean() call (no @PreDestroy management)
 *   request    → 1 per HTTP request (web only)
 *   session    → 1 per HTTP session (web only)
 *
 * INJECTION TYPES:
 *   Constructor → RECOMMENDED (explicit, immutable, testable)
 *   Field       → AVOID (hidden deps, reflection-only testing)
 *   Setter      → OPTIONAL deps only
 *
 * CIRCULAR DEPS:
 *   Constructor → fails at startup (good!)
 *   Setter/Field → 3-level cache resolves it (design smell though)
 *
 * @TRANSACTIONAL INTERNAL:
 *   BeanPostProcessor creates CGLIB proxy at step 8.
 *   Proxy intercepts method calls: begin-tx → real method → commit/rollback.
 *   Self-invocation (this.method()) bypasses proxy → no transaction!
 *
 * @VALUE RESOLUTION:
 *   PropertySourcesPlaceholderConfigurer (BeanFactoryPostProcessor) at step 3
 *   resolves ${property.key} before any bean is created.
 */
