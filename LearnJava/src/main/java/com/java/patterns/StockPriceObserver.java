package com.java.patterns.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER PATTERN — Event-Driven Notification System
 *
 * CONCEPT:
 *   Observer Pattern defines a one-to-many dependency between objects.
 *   When the SUBJECT (Observable) changes state, ALL registered OBSERVERS are notified.
 *
 *   Two roles:
 *   - Subject (StockMarket): holds state, manages a list of observers, notifies them on change.
 *   - Observer (PriceObserver): receives notification and reacts.
 *
 * TERMINOLOGY VARIANTS:
 *   Subject = Publisher = Observable
 *   Observer = Subscriber = Listener = Handler
 *
 * REAL-WORLD USES IN JAVA:
 *   - Java's built-in java.util.Observer/Observable (deprecated in Java 9 — use alternatives)
 *   - JavaFX Property binding (ChangeListener)
 *   - Spring ApplicationEvent / ApplicationListener — Spring's event bus uses Observer
 *   - Kafka: producers publish events, consumers observe
 *   - GUI event listeners: button.addActionListener(listener)
 *   - Android: LiveData observers in MVVM architecture
 *
 * WHY NOT USE java.util.Observable?
 *   - It's a class, not an interface → forces single inheritance
 *   - setChanged() must be called manually before notifyObservers() — error-prone
 *   - Deprecated in Java 9. Use custom interfaces (as shown here) or EventBus libraries.
 *
 * INTERVIEW FREQUENCY: Medium — common in system design discussions ("notification system")
 *   and in OOP design rounds ("how would you design a stock price alert system?").
 *
 * COMMON INTERVIEW QUESTIONS:
 *   1. "What is the difference between Observer and Pub-Sub?"
 *      Observer: direct reference between Subject and Observer (tightly coupled).
 *      Pub-Sub: message broker/bus between publisher and subscriber (loosely coupled).
 *      Kafka is Pub-Sub. GUI listeners are Observer.
 *   2. "What are the risks of Observer?" → Memory leaks if observers aren't unregistered.
 *   3. "How does Spring's ApplicationEvent relate to Observer?"
 *      Spring implements Observer: @EventListener methods are observers,
 *      ApplicationEventPublisher is the subject.
 */
public class StockPriceObserver {

    // =========================================================================
    // Observer Interface — the contract observers must implement
    // =========================================================================

    /**
     * All observers must implement this interface.
     * The subject calls update() on every registered observer when price changes.
     */
    interface PriceObserver {
        /**
         * Called by StockMarket whenever a stock price changes.
         *
         * @param symbol    the stock ticker (e.g., "GOOGL")
         * @param oldPrice  the previous price
         * @param newPrice  the updated price
         */
        void update(String symbol, double oldPrice, double newPrice);
    }

    // =========================================================================
    // Subject — StockMarket
    // =========================================================================

    /**
     * The Subject (Observable). Holds stock prices and notifies observers on changes.
     *
     * RESPONSIBILITIES:
     *   1. Maintain a list of registered observers.
     *   2. Allow observers to subscribe/unsubscribe (register/deregister).
     *   3. Notify all observers when state (stock price) changes.
     */
    static class StockMarket {
        private final List<PriceObserver> observers = new ArrayList<>();

        // Current stock symbol and price
        private String symbol;
        private double price;

        StockMarket(String symbol, double initialPrice) {
            this.symbol = symbol;
            this.price = initialPrice;
        }

        /** Register an observer — it will receive future notifications */
        public void addObserver(PriceObserver observer) {
            observers.add(observer);
        }

        /**
         * Unregister an observer — it will no longer receive notifications.
         * IMPORTANT: Always call this when an observer is no longer needed.
         * Failing to do so is a common source of MEMORY LEAKS in Observer pattern.
         */
        public void removeObserver(PriceObserver observer) {
            observers.remove(observer);
        }

        /**
         * Update the stock price and notify all registered observers.
         *
         * DESIGN DECISION: We save oldPrice before updating, so observers
         * can see both the old and new price — useful for calculating % change.
         */
        public void setPrice(double newPrice) {
            double oldPrice = this.price;
            this.price = newPrice;

            // Only notify if price actually changed (avoid spurious notifications)
            if (oldPrice != newPrice) {
                notifyObservers(oldPrice, newPrice);
            }
        }

        public double getPrice() { return price; }
        public String getSymbol() { return symbol; }

