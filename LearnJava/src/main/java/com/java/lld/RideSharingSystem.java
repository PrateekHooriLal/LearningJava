package com.java.lld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * CONCEPT: Ride Sharing System (Uber/Lyft/Ola)
 * =============================================================================
 *
 * Models the core matching and trip lifecycle of a ride-sharing platform.
 * This is a rich OOP + concurrency problem testing spatial reasoning,
 * state machines, and real-time matching.
 *
 * CORE CHALLENGE: Find the nearest available driver in real-time while
 * handling concurrent ride requests from thousands of riders.
 *
 * SYSTEM COMPONENTS:
 *   1. Driver Registry: track driver locations and availability
 *   2. Ride Matching: find nearest driver to a pickup location
 *   3. Trip State Machine: REQUESTED → MATCHED → IN_PROGRESS → COMPLETED
 *   4. Fare Calculation: base fare + per-km rate
 *   5. Location Updates: drivers send GPS updates every few seconds
 *
 * SPATIAL INDEXING (interview deep-dive):
 *   NAIVE: Scan all drivers → O(N), fine for N < 1000
 *   GEOHASH: Encode lat/lon as a base-32 string prefix. Nearby cells share prefix.
 *     → Group drivers by geohash cell → only scan nearby cells. O(k) where k = cell density.
 *     → Used by: MongoDB 2dsphere, PostGIS, Uber H3
 *   QUADTREE: 2D spatial tree. Recursively divide space into 4 quadrants.
 *     → O(log N) query for drivers within bounding box
 *     → Good for dynamic data (drivers moving frequently)
 *   R-TREE: Better than QuadTree for nearest-neighbor queries on dense data
 *     → Used by PostgreSQL PostGIS for geo queries
 *   S2 LIBRARY (Google): Sphere-based cell hierarchy (used by Uber, Google Maps)
 *
 * THIS IMPLEMENTATION: Simplified O(N) scan — clear and correct for interviews.
 * Mention the above optimizations when asked about scaling.
 *
 * DISTANCE CALCULATION:
 *   Haversine formula: exact great-circle distance on a sphere.
 *   We use simplified Euclidean distance (good for small areas, ~same city).
 *   Haversine formula:
 *     a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlon/2)
 *     c = 2·atan2(√a, √(1−a))
 *     d = R·c  where R = 6371 km
 *   1 degree latitude ≈ 111 km; 1 degree longitude ≈ 111·cos(lat) km
 *
 * FARE MODEL: $2 base + $1.5/km (simplified Uber UberX pricing)
 *   Real Uber: surge pricing (demand/supply ratio), wait time component, city-specific rates
 *
 * TIME COMPLEXITY:
 *   requestRide: O(D) where D = number of drivers (linear scan for nearest)
 *                O(log D) with spatial index
 *   updateDriverLocation: O(1) — hash map lookup
 *   getNearbyDrivers: O(D) — scan all drivers
 *
 * SPACE COMPLEXITY: O(D + T) where D = drivers, T = active trips
 *
 * INTERVIEW ANGLE:
 *   "Design Uber" is a classic FAANG system design question.
 *   Key areas: matching algorithm, real-time location, surge pricing, driver dispatch
 *
 * REAL-WORLD NOTES:
 *   - Uber uses H3 hexagonal grid for spatial partitioning
 *   - Lyft uses geohash-based proximity search
 *   - Both use Kafka for real-time location event streaming
 * =============================================================================
 */
public class RideSharingSystem {

    // =========================================================================
    // ENUMS
    // =========================================================================

    /**
     * Driver state machine:
     *   OFFLINE ←→ AVAILABLE ←→ ON_TRIP
     *
     * GOTCHA: A driver must be AVAILABLE to receive ride requests.
     * During a trip, driver is ON_TRIP — not matchable for new requests.
     * Driver can go OFFLINE at any time (unless mid-trip — policy decision).
     */
    public enum DriverStatus { AVAILABLE, ON_TRIP, OFFLINE }

    /**
     * Trip state machine:
     *   REQUESTED → MATCHED → IN_PROGRESS → COMPLETED
     *                       ↘ CANCELLED
     *
     * KEY INSIGHT: Each state transition is an explicit method call.
     * This makes the state machine auditable and easy to add hooks
     * (e.g., send notification on MATCHED, charge card on COMPLETED).
     */
    public enum TripStatus { REQUESTED, MATCHED, IN_PROGRESS, COMPLETED, CANCELLED }

