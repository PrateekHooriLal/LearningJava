package com.java.spring;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

/**
 * ============================================================
 * SPRING BOOT AUTO-CONFIGURATION INTERNALS
 * ============================================================
 *
 * CONCEPT:
 *   Auto-configuration is the "magic" behind Spring Boot's zero-config startup.
 *   When you add spring-boot-starter-web to pom.xml, you never write a bean for
 *   DispatcherServlet, Jackson, Tomcat — they just appear. This file explains HOW.
 *
 * THE CORE ANSWER:
 *   @SpringBootApplication = @EnableAutoConfiguration + @ComponentScan + @Configuration
 *
 *   @EnableAutoConfiguration tells Spring Boot to:
 *   1. Read all auto-configuration class names from:
 *      - Spring Boot 2.x: META-INF/spring.factories
 *      - Spring Boot 3.x: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *   2. For each listed AutoConfiguration class: evaluate @Conditional annotations
 *   3. If ALL conditions pass: register the beans from that auto-config class
 *   4. If ANY condition fails: skip the entire auto-config class
 *
 * CONDITIONAL ANNOTATIONS (the key to understanding auto-config):
 *   @ConditionalOnClass(DataSource.class)
 *     → only register this config IF DataSource.class is on the classpath
 *     → i.e., only if you added a JDBC driver dependency
 *
 *   @ConditionalOnMissingBean(DataSource.class)
 *     → only register this config IF no DataSource bean exists yet
 *     → i.e., back off if the user already defined their own DataSource
 *
 *   @ConditionalOnProperty("spring.datasource.url")
 *     → only register if this property is set in application.properties
 *
 * THE BACK-OFF PATTERN:
 *   Every Spring Boot auto-config class uses @ConditionalOnMissingBean.
 *   This means: "only create this bean if the user hasn't already defined one."
 *   This is how YOU override Spring Boot's defaults — just define your own @Bean
 *   of the same type. Spring Boot's auto-config backs off.
 *
 * INTERVIEW ANGLE:
 *   Q: "What is @SpringBootApplication?"
 *   Q: "How does Spring Boot auto-configure DataSource without any XML?"
 *   Q: "How do you override a Spring Boot auto-configured bean?"
 *   Q: "What is the difference between @Configuration and @AutoConfiguration?"
 *   Q: "How does @ConditionalOnMissingBean work?"
 *   Q: "Where does Spring Boot read the list of auto-configs from?"
 *
 * REAL-WORLD USE:
 *   Every Spring Boot application — understanding this explains:
 *   - Why adding a dependency "just works" (classpath condition triggers)
 *   - Why your @Bean overrides the auto-configured one (ConditionalOnMissingBean)
 *   - How to write your own Spring Boot starter library
 */
public class SpringAutoConfiguration {

    public static void main(String[] args) {
        System.out.println("=== 1. @SpringBootApplication decomposed ===");
        explainSpringBootApplication();

        System.out.println("\n=== 2. AUTO-CONFIG DISCOVERY (spring.factories / .imports) ===");
        simulateAutoConfigDiscovery();

        System.out.println("\n=== 3. @Conditional EVALUATION ===");
        simulateConditionalEvaluation();

        System.out.println("\n=== 4. THE BACK-OFF PATTERN — overriding auto-config ===");
        demonstrateBackOff();

        System.out.println("\n=== 5. COMMON AUTO-CONFIG CLASSES (real Spring Boot) ===");
        listCommonAutoConfigs();

        System.out.println("\n=== 6. WRITING YOUR OWN STARTER ===");
        explainCustomStarter();
    }

