package com.java.lld;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * =============================================================================
 * CONCEPT: Parking Lot System — Classic OOP Design Interview Problem
 * =============================================================================
 *
 * One of the most frequently asked LLD (Low-Level Design) questions at FAANG.
 * Tests your ability to model real-world entities with proper OOP:
 * encapsulation, inheritance, enums, and thread safety.
 *
 * ENTITIES:
 *   VehicleType → SpotType mapping is a key design decision.
 *   MOTORCYCLE can fit in SMALL. CAR fits in MEDIUM or LARGE.
 *   BUS only fits in LARGE (too big for MEDIUM).
 *   This is an "assignment compatibility" problem.
 *
 * KEY DESIGN DECISIONS:
 *   1. How to find the nearest available spot quickly?
 *      Simple: iterate floors in order, scan spots → O(floors × spots)
 *      Better: PriorityQueue<ParkingSpot> per SpotType, sorted by (floor, index)
 *      Best: TreeMap<Integer, Queue<ParkingSpot>> where key = floor number
 *
 *   2. How to support multiple spot types per vehicle?
 *      Use a preference list: CAR prefers MEDIUM first, falls back to LARGE.
 *      Implement as a strategy or just a simple loop over preferred types.
 *
 *   3. How to calculate fees?
 *      Store entry time in ticket. On exit, compute Duration.between(entry, now).
 *      Fee = ceil(hours) × hourlyRate. Use BigDecimal for money in production!
 *
 * TIME COMPLEXITY:
 *   - parkVehicle: O(F × S) naive where F = floors, S = spots per floor
 *                 O(log N) with PriorityQueue
 *   - releaseVehicle: O(1) — we have the spot reference in the ticket
 *   - getAvailableCount: O(F × S) — must scan all spots
 *
 * SPACE COMPLEXITY: O(F × S) for the spot grid
 *
 * WHEN TO USE (interview):
 *   Any system managing finite resources with typed slots:
 *   Airport gate assignment, hospital bed management, warehouse slot allocation
 *
 * INTERVIEW ANGLE:
 *   "Scale to 1000 floors and 100,000 spots?"
 *   → PriorityQueue<ParkingSpot> sorted by (floor, position) per SpotType
 *   → O(log N) park and release
 *
 *   "Add reservation feature?"
 *   → SpotStatus: AVAILABLE, RESERVED, OCCUPIED
 *   → Reservations expire after TTL (scheduler or lazy check)
 *
 *   "How to handle VIP spots or disability spots?"
 *   → Add PRIORITY field to ParkingSpot, filter in search
 *
 *   "Multi-entry/multi-exit gates?"
 *   → Each gate has its own thread. Synchronized spot assignment prevents double-booking.
 *
 * REAL-WORLD USE:
 *   - Smart parking systems (Sievert, ParkWhiz)
 *   - Airport parking management
 *   - Mall parking with floor guidance displays
 * =============================================================================
 */
public class ParkingLot {

    // =========================================================================
    // ENUMS — Use enums for fixed sets of constants; more type-safe than String/int
    // =========================================================================

    /**
     * Vehicle types determine which spot sizes are compatible.
     * KEY INSIGHT: Keep vehicle type separate from spot type — they're independent
     * axes. A future requirement might allow CAR to park in SMALL (compact spots).
     */
    public enum VehicleType {
        MOTORCYCLE,
        CAR,
        BUS;

        /** Returns compatible spot types in preference order (first = preferred). */
        public List<SpotType> compatibleSpots() {
            return switch (this) {
                case MOTORCYCLE -> List.of(SpotType.SMALL, SpotType.MEDIUM);  // SMALL first
                case CAR        -> List.of(SpotType.MEDIUM, SpotType.LARGE);  // MEDIUM preferred
                case BUS        -> List.of(SpotType.LARGE);                   // only LARGE fits
            };
        }

        /** Hourly parking fee in dollars. */
        public double hourlyRate() {
            return switch (this) {
                case MOTORCYCLE -> 1.0;
                case CAR        -> 2.0;
                case BUS        -> 5.0;
            };
        }
    }

    /**
     * Physical spot sizes — independent of what vehicles can park there.
     * GOTCHA: Don't conflate VehicleType with SpotType. A BUS uses a LARGE spot,
     * but a CAR can also use a LARGE spot (e.g., only large spots left).
     */
    public enum SpotType {
        SMALL,    // motorcycles
        MEDIUM,   // cars (primary)
        LARGE     // buses, oversized vehicles, overflow cars
    }

    // =========================================================================
    // DOMAIN CLASSES
    // =========================================================================

    /** Represents a vehicle entering/using the parking lot. */
    public static class Vehicle {
        private final String id;
        private final VehicleType type;

        public Vehicle(String id, VehicleType type) {
            this.id = id;
            this.type = type;
        }

