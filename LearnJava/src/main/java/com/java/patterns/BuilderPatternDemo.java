package com.java.patterns;

/**
 * BUILDER PATTERN — Constructing Complex Objects Step by Step
 *
 * PROBLEM IT SOLVES:
 *   When a class has many optional parameters, you face the "telescoping constructor" problem:
 *     DatabaseConfig(host, port)
 *     DatabaseConfig(host, port, username)
 *     DatabaseConfig(host, port, username, password)
 *     DatabaseConfig(host, port, username, password, poolSize)
 *     ... (exponential combinations)
 *
 *   Alternatively, using setters: object is mutable and can be in invalid state mid-construction.
 *
 *   Builder Pattern: separate the construction from the representation.
 *   Mandatory fields → required constructor parameters on Builder.
 *   Optional fields → chained setter methods returning 'this' (fluent API).
 *   build() → validates all fields, then constructs the immutable final object.
 *
 * REAL-WORLD USES IN JAVA:
 *   - StringBuilder (Java's own Builder pattern)
 *   - Lombok's @Builder annotation generates this automatically
 *   - HttpRequest.newBuilder() in Java 11 HTTP Client
 *   - AlertDialog.Builder in Android
 *   - Many Spring configuration classes use a Builder pattern
 *
 * INTERVIEW FREQUENCY: Medium. Asked more at companies using Lombok/Spring heavily.
 *
 * COMMON INTERVIEW QUESTIONS:
 *   1. "What is the difference between Builder and Factory?"
 *      Factory: creates in one step, hides which subtype is returned.
 *      Builder: creates step by step, assembles complex object from parts.
 *   2. "How does Lombok's @Builder work?"
 *      It generates an inner static Builder class with a fluent API at compile time.
 *   3. "What is the advantage of making the product immutable?"
 *      Thread safety — immutable objects can be shared freely without synchronization.
 *   4. "Why put the Builder as a static inner class?"
 *      It can access the outer class's private constructor directly.
 */
public class BuilderPatternDemo {

    public static void main(String[] args) {
        System.out.println("=== Builder Pattern: DatabaseConfig ===");

        // Fluent API: readable, no need to remember parameter order, all optional fields explicit
        DatabaseConfig config1 = new DatabaseConfig.Builder("localhost", 5432)
                .username("admin")
                .password("secret")
                .poolSize(10)
                .connectionTimeout(3000)
                .ssl(true)
                .build();
        System.out.println("Config 1: " + config1);

        // Minimal config: only required fields
        DatabaseConfig config2 = new DatabaseConfig.Builder("prod-db.example.com", 3306)
                .username("appuser")
                .password("prod-secret")
                .build();
        System.out.println("Config 2: " + config2);

        // Validation error: missing required username
        System.out.println("\n=== Validation Test ===");
        try {
            DatabaseConfig invalid = new DatabaseConfig.Builder("localhost", 5432)
                    .build(); // Should fail: username not set
        } catch (IllegalStateException e) {
            System.out.println("Caught expected error: " + e.getMessage());
        }

        // Validation error: invalid pool size
        try {
            DatabaseConfig invalid = new DatabaseConfig.Builder("localhost", 5432)
                    .username("admin")
                    .password("pwd")
                    .poolSize(-1) // Invalid
                    .build();
        } catch (IllegalArgumentException e) {
            System.out.println("Caught expected error: " + e.getMessage());
        }

        System.out.println("\n=== Immutability Check ===");
        // config1 cannot be modified after build() — no setters, all fields are final
        System.out.println("host: " + config1.getHost());
        System.out.println("port: " + config1.getPort());
        System.out.println("poolSize: " + config1.getPoolSize());
    }

    // =========================================================================
    // The PRODUCT — DatabaseConfig
    // =========================================================================

