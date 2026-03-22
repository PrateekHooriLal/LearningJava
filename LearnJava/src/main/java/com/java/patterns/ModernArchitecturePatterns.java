package com.java.patterns;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * ============================================================
 * MODERN DISTRIBUTED ARCHITECTURE PATTERNS
 * ============================================================
 *
 * CONCEPT:
 *   Patterns for building scalable, resilient distributed systems.
 *   Critical knowledge for SDE 3 / Staff Engineer interviews at FAANG.
 *   These patterns address problems that emerge ONLY at scale:
 *   - Consistency vs availability tradeoffs (CAP theorem)
 *   - Partial failures (network partitions, timeouts)
 *   - Independent scaling of different workloads
 *   - Eventual consistency and compensating transactions
 *
 * TIME/SPACE:
 *   Circuit Breaker: O(1) per call check
 *   Event Sourcing: O(n) state reconstruction (n = events since last snapshot)
 *   Saga: O(n) where n = number of saga steps
 *
 * INTERVIEW ANGLE (SDE 3 / System Design):
 *   - "How do you handle distributed transactions?" → Saga Pattern
 *   - "How do you scale reads vs writes independently?" → CQRS
 *   - "How do you prevent cascade failures?" → Circuit Breaker + Bulkhead
 *   - "How do you audit every state change?" → Event Sourcing
 *   - "What's eventual consistency?" → CQRS + Event Sourcing tradeoff
 *
 * REAL-WORLD USE:
 *   CQRS: Netflix, LinkedIn for high-read social feeds
 *   Event Sourcing: banking systems (every transaction is an event)
 *   Saga: e-commerce checkout (order + payment + inventory + shipping)
 *   Circuit Breaker: Netflix Hystrix, Resilience4j, AWS SDK
 *   Bulkhead: Hystrix thread pool isolation, Resilience4j bulkhead
 */
public class ModernArchitecturePatterns {

    // =========================================================
    // 1. CQRS (Command Query Responsibility Segregation)
    // =========================================================
    //
    // CONCEPT: Separate the model for READS (queries) from WRITES (commands).
    //   Commands: change state, return void (or ID), should NOT query data.
    //   Queries: read state, should NOT mutate anything.
    //   Coined by Greg Young (2010), based on CQS principle by Bertrand Meyer.
    //
    // WHY SEPARATE?
    //   - Read and write models have different shapes: writes enforce invariants,
    //     reads often need denormalized, joined, projected data.
    //   - Independent scaling: most systems are read-heavy (10:1 or 100:1 reads/writes).
    //     Scale read replicas without scaling write master.
    //   - Different optimization: writes → normalized relational DB;
    //     reads → denormalized NoSQL / materialized views / search index (Elasticsearch).
    //
    // WHEN TO USE:
    //   - High read/write asymmetry
    //   - Complex read requirements (many joins, aggregations)
    //   - Need for multiple read representations of same data
    //
    // GOTCHA: Eventual consistency.
    //   Write model publishes an event → read model updates asynchronously.
    //   Between the write and the read model update, reads are STALE.
    //   You must tell users: "Your changes will appear shortly."
    //   Not suitable for systems requiring read-after-write consistency.

    // COMMAND SIDE
    record CreateOrderCommand(String orderId, String customerId, List<String> items, double total) {}
    record UpdateOrderStatusCommand(String orderId, String newStatus) {}

    // QUERY SIDE (different projection — optimized for reads)
    record OrderSummaryView(String orderId, String customerId, int itemCount,
                            double total, String status, String lastUpdated) {}

    interface CommandHandler<C> {
        void handle(C command);
    }

    interface QueryHandler<Q, R> {
        R handle(Q query);
    }

    // Simulated event bus (in production: Kafka, RabbitMQ)
    static class InMemoryEventBus {
        private final Map<String, List<Consumer<Object>>> handlers = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <E> void publish(String eventType, E event) {
            handlers.getOrDefault(eventType, List.of())
                .forEach(h -> h.accept(event));
        }