        public String getId() { return id; }
        public VehicleType getType() { return type; }

        @Override
        public String toString() {
            return String.format("Vehicle[%s, %s]", id, type);
        }
    }

    /**
     * A single physical parking spot.
     * KEY INSIGHT: The spot knows its own position (floor, id) and state.
     * This makes it easy to find nearest spot — just sort by (floor, spotId).
     *
     * INTERVIEW: Should ParkingSpot implement Comparable?
     * → Yes, for PriorityQueue optimization: compare by (floor, spotId) to always
     *   get the nearest available spot in O(log N).
     */
    public static class ParkingSpot implements Comparable<ParkingSpot> {
        private final String spotId;
        private final int floor;
        private final SpotType type;
        private boolean isOccupied;

        public ParkingSpot(String spotId, int floor, SpotType type) {
            this.spotId = spotId;
            this.floor = floor;
            this.type = type;
            this.isOccupied = false;
        }

        public String getSpotId()     { return spotId; }
        public int getFloor()         { return floor; }
        public SpotType getType()     { return type; }
        public boolean isOccupied()   { return isOccupied; }
        public boolean isAvailable()  { return !isOccupied; }

        public void occupy()          { isOccupied = true; }
        public void free()            { isOccupied = false; }

        // Natural ordering: lower floor first, then by spotId (lexicographic)
        // This makes PriorityQueue<ParkingSpot> return nearest spots first
        @Override
        public int compareTo(ParkingSpot other) {
            if (this.floor != other.floor) return Integer.compare(this.floor, other.floor);
            return this.spotId.compareTo(other.spotId);
        }

        @Override
        public String toString() {
            return String.format("Spot[%s, floor=%d, %s, %s]",
                    spotId, floor, type, isOccupied ? "OCCUPIED" : "FREE");
        }
    }

    /**
     * Parking ticket — the receipt/token given to a driver when they park.
     * KEY INSIGHT: Ticket stores a REFERENCE to the ParkingSpot, not just the ID.
     * This allows O(1) spot release — no need to search for the spot on exit.
     *
     * In production: ticket would be stored in DB, spot reference = spotId foreign key.
     */
    public static class ParkingTicket {
        private static final AtomicInteger ticketCounter = new AtomicInteger(1000);

        private final String ticketId;
        private final Vehicle vehicle;
        private final ParkingSpot spot;
        private final Instant entryTime;

        public ParkingTicket(Vehicle vehicle, ParkingSpot spot) {
            this.ticketId = "T" + ticketCounter.getAndIncrement();
            this.vehicle = vehicle;
            this.spot = spot;
            this.entryTime = Instant.now();
        }

        public String getTicketId()   { return ticketId; }
        public Vehicle getVehicle()   { return vehicle; }
        public ParkingSpot getSpot()  { return spot; }
        public Instant getEntryTime() { return entryTime; }

        /**
         * Calculate fee based on duration and vehicle type.
         *
         * KEY INSIGHT: Always round UP hours (ceil) — industry standard.
         * A car parked for 1h 5min is charged for 2 hours.
         * In production use BigDecimal for currency arithmetic!
         *
         * COMPLEXITY: O(1)
         */
        public double calculateFee(Instant exitTime) {
            long minutes = Duration.between(entryTime, exitTime).toMinutes();
            // Minimum 1 hour charge; round up to next hour
            // GOTCHA: Math.ceil(0) = 0 → always charge at least 1 hour
            long hours = Math.max(1, (long) Math.ceil(minutes / 60.0));
            return hours * vehicle.getType().hourlyRate();
        }

        @Override
        public String toString() {
            return String.format("Ticket[%s, %s, spot=%s, entry=%s]",
                    ticketId, vehicle, spot.getSpotId(), entryTime);
        }
    }

    // =========================================================================
    // PARKING LOT SYSTEM
    // =========================================================================

    /**
     * The main parking lot management system.
     *
     * DESIGN PATTERN: Uses a 2D list (floors × spots) for clarity.
     * Optimization path: Replace List<List<ParkingSpot>> with
     * Map<SpotType, PriorityQueue<ParkingSpot>> for O(log N) find-nearest.
     *
     * THREAD SAFETY: All public methods are synchronized.
     * Finer-grained locking: use separate locks per floor, or per SpotType,
     * to allow concurrent parking on different floors.
     *
     * GOTCHA: synchronized on `this` means only one thread can park/exit at a time.
     * For a real multi-gate garage, this is a bottleneck. In production:
     *   → Use ReentrantLock per floor
     *   → Or use optimistic locking with CAS on spot.isOccupied
     */
    private final String name;
    private final List<List<ParkingSpot>> floors;
    // Optimization: per-type priority queues for O(log N) nearest spot
    private final Map<SpotType, PriorityQueue<ParkingSpot>> availableByType;
    private final Map<String, ParkingTicket> activeTickets;  // ticketId → ticket

