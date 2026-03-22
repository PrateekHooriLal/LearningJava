package com.java.concurrency.advanced;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.*;

/**
 * =============================================================================
 * CONCEPT: Reactive Streams — Java 9+ Flow API
 * =============================================================================
 *
 * Reactive Streams is a specification for asynchronous stream processing with
 * NON-BLOCKING BACKPRESSURE. Backpressure is the key concept — it allows the
 * consumer (Subscriber) to control the rate at which the producer (Publisher)
 * emits data, preventing the consumer from being overwhelmed.
 *
 * THE 4 INTERFACES (java.util.concurrent.Flow):
 * ─────────────────────────────────────────────────────────────────────────────
 * Publisher<T>:      Produces items. Must call onSubscribe, then onNext*, then onComplete/onError.
 * Subscriber<T>:     Consumes items. Calls subscription.request(n) to demand items.
 * Subscription:      Contract between Publisher and Subscriber. request(n) and cancel().
 * Processor<T,R>:    Both Subscriber<T> and Publisher<R> — transforms items (like a pipe).
 *
 * THE REACTIVE MANIFESTO:
 *   Reactive systems are: Responsive, Resilient, Elastic, Message-Driven.
 *   Reactive streams specifically address the Message-Driven + backpressure aspect.
 *
 * BACKPRESSURE EXPLAINED:
 *   Without backpressure: Publisher emits 1M items/s, Subscriber processes 100k/s.
 *   → Buffer fills → OutOfMemoryError (classic producer-consumer overflow).
 *   With backpressure: Subscriber says "give me 100 items" → processes them →
 *   "give me another 100" → Publisher waits → no overflow.
 *   → Like a TCP flow control window, or a supply chain with order triggers.
 *
 * CF vs REACTIVE STREAMS:
 * ┌────────────────────────────────────────────────────────────────┐
 * │ CompletableFuture              │ Reactive Streams (Flow)       │
 * ├────────────────────────────────────────────────────────────────┤
 * │ 0 or 1 async result            │ 0..N async results            │
 * │ Simple API                     │ More complex API              │
 * │ Built-in Java                  │ Need Project Reactor/RxJava   │
 * │ Good for single value async    │ Good for event streams        │
 * │ CF.allOf for fan-out           │ Flux.merge() for fan-out      │
 * └────────────────────────────────────────────────────────────────┘
 *
 * PRODUCTION IMPLEMENTATIONS:
 *   - Project Reactor: Mono<T> (0-1 items), Flux<T> (0-N items)
 *     Used by Spring WebFlux for reactive web
 *   - RxJava: Observable, Single, Completable, Maybe
 *     Used by Android development, Netflix backend
 *   - Akka Streams: Actor-based streams (Scala/Java)
 *     Used for large-scale event processing
 *
 * WHY REACTIVE (the real motivation):
 *   Traditional: 1 thread per request, thread blocks on I/O (DB query, HTTP call).
 *   With 10k concurrent requests: 10k OS threads × 512KB stack = 5GB RAM.
 *   Reactive: non-blocking I/O, a few threads handle millions of concurrent connections.
 *   Key: thread is never idle waiting for I/O — it processes other work.
 *   Same model as Node.js event loop, nginx, but in Java.
 *
 * NOTE ON JAVA'S FLOW API:
 *   Java 9 added Flow as a minimal interface set (no implementations).
 *   SubmissionPublisher is the only provided implementation.
 *   For real use: Project Reactor or RxJava implement Flow interfaces.
 *   This file implements from scratch for educational clarity.
 *
 * TIME COMPLEXITY: O(N) for N items; backpressure controls throughput
 * SPACE COMPLEXITY: O(B) where B = buffer size (controlled by subscriber)
 *
 * INTERVIEW ANGLE:
 *   "What is backpressure and why does it matter?"
 *   "How does Spring WebFlux differ from Spring MVC?"
 *   "When would you choose reactive over thread-per-request?"
 *
 * REAL-WORLD USE:
 *   - Spring WebFlux HTTP servers (reactive REST APIs)
 *   - Reactive database drivers (R2DBC)
 *   - Kafka consumer with backpressure
 *   - Real-time dashboards (streaming data to browser via SSE/WebSocket)
 * =============================================================================
 */
public class ReactiveStreamsIntro {

    // =========================================================================
    // SIMPLE INTEGER PUBLISHER
    // =========================================================================