        public void subscribe(String eventType, Consumer<Object> handler) {
            handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        }
    }

    // WRITE MODEL: normalized storage, enforces business invariants
    static class OrderWriteModel {
        // Simulated write DB (in production: PostgreSQL master)
        private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();
        private final InMemoryEventBus eventBus;

        OrderWriteModel(InMemoryEventBus eventBus) { this.eventBus = eventBus; }

        public void createOrder(CreateOrderCommand cmd) {
            // Enforce invariants (business rules on the write side)
            if (cmd.total() < 0) throw new IllegalArgumentException("Total cannot be negative");
            if (cmd.items().isEmpty()) throw new IllegalArgumentException("Order must have items");

            Map<String, Object> order = new HashMap<>();
            order.put("orderId", cmd.orderId());
            order.put("customerId", cmd.customerId());
            order.put("items", cmd.items());
            order.put("total", cmd.total());
            order.put("status", "PENDING");
            orders.put(cmd.orderId(), order);

            // KEY INSIGHT: After write, publish domain event so read model can update.
            //   This is where eventual consistency begins.
            System.out.println("    [WriteModel] Order created: " + cmd.orderId());
            eventBus.publish("ORDER_CREATED", cmd);
        }

        public void updateStatus(UpdateOrderStatusCommand cmd) {
            Map<String, Object> order = orders.get(cmd.orderId());
            if (order == null) throw new NoSuchElementException("Order not found: " + cmd.orderId());
            order.put("status", cmd.newStatus());
            System.out.println("    [WriteModel] Status updated: " + cmd.orderId() + " → " + cmd.newStatus());
            eventBus.publish("ORDER_STATUS_UPDATED", cmd);
        }
    }

    // READ MODEL: denormalized projection, optimized for queries
    // KEY INSIGHT: This is a SEPARATE data store. In production: Redis, Elasticsearch, etc.
    static class OrderReadModel {
        private final Map<String, OrderSummaryView> views = new ConcurrentHashMap<>();

        // Subscribe to events from write model and update read projection
        void onOrderCreated(Object event) {
            CreateOrderCommand cmd = (CreateOrderCommand) event;
            views.put(cmd.orderId(), new OrderSummaryView(
                cmd.orderId(), cmd.customerId(), cmd.items().size(),
                cmd.total(), "PENDING", java.time.Instant.now().toString()
            ));
            System.out.println("    [ReadModel] Projection updated for: " + cmd.orderId());
        }

        void onStatusUpdated(Object event) {
            UpdateOrderStatusCommand cmd = (UpdateOrderStatusCommand) event;
            OrderSummaryView old = views.get(cmd.orderId());
            if (old != null) {
                views.put(cmd.orderId(), new OrderSummaryView(
                    old.orderId(), old.customerId(), old.itemCount(),
                    old.total(), cmd.newStatus(), java.time.Instant.now().toString()
                ));
            }
        }

        public Optional<OrderSummaryView> findById(String orderId) {
            return Optional.ofNullable(views.get(orderId));
        }

        public List<OrderSummaryView> findByCustomer(String customerId) {
            return views.values().stream()
                .filter(v -> v.customerId().equals(customerId))
                .toList();
        }
    }

