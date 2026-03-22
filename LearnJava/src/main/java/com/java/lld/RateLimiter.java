package com.java.lld;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * =============================================================================
 * CONCEPT: Rate Limiter — 3 Classic Algorithms
 * =============================================================================
 *
 * A rate limiter controls how many requests a client can make in a given time
 * window. It protects downstream services from overload and enforces SLA quotas.
 *
 * THREE ALGORITHMS COMPARED:
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. TOKEN BUCKET
 *    - Tokens accumulate at `rate` tokens/second up to `capacity`.
 *    - Each request consumes 1 token.
 *    - Allows BURSTING: if bucket is full, you can fire `capacity` requests
 *      instantly. Long-term average is still capped at `rate`.
 *    - Used by: AWS API Gateway, GCP Cloud Endpoints, tc (Linux traffic control)
 *
 * 2. SLIDING WINDOW LOG
 *    - Store timestamps of all requests in a deque.
 *    - On each request, discard entries older than `windowMs`.
 *    - Allow if deque.size() < limit.
 *    - Precise: no boundary burst problem.
 *    - Memory: O(limit) per client — fine for low limits, bad for limit=1M.
 *
 * 3. FIXED WINDOW COUNTER
 *    - Divide time into fixed windows (e.g., each second).
 *    - Count requests in current window. Reset when window expires.
 *    - Simple, O(1) space. But has the DOUBLE-RATE BOUNDARY BUG:
 *      If limit=100/window and a window starts at T=0, a client can fire
 *      100 requests at T=0.99s and 100 more at T=1.01s → 200 requests in
 *      200ms. Classic FAANG interview gotcha.
 *
 * WHEN TO USE:
 *    - API gateways, login throttling, payment endpoints, scraping prevention
 *    - Token bucket when you want to allow short bursts but cap long-term rate
 *    - Sliding window when fairness/precision is critical
 *    - Fixed window when simplicity and atomicity matter (Redis INCR+EXPIRE)
 *
 * TIME COMPLEXITY:
 *    - Token Bucket: O(1) per allow()
 *    - Sliding Window: O(k) worst case where k = expired entries to clean,
 *      amortized O(1) because each timestamp is added and removed once
 *    - Fixed Window: O(1) per allow()
 *
 * SPACE COMPLEXITY:
 *    - Token Bucket: O(1)
 *    - Sliding Window: O(limit) — stores up to `limit` timestamps
 *    - Fixed Window: O(1)
 *
 * INTERVIEW ANGLE:
 *    "Design a rate limiter for a public REST API"
 *    → Start with requirements: per-user or global? What limit? Burst ok?
 *    → Mention all 3 algorithms with tradeoffs
 *    → Discuss distributed version: Redis atomic ops
 *
 * REAL-WORLD USE:
 *    - AWS API Gateway: token bucket per stage/method
 *    - Nginx: leaky bucket via limit_req module
 *    - Redis-Cell (redis module): sliding window with token bucket hybrid
 *    - Stripe API: 100 req/s per key with burst allowance
 *
 * DISTRIBUTED RATE LIMITING (interview follow-up):
 *    - Redis INCR + EXPIRE for fixed window (atomic with Lua script)
 *    - Redis Sorted Set (ZADD timestamp, ZREMRANGEBYSCORE, ZCARD) for sliding window
 *    - Redis-Cell module: CL.THROTTLE key max_burst count_per_period period
 *    - Key challenge: clock skew across nodes, network latency in decision path
 *
 * TOKEN BUCKET vs LEAKY BUCKET:
 *    - Token bucket: requests go through immediately if tokens available → bursty
 *    - Leaky bucket: requests are queued and processed at fixed output rate → smooth
 *    - Use leaky bucket when downstream cannot handle bursts (e.g., DB writes)
 *    - Use token bucket when bursty traffic is acceptable (CDN edge cache)
 * =============================================================================
 */
public class RateLimiter {

    // =========================================================================
    // ALGORITHM 1: TOKEN BUCKET
    // =========================================================================