    /**
     * A Publisher that emits integers from `start` to `end` (inclusive).
     *
     * PUBLISHER CONTRACT (Flow specification):
     *   1. Call subscriber.onSubscribe(subscription) — must be first call.
     *   2. Only emit items when subscription.request(n) has been called.
     *   3. Emit at most n items per request(n) call.
     *   4. Call onComplete() when done, or onError() on failure.
     *   5. After onComplete/onError: no more calls to subscriber.
     *
     * KEY INSIGHT: Publisher does not push — it responds to pull (request).
     * This inversion of control is what enables backpressure.
     *
     * GOTCHA: Never call onNext after onComplete/onError.
     * Never call onNext without a pending demand from request(n).
     * Violating these is a spec violation and leads to buggy behavior.
     */
    public static class IntegerPublisher implements Publisher<Integer> {
        private final int start;
        private final int end;

        public IntegerPublisher(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            // Create a new Subscription for this subscriber
            // Each subscribe() call gets its own independent subscription
            subscriber.onSubscribe(new IntegerSubscription(start, end, subscriber));
        }

        // =====================================================================
        // SUBSCRIPTION (the demand-supply contract between Publisher and Subscriber)
        // =====================================================================

        /**
         * Manages the flow of data between publisher and one subscriber.
         *
         * KEY INSIGHT: The Subscription is the "pipe" between them.
         *   - Subscriber calls request(n): "I can handle n more items"
         *   - Subscription tracks how many items are owed and delivers them
         *   - Subscriber calls cancel(): "I don't want more items"
         *
         * THREAD SAFETY: request() can be called from any thread.
         * We use AtomicLong for demand tracking to handle concurrent calls.
         *
         * OVERFLOW PROTECTION: If subscriber calls request(MAX_VALUE), we
         * cap at MAX_VALUE to prevent overflow (spec requirement).
         */
        private static class IntegerSubscription implements Subscription {
            private final AtomicInteger current;  // next item to emit
            private final int end;
            private final Subscriber<? super Integer> subscriber;
            private final AtomicLong demand = new AtomicLong(0);  // pending requests
            private volatile boolean cancelled = false;
            private final ExecutorService executor = Executors.newSingleThreadExecutor(
                    r -> { Thread t = new Thread(r, "publisher-thread"); t.setDaemon(true); return t; }
            );

            IntegerSubscription(int start, int end, Subscriber<? super Integer> subscriber) {
                this.current = new AtomicInteger(start);
                this.end = end;
                this.subscriber = subscriber;
            }

            /**
             * Subscriber requests n more items (backpressure control).
             *
             * KEY INSIGHT: request() is NON-BLOCKING — it queues work.
             * The actual emission happens on a separate thread (executor).
             * This allows the subscriber's thread to continue processing while
             * more items are being prepared.
             *
             * SPEC: request(n) with n <= 0 must throw IllegalArgumentException.
             *
             * @param n Number of items subscriber can accept (must be > 0)
             */
            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException(
                            "request(n) requires n > 0, got: " + n));
                    return;
                }
                if (cancelled) return;

                // Accumulate demand atomically (multiple request() calls add up)
                // Cap at Long.MAX_VALUE to prevent overflow
                demand.getAndUpdate(current -> {
                    long sum = current + n;
                    return sum < 0 ? Long.MAX_VALUE : sum;  // overflow protection
                });