    public ParkingLot(String name) {
        this.name = name;
        this.floors = new ArrayList<>();
        this.availableByType = new HashMap<>();
        this.activeTickets = new HashMap<>();
        for (SpotType type : SpotType.values()) {
            availableByType.put(type, new PriorityQueue<>());
        }
    }

    /**
     * Add a floor with specified spot configuration.
     * Called during initialization to build the parking structure.
     *
     * @param floorNumber  Floor index (0 = ground floor)
     * @param smallSpots   Number of SMALL spots on this floor
     * @param mediumSpots  Number of MEDIUM spots on this floor
     * @param largeSpots   Number of LARGE spots on this floor
     *
     * COMPLEXITY: O(S) where S = total spots on the floor
     */
    public synchronized void addFloor(int floorNumber, int smallSpots, int mediumSpots, int largeSpots) {
        List<ParkingSpot> floorSpots = new ArrayList<>();
        int spotIndex = 0;

        for (int i = 0; i < smallSpots; i++) {
            String spotId = String.format("F%d-S%02d", floorNumber, spotIndex++);
            ParkingSpot spot = new ParkingSpot(spotId, floorNumber, SpotType.SMALL);
            floorSpots.add(spot);
            availableByType.get(SpotType.SMALL).offer(spot);
        }
        for (int i = 0; i < mediumSpots; i++) {
            String spotId = String.format("F%d-M%02d", floorNumber, spotIndex++);
            ParkingSpot spot = new ParkingSpot(spotId, floorNumber, SpotType.MEDIUM);
            floorSpots.add(spot);
            availableByType.get(SpotType.MEDIUM).offer(spot);
        }
        for (int i = 0; i < largeSpots; i++) {
            String spotId = String.format("F%d-L%02d", floorNumber, spotIndex++);
            ParkingSpot spot = new ParkingSpot(spotId, floorNumber, SpotType.LARGE);
            floorSpots.add(spot);
            availableByType.get(SpotType.LARGE).offer(spot);
        }

        floors.add(floorSpots);
        System.out.printf("Added floor %d: %d small, %d medium, %d large spots%n",
                floorNumber, smallSpots, mediumSpots, largeSpots);
    }

    /**
     * Park a vehicle: find nearest compatible spot and issue a ticket.
     *
     * KEY INSIGHT: We try preferred spot types in order (from VehicleType.compatibleSpots()).
     * MOTORCYCLE: tries SMALL first, falls back to MEDIUM (wastes MEDIUM for a motorcycle)
     * CAR: tries MEDIUM first, falls back to LARGE
     * BUS: only LARGE — throws if none available
     *
     * Uses PriorityQueue for O(log N) "nearest available" lookup.
     * PriorityQueue is ordered by (floor, spotId) so peek() = lowest floor, lowest index.
     *
     * COMPLEXITY: O(log N) with PriorityQueue where N = total spots of a type
     *
     * @return ParkingTicket — keep this to exit!
     * @throws IllegalStateException if no compatible spot is available
     */
    public synchronized ParkingTicket parkVehicle(Vehicle vehicle) {
        // Try each compatible spot type in preference order
        for (SpotType preferredType : vehicle.getType().compatibleSpots()) {
            PriorityQueue<ParkingSpot> available = availableByType.get(preferredType);

            // KEY: PriorityQueue.poll() removes and returns the "nearest" spot
            // (lowest floor number due to Comparable implementation above)
            ParkingSpot spot = available.poll();
            if (spot != null) {
                spot.occupy();
                ParkingTicket ticket = new ParkingTicket(vehicle, spot);
                activeTickets.put(ticket.getTicketId(), ticket);
                System.out.printf("Parked %s at %s (ticket: %s)%n",
                        vehicle, spot, ticket.getTicketId());
                return ticket;
            }
        }

        throw new IllegalStateException("No available spot for " + vehicle.getType());
        // INTERVIEW Q: What should happen when lot is full?
        // Options: throw exception, return Optional.empty(), add to waitlist
        // Production: return Optional<ParkingTicket> — more idiomatic Java
    }

    /**
     * Release a vehicle: free the spot, compute and return the fee.
     *
     * KEY INSIGHT: The ticket contains a direct reference to the ParkingSpot,
     * so we can free it in O(1) without searching.
     * Then we put the spot back in the PriorityQueue for future allocation.
     *
     * COMPLEXITY: O(log N) for PriorityQueue.offer()
     *
     * @return Fee charged in dollars
     * @throws IllegalArgumentException if ticket is invalid/already used
     */
    public synchronized double releaseVehicle(ParkingTicket ticket) {
        if (!activeTickets.containsKey(ticket.getTicketId())) {
            throw new IllegalArgumentException("Invalid or already used ticket: " + ticket.getTicketId());
        }

        Instant exitTime = Instant.now();
        double fee = ticket.calculateFee(exitTime);

        // Free the spot and add back to the available pool
        ParkingSpot spot = ticket.getSpot();
        spot.free();
        availableByType.get(spot.getType()).offer(spot);  // back in priority queue

        activeTickets.remove(ticket.getTicketId());

        System.out.printf("Released %s from spot %s. Fee: $%.2f%n",
                ticket.getVehicle(), spot.getSpotId(), fee);
        return fee;
    }