    /**
     * Token Bucket Rate Limiter — allows bursting up to capacity.
     *
     * KEY INSIGHT: Instead of storing actual tokens in a real bucket (which would
     * require a background refill thread), we compute how many tokens WOULD have
     * accumulated since the last request using elapsed time. This is called
     * "lazy refill" or "virtual scheduling" — O(1), no background threads needed.
     *
     * THREAD SAFETY: synchronized on `this`. For ultra-high throughput,
     * consider AtomicLong + CAS loop, but that gets complex with double arithmetic.
     *
     * GOTCHA: Use System.nanoTime() not System.currentTimeMillis().
     *   - nanoTime() is monotonic (won't jump backward on NTP sync).
     *   - currentTimeMillis() can go backward when the system clock is adjusted.
     *   - Backward time jump → negative elapsed → tokens decrease → incorrect throttling.
     */
    public static class TokenBucketRateLimiter {
        private final double capacity;       // max tokens (burst size)
        private final double refillRate;     // tokens per nanosecond
        private double currentTokens;        // current token count
        private long lastRefillNanos;        // last refill timestamp

        /**
         * @param capacity  Maximum tokens (= max burst size)
         * @param rate      Tokens added per second (= sustained rate)
         *
         * EXAMPLE: capacity=10, rate=5 means:
         *   - Can burst 10 requests instantly
         *   - Long-term sustained rate: 5 req/s
         *   - After emptying bucket, tokens refill at 5/s (full again in 2s)
         */
        public TokenBucketRateLimiter(double capacity, double rate) {
            this.capacity = capacity;
            // IMPORTANT: convert rate from per-second to per-nanosecond
            // Using nanos avoids floating point issues with millisecond granularity
            this.refillRate = rate / 1_000_000_000.0;
            this.currentTokens = capacity;  // start full (common convention)
            this.lastRefillNanos = System.nanoTime();
        }

        /**
         * Check if request is allowed. Consumes 1 token if yes.
         *
         * KEY INSIGHT: Refill before checking. This ensures tokens accumulated
         * during idle time are available for the next burst.
         *
         * COMPLEXITY: O(1) time, O(1) space
         */
        public synchronized boolean allow() {
            refill();
            if (currentTokens >= 1.0) {
                currentTokens -= 1.0;
                return true;
            }
            return false;
            // INTERVIEW Q: What if you want to support different costs per request?
            // → Change signature to allow(double cost) and consume `cost` tokens.
            // → Useful for: large uploads cost 10 tokens, simple GETs cost 1.
        }

        // KEY INSIGHT: Lazy refill — compute accumulated tokens from elapsed time.
        // This avoids a background thread for refilling.
        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            // How many tokens accumulated during elapsed nanoseconds?
            double tokensToAdd = elapsed * refillRate;
            // GOTCHA: Cap at capacity — tokens don't stack beyond bucket size
            currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
            lastRefillNanos = now;
        }

        public synchronized double getCurrentTokens() { return currentTokens; }

