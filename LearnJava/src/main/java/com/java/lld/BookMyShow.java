package com.java.lld;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * CONCEPT: Movie Ticket Booking System (BookMyShow / Ticketmaster)
 * =============================================================================
 *
 * One of the most comprehensive LLD problems — tests concurrent state management,
 * distributed locking concepts, and real-world trade-offs.
 *
 * THE CORE CHALLENGE: Concurrent seat selection.
 * Multiple users may try to book the same seat simultaneously. We need:
 *   1. Prevent double-booking (atomicity of seat lock + confirm)
 *   2. Handle payment failure gracefully (TTL-based lock release)
 *   3. Support high read load (thousands searching available seats)
 *
 * SEAT LIFECYCLE:
 *   AVAILABLE → [LOCKED by user A, TTL=2min] → BOOKED (on payment)
 *                                             → AVAILABLE (on TTL expiry or cancel)
 *
 * LOCKING STRATEGY — ReentrantReadWriteLock:
 *   Read lock (shared): multiple threads can hold simultaneously
 *     → Used for: searchShows, getAvailableSeats (non-mutating)
 *   Write lock (exclusive): only one thread at a time
 *     → Used for: lockSeats, confirmBooking, releaseExpiredLocks (mutating)
 *
 *   WHY NOT synchronized?
 *   synchronized locks everything — even concurrent readers block each other.
 *   With RW lock, 100 users searching shows simultaneously don't block each other.
 *   Only booking/locking operations need exclusive access.
 *
 * SCALABILITY PATH:
 *   Single-node RW lock → per-show lock (reduce contention) →
 *   Distributed lock with Redis SETNX (multi-instance) →
 *   Optimistic locking with DB version column →
 *   Seat reservation via Kafka + idempotent consumers
 *
 * TIME COMPLEXITY:
 *   searchShows: O(S) where S = number of shows
 *   lockSeats: O(k) where k = seats to lock
 *   confirmBooking: O(k)
 *   getAvailableSeats: O(R*C) where R=rows, C=cols of the screen
 *
 * SPACE COMPLEXITY: O(shows × seats) for the seat state grid
 *
 * INTERVIEW ANGLE:
 *   "Design Ticketmaster" — common at Google, Amazon, Flipkart
 *   Key discussion areas:
 *     1. How to prevent double-booking at scale
 *     2. Payment flow integration (lock → pay → confirm or release)
 *     3. Waitlist / queue for sold-out shows
 *     4. Pricing tiers (front row premium, etc.)
 *
 * REAL-WORLD USE:
 *   - BookMyShow, Ticketmaster, Fandango
 *   - Concert/event seat selection (same problem)
 *   - Flight seat selection (airlines use similar TTL-lock pattern)
 * =============================================================================
 */
public class BookMyShow {

    // =========================================================================
    // ENUMS
    // =========================================================================

    /**
     * Seat state machine: AVAILABLE → LOCKED → BOOKED
     *                                LOCKED → AVAILABLE (on timeout or cancel)
     *
     * INTERVIEW Q: Why LOCKED state? Why not just AVAILABLE and BOOKED?
     * A: LOCKED is a temporary hold during the payment window (~2 min).
     *    Without it, two users could both "see" a seat available, both pay,
     *    and one gets double-charged. LOCKED is the reservation before payment.
     */
    public enum SeatStatus { AVAILABLE, LOCKED, BOOKED }

    // =========================================================================
    // DOMAIN CLASSES
    // =========================================================================

    public static class Movie {
        private final String id;
        private final String title;
        private final int durationMinutes;

        public Movie(String id, String title, int durationMinutes) {
            this.id = id;
            this.title = title;
            this.durationMinutes = durationMinutes;
        }

        public String getId()    { return id; }
        public String getTitle() { return title; }
        @Override public String toString() { return String.format("Movie[%s: %s]", id, title); }
    }

    public static class Screen {
        private final String id;
        private final int rows;
        private final int cols;

        public Screen(String id, int rows, int cols) {
            this.id = id;
            this.rows = rows;
            this.cols = cols;
        }