    // =========================================================
    // 2. EVENT SOURCING
    // =========================================================
    //
    // CONCEPT: Instead of storing CURRENT STATE, store a sequence of EVENTS
    //   (immutable facts). Current state = replay of all events.
    //   "Facts, not current state."
    //
    // KEY BENEFITS:
    //   1. FULL AUDIT LOG: complete history of every change, by whom, when.
    //   2. TIME TRAVEL: rebuild state as of any point in time.
    //   3. EVENT-DRIVEN INTEGRATION: downstream services react to events.
    //   4. DEBUGGING: reproduce bugs by replaying events.
    //
    // EVENT STORE STRUCTURE:
    //   [AggregateId] → [Event1, Event2, Event3, ...EventN]
    //   Events are IMMUTABLE and APPEND-ONLY. Never update/delete events.
    //
    // SNAPSHOTS:
    //   Replaying all events from the beginning is O(n) — expensive for old aggregates.
    //   Solution: periodically save a SNAPSHOT (state at event N).
    //   Load: start from last snapshot, replay only events after it.
    //
    // GOTCHA: Schema evolution.
    //   Old events are persisted forever. If the event schema changes,
    //   old events must still be processable. Solutions:
    //   - Event versioning (v1, v2 events, upcasters/transformers)
    //   - Don't remove fields (only add). Never rename.
    //   - Snapshot regularly to reduce replay of ancient events.

    // Domain events (immutable facts — record is perfect here)
    interface DomainEvent {
        String aggregateId();
        String eventType();
        long timestamp();
    }

    record AccountCreatedEvent(String aggregateId, String owner, double initialBalance,
                               long timestamp) implements DomainEvent {
        @Override public String eventType() { return "ACCOUNT_CREATED"; }
    }

    record MoneyDepositedEvent(String aggregateId, double amount, String description,
                               long timestamp) implements DomainEvent {
        @Override public String eventType() { return "MONEY_DEPOSITED"; }
    }

    record MoneyWithdrawnEvent(String aggregateId, double amount, String description,
                               long timestamp) implements DomainEvent {
        @Override public String eventType() { return "MONEY_WITHDRAWN"; }
    }

    // EVENT STORE: append-only log of domain events
    static class EventStore {
        private final Map<String, List<DomainEvent>> streams = new ConcurrentHashMap<>();

        public void append(DomainEvent event) {
            streams.computeIfAbsent(event.aggregateId(), k -> new ArrayList<>()).add(event);
            System.out.println("    [EventStore] Stored: " + event.eventType() +
                " for aggregate: " + event.aggregateId());
        }

        public List<DomainEvent> loadEvents(String aggregateId) {
            return Collections.unmodifiableList(
                streams.getOrDefault(aggregateId, List.of())
            );
        }
    }

    // AGGREGATE: reconstructs state by replaying events
    // KEY INSIGHT: apply(event) methods are deterministic — given same events,
    //   always produce same state. No side effects in apply().
    static class BankAccount {
        private String id;
        private String owner;
        private double balance;
        private int eventVersion = 0;

        // Reconstruct from event stream
        static BankAccount reconstruct(List<DomainEvent> events) {
            BankAccount account = new BankAccount();
            for (DomainEvent event : events) {
                account.apply(event);
                account.eventVersion++;
            }
            return account;
        }

        private void apply(DomainEvent event) {
            switch (event) {
                case AccountCreatedEvent e -> {
                    this.id = e.aggregateId();
                    this.owner = e.owner();
                    this.balance = e.initialBalance();
                }
                case MoneyDepositedEvent e -> this.balance += e.amount();
                case MoneyWithdrawnEvent e -> this.balance -= e.amount();
                default -> throw new IllegalArgumentException("Unknown event: " + event.eventType());
            }
        }

        @Override public String toString() {
            return "BankAccount{id=" + id + ", owner=" + owner +
                ", balance=" + balance + ", version=" + eventVersion + "}";
        }
    }