    // =========================================================================
    // LOCATION
    // =========================================================================

    /**
     * Immutable GPS coordinate.
     *
     * KEY INSIGHT: Make Location immutable! Location is a value object —
     * it describes a point in space. When a driver moves, we replace their
     * location reference entirely rather than mutating coordinates.
     * Immutability avoids race conditions on location reads.
     */
    public static class Location {
        private final double lat;
        private final double lon;

        public Location(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public double getLat() { return lat; }
        public double getLon() { return lon; }

        /**
         * Simplified Euclidean distance (approximation for nearby points).
         *
         * KEY INSIGHT: For production use Haversine (accounts for Earth's curvature).
         * For an interview, mention Haversine exists and implement simplified version.
         *
         * 1 degree ≈ 111km, so multiply by 111 to get approximate km.
         * This is accurate within ~1% for distances under 100km at mid-latitudes.
         *
         * COMPLEXITY: O(1)
         *
         * @return Distance in approximate kilometers
         */
        public double distanceTo(Location other) {
            double dlat = (other.lat - this.lat) * 111.0;  // degrees to km
            double dlon = (other.lon - this.lon) * 111.0;
            return Math.sqrt(dlat * dlat + dlon * dlon);
            // INTERVIEW: Full Haversine:
            // double R = 6371; // Earth radius in km
            // double dLatRad = Math.toRadians(other.lat - lat);
            // double dLonRad = Math.toRadians(other.lon - lon);
            // double a = Math.sin(dLatRad/2)*Math.sin(dLatRad/2) +
            //            Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(other.lat))*
            //            Math.sin(dLonRad/2)*Math.sin(dLonRad/2);
            // double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            // return R * c;
        }

        @Override
        public String toString() {
            return String.format("(%.4f, %.4f)", lat, lon);
        }
    }

    // =========================================================================
    // ENTITIES
    // =========================================================================

    /**
     * Driver entity with mutable location and status.
     *
     * THREAD SAFETY: Location is written from updateDriverLocation() and read
     * from requestRide()/getNearbyDrivers(). We use volatile for location
     * visibility across threads. For status, use the system-level lock.
     *
     * In a real system: driver state is stored in a distributed cache (Redis)
     * or DB so multiple app instances share the same view.
     */
    public static class Driver {
        private final String id;
        private final String name;
        private volatile Location location;  // volatile: safe cross-thread reads
        private DriverStatus status;
        private double rating;

        public Driver(String id, String name, Location initialLocation) {
            this.id = id;
            this.name = name;
            this.location = initialLocation;
            this.status = DriverStatus.AVAILABLE;
            this.rating = 4.5;
        }

        public String getId()            { return id; }
        public String getName()          { return name; }
        public Location getLocation()    { return location; }
        public DriverStatus getStatus()  { return status; }
        public double getRating()        { return rating; }

        // Called under system write lock
        public void setLocation(Location location) { this.location = location; }
        public void setStatus(DriverStatus status) { this.status = status; }
        public void setRating(double rating)       { this.rating = rating; }

        @Override
        public String toString() {
            return String.format("Driver[%s, %s, %s, %.1f★]", id, name, status, rating);
        }
    }

    public static class Rider {
        private final String id;
        private final String name;
        private Location location;

        public Rider(String id, String name, Location location) {
            this.id = id;
            this.name = name;
            this.location = location;
        }

        public String getId()          { return id; }
        public String getName()        { return name; }
        public Location getLocation()  { return location; }
        public void setLocation(Location l) { this.location = l; }

        @Override
        public String toString() {
            return String.format("Rider[%s, %s]", id, name);
        }
    }

    /**
     * Trip — represents a complete ride from request to completion.
     *
     * KEY INSIGHT: Trip is the central aggregate that links rider, driver,
     * locations, status, and financial data. It's the audit trail.
     *
     * In production: Trip is stored in DB with all state transitions logged
     * for billing, dispute resolution, and analytics.
     */
    public static class Trip {
        private static final AtomicInteger counter = new AtomicInteger(1);

