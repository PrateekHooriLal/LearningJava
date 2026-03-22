package com.java.spring;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * ============================================================
 * SPRING INTERNALS — Reimplemented in Plain Java for Deep Understanding
 * ============================================================
 *
 * CONCEPT:
 *   Spring Framework is fundamentally built on three pillars:
 *   1. IoC Container / Dependency Injection (DI)
 *   2. AOP (Aspect-Oriented Programming) via proxies
 *   3. Event-driven lifecycle management
 *
 *   This file demonstrates Spring's internal mechanisms using ONLY
 *   java.lang.reflect.* — no Spring dependency. Understanding these
 *   internals is what separates SDE 2 from SDE 3 Spring knowledge.
 *
 * WHY THIS MATTERS FOR FAANG INTERVIEWS:
 *   "How does @Transactional work?" is a classic SDE 3 question.
 *   If you just say "Spring handles it" — that's SDE 2.
 *   If you say "CGLIB creates a subclass proxy that wraps the bean,
 *   intercepts method calls, opens a transaction before the call,
 *   commits/rolls back after" — that's SDE 3.
 *
 * INTERVIEW ANGLE:
 *   - @Transactional self-invocation is the #1 Spring production bug.
 *   - N+1 query problem is the #1 Hibernate production performance issue.
 *   - AOP = proxy pattern at the framework level (JDK proxy or CGLIB).
 *   - @Async has multiple non-obvious failure modes.
 *
 * NOTE: All code uses ONLY java.lang.reflect.* and standard Java.
 *   No Spring dependency needed. The patterns are identical to Spring's actual internals.
 */
public class SpringInternals {

    // =========================================================
    // 1. @Transactional PROXY BYPASS (MOST COMMON SPRING GOTCHA)
    // =========================================================
    //
    // THE PROBLEM: When you call a @Transactional method on `this`, you bypass
    //   the Spring proxy that enforces the transaction.
    //
    // WHY IT HAPPENS:
    //   Spring creates a PROXY that wraps your bean:
    //     MyService proxy = CGLIB.createSubclassProxy(MyService.class);
    //   When external code calls proxy.doWork():
    //     → proxy intercepts → opens transaction → calls real bean.doWork() → commit/rollback
    //   When doWork() calls this.helperMethod() (self-call):
    //     → goes DIRECTLY to real bean (not through proxy)
    //     → no transaction interception!
    //
    // MECHANISM:
    //   CGLIB proxy is a SUBCLASS of your bean. It overrides your @Transactional methods.
    //   The REAL bean object doesn't know about the proxy.
    //   `this` inside the real bean = the real bean (no proxy).
    //   External caller → proxy → real bean.
    //   Real bean calling itself → real bean directly (bypasses proxy).
    //
    // THREE FIXES:
    //   FIX 1: Inject self from container (hacky but works):
    //     @Autowired MyService self; → self.helperMethod() goes through proxy
    //   FIX 2: Use AspectJ mode (@EnableAspectJAutoProxy(proxyTargetClass=true, exposeProxy=true)
    //     + AopContext.currentProxy()) — AspectJ weaves at compile/load time, bypasses proxy issue
    //   FIX 3: Refactor — move the helper to a separate @Service bean. Best practice!
    //
    // DEMO: We simulate this with a manual proxy.