    // =========================================================
    // 3. SAGA PATTERN
    // =========================================================
    //
    // CONCEPT: Manage data consistency across microservices without 2PC
    //   (two-phase commit, which is synchronous and blocks resources).
    //   Saga = sequence of LOCAL transactions. Each step publishes an event.
    //   On failure: execute COMPENSATING transactions to undo completed steps.
    //
    // TWO SAGA STYLES:
    //   CHOREOGRAPHY: services react to events from each other.
    //     + Decoupled, no central coordinator
    //     - Hard to track overall saga state, complex failure scenarios
    //   ORCHESTRATION: central coordinator (saga orchestrator) sends commands.
    //     + Explicit flow, easy to monitor and debug
    //     - Coordinator can become a bottleneck (but not a DB transaction bottleneck)
    //     PREFER for complex multi-step sagas.
    //
    // COMPENSATING TRANSACTIONS:
    //   NOT a rollback (data has changed — other services may have seen it).
    //   A compensation is a FORWARD action that undoes the business effect.
    //   E.g., step 1 "reserve inventory" → compensation "release inventory".
    //   Compensations must be idempotent (can be called multiple times safely).
    //
    // GOTCHA: Idempotency.
    //   Network can deliver a message multiple times. Each saga step must be
    //   idempotent: processing the same command twice = same result.
    //   Use idempotency keys / check-and-set with version numbers.

    enum SagaStatus { STARTED, COMPLETED, COMPENSATING, COMPENSATED, FAILED }

    record SagaStep(String name, Runnable action, Runnable compensation) {}

    // ORCHESTRATION-style Saga
    static class SagaOrchestrator {
        private final List<SagaStep> steps = new ArrayList<>();
        private final Deque<SagaStep> completedSteps = new ArrayDeque<>();
        private SagaStatus status = SagaStatus.STARTED;

        public SagaOrchestrator addStep(String name, Runnable action, Runnable compensation) {
            steps.add(new SagaStep(name, action, compensation));
            return this; // fluent
        }

        public SagaStatus execute() {
            System.out.println("    [Saga] Starting...");
            for (SagaStep step : steps) {
                try {
                    System.out.println("    [Saga] Executing: " + step.name());
                    step.action().run();
                    completedSteps.push(step); // track for potential compensation
                } catch (Exception e) {
                    System.out.println("    [Saga] FAILED at: " + step.name() + " → " + e.getMessage());
                    compensate();
                    return status;
                }
            }
            status = SagaStatus.COMPLETED;
            System.out.println("    [Saga] COMPLETED successfully");
            return status;
        }

        // Execute compensating transactions in REVERSE order
        // KEY INSIGHT: Reverse order is critical — undo last thing first,
        //   like a stack of transactions.
        private void compensate() {
            status = SagaStatus.COMPENSATING;
            System.out.println("    [Saga] Starting compensation (reverse order)...");
            while (!completedSteps.isEmpty()) {
                SagaStep step = completedSteps.pop();
                try {
                    System.out.println("    [Saga] Compensating: " + step.name());
                    step.compensation().run();
                } catch (Exception e) {
                    System.out.println("    [Saga] Compensation FAILED for: " + step.name());
                    // In production: alert ops team, manual intervention needed
                    status = SagaStatus.FAILED;
                    return;
                }
            }
            status = SagaStatus.COMPENSATED;
            System.out.println("    [Saga] COMPENSATED successfully");
        }

        public SagaStatus getStatus() { return status; }
    }

    // =========================================================
    // 4. CIRCUIT BREAKER
    // =========================================================
    //
    // CONCEPT: Prevent cascade failures. When a downstream service is failing,
    //   stop calling it (open the circuit) to give it time to recover.
    //   Named after electrical circuit breakers that protect wiring.
    //
    // THREE STATES:
    //   CLOSED: normal operation. Calls pass through. Count failures.
    //   OPEN: failures exceeded threshold. REJECT all calls immediately (fail fast).
    //         No calls reach the failing service → service can recover.
    //   HALF_OPEN: after timeout, allow ONE probe call. If success → CLOSED.
    //              If failure → back to OPEN.
    //
    // STATE TRANSITIONS:
    //   CLOSED →(failures >= threshold)→ OPEN
    //   OPEN →(timeout elapsed)→ HALF_OPEN
    //   HALF_OPEN →(success)→ CLOSED
    //   HALF_OPEN →(failure)→ OPEN
    //
    // WHEN TO USE:
    //   All calls to external services (databases, APIs, message queues).
    //   Especially critical in microservices where one failure can cascade.
    //
    // REAL JAVA LIBRARIES:
    //   Netflix Hystrix (deprecated), Resilience4j (current standard), Polly (.NET)
    //
    // GOTCHA: Don't use for expected errors (e.g., 404 Not Found).
    //   Only trip the breaker on systemic failures (timeout, 500, connection refused).
    //   Also: Circuit breaker should be per-dependency, not global.