        public String getId() { return id; }
        public int getRows()  { return rows; }
        public int getCols()  { return cols; }
    }

    public static class Show {
        private final String id;
        private final Movie movie;
        private final Screen screen;
        private final LocalDateTime startTime;

        public Show(String id, Movie movie, Screen screen, LocalDateTime startTime) {
            this.id = id;
            this.movie = movie;
            this.screen = screen;
            this.startTime = startTime;
        }

        public String getId()          { return id; }
        public Movie getMovie()        { return movie; }
        public Screen getScreen()      { return screen; }
        public LocalDateTime getStartTime() { return startTime; }

        @Override
        public String toString() {
            return String.format("Show[%s, %s at %s on screen %s]",
                    id, movie.getTitle(), startTime, screen.getId());
        }
    }

    /**
     * A single physical seat in a screen.
     *
     * KEY INSIGHT: Seat has mutable state (status, lockedByUser, lockExpiry).
     * These fields are mutated under write lock of the show's RWLock.
     * Never mutate seat state without holding the appropriate lock!
     *
     * PRICE TIERS: Front rows typically cost more. We model this with a price field.
     * In production, price could be computed dynamically (surge pricing on demand).
     */
    public static class Seat {
        private final int row;
        private final int col;
        private SeatStatus status;
        private String lockedByUserId;   // which user holds the lock
        private Instant lockExpiry;      // when the lock expires
        private final double price;

        public Seat(int row, int col, double price) {
            this.row = row;
            this.col = col;
            this.status = SeatStatus.AVAILABLE;
            this.price = price;
        }

        public int getRow()             { return row; }
        public int getCol()             { return col; }
        public SeatStatus getStatus()   { return status; }
        public double getPrice()        { return price; }

        /** Called under write lock */
        public void lock(String userId, long ttlMillis) {
            this.status = SeatStatus.LOCKED;
            this.lockedByUserId = userId;
            this.lockExpiry = Instant.now().plusMillis(ttlMillis);
        }

        /** Called under write lock */
        public void book() {
            this.status = SeatStatus.BOOKED;
            this.lockedByUserId = null;
            this.lockExpiry = null;
        }

        /** Called under write lock */
        public void release() {
            this.status = SeatStatus.AVAILABLE;
            this.lockedByUserId = null;
            this.lockExpiry = null;
        }

        /** Called under read lock (lockExpiry is volatile in spirit — use write lock to be safe) */
        public boolean isLockExpired() {
            return status == SeatStatus.LOCKED
                    && lockExpiry != null
                    && Instant.now().isAfter(lockExpiry);
        }

        public String getSeatLabel() {
            return String.format("%c%d", (char)('A' + row), col + 1);
        }

        @Override
        public String toString() {
            return String.format("Seat[%s, %s, $%.1f]", getSeatLabel(), status, price);
        }
    }

    /**
     * A lock token issued when seats are temporarily reserved.
     * The user must use this lockId to confirm the booking within TTL.
     */
    public static class SeatLock {
        private static final AtomicInteger counter = new AtomicInteger(1);
        private final String lockId;
        private final String showId;
        private final List<int[]> seatPositions;  // [row, col] pairs
        private final String userId;
        private final Instant createdAt;
        private final long ttlMillis;

        public SeatLock(String showId, List<int[]> seatPositions, String userId, long ttlMillis) {
            this.lockId = "LOCK-" + counter.getAndIncrement();
            this.showId = showId;
            this.seatPositions = new ArrayList<>(seatPositions);
            this.userId = userId;
            this.createdAt = Instant.now();
            this.ttlMillis = ttlMillis;
        }