    /**
     * Immutable database configuration object.
     * ALL fields are final — set once in the constructor, never changed.
     * No public setters — prevents partial/invalid state after construction.
     *
     * This is thread-safe without synchronization because it's immutable.
     */
    static final class DatabaseConfig {
        // Mandatory fields (must be provided)
        private final String host;
        private final int port;

        // Optional fields (have sensible defaults in Builder)
        private final String username;
        private final String password;
        private final int poolSize;
        private final int connectionTimeout; // milliseconds
        private final boolean ssl;

        // Private constructor — only the inner Builder can call it
        private DatabaseConfig(Builder builder) {
            this.host              = builder.host;
            this.port              = builder.port;
            this.username          = builder.username;
            this.password          = builder.password;
            this.poolSize          = builder.poolSize;
            this.connectionTimeout = builder.connectionTimeout;
            this.ssl               = builder.ssl;
        }

        // Getters (read-only access) — no setters
        public String getHost()            { return host; }
        public int    getPort()            { return port; }
        public String getUsername()        { return username; }
        public int    getPoolSize()        { return poolSize; }
        public int    getConnectionTimeout(){ return connectionTimeout; }
        public boolean isSsl()             { return ssl; }

        @Override
        public String toString() {
            return String.format(
                "DatabaseConfig{host='%s', port=%d, username='%s', poolSize=%d, timeout=%dms, ssl=%b}",
                host, port, username, poolSize, connectionTimeout, ssl
            );
        }

        // =====================================================================
        // The BUILDER — static inner class
        // =====================================================================

        /**
         * Fluent builder for DatabaseConfig.
         *
         * MANDATORY fields are in the Builder constructor — compile-time enforcement.
         * OPTIONAL fields have defaults set below.
         *
         * FLUENT API: Each setter returns 'this' (the Builder), enabling method chaining:
         *   builder.username("x").password("y").ssl(true).build()
         */
        static class Builder {
            // Mandatory fields (set via constructor)
            private final String host;
            private final int port;

            // Optional fields with default values
            private String username;
            private String password;
            private int poolSize          = 5;    // Default: 5 connections
            private int connectionTimeout  = 5000; // Default: 5 seconds
            private boolean ssl           = false; // Default: no SSL

            /**
             * Builder constructor — only mandatory fields here.
             * If host or port are invalid, fail fast at the start.
             */
            public Builder(String host, int port) {
                if (host == null || host.isBlank()) {
                    throw new IllegalArgumentException("Host cannot be null or blank");
                }
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
                }
                this.host = host;
                this.port = port;
            }

            // Fluent setters — each returns 'this' for chaining

            public Builder username(String username) {
                this.username = username;
                return this; // Return Builder for chaining
            }

            public Builder password(String password) {
                this.password = password;
                return this;
            }

            public Builder poolSize(int poolSize) {
                if (poolSize < 1) throw new IllegalArgumentException("Pool size must be >= 1, got: " + poolSize);
                this.poolSize = poolSize;
                return this;
            }

            public Builder connectionTimeout(int millis) {
                if (millis < 0) throw new IllegalArgumentException("Timeout cannot be negative");
                this.connectionTimeout = millis;
                return this;
            }

            public Builder ssl(boolean ssl) {
                this.ssl = ssl;
                return this;
            }

            /**
             * Validates all fields, then constructs the immutable DatabaseConfig.
             *
             * WHY VALIDATE HERE AND NOT IN SETTERS?
             *   Some validations require cross-field checks (e.g., if ssl=true, port should be 5433).
             *   Also: setters could be called in any order, so full validation only makes sense
             *   when all fields are known (at build time).
             *
             * @throws IllegalStateException if required optional fields are missing
             */
            public DatabaseConfig build() {
                // Cross-field validations
                if (username == null || username.isBlank()) {
                    throw new IllegalStateException("Username is required");
                }
                if (password == null || password.isBlank()) {
                    throw new IllegalStateException("Password is required");
                }

                // Construct immutable product
                return new DatabaseConfig(this);
            }
        }
    }
}