                // Emit items on a background thread (non-blocking for caller)
                executor.execute(this::emit);
            }

            /**
             * Emit items up to the current demand.
             *
             * PATTERN: consume demand atomically, emit each item.
             * The getAndDecrement loop ensures exactly 1 item per demand unit.
             */
            private void emit() {
                while (!cancelled && current.get() <= end && demand.get() > 0) {
                    demand.decrementAndGet();
                    int item = current.getAndIncrement();
                    if (item <= end) {
                        subscriber.onNext(item);  // deliver item
                    }
                }
                if (!cancelled && current.get() > end) {
                    subscriber.onComplete();      // all items delivered
                    executor.shutdown();
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
                executor.shutdown();
                System.out.println("  [Publisher] Subscription cancelled");
            }
        }
    }

    // =========================================================================
    // SUBSCRIBER WITH BACKPRESSURE
    // =========================================================================

    /**
     * A Subscriber that demonstrates controlled backpressure.
     *
     * SUBSCRIBER CONTRACT:
     *   1. onSubscribe(subscription): called once. Store the subscription.
     *      Call subscription.request(n) to start receiving items.
     *   2. onNext(item): called for each item (up to requested demand).
     *      Process item, optionally call request(n) for more.
     *   3. onError(throwable): terminal. No more items. Handle/log the error.
     *   4. onComplete(): terminal. No more items. Cleanup.
     *
     * BATCHING PATTERN: Request N items at a time (batch size).
     *   After processing N items, request N more.
     *   This is the common "pull-in-batches" backpressure pattern.
     *   Like a DB cursor fetching pages.
     *
     * REAL-WORLD: Spring WebFlux reactor subscribers request(Long.MAX_VALUE) for hot
     * producers (or a batch size for cold producers backed by slow I/O).
     */
    public static class BackpressureSubscriber implements Subscriber<Integer> {
        private final String name;
        private final int batchSize;          // items to request per batch
        private Subscription subscription;
        private final List<Integer> received = new ArrayList<>();
        private int processedCount = 0;
        private final CountDownLatch completeLatch = new CountDownLatch(1);

        public BackpressureSubscriber(String name, int batchSize) {
            this.name = name;
            this.batchSize = batchSize;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            System.out.printf("  [%s] Subscribed, requesting first batch of %d%n", name, batchSize);
            // CRITICAL: Must call request() to start receiving items.
            // Without request(), publisher never calls onNext (backpressure!).
            subscription.request(batchSize);
        }

        @Override
        public void onNext(Integer item) {
            received.add(item);
            processedCount++;

            // Simulate processing time for every item
            // (In reality, this might be a DB write, HTTP call, etc.)
            try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // When we've processed a full batch, request another batch
            // This is the backpressure loop: process N, request N more
            if (processedCount % batchSize == 0) {
                System.out.printf("  [%s] Processed %d items, requesting %d more%n",
                        name, processedCount, batchSize);
                subscription.request(batchSize);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.printf("  [%s] ERROR: %s%n", name, throwable.getMessage());
            completeLatch.countDown();
        }

        @Override
        public void onComplete() {
            System.out.printf("  [%s] Completed! Total received: %d%n", name, received.size());
            completeLatch.countDown();
        }

        public void awaitCompletion(long timeoutMs) throws InterruptedException {
            completeLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        }

        public List<Integer> getReceived() { return Collections.unmodifiableList(received); }
    }

    // =========================================================================
    // PROCESSOR: transforms items (Publisher + Subscriber combined)
    // =========================================================================

    /**
     * A Processor that multiplies each integer by a factor.
     *
     * Processor<T,R> = Subscriber<T> + Publisher<R>
     * It receives items from an upstream Publisher and emits transformed items
     * to a downstream Subscriber.
     *
     * MENTAL MODEL: Like a function in the middle of a stream pipeline.
     *   Publisher → Processor(x → x*2) → Subscriber
     *   Comparable to .map() in Stream API, but async and with backpressure.
     *
     * GOTCHA: Processor must handle backpressure from BOTH sides:
     *   - From downstream: subscriber requests N items from the processor
     *   - To upstream: processor requests N items from its upstream publisher
     *   - Naive implementation: just pass requests through. Complex implementations buffer.
     *
     * This uses Java's SubmissionPublisher for simplicity in the publish half.
     */
    public static class MultiplyProcessor
            extends SubmissionPublisher<Integer>
            implements Processor<Integer, Integer> {

        private final int factor;
        private Subscription upstreamSubscription;

        public MultiplyProcessor(int factor) {
            super();  // SubmissionPublisher handles the Publisher<Integer> side
            this.factor = factor;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.upstreamSubscription = subscription;
            // Request items from upstream (pass-through demand)
            subscription.request(Long.MAX_VALUE);  // for simplicity, request all
            System.out.printf("  [Processor x%d] subscribed to upstream%n", factor);
        }

        @Override
        public void onNext(Integer item) {
            // Transform: multiply by factor, submit to all downstream subscribers
            int transformed = item * factor;
            submit(transformed);  // SubmissionPublisher.submit() handles backpressure to downstream
        }

        @Override
        public void onError(Throwable throwable) {
            closeExceptionally(throwable);  // propagate error to downstream
        }

        @Override
        public void onComplete() {
            close();  // close SubmissionPublisher → triggers onComplete on downstream
            System.out.printf("  [Processor x%d] complete%n", factor);
        }
    }

    // =========================================================================
    // DEMO: SubmissionPublisher (Java's built-in Publisher)
    // =========================================================================

    /**
     * Java 9's SubmissionPublisher — the standard Publisher implementation.
     *
     * It handles:
     *   - Multiple subscribers
     *   - Backpressure via bounded async buffers
     *   - Executor for async delivery
     *   - Thread-safe submit()
     *
     * submit(item) blocks if any subscriber's buffer is full (backpressure!).
     * offer(item, timeout) is non-blocking — drops item on timeout.
     *
     * Common use: push events to multiple subscribers (pub/sub within JVM).
     */
    private static void demoSubmissionPublisher() throws Exception {
        System.out.println("\n=== SubmissionPublisher Demo ===");

        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()) {
            // Subscriber 1: fast (requests in bulk)
            publisher.subscribe(new Subscriber<>() {
                Subscription sub;
                int count = 0;
                @Override public void onSubscribe(Subscription s) {
                    this.sub = s; s.request(Long.MAX_VALUE);
                    System.out.println("  Fast subscriber ready");
                }
                @Override public void onNext(String item) {
                    count++;
                    System.out.println("  Fast received: " + item);
                    sub.request(1);
                }
                @Override public void onError(Throwable t) { System.out.println("  Fast error: " + t); }
                @Override public void onComplete() { System.out.println("  Fast done, received: " + count); }
            });

            // Emit items
            for (int i = 1; i <= 5; i++) {
                System.out.println("[Publisher] submitting item " + i);
                publisher.submit("Item-" + i);
                Thread.sleep(20);
            }

            publisher.close();  // signals onComplete to all subscribers
            Thread.sleep(200);  // let subscribers process remaining items
        }
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.out.println("=== Reactive Streams (Java 9 Flow API) Demo ===\n");

        // DEMO 1: Custom Publisher + Backpressure Subscriber
        System.out.println("=== DEMO 1: Custom Publisher with Backpressure ===");
        IntegerPublisher publisher = new IntegerPublisher(1, 20);
        BackpressureSubscriber subscriber = new BackpressureSubscriber("SlowConsumer", 5);
        publisher.subscribe(subscriber);
        subscriber.awaitCompletion(5000);
        System.out.println("Received items: " + subscriber.getReceived());

        // DEMO 2: Processor (transform pipeline)
        System.out.println("\n=== DEMO 2: Processor (map transform) ===");
        IntegerPublisher sourcePublisher = new IntegerPublisher(1, 10);
        MultiplyProcessor processor = new MultiplyProcessor(3);  // x * 3
        BackpressureSubscriber finalSubscriber = new BackpressureSubscriber("FinalSub", 5);

        // Wire the pipeline: source → processor → subscriber
        sourcePublisher.subscribe(processor);   // processor subscribes to source
        processor.subscribe(finalSubscriber);   // finalSubscriber subscribes to processor

        finalSubscriber.awaitCompletion(5000);
        System.out.println("Transformed items (×3): " + finalSubscriber.getReceived());
        // Expected: [3, 6, 9, 12, 15, 18, 21, 24, 27, 30]

        // DEMO 3: SubmissionPublisher
        demoSubmissionPublisher();

        System.out.println("\n=== Reactive Streams Key Concepts ===");
        System.out.println("Publisher    : emits items on demand (pull-based)");
        System.out.println("Subscriber   : requests items (controls rate via request(n))");
        System.out.println("Subscription : contract — request(n) = give me n items");
        System.out.println("Processor    : both subscriber and publisher (transformation)");
        System.out.println("Backpressure : subscriber drives rate, prevents OOM");

        System.out.println("\n=== Production Libraries ===");
        System.out.println("Project Reactor (Spring WebFlux):");
        System.out.println("  Mono<T>   : 0 or 1 item async (like CF<Optional<T>>)");
        System.out.println("  Flux<T>   : 0..N items async (like CF<Stream<T>>)");
        System.out.println("  Example   : Flux.range(1,100).map(x->x*2).filter(x->x>10).subscribe(...)");
        System.out.println("RxJava      : Observable, Single, Maybe, Completable");
        System.out.println("  Used in   : Android, Netflix backend, reactive clients");

        /*
         * INTERVIEW Q: "What is backpressure and why does it matter?"
         * A: Backpressure is the ability of a consumer to signal to the producer
         *    to slow down. Without it, a fast producer overwhelms a slow consumer,
         *    causing buffer overflow (OOM) or dropped data.
         *    In Flow API: implemented via subscription.request(n) — the subscriber
         *    only asks for what it can handle. Publisher must wait for demand.
         *    Real-world analogy: TCP flow control window, kafka consumer lag management.
         *
         * INTERVIEW Q: "When is reactive programming better than thread-per-request?"
         * A: For high-concurrency I/O bound systems (10k+ concurrent connections).
         *    Thread-per-request: 10k threads × 512KB stack = 5GB just for stacks.
         *    Reactive: ~50 threads handle 10k concurrent connections non-blocking.
         *    But: reactive code is harder to write, debug, and reason about.
         *    Java 21 virtual threads are an alternative: thread-per-request model
         *    with ~1KB heap per thread (not OS stack) — simpler code than reactive.
         *
         * INTERVIEW Q: "Explain the difference between hot and cold publishers."
         * A: Cold publisher: starts emitting only when subscribed. Each subscriber
         *    gets the full sequence independently (like a YouTube video on demand).
         *    Hot publisher: emits regardless of subscribers. Late subscribers miss
         *    past items (like a live TV broadcast).
         *    SubmissionPublisher is hot. IntegerPublisher above is cold.
         */
    }
}