    // =========================================================
    // SIMULATED ANNOTATIONS
    // =========================================================

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface Configuration {}

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface AutoConfiguration {}  // Spring Boot 3 — replaces @Configuration on auto-configs

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    @interface Bean {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    @interface ConditionalOnClass { String value(); }   // class name to check on classpath

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    @interface ConditionalOnMissingBean { String value() default ""; }  // bean type to check

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD})
    @interface ConditionalOnProperty { String value(); }  // property key to check

    // =========================================================
    // 1. @SpringBootApplication DECOMPOSED
    // =========================================================
    //
    // @SpringBootApplication is a COMPOSED annotation — it's 3 annotations in one:
    //
    //   @SpringBootConfiguration  (= @Configuration — marks this as config class)
    //   @EnableAutoConfiguration  (= triggers auto-config loading)
    //   @ComponentScan            (= scans current package and subpackages for @Component)
    //
    // So when you write:
    //   @SpringBootApplication
    //   public class MyApp { public static void main(String[] args) {
    //       SpringApplication.run(MyApp.class, args);
    //   }}
    //
    // Spring does:
    //   1. ComponentScan from MyApp's package downward → registers your @Service/@Repository beans
    //   2. EnableAutoConfiguration → loads and evaluates all auto-config classes
    //   3. Configuration → MyApp class itself can have @Bean methods
    //
    // INTERVIEW Q: "What does @SpringBootApplication do?"
    // A: It's a composed annotation combining @Configuration, @ComponentScan,
    //    and @EnableAutoConfiguration. ComponentScan registers your beans;
    //    EnableAutoConfiguration loads Spring Boot's auto-configs from
    //    spring.factories/AutoConfiguration.imports and conditionally registers
    //    framework beans (DataSource, DispatcherServlet, Jackson, etc.).

    static void explainSpringBootApplication() {
        System.out.println("  @SpringBootApplication = 3 annotations:");
        System.out.println("    @SpringBootConfiguration  → marks class as @Configuration");
        System.out.println("    @ComponentScan            → scans current package for @Component");
        System.out.println("    @EnableAutoConfiguration  → loads all auto-config classes");
        System.out.println();
        System.out.println("  Execution order in SpringApplication.run():");
        System.out.println("    1. Create ApplicationContext");
        System.out.println("    2. Load @Configuration + @ComponentScan beans (YOUR beans)");
        System.out.println("    3. Load auto-configuration classes from imports file");
        System.out.println("    4. Evaluate @Conditional on each auto-config");
        System.out.println("    5. Register beans that pass all conditions");
        System.out.println("    6. Refresh context (see SpringBeanLifecycle.java)");
    }

    // =========================================================
    // 2. AUTO-CONFIG DISCOVERY
    // =========================================================
    //
    // Spring Boot 2.x:
    //   File: META-INF/spring.factories (inside spring-boot-autoconfigure.jar)
    //   Format:
    //     org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
    //       org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
    //       org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
    //       org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration,\
    //       ... (140+ entries)
    //
    // Spring Boot 3.x (changed to fix startup performance):
    //   File: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    //   Format: one class per line (cleaner, faster to parse)
    //
    // LOADING MECHANISM:
    //   AutoConfigurationImportSelector.selectImports() reads this file
    //   and returns the class names to be loaded as @Configuration sources.
    //   SpringFactoriesLoader (Boot 2) / ImportCandidates (Boot 3) does the file reading.
    //
    // INTERVIEW Q: "Where does Spring Boot get the list of auto-configurations?"
    // A: From META-INF/spring.factories (Boot 2) or
    //    META-INF/spring/AutoConfiguration.imports (Boot 3) inside each starter JAR.
    //    @EnableAutoConfiguration triggers AutoConfigurationImportSelector which
    //    reads these files and adds the listed classes as @Configuration sources.

    // Simulated auto-config registry (like spring.factories)
    static final List<String> AUTO_CONFIG_CLASSES = List.of(
        "DataSourceAutoConfiguration",
        "JpaAutoConfiguration",
        "WebMvcAutoConfiguration",
        "JacksonAutoConfiguration",
        "SecurityAutoConfiguration",
        "CacheAutoConfiguration",
        "RedisAutoConfiguration"
    );

    static void simulateAutoConfigDiscovery() {
        System.out.println("  Contents of AutoConfiguration.imports (simulated):");
        AUTO_CONFIG_CLASSES.forEach(c ->
            System.out.println("    org.springframework.boot.autoconfigure." + c));
        System.out.println();
        System.out.println("  These are loaded by AutoConfigurationImportSelector");
        System.out.println("  Each is evaluated against @Conditional annotations");
        System.out.println("  Only those passing ALL conditions are registered");
    }

    // =========================================================
    // 3. @Conditional EVALUATION
    // =========================================================
    //
    // Each auto-config class has @Conditional annotations that act as GUARDS.
    // If ANY condition fails → the entire class is SKIPPED (not registered).
    //
    // EVALUATION ORDER matters:
    //   1. @ConditionalOnClass     — fastest check (classpath scan)
    //   2. @ConditionalOnProperty  — read from Environment (application.properties)
    //   3. @ConditionalOnBean      — check existing beans (slower)
    //   4. @ConditionalOnMissingBean — check absence of beans (slowest)
    //
    // REAL EXAMPLE — DataSourceAutoConfiguration:
    //   @Configuration
    //   @ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
    //   @ConditionalOnMissingBean(DataSource.class)
    //   @EnableConfigurationProperties(DataSourceProperties.class)
    //   public class DataSourceAutoConfiguration { ... }
    //
    //   Reads as:
    //   "Register a DataSource bean ONLY IF:
    //    - DataSource class is on the classpath (jdbc jar present)
    //    - No DataSource bean already defined by user"
    //
    // HOW @ConditionalOnClass works internally:
    //   Uses ASM (bytecode reader — same as component scanning) to check
    //   if the class exists in the classpath WITHOUT actually loading it.
    //   This is critical for startup performance — avoids ClassNotFoundException.
    //
    // INTERVIEW Q: "What happens if I add spring-boot-starter-data-jpa but don't
    //               configure a DataSource?"
    // A: DataSourceAutoConfiguration finds DataSource.class on the classpath
    //    (from the JPA starter) and tries to auto-configure one. If no
    //    spring.datasource.url is set and no embedded DB is present, it will
    //    fail with a BeanCreationException at startup.
    //    Fix: either configure a DataSource, add H2 (embedded), or exclude:
    //    @SpringBootApplication(exclude = DataSourceAutoConfiguration.class)

    // Simulates the classpath and bean registry for condition evaluation
    static final Set<String> CLASSPATH   = Set.of("DataSource", "Jackson", "Tomcat");
    static final Set<String> USER_BEANS  = Set.of(); // user hasn't defined any beans yet
    static final Map<String, String> PROPERTIES = Map.of(
        "spring.datasource.url", "jdbc:h2:mem:test",
        "spring.cache.type",     "redis"
    );

    static void simulateConditionalEvaluation() {
        System.out.println("  Classpath: " + CLASSPATH);
        System.out.println("  User-defined beans: " + (USER_BEANS.isEmpty() ? "(none)" : USER_BEANS));
        System.out.println();

        evaluateAutoConfig("DataSourceAutoConfiguration",
            () -> CLASSPATH.contains("DataSource"),           // @ConditionalOnClass
            () -> !USER_BEANS.contains("DataSource"),         // @ConditionalOnMissingBean
            () -> PROPERTIES.containsKey("spring.datasource.url") // @ConditionalOnProperty
        );

        evaluateAutoConfig("JacksonAutoConfiguration",
            () -> CLASSPATH.contains("Jackson"),
            () -> !USER_BEANS.contains("ObjectMapper"),
            null
        );

        evaluateAutoConfig("SecurityAutoConfiguration",
            () -> CLASSPATH.contains("SecurityFilterChain"), // NOT on classpath
            null,
            null
        );

        evaluateAutoConfig("RedisAutoConfiguration",
            () -> CLASSPATH.contains("RedisClient"),         // NOT on classpath
            null,
            null
        );
    }

    static void evaluateAutoConfig(String name,
                                    BooleanSupplier onClass,
                                    BooleanSupplier onMissingBean,
                                    BooleanSupplier onProperty) {
        boolean passes = true;
        StringBuilder reason = new StringBuilder();

        if (onClass != null && !onClass.getAsBoolean()) {
            passes = false; reason.append("@ConditionalOnClass FAILED");
        }
        if (passes && onMissingBean != null && !onMissingBean.getAsBoolean()) {
            passes = false; reason.append("@ConditionalOnMissingBean FAILED");
        }
        if (passes && onProperty != null && !onProperty.getAsBoolean()) {
            passes = false; reason.append("@ConditionalOnProperty FAILED");
        }

        System.out.println("  " + name + ": " + (passes ? "✓ REGISTERED" : "✗ SKIPPED — " + reason));
    }

    // =========================================================
    // 4. THE BACK-OFF PATTERN — overriding auto-config
    // =========================================================
    //
    // RULE: Every Spring Boot auto-config uses @ConditionalOnMissingBean.
    //       This means YOU always win — your beans take priority.
    //
    // To override any Spring Boot auto-configured bean:
    //   Simply define your own @Bean of the same type in a @Configuration class.
    //   Spring Boot's auto-config will see it via @ConditionalOnMissingBean and BACK OFF.
    //
    // EXAMPLE — Custom ObjectMapper (Jackson):
    //   JacksonAutoConfiguration is:
    //     @ConditionalOnMissingBean(ObjectMapper.class)
    //   So if you define:
    //     @Bean ObjectMapper objectMapper() { return new ObjectMapper()...; }
    //   Spring Boot's default ObjectMapper is NOT created.
    //
    // EXAMPLE — Custom DataSource:
    //   @Bean
    //   @ConfigurationProperties("app.datasource")
    //   public DataSource dataSource() { return DataSourceBuilder.create().build(); }
    //   → DataSourceAutoConfiguration backs off completely.
    //
    // EXAMPLE — Exclude entirely:
    //   @SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
    //   → Useful when you have the JAR on classpath but don't want auto-config at all.
    //
    // INTERVIEW Q: "How do you override a Spring Boot auto-configured bean?"
    // A: Define your own @Bean of the same type in a @Configuration class.
    //    Auto-configuration uses @ConditionalOnMissingBean, so your bean
    //    takes precedence and the auto-configured one is never created.
    //    Alternatively, use @SpringBootApplication(exclude=...) to skip entirely.

    static void demonstrateBackOff() {
        System.out.println("  Scenario: user defines their own DataSource @Bean");
        System.out.println();

        Set<String> userDefinedBeans = Set.of("DataSource"); // user has their own

        boolean dataSourceAutoConfigRegistered =
            CLASSPATH.contains("DataSource")                       // OnClass: pass
            && !userDefinedBeans.contains("DataSource");           // OnMissingBean: FAIL

        System.out.println("  DataSourceAutoConfiguration registered: "
            + dataSourceAutoConfigRegistered);  // false — backed off
        System.out.println("  → Spring Boot's DataSource BACKED OFF.");
        System.out.println("  → User's DataSource @Bean is used instead.");
        System.out.println();
        System.out.println("  Ways to override auto-configuration:");
        System.out.println("    1. Define your own @Bean of the same type (back-off pattern)");
        System.out.println("    2. Set spring.*.* properties (many auto-configs are property-driven)");
        System.out.println("    3. @SpringBootApplication(exclude = SomeAutoConfig.class)");
        System.out.println("    4. spring.autoconfigure.exclude=... in application.properties");
    }

    // =========================================================
    // 5. COMMON AUTO-CONFIG CLASSES AND WHAT THEY DO
    // =========================================================

    static void listCommonAutoConfigs() {
        System.out.println("  Key Spring Boot auto-configuration classes:");
        System.out.println();
        System.out.printf("  %-45s %s%n", "Auto-Config Class", "Condition → Registers");
        System.out.printf("  %-45s %s%n", "-".repeat(45), "-".repeat(40));

        String[][] configs = {
            {"DataSourceAutoConfiguration",
             "@ConditionalOnClass(DataSource) → DataSource, DataSourceProperties"},
            {"JpaAutoConfiguration",
             "@ConditionalOnClass(JpaRepository) → EntityManagerFactory, JpaTransactionManager"},
            {"WebMvcAutoConfiguration",
             "@ConditionalOnClass(DispatcherServlet) → DispatcherServlet, ViewResolvers"},
            {"JacksonAutoConfiguration",
             "@ConditionalOnClass(ObjectMapper) → ObjectMapper with defaults"},
            {"SecurityAutoConfiguration",
             "@ConditionalOnClass(AuthenticationManager) → SecurityFilterChain"},
            {"TransactionAutoConfiguration",
             "@ConditionalOnClass(PlatformTransactionManager) → @Transactional support"},
            {"CacheAutoConfiguration",
             "@ConditionalOnClass(CacheManager) → CacheManager (Caffeine/Redis/etc)"},
            {"RedisAutoConfiguration",
             "@ConditionalOnClass(RedisClient) → RedisTemplate, StringRedisTemplate"},
            {"AopAutoConfiguration",
             "@ConditionalOnClass(Advice) → @EnableAspectJAutoProxy"},
        };

        for (String[] c : configs) {
            System.out.printf("  %-45s %s%n", c[0], c[1]);
        }

        System.out.println();
        System.out.println("  To see all active auto-configs at startup:");
        System.out.println("    Set: logging.level.org.springframework.boot.autoconfigure=DEBUG");
        System.out.println("    Or:  --debug flag → prints ConditionEvaluationReport");
    }

    // =========================================================
    // 6. WRITING YOUR OWN STARTER
    // =========================================================
    //
    // A Spring Boot starter = two Maven modules:
    //   1. my-library-autoconfigure  — contains the @AutoConfiguration class + conditions
    //   2. my-library-spring-boot-starter — thin POM that depends on both the library + autoconfigure
    //
    // Structure of the autoconfigure module:
    //   src/main/resources/META-INF/spring/
    //     org.springframework.boot.autoconfigure.AutoConfiguration.imports
    //     (contains: com.example.MyLibraryAutoConfiguration)
    //
    //   @AutoConfiguration               // Spring Boot 3 — signals this is auto-config
    //   @ConditionalOnClass(MyService.class)
    //   @EnableConfigurationProperties(MyProperties.class)
    //   public class MyLibraryAutoConfiguration {
    //       @Bean
    //       @ConditionalOnMissingBean
    //       public MyService myService(MyProperties props) {
    //           return new MyService(props.getApiKey());
    //       }
    //   }
    //
    //   @ConfigurationProperties(prefix = "my.library")
    //   public class MyProperties {
    //       private String apiKey;
    //       // getters/setters
    //   }
    //
    // USER adds to pom.xml: <dependency>my-library-spring-boot-starter</dependency>
    // USER adds to application.properties: my.library.api-key=abc123
    // → Auto-config fires, MyService bean appears in context. Zero config needed.
    //
    // INTERVIEW Q: "How would you create a reusable Spring Boot component for your team?"
    // A: Create a Spring Boot starter. Write an @AutoConfiguration class with
    //    @ConditionalOnClass + @ConditionalOnMissingBean guards, register it in
    //    AutoConfiguration.imports, and publish as a Maven artifact.
    //    Teams add one dependency and the beans auto-configure themselves.

    static void explainCustomStarter() {
        System.out.println("  Custom Starter structure:");
        System.out.println("    my-starter/");
        System.out.println("      my-library-autoconfigure/");
        System.out.println("        src/main/java/MyLibraryAutoConfiguration.java");
        System.out.println("        src/main/java/MyProperties.java");
        System.out.println("        src/main/resources/META-INF/spring/");
        System.out.println("          ...AutoConfiguration.imports  ← registers the auto-config");
        System.out.println("      my-library-spring-boot-starter/");
        System.out.println("        pom.xml  ← depends on library + autoconfigure");
        System.out.println();
        System.out.println("  Key annotations on auto-config class:");
        System.out.println("    @AutoConfiguration                  ← Boot 3 (was @Configuration)");
        System.out.println("    @ConditionalOnClass(MyService.class) ← only if lib is on classpath");
        System.out.println("    @ConditionalOnMissingBean            ← back off if user defined own");
        System.out.println("    @EnableConfigurationProperties       ← bind application.properties");
    }
}