        private final String tripId;
        private final Rider rider;
        private Driver driver;           // assigned on match
        private final Location pickup;
        private final Location dropoff;
        private TripStatus status;
        private double fare;
        private long startTimeMs;
        private long endTimeMs;

        public Trip(Rider rider, Location pickup, Location dropoff) {
            this.tripId = "TRIP-" + counter.getAndIncrement();
            this.rider = rider;
            this.pickup = pickup;
            this.dropoff = dropoff;
            this.status = TripStatus.REQUESTED;
        }

        public String getTripId()     { return tripId; }
        public Rider getRider()       { return rider; }
        public Driver getDriver()     { return driver; }
        public TripStatus getStatus() { return status; }
        public double getFare()       { return fare; }
        public Location getPickup()   { return pickup; }
        public Location getDropoff()  { return dropoff; }

        public void assignDriver(Driver d)   { this.driver = d; this.status = TripStatus.MATCHED; }
        public void start()                  { this.status = TripStatus.IN_PROGRESS; this.startTimeMs = System.currentTimeMillis(); }
        public void complete(double fare)    { this.status = TripStatus.COMPLETED; this.fare = fare; this.endTimeMs = System.currentTimeMillis(); }
        public void cancel()                 { this.status = TripStatus.CANCELLED; }

        @Override
        public String toString() {
            return String.format("Trip[%s, %s→%s, status=%s, fare=$%.2f, driver=%s]",
                    tripId, pickup, dropoff, status, fare,
                    driver != null ? driver.getName() : "unassigned");
        }
    }

    // =========================================================================
    // RIDE SHARING SYSTEM
    // =========================================================================

    // driverId → Driver
    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();
    // tripId → Trip
    private final Map<String, Trip> trips = new ConcurrentHashMap<>();
    // Lock for matching decisions (find nearest driver + assign atomically)
    private final ReentrantLock matchingLock = new ReentrantLock();

    // Fare constants
    private static final double BASE_FARE  = 2.0;   // $ flat fee
    private static final double RATE_PER_KM = 1.5;  // $ per km

    /**
     * Register a driver into the system.
     * COMPLEXITY: O(1)
     */
    public void registerDriver(Driver driver) {
        drivers.put(driver.getId(), driver);
        System.out.printf("Registered: %s at %s%n", driver, driver.getLocation());
    }

    /**
     * Request a ride: find nearest available driver, create and match a trip.
     *
     * KEY INSIGHT: The find-nearest + assign must be ATOMIC.
     * Without a lock: two riders could both "find" the same nearest driver,
     * both assign them, and the driver ends up with two simultaneous trips.
     * The matchingLock ensures only one match decision happens at a time.
     *
     * OPTIMIZATION: This is O(D) — scans all drivers to find nearest.
     * With QuadTree/Geohash: O(log D) or O(k) where k = nearby candidates.
     *
     * COMPLEXITY: O(D) where D = number of drivers
     *
     * @return Created and matched Trip
     * @throws IllegalStateException if no driver is available
     */
    public Trip requestRide(Rider rider, Location pickup, Location dropoff) {
        System.out.printf("%nRide requested by %s from %s to %s%n",
                rider.getName(), pickup, dropoff);

        matchingLock.lock();
        try {
            // Find nearest AVAILABLE driver to the pickup location
            Driver nearest = findNearestAvailableDriver(pickup);
            if (nearest == null) {
                throw new IllegalStateException("No available drivers nearby. Please try again.");
            }

            // Create trip and assign driver atomically
            Trip trip = new Trip(rider, pickup, dropoff);
            trip.assignDriver(nearest);
            nearest.setStatus(DriverStatus.ON_TRIP);

            trips.put(trip.getTripId(), trip);

            double distToPickup = nearest.getLocation().distanceTo(pickup);
            System.out.printf("Matched %s with %s (%.2f km away)%n",
                    trip.getTripId(), nearest.getName(), distToPickup);
            return trip;

        } finally {
            matchingLock.unlock();
        }
    }