        /**
         * Notifies all registered observers about the price change.
         * Uses a snapshot of the list to avoid ConcurrentModificationException
         * if an observer's update() triggers add/remove (defensive copy).
         */
        private void notifyObservers(double oldPrice, double newPrice) {
            // Defensive copy — prevents issues if an observer modifies the list during iteration
            List<PriceObserver> snapshot = new ArrayList<>(observers);
            for (PriceObserver observer : snapshot) {
                observer.update(symbol, oldPrice, newPrice);
            }
        }
    }

    // =========================================================================
    // Concrete Observers
    // =========================================================================

    /** Observer 1: Email Alert — sends an email when price changes by > 5% */
    static class EmailAlert implements PriceObserver {
        private final String email;
        private final double alertThreshold; // % change that triggers an alert

        EmailAlert(String email, double alertThresholdPercent) {
            this.email = email;
            this.alertThreshold = alertThresholdPercent;
        }

        @Override
        public void update(String symbol, double oldPrice, double newPrice) {
            double changePercent = Math.abs((newPrice - oldPrice) / oldPrice * 100);

            if (changePercent >= alertThreshold) {
                // In production: send an actual email via JavaMailSender / SES
                String direction = newPrice > oldPrice ? "↑" : "↓";
                System.out.printf("[EmailAlert → %s] %s %s: %.2f → %.2f (%.1f%% change)%n",
                        email, symbol, direction, oldPrice, newPrice, changePercent);
            } else {
                System.out.printf("[EmailAlert] %s: price change %.1f%% below threshold (%.0f%%)%n",
                        symbol, changePercent, alertThreshold);
            }
        }
    }

    /** Observer 2: SMS Alert — sends SMS for any price change */
    static class SMSAlert implements PriceObserver {
        private final String phoneNumber;

        SMSAlert(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        @Override
        public void update(String symbol, double oldPrice, double newPrice) {
            // In production: send SMS via Twilio / SNS
            System.out.printf("[SMSAlert → %s] %s changed: $%.2f → $%.2f%n",
                    phoneNumber, symbol, oldPrice, newPrice);
        }
    }

    /** Observer 3: Dashboard — updates the live price display on a UI */
    static class Dashboard implements PriceObserver {
        private final String dashboardName;

        Dashboard(String dashboardName) {
            this.dashboardName = dashboardName;
        }

        @Override
        public void update(String symbol, double oldPrice, double newPrice) {
            double change = newPrice - oldPrice;
            String arrow = change > 0 ? "▲" : "▼";
            // In production: push to WebSocket / SSE for real-time UI update
            System.out.printf("[Dashboard: %s] %s %s $%.2f (was $%.2f)%n",
                    dashboardName, symbol, arrow, newPrice, oldPrice);
        }
    }

    // =========================================================================
    // main()
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Observer Pattern: Stock Price Alert System ===\n");

        // Create the subject
        StockMarket google = new StockMarket("GOOGL", 150.00);

        // Create observers
        PriceObserver emailAlert   = new EmailAlert("trader@example.com", 2.0); // Alert if > 2% change
        PriceObserver smsAlert     = new SMSAlert("+91-9999999999");
        PriceObserver dashboard    = new Dashboard("Main Trading Desk");

        // Register observers — they will now receive updates
        google.addObserver(emailAlert);
        google.addObserver(smsAlert);
        google.addObserver(dashboard);

        System.out.println("--- Price Update 1: $150.00 → $153.00 (2% rise) ---");
        google.setPrice(153.00); // Should trigger all 3 observers

        System.out.println("\n--- Price Update 2: $153.00 → $153.75 (small change) ---");
        google.setPrice(153.75); // Email threshold not met, but SMS and Dashboard still notify

        System.out.println("\n--- Price Update 3: Same price (no notification) ---");
        google.setPrice(153.75); // No change → notifyObservers NOT called

        System.out.println("\n--- Unregister SMS Observer ---");
        google.removeObserver(smsAlert); // SMS alert is no longer interested

        System.out.println("\n--- Price Update 4: $153.75 → $145.00 (crash!) ---");
        google.setPrice(145.00); // Only emailAlert and dashboard notified now

        System.out.println("\n--- Key Takeaways ---");
        System.out.println("1. StockMarket knows nothing about EmailAlert/SMS/Dashboard internals.");
        System.out.println("2. Adding a new AlertType (e.g., SlackAlert): implement PriceObserver, addObserver().");
        System.out.println("3. Spring equivalent: @EventListener method = observer, ApplicationEventPublisher = subject.");
    }
}