/*
 * ============================================================
 * INTERVIEW CHEAT SHEET — Spring Boot Auto-Configuration
 * ============================================================
 *
 * @SpringBootApplication =
 *   @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
 *
 * AUTO-CONFIG DISCOVERY:
 *   Boot 2: META-INF/spring.factories (key = EnableAutoConfiguration)
 *   Boot 3: META-INF/spring/AutoConfiguration.imports (one class per line)
 *   Loaded by: AutoConfigurationImportSelector
 *
 * CONDITIONAL ANNOTATIONS (evaluation order):
 *   @ConditionalOnClass        → class on classpath? (fast — ASM, no classloading)
 *   @ConditionalOnMissingClass → class NOT on classpath?
 *   @ConditionalOnProperty     → property set in application.properties?
 *   @ConditionalOnBean         → bean of type exists?
 *   @ConditionalOnMissingBean  → bean of type NOT yet defined?
 *   @ConditionalOnWebApplication → running as web app?
 *
 * THE BACK-OFF PATTERN:
 *   All auto-configs use @ConditionalOnMissingBean.
 *   You override by defining your own @Bean of the same type.
 *   Or: @SpringBootApplication(exclude = SomeAutoConfig.class)
 *   Or: spring.autoconfigure.exclude= in application.properties
 *
 * HOW @ConditionalOnClass works:
 *   Uses ASM bytecode reader (not ClassLoader) — no ClassNotFoundException risk.
 *   Checks class file existence without loading the class.
 *
 * DEBUGGING AUTO-CONFIG:
 *   Run with --debug flag or set:
 *   logging.level.org.springframework.boot.autoconfigure=DEBUG
 *   → prints ConditionEvaluationReport showing matched/unmatched conditions
 *
 * ONE-LINER ANSWERS:
 *   "How does Spring Boot know to create a DataSource?"
 *   → DataSource.class is on classpath → DataSourceAutoConfiguration conditions pass
 *      → DataSource bean auto-registered from application.properties values
 *
 *   "How do you stop Spring Boot from auto-configuring something?"
 *   → Define your own bean (back-off) OR exclude the auto-config class
 *
 *   "What's in spring.factories?"
 *   → List of AutoConfiguration class names to load, keyed by EnableAutoConfiguration
 */