        @Override
        public String toString() {
            return String.format("TokenBucket[capacity=%.0f, tokens=%.2f]", capacity, currentTokens);
        }
    }

    // =========================================================================
    // ALGORITHM 2: SLIDING WINDOW LOG
    // =========================================================================

    /**
     * Sliding Window Rate Limiter — precise, no boundary burst.
     *
     * KEY INSIGHT: We maintain a sliding window of the last `windowMs` milliseconds.
     * The window moves with time — it always covers [now - windowMs, now].
     * Unlike fixed window, there's no "reset boundary" that clients can exploit.
     *
     * SPACE TRADEOFF: Must store up to `limit` timestamps per client.
     *   - For limit=1000, that's 1000 longs = 8KB per client.
     *   - For limit=1M (unlikely), that's 8MB per client — use fixed window instead.
     *
     * AMORTIZED O(1): Each timestamp is added once and removed once.
     * In the worst case (all entries expired at once), a single call does O(limit)
     * cleanup, but averaged over all calls it's O(1).
     *
     * THREAD SAFETY: synchronized on `this`
     */
    public static class SlidingWindowRateLimiter {
        private final int limit;         // max requests per window
        private final long windowMs;     // window size in milliseconds
        private final Deque<Long> timestamps;  // timestamps of recent requests

        /**
         * @param limit    Max requests allowed in the window
         * @param windowMs Window size in milliseconds
         *
         * EXAMPLE: limit=100, windowMs=1000 → max 100 req/s
         */
        public SlidingWindowRateLimiter(int limit, long windowMs) {
            this.limit = limit;
            this.windowMs = windowMs;
            this.timestamps = new ArrayDeque<>();
        }

        /**
         * Allow request if fewer than `limit` requests in the last `windowMs`.
         *
         * COMPLEXITY: O(k) worst case where k = number of expired entries,
         *             O(1) amortized
         *
         * GOTCHA: Use System.currentTimeMillis() here (not nanoTime) because
         * we're doing arithmetic with millisecond window boundaries.
         * Both are fine; just be consistent within one implementation.
         */
        public synchronized boolean allow() {
            long now = System.currentTimeMillis();
            long windowStart = now - windowMs;

            // Remove timestamps outside the current window (older than windowStart)
            // IMPORTANT: Remove from FRONT of deque (oldest entries are at front)
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < limit) {
                timestamps.addLast(now);  // record this request
                return true;
            }
            return false;
        }

        public synchronized int getCurrentCount() { return timestamps.size(); }

        @Override
        public String toString() {
            return String.format("SlidingWindow[limit=%d, current=%d]", limit, timestamps.size());
        }
    }

    // =========================================================================
    // ALGORITHM 3: FIXED WINDOW COUNTER
    // =========================================================================

    /**
     * Fixed Window Rate Limiter — simplest, but has boundary vulnerability.
     *
     * KEY INSIGHT: Time is divided into fixed windows of `windowMs` size.
     * Window number = currentTimeMillis / windowMs (integer division).
     * When the window number changes, the counter resets.
     *
     * THE BOUNDARY BUG (classic interview gotcha):
     *   Window A: [0, 1000ms), limit=100
     *   Window B: [1000ms, 2000ms), limit=100
     *   Attack: send 100 requests at T=999ms (end of A), 100 requests at T=1001ms (start of B)
     *   → 200 requests in just 2ms window → your downstream sees 200x burst
     *
     * WHY STILL USED?
     *   - O(1) space, O(1) time
     *   - Maps perfectly to Redis INCR + EXPIRE (atomic, distributed)
     *   - For most use cases (human users), boundary attack is theoretical
     *   - Sliding window counter (hybrid) solves this: use weighted average of
     *     current window count + (fraction of previous window) × prev window count
     *
     * THREAD SAFETY: synchronized on `this`
     */
    public static class FixedWindowRateLimiter {
        private final int limit;          // max requests per window
        private final long windowMs;      // window duration in milliseconds
        private long currentWindowStart;  // start of current window
        private int requestCount;         // requests in current window

        /**
         * @param limit    Max requests per window
         * @param windowMs Window duration in milliseconds
         */
        public FixedWindowRateLimiter(int limit, long windowMs) {
            this.limit = limit;
            this.windowMs = windowMs;
            this.currentWindowStart = System.currentTimeMillis();
            this.requestCount = 0;
        }

        /**
         * Allow request if count in current window < limit.
         * Resets counter when entering a new window.
         *
         * COMPLEXITY: O(1) time, O(1) space
         *
         * INTERVIEW Q: How would you store this in Redis?
         *   → key = "ratelimit:{userId}:{windowNumber}"
         *   → INCR key → returns new count
         *   → if new count == 1: EXPIRE key windowSeconds  (set expiry on first request)
         *   → if new count > limit: reject
         *   → GOTCHA: INCR + EXPIRE is NOT atomic → use Lua script or MULTI/EXEC
         */
        public synchronized boolean allow() {
            long now = System.currentTimeMillis();

            // Check if we've moved into a new window
            if (now - currentWindowStart >= windowMs) {
                // Reset for new window
                // GOTCHA: Don't set currentWindowStart = now, that causes drift.
                // Set it to the exact window boundary: (now / windowMs) * windowMs
                currentWindowStart = (now / windowMs) * windowMs;
                requestCount = 0;
            }

            if (requestCount < limit) {
                requestCount++;
                return true;
            }
            return false;
        }

        public synchronized int getCurrentCount() { return requestCount; }

        @Override
        public String toString() {
            return String.format("FixedWindow[limit=%d, count=%d]", limit, requestCount);
        }
    }

    // =========================================================================
    // MAIN — Demo and Tests
    // =========================================================================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Rate Limiter Demo ===\n");

        // --- TOKEN BUCKET DEMO ---
        System.out.println("--- Token Bucket (capacity=5, rate=2/s) ---");
        TokenBucketRateLimiter tokenBucket = new TokenBucketRateLimiter(5, 2);

        // Burst: fire 7 requests immediately, first 5 succeed (bucket starts full)
        int allowed = 0, denied = 0;
        for (int i = 0; i < 7; i++) {
            if (tokenBucket.allow()) allowed++;
            else denied++;
        }
        System.out.printf("Burst of 7: %d allowed, %d denied (expected 5 allowed, 2 denied)%n",
                allowed, denied);

        // Wait 1 second → 2 tokens refilled
        Thread.sleep(1000);
        allowed = 0;
        for (int i = 0; i < 3; i++) {
            if (tokenBucket.allow()) allowed++;
        }
        System.out.printf("After 1s wait, burst of 3: %d allowed (expected 2)%n", allowed);

        System.out.println();

        // --- SLIDING WINDOW DEMO ---
        System.out.println("--- Sliding Window (limit=3 per 500ms) ---");
        SlidingWindowRateLimiter sliding = new SlidingWindowRateLimiter(3, 500);

        for (int i = 0; i < 5; i++) {
            System.out.printf("  Request %d: %s%n", i + 1, sliding.allow() ? "ALLOWED" : "DENIED");
        }
        // First 3 allowed, 4th and 5th denied

        Thread.sleep(600); // slide window forward
        System.out.println("  After 600ms:");
        for (int i = 0; i < 3; i++) {
            System.out.printf("  Request %d: %s%n", i + 1, sliding.allow() ? "ALLOWED" : "DENIED");
        }
        // Should allow 3 again (old timestamps expired)

        System.out.println();

        // --- FIXED WINDOW DEMO ---
        System.out.println("--- Fixed Window (limit=3 per 1s) ---");
        FixedWindowRateLimiter fixed = new FixedWindowRateLimiter(3, 1000);

        for (int i = 0; i < 5; i++) {
            System.out.printf("  Request %d: %s (count=%d)%n",
                    i + 1, fixed.allow() ? "ALLOWED" : "DENIED", fixed.getCurrentCount());
        }

        Thread.sleep(1100);
        System.out.println("  After 1.1s (new window):");
        System.out.printf("  Request 1: %s%n", fixed.allow() ? "ALLOWED" : "DENIED");

        System.out.println();

        // --- CONCURRENCY STRESS TEST ---
        System.out.println("--- Concurrency Test: 100 threads, Token Bucket capacity=10 rate=100 ---");
        TokenBucketRateLimiter stressTest = new TokenBucketRateLimiter(10, 100);
        AtomicLong atomicAllowed = new AtomicLong(0);
        Thread[] threads = new Thread[100];

        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
                if (stressTest.allow()) atomicAllowed.incrementAndGet();
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.printf("100 concurrent requests: %d allowed (expected ~10 since bucket starts full)%n",
                atomicAllowed.get());

        System.out.println("\n=== Algorithm Summary ===");
        System.out.println("Token Bucket  : Allows bursts, O(1), best for APIs with burst allowance");
        System.out.println("Sliding Window: No boundary bug, O(limit) space, precise");
        System.out.println("Fixed Window  : Simplest, O(1), boundary vulnerability, maps to Redis INCR");

        /*
         * INTERVIEW Q: "Which would you use for production?"
         * A: Depends on requirements:
         *   - If distributed (multiple app nodes): Redis-based fixed window or sliding window
         *   - If single node, burst ok: token bucket
         *   - If precise fairness required: sliding window
         *   - Most real systems: token bucket at edge (API gateway) +
         *     fixed window per user in Redis (simple, scalable)
         *
         * INTERVIEW Q: "How would you make this per-user in a distributed system?"
         * A: Each user gets their own key in Redis. Use Lua script to atomically
         *    check-and-increment. Lua runs atomically on Redis server.
         *    lua: local count = redis.call('INCR', key)
         *         if count == 1 then redis.call('EXPIRE', key, window) end
         *         if count > limit then return 0 else return 1 end
         */
    }
}