    /**
     * Count available spots of a specific type.
     * KEY INSIGHT: PriorityQueue.size() is O(1) — no need to scan all spots.
     *
     * COMPLEXITY: O(1) using PriorityQueue.size()
     */
    public synchronized int getAvailableCount(SpotType type) {
        return availableByType.get(type).size();
    }

    /** Print full availability summary. */
    public synchronized void printAvailability() {
        System.out.printf("%nPARKING LOT: %s%n", name);
        System.out.println("─".repeat(40));
        for (SpotType type : SpotType.values()) {
            System.out.printf("  %-8s available: %d%n", type, getAvailableCount(type));
        }
        System.out.printf("  Active tickets: %d%n", activeTickets.size());
    }

    // =========================================================================
    // MAIN — Demo
    // =========================================================================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Parking Lot System Demo ===\n");

        // Build the lot: 2 floors
        ParkingLot lot = new ParkingLot("Downtown Garage");
        lot.addFloor(0, 5, 10, 3);  // Ground: 5 small, 10 medium, 3 large
        lot.addFloor(1, 3, 8,  2);  // Floor 1: 3 small, 8 medium, 2 large

        lot.printAvailability();

        // Park various vehicles
        System.out.println("\n--- Parking vehicles ---");
        Vehicle moto1 = new Vehicle("MOTO-001", VehicleType.MOTORCYCLE);
        Vehicle car1  = new Vehicle("CAR-001",  VehicleType.CAR);
        Vehicle car2  = new Vehicle("CAR-002",  VehicleType.CAR);
        Vehicle bus1  = new Vehicle("BUS-001",  VehicleType.BUS);

        ParkingTicket t1 = lot.parkVehicle(moto1);
        ParkingTicket t2 = lot.parkVehicle(car1);
        ParkingTicket t3 = lot.parkVehicle(car2);
        ParkingTicket t4 = lot.parkVehicle(bus1);

        lot.printAvailability();

        // Simulate some time passing and release
        System.out.println("\n--- Releasing vehicles ---");
        Thread.sleep(100);  // small delay to show non-zero fee logic
        lot.releaseVehicle(t2);  // release car1
        lot.releaseVehicle(t4);  // release bus1

        lot.printAvailability();

        // Test spot reuse: park car again in the freed spot
        System.out.println("\n--- Re-parking after release ---");
        Vehicle car3 = new Vehicle("CAR-003", VehicleType.CAR);
        ParkingTicket t5 = lot.parkVehicle(car3);
        System.out.println("Car3 reused freed spot: " + t5.getSpot());

        // Fill up LARGE spots to show BUS rejection
        System.out.println("\n--- Fill all LARGE spots ---");
        List<ParkingTicket> busTickets = new ArrayList<>();
        try {
            while (lot.getAvailableCount(SpotType.LARGE) > 0) {
                Vehicle b = new Vehicle("BUS-FILL-" + busTickets.size(), VehicleType.BUS);
                busTickets.add(lot.parkVehicle(b));
            }
            Vehicle lastBus = new Vehicle("BUS-OVERFLOW", VehicleType.BUS);
            lot.parkVehicle(lastBus);  // should throw
        } catch (IllegalStateException e) {
            System.out.println("Expected: " + e.getMessage());
        }

        lot.printAvailability();

        /*
         * INTERVIEW Q: "How would you add a VIP spot feature?"
         * A: Add SpotType.VIP or a boolean vip field on ParkingSpot.
         *    ParkingTicket carries a flag indicating VIP user.
         *    The assignment logic first tries VIP spots for VIP users.
         *
         * INTERVIEW Q: "How would you add time-based pricing?"
         * A: Replace fixed hourlyRate() with a PricingStrategy interface.
         *    DaytimePricing: $3/hr 8am-6pm, $1/hr otherwise.
         *    Strategy pattern makes this pluggable without changing core logic.
         *
         * INTERVIEW Q: "Handle multi-car entry simultaneously?"
         * A: For high-throughput: Replace `synchronized` with ConcurrentHashMap
         *    and AtomicBoolean per spot (CAS: compareAndSet(false, true)).
         *    Avoids full lot-wide lock while still being thread-safe per spot.
         */
    }
}