    /**
     * Find the nearest AVAILABLE driver to a given location.
     *
     * ALGORITHM: Linear scan — O(D). Works for small fleets (<10k drivers per region).
     * For large fleets, replace with spatial index (QuadTree, R-Tree, Geohash).
     *
     * INTERVIEW DEEP-DIVE: QuadTree for ride matching
     *   - Root = entire city bounding box
     *   - Each node covers a rectangular region
     *   - If region has > threshold points, split into 4 quadrants
     *   - Query: start from root, recurse into quadrants that overlap search circle
     *   - O(log N) for balanced tree; update O(log N) on driver movement
     *   - Problem: frequent driver movement means frequent tree rebalancing
     *   - Solution: lazy update (update only when driver moves significantly)
     *
     * COMPLEXITY: O(D) this implementation
     *
     * @return Nearest available driver, or null if none available
     */
    private Driver findNearestAvailableDriver(Location pickup) {
        Driver nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Driver driver : drivers.values()) {
            if (driver.getStatus() != DriverStatus.AVAILABLE) continue;

            double dist = driver.getLocation().distanceTo(pickup);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = driver;
            }
        }
        return nearest;
    }

    /**
     * Start a trip: MATCHED → IN_PROGRESS.
     * Called when driver arrives at pickup and rider boards.
     *
     * COMPLEXITY: O(1)
     */
    public void startTrip(String tripId) {
        Trip trip = getTrip(tripId);
        if (trip.getStatus() != TripStatus.MATCHED) {
            throw new IllegalStateException("Trip " + tripId + " is not in MATCHED state");
        }
        trip.start();
        System.out.printf("Trip started: %s%n", tripId);
    }

    /**
     * Complete a trip: IN_PROGRESS → COMPLETED.
     * Calculates fare, frees the driver back to AVAILABLE.
     *
     * FARE: base + distance * rate_per_km
     *   Real platforms also add: time component, surge multiplier, tolls, tips
     *
     * KEY INSIGHT: Driver location is updated to the dropoff location —
     * they're now near the dropoff point for their next pickup.
     *
     * COMPLEXITY: O(1)
     */
    public double completeTrip(String tripId) {
        Trip trip = getTrip(tripId);
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Trip " + tripId + " is not IN_PROGRESS");
        }

        double distanceKm = trip.getPickup().distanceTo(trip.getDropoff());
        double fare = BASE_FARE + (distanceKm * RATE_PER_KM);
        // Round to 2 decimal places
        fare = Math.round(fare * 100.0) / 100.0;

        trip.complete(fare);

        // Free driver — update location to dropoff (driver is now near dropoff)
        Driver driver = trip.getDriver();
        driver.setLocation(trip.getDropoff());
        driver.setStatus(DriverStatus.AVAILABLE);

        System.out.printf("Trip completed: %s. Distance: %.2f km, Fare: $%.2f%n",
                tripId, distanceKm, fare);
        return fare;
    }

    /**
     * Update a driver's current GPS location.
     * Called by the driver's mobile app every few seconds.
     *
     * KEY INSIGHT: Location update is frequent (every 5s per driver).
     * With 100k drivers: 20,000 updates/second. Must be O(1) and non-blocking.
     * volatile on location field ensures visibility without locking.
     *
     * REAL SYSTEM: Driver location events → Kafka topic → consumed by matching service
     * and stored in Redis (low-latency geo queries using Redis GEOADD/GEORADIUS).
     *
     * COMPLEXITY: O(1)
     */
    public void updateDriverLocation(String driverId, Location newLocation) {
        Driver driver = drivers.get(driverId);
        if (driver == null) throw new IllegalArgumentException("Driver not found: " + driverId);
        driver.setLocation(newLocation);
        System.out.printf("Driver %s moved to %s%n", driver.getName(), newLocation);
    }

    /**
     * Get all available drivers within a radius.
     *
     * KEY INSIGHT: This is the "driver availability map" shown in the Uber app.
     * In production: query Redis GEORADIUS command for O(N+log M) performance
     * where N = results, M = total drivers.
     *
     * COMPLEXITY: O(D)
     */
    public List<Driver> getNearbyDrivers(Location center, double radiusKm) {
        return drivers.values().stream()
                .filter(d -> d.getStatus() == DriverStatus.AVAILABLE)
                .filter(d -> d.getLocation().distanceTo(center) <= radiusKm)
                .sorted(Comparator.comparingDouble(d -> d.getLocation().distanceTo(center)))
                .collect(Collectors.toList());
    }

    private Trip getTrip(String tripId) {
        Trip trip = trips.get(tripId);
        if (trip == null) throw new IllegalArgumentException("Trip not found: " + tripId);
        return trip;
    }

    // =========================================================================
    // MAIN — Demo
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Ride Sharing System Demo ===\n");

        RideSharingSystem system = new RideSharingSystem();

        // Register drivers
        Driver d1 = new Driver("D1", "Alice",  new Location(12.9716, 77.5946));  // Bangalore center
        Driver d2 = new Driver("D2", "Bob",    new Location(12.9750, 77.5980));
        Driver d3 = new Driver("D3", "Charlie", new Location(12.9800, 77.5900));
        system.registerDriver(d1);
        system.registerDriver(d2);
        system.registerDriver(d3);

        // View nearby drivers
        Location user = new Location(12.9720, 77.5950);
        System.out.println("\nNearby drivers (within 2 km):");
        List<Driver> nearby = system.getNearbyDrivers(user, 2.0);
        nearby.forEach(d -> System.out.printf("  %s at %.3f km%n",
                d, d.getLocation().distanceTo(user)));

        // Request ride 1
        Rider rider1 = new Rider("R1", "Eve", user);
        Location pickup1  = new Location(12.9720, 77.5950);
        Location dropoff1 = new Location(12.9900, 77.6100);  // ~3.3 km

        Trip trip1 = system.requestRide(rider1, pickup1, dropoff1);
        System.out.println("Trip created: " + trip1);

        // Request ride 2 simultaneously — should get a different driver
        Rider rider2 = new Rider("R2", "Frank", new Location(12.9760, 77.5970));
        Location pickup2  = new Location(12.9760, 77.5970);
        Location dropoff2 = new Location(12.9850, 77.6000);  // ~1.7 km
        Trip trip2 = system.requestRide(rider2, pickup2, dropoff2);
        System.out.println("Trip created: " + trip2);

        // Start and complete trip 1
        System.out.println();
        system.startTrip(trip1.getTripId());
        double fare1 = system.completeTrip(trip1.getTripId());
        System.out.printf("Rider %s paid: $%.2f%n", rider1.getName(), fare1);

        // Start and complete trip 2
        system.startTrip(trip2.getTripId());
        double fare2 = system.completeTrip(trip2.getTripId());
        System.out.printf("Rider %s paid: $%.2f%n", rider2.getName(), fare2);

        // Alice (D1) is now near the dropoff and can take new rides
        System.out.println("\nDriver statuses after trips:");
        system.drivers.values().forEach(d ->
                System.out.printf("  %s at %s%n", d, d.getLocation()));

        // Test no-driver-available scenario
        System.out.println("\n--- No drivers available test ---");
        // Mark all drivers offline
        system.drivers.values().forEach(d -> d.setStatus(DriverStatus.OFFLINE));
        try {
            Rider rider3 = new Rider("R3", "Grace", user);
            system.requestRide(rider3, pickup1, dropoff1);
        } catch (IllegalStateException e) {
            System.out.println("Expected: " + e.getMessage());
        }

        System.out.println("\n=== System Summary ===");
        System.out.printf("Total trips: %d%n", system.trips.size());
        system.trips.values().forEach(t -> System.out.printf("  %s%n", t));

        /*
         * INTERVIEW Q: "How would you implement surge pricing?"
         * A: Monitor demand/supply ratio per geographic zone in real-time.
         *    surgeMultiplier = f(activeRequests / availableDrivers)
         *    Apply multiplier to base fare. Cap at some maximum (e.g., 5x).
         *    Notify riders with countdown so they can wait for surge to drop.
         *    Uber uses a model where showing the surge price increases driver supply
         *    (drivers go online when they see high prices in an area).
         *
         * INTERVIEW Q: "How to handle driver going offline mid-trip?"
         * A: Driver app sends heartbeats every 30s. If no heartbeat for 90s,
         *    mark driver as POTENTIALLY_OFFLINE.
         *    If trip is active: alert ops team, attempt reassignment (cancel + re-request).
         *    Driver's state is restored on reconnect; trip isn't double-billed.
         *
         * INTERVIEW Q: "How to scale location updates for 1M+ drivers?"
         * A: Kafka topic with driverId as partition key → consumed by geo-index service.
         *    Redis GEOADD for fast proximity queries.
         *    In-memory QuadTree per region (city) rebuilt from Redis periodically.
         *    Delta compression on location stream (send only changed positions).
         */
    }
}