    // Simulate @Transactional annotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Transactional {
        boolean readOnly() default false;
    }

    // Simulate the REAL service bean (what you write)
    static class OrderService {
        // BUGGY: saveOrderAndSendEmail calls this.sendConfirmationEmail
        //        If saveOrderAndSendEmail is @Transactional via proxy,
        //        the @Transactional on sendConfirmationEmail is IGNORED (self-call)
        @Transactional
        public void saveOrderAndSendEmail(String orderId) {
            System.out.println("    [Real] saveOrderAndSendEmail: " + orderId);
            System.out.println("    [Real] Saving order...");
            // SELF-CALL: bypasses proxy! sendConfirmationEmail's @Transactional is lost.
            sendConfirmationEmail(orderId); // this.sendConfirmationEmail = no proxy interception
        }

        @Transactional(readOnly = true)
        public void sendConfirmationEmail(String orderId) {
            System.out.println("    [Real] sendConfirmationEmail: " + orderId);
        }

        @Transactional
        public void createOrder(String orderId) {
            System.out.println("    [Real] createOrder: " + orderId + " (called via proxy → has TX)");
        }
    }

    // Manual PROXY that simulates Spring's @Transactional interception
    // This is essentially what CGLIB/JDK Dynamic Proxy does in Spring
    static class TransactionalProxy implements InvocationHandler {
        private final Object target; // the real bean

        TransactionalProxy(Object target) { this.target = target; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Transactional tx = method.getAnnotation(Transactional.class);

            if (tx != null) {
                System.out.println("    [Proxy] @Transactional found on: " + method.getName());
                System.out.println("    [Proxy] BEGIN TRANSACTION (readOnly=" + tx.readOnly() + ")");
                try {
                    Object result = method.invoke(target, args); // call real bean
                    System.out.println("    [Proxy] COMMIT TRANSACTION");
                    return result;
                } catch (Exception e) {
                    System.out.println("    [Proxy] ROLLBACK TRANSACTION: " + e.getMessage());
                    throw e;
                }
            } else {
                return method.invoke(target, args); // no @Transactional, call directly
            }
        }
    }

    // Create a JDK dynamic proxy (Spring uses CGLIB for classes, JDK Proxy for interfaces)
    @SuppressWarnings("unchecked")
    static <T> T createTransactionalProxy(T target, Class<?>... interfaces) {
        return (T) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            interfaces,
            new TransactionalProxy(target)
        );
    }

    // Interface version (needed for JDK Proxy — JDK proxy requires an interface)
    interface OrderServiceInterface {
        @Transactional void saveOrderAndSendEmail(String orderId);
        @Transactional(readOnly = true) void sendConfirmationEmail(String orderId);
        @Transactional void createOrder(String orderId);
    }

    // Implementing the interface (real Spring beans implement their interfaces or rely on CGLIB)
    static class OrderServiceImpl implements OrderServiceInterface {
        // The actual implementation without proxy logic
        @Override
        @Transactional
        public void saveOrderAndSendEmail(String orderId) {
            System.out.println("      [Impl] saveOrder: " + orderId);
            // GOTCHA: If we called sendConfirmationEmail(orderId) here via this,
            // the proxy would NOT intercept that inner call.
        }

        @Override
        @Transactional(readOnly = true)
        public void sendConfirmationEmail(String orderId) {
            System.out.println("      [Impl] sendEmail: " + orderId);
        }

        @Override
        @Transactional
        public void createOrder(String orderId) {
            System.out.println("      [Impl] createOrder: " + orderId);
        }
    }

    // =========================================================
    // 2. N+1 QUERY PROBLEM (Hibernate/JPA)
    // =========================================================
    //
    // THE PROBLEM:
    //   You load 100 Order records (1 query), then access order.getCustomer()
    //   for each order (100 additional queries) → 1 + 100 = 101 queries total.
    //   This is the N+1 problem: 1 query to load N items + N queries to load their associations.
    //
    // WHY IT HAPPENS:
    //   JPA/Hibernate default: @ManyToOne and @OneToMany are LAZY loaded.
    //   getCustomer() → proxy → triggers SQL: "SELECT * FROM customers WHERE id = ?"
    //   One SQL per customer access = N additional queries.
    //
    // HOW TO DETECT:
    //   Enable query logging: spring.jpa.show-sql=true, spring.jpa.properties.hibernate.format_sql=true
    //   Use hibernate-statistics or tools like P6Spy, datasource-proxy.
    //
    // FIXES:
    //   FIX 1: JPQL JOIN FETCH: "SELECT o FROM Order o JOIN FETCH o.customer"
    //     → Single query with JOIN, fetches all data at once. Best for most cases.
    //   FIX 2: @EntityGraph: @EntityGraph(attributePaths = {"customer"}) on repository method
    //     → Declarative JOIN FETCH, no JPQL needed.
    //   FIX 3: @BatchSize(size = 20): Hibernate fetches related entities in batches.
    //     → 1 + ceil(N/20) queries instead of 1 + N queries. Good compromise.
    //   FIX 4: DTO projection with constructor query:
    //     "SELECT new OrderDTO(o.id, c.name) FROM Order o JOIN o.customer c"
    //     → Only fetches needed columns. Best for read-only views.
    //
    // DEMO: Simulate the problem and solutions with mock queries.

    record Customer(long id, String name) {}
    record Order(long id, String product, long customerId) {}
    record OrderDTO(long orderId, String product, String customerName) {} // DTO projection

    static int queryCount = 0; // count queries to demonstrate the N+1 problem

    static List<Order> findAllOrders() {
        queryCount++;
        System.out.println("      [DB] SELECT * FROM orders (query #" + queryCount + ")");
        // Simulate 5 orders
        return List.of(
            new Order(1, "Laptop", 1), new Order(2, "Phone", 2),
            new Order(3, "Tablet", 1), new Order(4, "Monitor", 3),
            new Order(5, "Keyboard", 2)
        );
    }

    static Customer findCustomerById(long id) {
        queryCount++;
        System.out.println("      [DB] SELECT * FROM customers WHERE id=" + id + " (query #" + queryCount + ")");
        return switch ((int)id) {
            case 1 -> new Customer(1, "Alice");
            case 2 -> new Customer(2, "Bob");
            case 3 -> new Customer(3, "Carol");
            default -> null;
        };
    }

    static List<OrderDTO> findAllOrdersWithCustomerJoinFetch() {
        queryCount++;
        System.out.println("      [DB] SELECT o.id, o.product, c.name FROM orders o JOIN customers c ON o.customer_id=c.id (query #" + queryCount + ") [JOIN FETCH FIX]");
        return List.of(
            new OrderDTO(1, "Laptop", "Alice"), new OrderDTO(2, "Phone", "Bob"),
            new OrderDTO(3, "Tablet", "Alice"), new OrderDTO(4, "Monitor", "Carol"),
            new OrderDTO(5, "Keyboard", "Bob")
        );
    }

    // =========================================================
    // 3. AOP INTERNALS — Proxy-based AOP (JDK Proxy + InvocationHandler)
    // =========================================================
    //
    // HOW SPRING AOP WORKS:
    //   1. Spring scans for @Aspect classes and @Around/@Before/@After advice methods.
    //   2. For each bean that matches a pointcut expression, Spring creates a PROXY.
    //      - If bean implements an interface: JDK dynamic proxy (Proxy.newProxyInstance)
    //      - If bean doesn't implement interface: CGLIB (creates a subclass at runtime)
    //   3. The proxy is registered in ApplicationContext instead of the real bean.
    //   4. All external calls to the bean go through the proxy.
    //   5. Proxy intercepts → runs advice → calls real method → runs more advice.
    //
    // JDK PROXY vs CGLIB:
    //   JDK Proxy: works only for interfaces. Uses java.lang.reflect.Proxy.
    //     Spring default for beans implementing interfaces.
    //   CGLIB (Code Generation Library): creates runtime subclass of target class.
    //     Works without interfaces. Spring default when proxyTargetClass=true.
    //     LIMITATION: can't proxy final classes or final methods.
    //     GOTCHA: Don't make your @Service classes final when using Spring AOP.
    //
    // PERFORMANCE:
    //   JDK Proxy: reflection-based, slower per-call. Simple, no bytecode generation.
    //   CGLIB: generates bytecode, compiles to native, fast after JIT warms up.
    //   Modern Java (JDK 9+) with strong encapsulation: CGLIB faces --add-opens requirements.

    // Simulating @Transactional-like AOP via InvocationHandler
    static class AopProxy implements InvocationHandler {
        private final Object target;
        private final List<String> callLog = new ArrayList<>();

        AopProxy(Object target) { this.target = target; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // BEFORE ADVICE (like @Before or the "before" part of @Around)
            System.out.println("    [AOP] >>> ENTERING: " + method.getName() +
                Arrays.toString(args != null ? args : new Object[]{}));
            callLog.add("ENTER:" + method.getName());

            long start = System.nanoTime();
            try {
                // PROCEED (call the real method — like ProceedingJoinPoint.proceed())
                Object result = method.invoke(target, args);

                // AFTER RETURNING ADVICE
                long elapsed = System.nanoTime() - start;
                System.out.println("    [AOP] <<< RETURNING: " + method.getName() +
                    " elapsed=" + elapsed / 1000 + "µs result=" + result);
                callLog.add("RETURN:" + method.getName());
                return result;

            } catch (InvocationTargetException e) {
                // AFTER THROWING ADVICE
                System.out.println("    [AOP] !!! THROWING: " + method.getName() +
                    " cause=" + e.getCause().getMessage());
                callLog.add("THROW:" + method.getName());
                throw e.getCause(); // unwrap InvocationTargetException
            }
        }

        List<String> getCallLog() { return Collections.unmodifiableList(callLog); }
    }

    interface CalculatorService {
        int add(int a, int b);
        int divide(int a, int b);
    }

    static class CalculatorServiceImpl implements CalculatorService {
        @Override public int add(int a, int b) { return a + b; }
        @Override public int divide(int a, int b) {
            if (b == 0) throw new ArithmeticException("Division by zero");
            return a / b;
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T createAopProxy(T target, Class<?>... interfaces) {
        AopProxy handler = new AopProxy(target);
        T proxy = (T) Proxy.newProxyInstance(
            target.getClass().getClassLoader(), interfaces, handler);
        return proxy;
    }

    // =========================================================
    // 4. @Async GOTCHAS
    // =========================================================
    //
    // GOTCHA 1: SELF-INVOCATION (same as @Transactional)
    //   Calling an @Async method from within the same class bypasses the proxy.
    //   Solution: inject self or move to another @Service.
    //
    // GOTCHA 2: EXCEPTION SWALLOWING
    //   If return type is void: exceptions are silently swallowed!
    //   AsyncUncaughtExceptionHandler must be configured to catch them.
    //   If return type is Future<T>: exceptions are captured in the Future.
    //     get() throws ExecutionException wrapping the original exception.
    //   If return type is CompletableFuture<T>: exceptions propagate via thenApply/handle.
    //
    // GOTCHA 3: ThreadLocal doesn't propagate
    //   @Async creates a new thread from the thread pool.
    //   ThreadLocal values from the calling thread are NOT present.
    //   Solution: TaskDecorator — Spring's hook to copy context to async thread.
    //   Example: copy SecurityContextHolder, MDC logging context, request attributes.
    //
    // DEMO: Show exception behavior with Callable (simulates CompletableFuture<T> pattern)

    static void demonstrateAsyncPatterns() throws Exception {
        System.out.println("\n--- @Async Patterns ---");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // GOTCHA 2: void async — exception swallowed (simulated)
        System.out.println("  Void async — exception silently lost:");
        Future<?> voidFuture = executor.submit(() -> {
            System.out.println("    Async task starting on thread: " + Thread.currentThread().getName());
            throw new RuntimeException("This exception is swallowed if return type is void!");
        });
        try {
            voidFuture.get();
        } catch (ExecutionException e) {
            System.out.println("    Exception captured in Future: " + e.getCause().getMessage());
        }

        // GOTCHA 3: ThreadLocal not propagated
        ThreadLocal<String> requestId = new ThreadLocal<>();
        requestId.set("REQ-123");
        System.out.println("\n  ThreadLocal propagation:");
        System.out.println("    Calling thread context: " + requestId.get());

        Future<String> contextFuture = executor.submit(() -> {
            String ctx = requestId.get(); // NULL in async thread!
            return "Async thread context: " + ctx;
        });
        System.out.println("    " + contextFuture.get()); // "Async thread context: null"

        // FIX: Capture value before submitting, pass explicitly
        String capturedId = requestId.get(); // capture before async
        Future<String> fixedFuture = executor.submit(() ->
            "Fixed: context explicitly passed: " + capturedId);
        System.out.println("    " + fixedFuture.get());

        executor.shutdown();
        requestId.remove(); // cleanup ThreadLocal
    }

    // =========================================================
    // 5. BEAN LIFECYCLE — Manual simulation
    // =========================================================
    //
    // FULL SPRING BEAN LIFECYCLE:
    //   1. Instantiate (call constructor)
    //   2. Inject dependencies (@Autowired, @Value)
    //   3. BeanNameAware.setBeanName()
    //   4. BeanFactoryAware.setBeanFactory()
    //   5. ApplicationContextAware.setApplicationContext()
    //   6. BeanPostProcessor.postProcessBeforeInitialization()
    //   7. @PostConstruct methods
    //   8. InitializingBean.afterPropertiesSet()
    //   9. Custom init-method
    //  10. BeanPostProcessor.postProcessAfterInitialization() ← THIS IS WHERE AOP PROXIES ARE CREATED
    //      (Spring's AbstractAutoProxyCreator is a BeanPostProcessor)
    //  11. Bean is ready for use
    //  ...
    //  N. @PreDestroy methods (on container shutdown)
    //  N+1. DisposableBean.destroy()
    //  N+2. Custom destroy-method
    //
    // KEY INSIGHT: AOP proxy creation happens in step 10 (BeanPostProcessor.postProcessAfterInitialization).
    //   The real bean is created first, then the proxy wraps it.
    //   That's why self-calls on `this` bypass the proxy — `this` is the real bean, not the proxy.

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface PostConstruct {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface PreDestroy {}

    interface BeanPostProcessor {
        default Object postProcessBeforeInitialization(Object bean, String beanName) { return bean; }
        default Object postProcessAfterInitialization(Object bean, String beanName) { return bean; }
    }

    static class DatabaseConnectionPool {
        private final String url;
        private final int maxConnections;
        private boolean initialized = false;

        DatabaseConnectionPool(String url, int maxConnections) {
            System.out.println("    [Lifecycle] 1. Constructor called");
            this.url = url;
            this.maxConnections = maxConnections;
        }

        @PostConstruct
        public void init() {
            System.out.println("    [Lifecycle] 7. @PostConstruct: initializing pool with " +
                maxConnections + " connections to " + url);
            this.initialized = true;
        }

        @PreDestroy
        public void destroy() {
            System.out.println("    [Lifecycle] N. @PreDestroy: closing all connections");
            this.initialized = false;
        }

        public boolean isInitialized() { return initialized; }
    }

    // Simplified IoC container simulating Spring lifecycle
    static class SimpleBeanContainer {
        private final Map<String, Object> beans = new LinkedHashMap<>();
        private final List<BeanPostProcessor> postProcessors = new ArrayList<>();

        void addBeanPostProcessor(BeanPostProcessor processor) {
            postProcessors.add(processor);
        }

        void registerBean(String name, Object bean) throws Exception {
            System.out.println("    [Container] Registering bean: " + name);

            // Step 6: postProcessBeforeInitialization
            Object processed = bean;
            for (BeanPostProcessor pp : postProcessors) {
                processed = pp.postProcessBeforeInitialization(processed, name);
            }

            // Step 7: @PostConstruct
            for (Method m : processed.getClass().getMethods()) {
                if (m.isAnnotationPresent(PostConstruct.class)) {
                    System.out.println("    [Container] Calling @PostConstruct: " + m.getName());
                    m.invoke(processed);
                }
            }

            // Step 10: postProcessAfterInitialization (AOP proxy creation happens here in Spring)
            for (BeanPostProcessor pp : postProcessors) {
                processed = pp.postProcessAfterInitialization(processed, name);
            }

            beans.put(name, processed);
        }

        void shutdown() throws Exception {
            System.out.println("    [Container] Shutting down...");
            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                for (Method m : entry.getValue().getClass().getMethods()) {
                    if (m.isAnnotationPresent(PreDestroy.class)) {
                        System.out.println("    [Container] Calling @PreDestroy: " + m.getName());
                        m.invoke(entry.getValue());
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        <T> T getBean(String name) { return (T) beans.get(name); }
    }

    // =========================================================
    // 6. @Cacheable INTERNALS — Simple implementation
    // =========================================================
    //
    // HOW SPRING @Cacheable WORKS:
    //   1. Spring creates a proxy for the bean with @Cacheable methods.
    //   2. Proxy intercepts the method call.
    //   3. Compute cache key (default: method params; custom: SpEL expression).
    //   4. Check cache (Redis, Caffeine, EhCache, etc.).
    //   5. CACHE HIT: return cached value, skip real method.
    //   6. CACHE MISS: call real method, store result in cache, return result.
    //
    // GOTCHA 1: Same proxy bypass issue as @Transactional.
    //   self.doWork() from within the bean bypasses @Cacheable proxy.
    // GOTCHA 2: Cache eviction: @CacheEvict must be on a method called EXTERNALLY.
    // GOTCHA 3: @Cacheable on methods returning void: pointless (nothing to cache).
    // GOTCHA 4: Cache key collision: if two different methods use same cache name
    //   and have same param values, they share a cache entry — WRONG!
    //   Fix: use explicit key="#className + #method + #param" or different cacheNames.

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Cacheable {
        String cacheName() default "default";
    }

    static class CacheableProxy implements InvocationHandler {
        private final Object target;
        // Simulated cache: cacheName → (key → value)
        private final Map<String, Map<String, Object>> cacheStore = new HashMap<>();

        CacheableProxy(Object target) { this.target = target; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Cacheable cacheable = method.getAnnotation(Cacheable.class);

            if (cacheable != null) {
                String cacheName = cacheable.cacheName();
                // Cache key: method name + args (Spring uses SpEL by default)
                String cacheKey = method.getName() + ":" + Arrays.toString(args);

                Map<String, Object> cache = cacheStore.computeIfAbsent(cacheName, k -> new HashMap<>());

                if (cache.containsKey(cacheKey)) {
                    System.out.println("    [Cache] HIT for key: " + cacheKey);
                    return cache.get(cacheKey); // return cached result, skip real method
                }

                System.out.println("    [Cache] MISS for key: " + cacheKey + " — calling real method");
                Object result = method.invoke(target, args);
                cache.put(cacheKey, result); // store in cache
                System.out.println("    [Cache] Stored result in cache: " + cacheKey);
                return result;
            }

            return method.invoke(target, args); // no @Cacheable, call directly
        }
    }

    interface ProductService {
        @Cacheable(cacheName = "products") String findProductById(int id);
        @Cacheable(cacheName = "products") List<String> findAllProducts();
    }

    static class ProductServiceImpl implements ProductService {
        private int realCallCount = 0;

        @Override
        public String findProductById(int id) {
            realCallCount++;
            System.out.println("      [Real] findProductById(" + id + ") — real call #" + realCallCount);
            return "Product-" + id;
        }

        @Override
        public List<String> findAllProducts() {
            realCallCount++;
            System.out.println("      [Real] findAllProducts — real call #" + realCallCount);
            return List.of("Product-1", "Product-2", "Product-3");
        }

        int getRealCallCount() { return realCallCount; }
    }

    // =========================================================
    // MAIN: Demo all patterns
    // =========================================================
    public static void main(String[] args) throws Exception {
        System.out.println("=== SPRING INTERNALS DEMO ===");

        System.out.println("\n--- 1. @Transactional Proxy (self-invocation gotcha) ---");
        {
            // External call through proxy → @Transactional works
            OrderServiceImpl realBean = new OrderServiceImpl();
            OrderServiceInterface proxy = (OrderServiceInterface) Proxy.newProxyInstance(
                realBean.getClass().getClassLoader(),
                new Class[]{OrderServiceInterface.class},
                new TransactionalProxy(realBean)
            );

            System.out.println("External call through proxy:");
            proxy.createOrder("ORD-001"); // proxy intercepts → TX applied

            System.out.println("\nSelf-call demo (would bypass proxy in real Spring):");
            System.out.println("  [NOTE] In Spring: if saveOrderAndSendEmail calls this.sendConfirmationEmail,");
            System.out.println("  the proxy is bypassed. The inner call runs WITHOUT a transaction.");
            System.out.println("  FIX: Extract sendConfirmationEmail to a separate @Service class.");
        }

        System.out.println("\n--- 2. N+1 Query Problem ---");
        {
            queryCount = 0;
            System.out.println("BROKEN (N+1):");
            List<Order> orders = findAllOrders(); // 1 query
            for (Order order : orders) {
                Customer customer = findCustomerById(order.customerId()); // N queries!
                System.out.println("      Order " + order.id() + " → " + customer.name());
            }
            System.out.println("  Total queries: " + queryCount + " (1 + N = 1 + " + orders.size() + ")");

            queryCount = 0;
            System.out.println("\nFIXED (JOIN FETCH):");
            List<OrderDTO> dtos = findAllOrdersWithCustomerJoinFetch(); // 1 query
            dtos.forEach(dto -> System.out.println("      Order " + dto.orderId() + " → " + dto.customerName()));
            System.out.println("  Total queries: " + queryCount + " (1 only!)");
        }

        System.out.println("\n--- 3. AOP Proxy Internals ---");
        {
            CalculatorServiceImpl realCalc = new CalculatorServiceImpl();
            CalculatorService proxy = (CalculatorService) Proxy.newProxyInstance(
                realCalc.getClass().getClassLoader(),
                new Class[]{CalculatorService.class},
                new AopProxy(realCalc)
            );

            System.out.println("Normal call:");
            int result = proxy.add(3, 4);
            System.out.println("  Result: " + result);

            System.out.println("\nException call:");
            try {
                proxy.divide(10, 0);
            } catch (ArithmeticException e) {
                System.out.println("  Caught: " + e.getMessage());
            }
        }

        System.out.println("\n--- 4. @Async Gotchas ---");
        demonstrateAsyncPatterns();

        System.out.println("\n--- 5. Bean Lifecycle ---");
        {
            SimpleBeanContainer container = new SimpleBeanContainer();
            container.addBeanPostProcessor(new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String name) {
                    System.out.println("    [BPP] postProcessAfterInitialization for: " + name +
                        " ← THIS IS WHERE SPRING CREATES AOP PROXY");
                    return bean; // In Spring: would return a proxy wrapping `bean`
                }
            });

            DatabaseConnectionPool pool = new DatabaseConnectionPool("jdbc:postgresql://localhost/mydb", 10);
            container.registerBean("connectionPool", pool);
            System.out.println("    Bean ready, initialized: " + container.<DatabaseConnectionPool>getBean("connectionPool").isInitialized());
            container.shutdown();
        }

        System.out.println("\n--- 6. @Cacheable Proxy ---");
        {
            ProductServiceImpl realService = new ProductServiceImpl();
            CacheableProxy handler = new CacheableProxy(realService);
            ProductService proxy = (ProductService) Proxy.newProxyInstance(
                realService.getClass().getClassLoader(),
                new Class[]{ProductService.class},
                handler
            );

            System.out.println("First calls (cache miss):");
            proxy.findProductById(1);
            proxy.findProductById(2);
            proxy.findAllProducts();

            System.out.println("Second calls (cache hit — real method NOT called):");
            proxy.findProductById(1); // HIT
            proxy.findProductById(2); // HIT
            proxy.findAllProducts();  // HIT

            System.out.println("Real method called " + realService.getRealCallCount() + " times (only 3, not 6)");
        }

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: @Transactional self-invocation — explain the bug and fix.");
        System.out.println("A: Spring creates a CGLIB/JDK Proxy around your bean. External calls");
        System.out.println("   go through the proxy (which adds transaction logic). `this` inside");
        System.out.println("   the bean = real object, not proxy. Self-calls bypass the proxy.");
        System.out.println("   Fix: inject self, use AspectJ weaving, or extract to separate bean.");
        System.out.println();
        System.out.println("Q: How does @Cacheable decide the cache key?");
        System.out.println("A: Default: all method parameters. SpEL for custom keys.");
        System.out.println("   @Cacheable(key = \"#userId\") — use only userId as key.");
        System.out.println("   @Cacheable(condition = \"#id > 0\") — conditional caching.");
        System.out.println("   GOTCHA: null return values — Spring caches null by default.");
        System.out.println("   Use unless = \"#result == null\" to avoid caching nulls.");
        System.out.println();
        System.out.println("Q: JDK Proxy vs CGLIB?");
        System.out.println("A: JDK Proxy: needs interface, uses java.lang.reflect.Proxy.");
        System.out.println("   CGLIB: generates a subclass, works without interface.");
        System.out.println("   Spring uses CGLIB by default (proxyTargetClass=true in recent versions).");
        System.out.println("   GOTCHA: CGLIB can't proxy final classes or final methods.");
        System.out.println("   Don't make your @Service or @Repository classes final!");
    }
}