    enum CircuitBreakerState { CLOSED, OPEN, HALF_OPEN }

    static class CircuitBreaker {
        private final String name;
        private final int failureThreshold;       // failures before OPEN
        private final long openTimeoutMs;          // how long to stay OPEN

        private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile long openedAt = 0;

        CircuitBreaker(String name, int failureThreshold, long openTimeoutMs) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.openTimeoutMs = openTimeoutMs;
        }

        public <T> T call(Supplier<T> action, Supplier<T> fallback) {
            switch (state) {
                case OPEN -> {
                    // Check if timeout has elapsed → try HALF_OPEN
                    if (System.currentTimeMillis() - openedAt > openTimeoutMs) {
                        System.out.println("    [CB:" + name + "] OPEN→HALF_OPEN (probing...)");
                        state = CircuitBreakerState.HALF_OPEN;
                    } else {
                        System.out.println("    [CB:" + name + "] OPEN: fast-fail, using fallback");
                        return fallback.get();
                    }
                }
                case HALF_OPEN -> {
                    System.out.println("    [CB:" + name + "] HALF_OPEN: trying probe call...");
                }
                case CLOSED -> {} // normal, proceed
            }

            try {
                T result = action.get();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                System.out.println("    [CB:" + name + "] Call failed: " + e.getMessage());
                return fallback.get();
            }
        }

        private void onSuccess() {
            if (state == CircuitBreakerState.HALF_OPEN) {
                state = CircuitBreakerState.CLOSED;
                failureCount.set(0);
                System.out.println("    [CB:" + name + "] HALF_OPEN→CLOSED (service recovered!)");
            } else {
                failureCount.set(0); // reset on success
            }
        }

        private void onFailure() {
            if (state == CircuitBreakerState.HALF_OPEN) {
                state = CircuitBreakerState.OPEN;
                openedAt = System.currentTimeMillis();
                System.out.println("    [CB:" + name + "] HALF_OPEN→OPEN (probe failed)");
            } else {
                int failures = failureCount.incrementAndGet();
                if (failures >= failureThreshold) {
                    state = CircuitBreakerState.OPEN;
                    openedAt = System.currentTimeMillis();
                    System.out.println("    [CB:" + name + "] CLOSED→OPEN (threshold=" +
                        failureThreshold + " reached)");
                }
            }
        }