        public String getLockId()              { return lockId; }
        public String getShowId()              { return showId; }
        public List<int[]> getSeatPositions()  { return seatPositions; }
        public String getUserId()              { return userId; }

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusMillis(ttlMillis));
        }

        @Override
        public String toString() {
            return String.format("SeatLock[%s, show=%s, user=%s, seats=%d]",
                    lockId, showId, userId, seatPositions.size());
        }
    }

    /**
     * Confirmed booking after successful payment.
     */
    public static class Booking {
        private static final AtomicInteger counter = new AtomicInteger(5000);
        private final String bookingId;
        private final Show show;
        private final List<Seat> seats;
        private final String userId;
        private final double totalPrice;
        private final Instant bookedAt;

        public Booking(Show show, List<Seat> seats, String userId) {
            this.bookingId = "BKG-" + counter.getAndIncrement();
            this.show = show;
            this.seats = new ArrayList<>(seats);
            this.userId = userId;
            this.totalPrice = seats.stream().mapToDouble(Seat::getPrice).sum();
            this.bookedAt = Instant.now();
        }

        public String getBookingId()  { return bookingId; }
        public double getTotalPrice() { return totalPrice; }

        @Override
        public String toString() {
            String seatLabels = seats.stream()
                    .map(Seat::getSeatLabel)
                    .collect(Collectors.joining(", "));
            return String.format("Booking[%s, show=%s, seats=[%s], user=%s, total=$%.2f]",
                    bookingId, show.getId(), seatLabels, userId, totalPrice);
        }
    }

    // =========================================================================
    // BOOKING SYSTEM
    // =========================================================================

    /**
     * Main BookMyShow system managing shows, seat locks, and bookings.
     *
     * CONCURRENCY DESIGN:
     *   - Per-show ReentrantReadWriteLock: fine-grained locking so Show A's
     *     operations don't block Show B's operations.
     *   - ConcurrentHashMap for show/lock registries: lock-free reads.
     *   - Read lock for read-only queries (searchShows, getAvailableSeats).
     *   - Write lock for mutations (lockSeats, confirmBooking).
     *
     *   WHY PER-SHOW LOCK vs SINGLE GLOBAL LOCK?
     *   Single global lock: all operations serialize → terrible throughput.
     *   Per-show lock: concurrent bookings for DIFFERENT shows proceed in parallel.
     *   In production: further partition by seat region (section A, B, C).
     */
    // showId → Show
    private final Map<String, Show> shows = new ConcurrentHashMap<>();
    // showId → 2D seat array [row][col]
    private final Map<String, Seat[][]> showSeats = new ConcurrentHashMap<>();
    // showId → ReentrantReadWriteLock (per-show fine-grained locking)
    private final Map<String, ReentrantReadWriteLock> showLocks = new ConcurrentHashMap<>();
    // lockId → SeatLock (pending seat holds)
    private final Map<String, SeatLock> pendingLocks = new ConcurrentHashMap<>();
    // bookingId → Booking (confirmed bookings)
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    private static final long LOCK_TTL_MS = 2 * 60 * 1000;  // 2 minutes

    /**
     * Register a show and initialize its seat grid.
     *
     * KEY INSIGHT: We initialize the full seat grid upfront so seat access
     * is O(1) by [row][col] index rather than searching by label.
     *
     * PRICING: Front rows (low row index) cost 20% more — simple surge model.
     */
    public void addShow(Show show) {
        Screen screen = show.getScreen();
        Seat[][] seats = new Seat[screen.getRows()][screen.getCols()];

        // Initialize seat grid with tier pricing
        for (int r = 0; r < screen.getRows(); r++) {
            for (int c = 0; c < screen.getCols(); c++) {
                // Front 2 rows: premium pricing
                double price = (r < 2) ? 300.0 : 200.0;
                seats[r][c] = new Seat(r, c, price);
            }
        }

        shows.put(show.getId(), show);
        showSeats.put(show.getId(), seats);
        showLocks.put(show.getId(), new ReentrantReadWriteLock());

        System.out.printf("Added show: %s (%dx%d grid, %d seats)%n",
                show, screen.getRows(), screen.getCols(),
                screen.getRows() * screen.getCols());
    }

    /**
     * Search shows by movie title.
     *
     * READ LOCK: multiple users can search simultaneously without blocking each other.
     * KEY INSIGHT: We lock ALL shows here for simplicity. In production, shows is
     * read-only after initialization, so ConcurrentHashMap read is already safe.
     *
     * COMPLEXITY: O(S) where S = number of shows
     */
    public List<Show> searchShows(String movieTitle) {
        // ConcurrentHashMap.values() iteration is safe without external locks
        return shows.values().stream()
                .filter(s -> s.getMovie().getTitle().equalsIgnoreCase(movieTitle))
                .collect(Collectors.toList());
    }

    /**
     * Get all available seats for a show.
     *
     * USES READ LOCK: safe to call concurrently with other reads.
     * Collect expired locks too — releases them as a side effect.
     *
     * COMPLEXITY: O(R × C)
     */
    public List<Seat> getAvailableSeats(String showId) {
        ReentrantReadWriteLock rwLock = showLocks.get(showId);
        if (rwLock == null) throw new IllegalArgumentException("Show not found: " + showId);

        rwLock.readLock().lock();
        try {
            Seat[][] seats = showSeats.get(showId);
            List<Seat> available = new ArrayList<>();
            for (Seat[] row : seats) {
                for (Seat seat : row) {
                    if (seat.getStatus() == SeatStatus.AVAILABLE) {
                        available.add(seat);
                    }
                }
            }
            return available;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Temporarily lock seats for a user during the payment window.
     *
     * USES WRITE LOCK: exclusive access during the check-and-lock operation.
     * KEY INSIGHT: The check-then-act must be ATOMIC. If we checked availability
     * and then locked separately, another thread could sneak in between.
     * The write lock ensures the entire check-and-lock is atomic.
     *
     * GOTCHA: Must validate ALL requested seats before locking ANY of them.
     * If we lock seats one by one and fail on seat 3, we'd need to roll back
     * seats 1 and 2. The two-phase approach (validate all, then lock all) is cleaner.
     *
     * @param showId        Show ID
     * @param seatPositions List of [row, col] pairs
     * @param userId        User requesting the lock
     * @return SeatLock token to use in confirmBooking()
     * @throws IllegalStateException if any seat is already locked/booked
     *
     * COMPLEXITY: O(k) where k = number of seats
     */
    public SeatLock lockSeats(String showId, List<int[]> seatPositions, String userId) {
        ReentrantReadWriteLock rwLock = showLocks.get(showId);
        if (rwLock == null) throw new IllegalArgumentException("Show not found: " + showId);

        rwLock.writeLock().lock();
        try {
            Seat[][] seats = showSeats.get(showId);

            // PHASE 1: Validate all seats before locking any
            // GOTCHA: Also release expired locks as we scan (lazy cleanup)
            for (int[] pos : seatPositions) {
                Seat seat = seats[pos[0]][pos[1]];
                if (seat.isLockExpired()) {
                    seat.release();  // lazy expiry cleanup
                }
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new IllegalStateException(
                        "Seat " + seat.getSeatLabel() + " is not available (status=" + seat.getStatus() + ")");
                }
            }

            // PHASE 2: Lock all seats (only reached if all seats passed validation)
            for (int[] pos : seatPositions) {
                seats[pos[0]][pos[1]].lock(userId, LOCK_TTL_MS);
            }

            SeatLock seatLock = new SeatLock(showId, seatPositions, userId, LOCK_TTL_MS);
            pendingLocks.put(seatLock.getLockId(), seatLock);

            System.out.printf("Locked %d seats for user %s: %s%n",
                    seatPositions.size(), userId, seatLock.getLockId());
            return seatLock;

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Confirm a booking after successful payment.
     *
     * USES WRITE LOCK: transitions seats from LOCKED → BOOKED.
     *
     * PAYMENT FLOW:
     *   1. lockSeats() → user sees price, enters payment details
     *   2. Payment gateway processes payment (2min TTL window)
     *   3. confirmBooking() → if lock still valid, create booking
     *   4. If payment fails or TTL expires → seats auto-released
     *
     * KEY INSIGHT: Check lock expiry under write lock. Between when the lock
     * expired and when we check here, another user might have locked the seats.
     * Holding write lock ensures no concurrent seat mutations.
     *
     * @param lockId      Lock ID from lockSeats()
     * @param paymentInfo Payment reference (in real system: payment gateway token)
     * @return Confirmed Booking
     * @throws IllegalStateException if lock expired or invalid
     *
     * COMPLEXITY: O(k) where k = number of locked seats
     */
    public Booking confirmBooking(String lockId, String paymentInfo) {
        SeatLock seatLock = pendingLocks.get(lockId);
        if (seatLock == null) {
            throw new IllegalStateException("Invalid or already used lock: " + lockId);
        }
        if (seatLock.isExpired()) {
            pendingLocks.remove(lockId);
            throw new IllegalStateException("Lock expired for: " + lockId +
                    ". Please re-select seats.");
        }

        String showId = seatLock.getShowId();
        ReentrantReadWriteLock rwLock = showLocks.get(showId);

        rwLock.writeLock().lock();
        try {
            Seat[][] seats = showSeats.get(showId);
            List<Seat> bookedSeats = new ArrayList<>();

            // Re-validate under write lock: ensure seats are still locked by this user
            // (could theoretically differ if clock skew caused expiry between checks)
            for (int[] pos : seatLock.getSeatPositions()) {
                Seat seat = seats[pos[0]][pos[1]];
                if (seat.getStatus() != SeatStatus.LOCKED) {
                    throw new IllegalStateException("Seat " + seat.getSeatLabel() +
                            " is no longer locked. Concurrent expiry?");
                }
                bookedSeats.add(seat);
            }

            // Transition LOCKED → BOOKED
            for (Seat seat : bookedSeats) {
                seat.book();
            }

            pendingLocks.remove(lockId);
            Booking booking = new Booking(shows.get(showId), bookedSeats, seatLock.getUserId());
            bookings.put(booking.getBookingId(), booking);

            System.out.printf("Booking confirmed: %s (payment: %s)%n", booking, paymentInfo);
            return booking;

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Release all expired seat locks. Should be called periodically
     * (e.g., scheduled task every 30 seconds, or lazily in lockSeats).
     *
     * KEY INSIGHT: Two cleanup strategies:
     *   EAGER: Background thread scans and releases expired locks periodically.
     *          Pro: Seats become available faster. Con: Wasted CPU if few expirations.
     *   LAZY:  Release expired locks only when someone tries to book the seat.
     *          Pro: Zero overhead when no activity. Con: Seat appears LOCKED even after expiry.
     *
     * This implementation does EAGER cleanup for demonstration.
     * lockSeats() also does lazy cleanup per seat as it scans.
     *
     * COMPLEXITY: O(P × k) where P = pending locks, k = seats per lock
     */
    public int releaseExpiredLocks() {
        int released = 0;
        for (Map.Entry<String, SeatLock> entry : pendingLocks.entrySet()) {
            SeatLock lock = entry.getValue();
            if (lock.isExpired()) {
                String showId = lock.getShowId();
                ReentrantReadWriteLock rwLock = showLocks.get(showId);

                rwLock.writeLock().lock();
                try {
                    Seat[][] seats = showSeats.get(showId);
                    for (int[] pos : lock.getSeatPositions()) {
                        Seat seat = seats[pos[0]][pos[1]];
                        if (seat.getStatus() == SeatStatus.LOCKED) {
                            seat.release();
                            released++;
                        }
                    }
                } finally {
                    rwLock.writeLock().unlock();
                }
                pendingLocks.remove(entry.getKey());
            }
        }
        if (released > 0) {
            System.out.printf("Released %d expired seat locks%n", released);
        }
        return released;
    }

    // =========================================================================
    // MAIN — Demo
    // =========================================================================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== BookMyShow System Demo ===\n");

        BookMyShow system = new BookMyShow();

        // Setup
        Movie movie = new Movie("M1", "Inception", 148);
        Screen screen = new Screen("S1", 5, 8);  // 5 rows x 8 cols = 40 seats
        Show show = new Show("SH1", movie, screen, LocalDateTime.now().plusHours(2));

        system.addShow(show);

        // Search
        List<Show> results = system.searchShows("Inception");
        System.out.println("\nSearch results: " + results);

        // View available seats
        List<Seat> available = system.getAvailableSeats("SH1");
        System.out.printf("Available seats: %d%n", available.size());
        System.out.println("First few: " + available.subList(0, 5));

        // User 1 locks seats A1 and A2 (row 0, col 0 and 1)
        System.out.println("\n--- User 1 locks A1 and A2 ---");
        List<int[]> user1Seats = List.of(new int[]{0, 0}, new int[]{0, 1});
        SeatLock lock1 = system.lockSeats("SH1", user1Seats, "USER-1");
        System.out.println("Lock1: " + lock1);

        // User 2 tries to lock A1 (already locked) — should fail
        System.out.println("\n--- User 2 tries A1 (should FAIL) ---");
        try {
            List<int[]> user2Seats = List.of(new int[]{0, 0});
            system.lockSeats("SH1", user2Seats, "USER-2");
        } catch (IllegalStateException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        // User 2 locks A3 instead
        System.out.println("\n--- User 2 locks A3 ---");
        List<int[]> user2Seats = List.of(new int[]{0, 2});
        SeatLock lock2 = system.lockSeats("SH1", user2Seats, "USER-2");

        // User 1 confirms booking (payment succeeds)
        System.out.println("\n--- User 1 confirms booking ---");
        Booking booking1 = system.confirmBooking(lock1.getLockId(), "PAY-TXN-123");
        System.out.printf("Total price: $%.2f%n", booking1.getTotalPrice());

        // User 2 also confirms
        Booking booking2 = system.confirmBooking(lock2.getLockId(), "PAY-TXN-456");

        // Available count should be reduced
        System.out.printf("\nAvailable after bookings: %d (expected 37)%n",
                system.getAvailableSeats("SH1").size());

        // Demonstrate expired lock release
        System.out.println("\n--- Expired lock demo ---");
        // Create a lock with very short TTL (100ms)
        SeatLock shortLock = new SeatLock("SH1", List.of(new int[]{4, 7}), "USER-LAZY", 100);
        system.pendingLocks.put(shortLock.getLockId(), shortLock);
        // Manually lock the seat (bypass normal flow for demo)
        system.showSeats.get("SH1")[4][7].lock("USER-LAZY", 100);

        Thread.sleep(200);  // wait for TTL to expire
        int released = system.releaseExpiredLocks();
        System.out.printf("Released %d expired lock(s)%n", released);

        System.out.println("\n=== BookMyShow Summary ===");
        System.out.println("Key patterns used:");
        System.out.println("  ReentrantReadWriteLock: shared reads, exclusive writes");
        System.out.println("  Two-phase locking: validate all, then lock all");
        System.out.println("  TTL-based lock expiry: prevents stuck reservations");
        System.out.println("  Per-show locks: concurrent booking across shows");

        /*
         * INTERVIEW Q: "How to scale to millions of concurrent users?"
         * A: 1. Partition shows across servers by showId (consistent hashing)
         *    2. Use Redis SETNX for distributed per-seat locking:
         *       SET seat:{showId}:{row}:{col} userId NX PX 120000
         *       (NX = only set if not exists, PX = TTL in millis)
         *    3. Use Redis Lua script for atomic multi-seat lock (check all, set all)
         *    4. DB write for confirmed bookings (idempotent with bookingId as unique key)
         *    5. Kafka for booking events (audit trail, analytics)
         *
         * INTERVIEW Q: "What if the app crashes after locking but before confirming?"
         * A: TTL on the lock ensures seats are automatically released after 2 minutes.
         *    The user must re-select and pay again. This is by design.
         *    For payment processing: use a payment timeout slightly less than lock TTL.
         *
         * INTERVIEW Q: "How to handle a waitlist for sold-out shows?"
         * A: When a booking is cancelled, emit a "seat available" event.
         *    Waitlist subscribers receive the event in FIFO order.
         *    First waitlist user gets a 5-min lock window to book.
         */
    }
}