        public CircuitBreakerState getState() { return state; }
    }

    // =========================================================
    // 5. BULKHEAD PATTERN
    // =========================================================
    //
    // CONCEPT: Isolate elements of an application into pools so that
    //   if one fails, the others continue to function.
    //   Named after ship bulkheads: compartments isolate flooding.
    //
    // TWO IMPLEMENTATIONS:
    //   1. THREAD POOL ISOLATION: separate thread pools per downstream service.
    //      If ServiceA's pool exhausts (all threads busy), ServiceB's pool unaffected.
    //      Used by Hystrix.
    //   2. SEMAPHORE ISOLATION: limit concurrent calls to a service.
    //      Lighter than thread pools. Used by Resilience4j.
    //
    // WHEN TO USE:
    //   - Multiple consumers of shared resources
    //   - One slow consumer should not starve others
    //   - Critical paths need guaranteed capacity
    //
    // BULKHEAD vs CIRCUIT BREAKER:
    //   Circuit Breaker: detects failure and stops calls → recovery
    //   Bulkhead: limits concurrent load → prevents overload in first place
    //   Use BOTH together: Bulkhead first (limit load), Circuit Breaker second (detect failure)
    //
    // GOTCHA: Setting bulkhead size too small → legitimate traffic rejected.
    //   Too large → doesn't protect. Tune based on load testing and service SLAs.

    static class BulkheadService {
        private final String name;
        private final Semaphore semaphore;

        BulkheadService(String name, int maxConcurrentCalls) {
            this.name = name;
            this.semaphore = new Semaphore(maxConcurrentCalls);
        }

        // KEY INSIGHT: tryAcquire(timeout) — if semaphore is full, reject fast.
        //   This prevents thread starvation when the downstream service is slow.
        public <T> T call(Supplier<T> action, Supplier<T> fallback) {
            boolean acquired = false;
            try {
                acquired = semaphore.tryAcquire(100, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    System.out.println("    [Bulkhead:" + name + "] REJECTED: at capacity (" +
                        (semaphore.availablePermits()) + " permits left)");
                    return fallback.get();
                }
                System.out.println("    [Bulkhead:" + name + "] Executing (" +
                    semaphore.availablePermits() + " permits remaining)");
                return action.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return fallback.get();
            } finally {
                if (acquired) semaphore.release();
            }
        }

        public int getAvailablePermits() { return semaphore.availablePermits(); }
    }

    // =========================================================
    // MAIN: Demo all patterns
    // =========================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. CQRS PATTERN ===");
        {
            InMemoryEventBus bus = new InMemoryEventBus();
            OrderWriteModel writeModel = new OrderWriteModel(bus);
            OrderReadModel readModel = new OrderReadModel();

            // Wire read model to event bus (in production: Kafka consumer group)
            bus.subscribe("ORDER_CREATED", readModel::onOrderCreated);
            bus.subscribe("ORDER_STATUS_UPDATED", readModel::onStatusUpdated);

            writeModel.createOrder(new CreateOrderCommand(
                "ORD-001", "CUST-1", List.of("Item A", "Item B"), 99.99));
            writeModel.updateStatus(new UpdateOrderStatusCommand("ORD-001", "SHIPPED"));

            readModel.findById("ORD-001").ifPresent(v ->
                System.out.println("    [ReadModel] Query result: " + v));
        }

        System.out.println("\n=== 2. EVENT SOURCING ===");
        {
            EventStore store = new EventStore();
            String accountId = "ACC-001";

            // Append events (immutable facts)
            store.append(new AccountCreatedEvent(accountId, "Alice", 1000.0,
                System.currentTimeMillis()));
            store.append(new MoneyDepositedEvent(accountId, 500.0, "Salary",
                System.currentTimeMillis()));
            store.append(new MoneyWithdrawnEvent(accountId, 200.0, "Rent",
                System.currentTimeMillis()));
            store.append(new MoneyDepositedEvent(accountId, 100.0, "Bonus",
                System.currentTimeMillis()));

            // Reconstruct state from events (time travel possible!)
            List<DomainEvent> events = store.loadEvents(accountId);
            BankAccount account = BankAccount.reconstruct(events);
            System.out.println("    Reconstructed state: " + account);
            // Expected balance: 1000 + 500 - 200 + 100 = 1400
        }

        System.out.println("\n=== 3. SAGA PATTERN (Orchestration) ===");
        {
            System.out.println("-- Happy path (all steps succeed) --");
            SagaOrchestrator happySaga = new SagaOrchestrator()
                .addStep("Reserve Inventory",
                    () -> System.out.println("      ✓ Inventory reserved"),
                    () -> System.out.println("      ↩ Inventory released"))
                .addStep("Charge Payment",
                    () -> System.out.println("      ✓ Payment charged"),
                    () -> System.out.println("      ↩ Payment refunded"))
                .addStep("Create Shipment",
                    () -> System.out.println("      ✓ Shipment created"),
                    () -> System.out.println("      ↩ Shipment cancelled"));

            SagaStatus happyResult = happySaga.execute();
            System.out.println("    Result: " + happyResult);

            System.out.println("\n-- Failure at step 3 (compensation runs) --");
            SagaOrchestrator failSaga = new SagaOrchestrator()
                .addStep("Reserve Inventory",
                    () -> System.out.println("      ✓ Inventory reserved"),
                    () -> System.out.println("      ↩ Inventory released"))
                .addStep("Charge Payment",
                    () -> System.out.println("      ✓ Payment charged"),
                    () -> System.out.println("      ↩ Payment refunded"))
                .addStep("Create Shipment",
                    () -> { throw new RuntimeException("Shipping service down"); },
                    () -> System.out.println("      ↩ Shipment cancelled"));

            SagaStatus failResult = failSaga.execute();
            System.out.println("    Result: " + failResult);
        }

        System.out.println("\n=== 4. CIRCUIT BREAKER ===");
        {
            CircuitBreaker cb = new CircuitBreaker("PaymentService", 2, 500);
            AtomicInteger callCount = new AtomicInteger(0);

            Supplier<String> action = () -> {
                int n = callCount.incrementAndGet();
                if (n <= 3) throw new RuntimeException("Service unavailable");
                return "Payment processed";
            };
            Supplier<String> fallback = () -> "Cached payment data";

            for (int i = 0; i < 5; i++) {
                String result = cb.call(action, fallback);
                System.out.println("    Call #" + (i+1) + ": '" + result +
                    "' state=" + cb.getState());
            }

            // Wait for OPEN→HALF_OPEN transition
            System.out.println("    Waiting for circuit to probe...");
            Thread.sleep(600);
            String result = cb.call(action, fallback); // now returns success
            System.out.println("    After timeout: '" + result + "' state=" + cb.getState());
        }

        System.out.println("\n=== 5. BULKHEAD PATTERN ===");
        {
            BulkheadService paymentBulkhead = new BulkheadService("Payment", 2);
            BulkheadService inventoryBulkhead = new BulkheadService("Inventory", 5);

            // Simulate concurrent calls exceeding bulkhead capacity
            for (int i = 0; i < 4; i++) {
                final int callId = i + 1;
                String result = paymentBulkhead.call(
                    () -> {
                        Thread.sleep(50);
                        return "Payment call " + callId + " done";
                    },
                    () -> "Payment service busy"
                );
                System.out.println("    Call " + callId + ": " + result);
            }

            // Inventory bulkhead is separate — still has capacity
            String invResult = inventoryBulkhead.call(
                () -> "Inventory OK",
                () -> "Inventory busy"
            );
            System.out.println("    Inventory check: " + invResult + " (isolated from Payment issues)");
        }

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: CQRS eventual consistency — how do you handle stale reads?");
        System.out.println("A: Options: (1) Accept it — show UI 'changes may take a moment'.");
        System.out.println("   (2) Read-your-writes consistency: after write, redirect to read");
        System.out.println("   model after small delay. (3) Optimistic UI update: update client");
        System.out.println("   state immediately, reconcile with server later.");
        System.out.println();
        System.out.println("Q: Event Sourcing vs CQRS?");
        System.out.println("A: Different concerns, often used together but not required.");
        System.out.println("   Event Sourcing: storage mechanism (events instead of state).");
        System.out.println("   CQRS: query/command separation. You can do CQRS without ES");
        System.out.println("   (two DB tables, one for writes, one for reads) or ES without CQRS.");
        System.out.println();
        System.out.println("Q: Why Saga instead of 2PC (two-phase commit)?");
        System.out.println("A: 2PC requires distributed locks — blocks resources across services,");
        System.out.println("   single coordinator failure stops everything, latency spikes.");
        System.out.println("   Saga: async, no locks, compensating transactions for rollback.");
        System.out.println("   Trade-off: eventual consistency vs strong consistency.");
    }
}
