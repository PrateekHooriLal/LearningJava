package com.java.patterns;

import java.util.*;

/**
 * ============================================================
 * BEHAVIORAL PATTERNS — 9 GoF Behavioral Design Patterns
 * ============================================================
 *
 * CONCEPT:
 *   Behavioral patterns deal with COMMUNICATION and RESPONSIBILITY between
 *   objects. They describe how objects interact and distribute responsibility,
 *   making complex flows manageable.
 *
 * WHEN TO USE:
 *   - When you want to decouple senders from receivers (Chain, Command, Mediator)
 *   - When you need to capture/restore state (Memento)
 *   - When an object's behavior must change based on its state (State)
 *   - When you need interchangeable algorithms (Strategy)
 *   - When you want to add operations without modifying classes (Visitor)
 *
 * TIME/SPACE:
 *   Most behavioral patterns: O(1) dispatch overhead per call.
 *   Chain of Responsibility: O(n) worst case (traverses all handlers).
 *   Command with undo: O(n) space for history stack.
 *
 * INTERVIEW ANGLE:
 *   - "Strategy vs Template Method": Strategy=composition (inject algorithm),
 *     Template=inheritance (override steps). Prefer composition (Strategy).
 *   - "Command pattern enables undo/redo, transaction log, and queue of work."
 *   - "Visitor uses double dispatch — why? To resolve both receiver type
 *     AND visitor type at runtime."
 *   - "State vs Strategy": both delegate behavior. State object knows the
 *     context and transitions itself; Strategy is passive (context decides).
 *
 * REAL-WORLD USE:
 *   Chain: Spring Security filter chain, Servlet filters, Express.js middleware
 *   Command: Git commits, database transactions, job queues
 *   State: TCP connection, vending machine, traffic light
 *   Strategy: java.util.Comparator, sorting algorithms, payment methods
 *   Visitor: Compiler AST traversal, XML DOM processing
 *   Mediator: Air traffic control, event bus / message broker
 */
public class BehavioralPatterns {

    // =========================================================
    // 1. CHAIN OF RESPONSIBILITY
    // =========================================================
    //
    // CONCEPT: Pass a request along a chain of handlers. Each handler
    //   decides to handle the request OR pass it to the next handler.
    //
    // WHEN TO USE:
    //   - More than one object may handle a request (unknown at compile time)
    //   - Set of handlers should be specifiable dynamically
    //   - HTTP middleware, logging levels, approval workflows
    //
    // REAL JAVA EXAMPLE:
    //   javax.servlet.Filter chain
    //   Spring Security FilterChainProxy
    //   java.util.logging.Logger (parent handlers)
    //
    // GOTCHA: A request may go unhandled if no handler in the chain handles it.
    //   Always have a default/fallback handler at the end.
    //   Also: chain can be misconfigured (wrong order, missing links).

    // Handler interface with fluent builder-style chaining
    interface HttpHandler {
        void handle(HttpRequest request, HttpResponse response);
        // Fluent builder: auth.then(rateLimit).then(logging).then(actual)
        default HttpHandler then(HttpHandler next) {
            return (req, res) -> {
                this.handle(req, res);
                if (!res.isHandled()) {
                    next.handle(req, res);
                }
            };
        }
    }

    static class HttpRequest {
        final String path;
        final String token;
        final String userId;
        HttpRequest(String path, String token, String userId) {
            this.path = path; this.token = token; this.userId = userId;
        }
    }
    static class HttpResponse {
        private boolean handled = false;
        private int statusCode = 200;
        private String body = "";
        boolean isHandled() { return handled; }
        void setHandled(boolean h) { this.handled = h; }
        void send(int code, String body) {
            this.statusCode = code; this.body = body; this.handled = true;
            System.out.println("  [Response] " + code + ": " + body);
        }
    }

    // Each middleware: auth → rate-limit → logging → actual handler
    static class AuthMiddleware implements HttpHandler {
        @Override public void handle(HttpRequest req, HttpResponse res) {
            System.out.println("  [Auth] Checking token: " + req.token);
            if (req.token == null || req.token.isEmpty()) {
                res.send(401, "Unauthorized");
            }
            // No else: if auth passes, do nothing — let chain continue
        }
    }

    static class RateLimitMiddleware implements HttpHandler {
        private final Map<String, Integer> callCounts = new HashMap<>();
        private final int maxCalls;
        RateLimitMiddleware(int maxCalls) { this.maxCalls = maxCalls; }

        @Override public void handle(HttpRequest req, HttpResponse res) {
            int count = callCounts.merge(req.userId, 1, Integer::sum);
            System.out.println("  [RateLimit] User " + req.userId + ": call #" + count);
            if (count > maxCalls) {
                res.send(429, "Too Many Requests");
            }
        }
    }

    static class LoggingMiddleware implements HttpHandler {
        @Override public void handle(HttpRequest req, HttpResponse res) {
            System.out.println("  [Log] " + req.path + " from user=" + req.userId);
            // Logging doesn't stop the chain
        }
    }

    static class ActualHandler implements HttpHandler {
        @Override public void handle(HttpRequest req, HttpResponse res) {
            res.send(200, "OK: " + req.path);
        }
    }

    // =========================================================
    // 2. COMMAND PATTERN
    // =========================================================
    //
    // CONCEPT: Encapsulate a request as an object, letting you parameterize
    //   clients with different requests, queue/log requests, and support undo.
    //
    // WHEN TO USE:
    //   - Need undo/redo functionality
    //   - Need to log, queue, or serialize requests
    //   - Need to support macro operations (composite command)
    //   - Decoupling the invoker (who calls) from the receiver (what executes)
    //
    // REAL JAVA EXAMPLE:
    //   javax.swing.Action (Swing UI commands)
    //   java.lang.Runnable (execute without undo)
    //   java.util.concurrent.Callable
    //   Git: every commit is a Command, git revert is the undo
    //
    // GOTCHA: Undo can be tricky. Some operations are inherently irreversible
    //   (e.g., deleting a file). Store the PREVIOUS state in the command object.

    interface EditorCommand {
        void execute();
        void undo();
    }

    // Receiver: the actual text buffer
    static class TextBuffer {
        private final StringBuilder content = new StringBuilder();
        public String getContent() { return content.toString(); }
        public void append(String text) { content.append(text); }
        public void delete(int start, int len) { content.delete(start, start + len); }
        public int length() { return content.length(); }
    }

    // CONCRETE COMMANDS: each knows how to execute AND undo
    static class InsertCommand implements EditorCommand {
        private final TextBuffer buffer;
        private final String text;
        private final int position; // where to insert

        InsertCommand(TextBuffer buffer, String text, int position) {
            this.buffer = buffer; this.text = text; this.position = position;
        }

        @Override public void execute() {
            // KEY INSIGHT: Command stores enough info to both execute AND undo
            buffer.append(text); // simplified: append at end
            System.out.println("    [Cmd] Inserted: '" + text + "'");
        }

        @Override public void undo() {
            // Remove what was inserted
            int len = text.length();
            int start = buffer.length() - len;
            if (start >= 0) buffer.delete(start, len);
            System.out.println("    [Cmd] Undid insert of: '" + text + "'");
        }
    }

    // MACRO COMMAND: composite of commands
    static class MacroCommand implements EditorCommand {
        private final List<EditorCommand> commands;
        MacroCommand(EditorCommand... cmds) { commands = List.of(cmds); }

        @Override public void execute() { commands.forEach(EditorCommand::execute); }

        @Override public void undo() {
            // Undo in REVERSE order — critical!
            ListIterator<EditorCommand> it = commands.listIterator(commands.size());
            while (it.hasPrevious()) it.previous().undo();
        }
    }

    // INVOKER: manages command execution and history (undo/redo stack)
    static class TextEditor {
        private final TextBuffer buffer = new TextBuffer();
        // KEY INSIGHT: Use Deque as stack. ArrayDeque is faster than Stack.
        private final Deque<EditorCommand> undoStack = new ArrayDeque<>();
        private final Deque<EditorCommand> redoStack = new ArrayDeque<>();

        public void executeCommand(EditorCommand cmd) {
            cmd.execute();
            undoStack.push(cmd);
            redoStack.clear(); // new action clears redo history (like real editors)
        }

        public void undo() {
            if (undoStack.isEmpty()) { System.out.println("    Nothing to undo"); return; }
            EditorCommand cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }

        public void redo() {
            if (redoStack.isEmpty()) { System.out.println("    Nothing to redo"); return; }
            EditorCommand cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
        }

        public String getContent() { return buffer.getContent(); }
        public TextBuffer getBuffer() { return buffer; }
    }

    // =========================================================
    // 3. INTERPRETER PATTERN
    // =========================================================
    //
    // CONCEPT: Define a grammar for a language, and provide an interpreter
    //   that uses the grammar to interpret sentences. Use composite to represent
    //   grammatical rules.
    //
    // WHEN TO USE:
    //   - Parsing simple grammars (DSLs, configuration, math expressions)
    //   - When grammar is simple and efficiency is not critical
    //   - NOTE: For complex grammars, use parser generators (ANTLR) instead
    //
    // REAL JAVA EXAMPLE:
    //   java.util.regex.Pattern — regex is an interpreter
    //   Spring EL (SpEL), OGNL
    //   SQL parsing
    //
    // GOTCHA: Interpreter can cause a class explosion for complex grammars.
    //   Each grammar rule becomes a class. Consider alternatives for complex langs.

    interface Expression {
        int interpret();
    }

    static class NumberExpression implements Expression {
        private final int value;
        NumberExpression(int value) { this.value = value; }
        @Override public int interpret() { return value; }
    }

    static class AddExpression implements Expression {
        private final Expression left, right;
        AddExpression(Expression left, Expression right) { this.left = left; this.right = right; }
        @Override public int interpret() { return left.interpret() + right.interpret(); }
    }

    static class MultiplyExpression implements Expression {
        private final Expression left, right;
        MultiplyExpression(Expression left, Expression right) { this.left = left; this.right = right; }
        @Override public int interpret() { return left.interpret() * right.interpret(); }
    }

    // =========================================================
    // 4. MEDIATOR PATTERN
    // =========================================================
    //
    // CONCEPT: Define an object that encapsulates how a set of objects interact.
    //   Promotes loose coupling by keeping objects from referring to each other
    //   explicitly. Many-to-many → many-to-one (through mediator).
    //
    // WHEN TO USE:
    //   - Many objects communicate in complex ways (lots of cross-references)
    //   - Want to reuse components but they're coupled to too many others
    //   - Air traffic control, chat rooms, event buses, UI form logic
    //
    // REAL JAVA EXAMPLE:
    //   java.util.Timer — mediates between tasks and scheduling
    //   Executor/ExecutorService — mediates between tasks and threads
    //   Spring ApplicationEventPublisher
    //
    // GOTCHA: Mediator itself can become a "God Object" if not careful.
    //   It centralizes ALL coordination logic — keep it focused.

    interface ChatMediator {
        void sendMessage(String message, ChatUser sender);
        void addUser(ChatUser user);
    }

    static class ChatRoom implements ChatMediator {
        private final List<ChatUser> users = new ArrayList<>();

        @Override public void addUser(ChatUser user) { users.add(user); }

        @Override public void sendMessage(String message, ChatUser sender) {
            // KEY INSIGHT: Users don't hold references to each other.
            //   ChatRoom is the hub. Removing one user doesn't affect others.
            for (ChatUser user : users) {
                if (user != sender) { // don't send to self
                    user.receive(message, sender.getName());
                }
            }
        }
    }

    static class ChatUser {
        private final String name;
        private final ChatMediator mediator;

        ChatUser(String name, ChatMediator mediator) {
            this.name = name;
            this.mediator = mediator;
            mediator.addUser(this);
        }

        public String getName() { return name; }

        public void send(String message) {
            System.out.println("  [" + name + "] sends: " + message);
            mediator.sendMessage(message, this); // route through mediator
        }

        public void receive(String message, String from) {
            System.out.println("  [" + name + "] receives from [" + from + "]: " + message);
        }
    }

    // =========================================================
    // 5. MEMENTO PATTERN
    // =========================================================
    //
    // CONCEPT: Without violating encapsulation, capture and externalize an
    //   object's internal state so it can be restored later.
    //
    // THREE ROLES:
    //   ORIGINATOR: creates mementos and restores from them (TextEditorWithHistory)
    //   MEMENTO: stores the state (immutable snapshot)
    //   CARETAKER: holds mementos, doesn't look inside them (HistoryManager)
    //
    // WHEN TO USE:
    //   - Undo/redo mechanisms
    //   - Snapshots / save points in games
    //   - Transactional operations (rollback on failure)
    //
    // REAL JAVA EXAMPLE:
    //   java.io.Serializable (serialize = memento)
    //   javax.swing.undo.UndoManager
    //   git stash, git reset
    //
    // GOTCHA: Mementos can be memory-expensive if state is large.
    //   Solution: incremental mementos (store only the DIFF, not full state).
    //   Also: Memento must be truly immutable to prevent state corruption.

    // MEMENTO: immutable snapshot of state
    static final class EditorMemento {
        private final String content; // immutable copy of state
        private final int cursorPos;

        // KEY INSIGHT: Constructor takes value (not reference) — true copy
        EditorMemento(String content, int cursorPos) {
            this.content = content; this.cursorPos = cursorPos;
        }
        // Package-private accessors — caretaker can't inspect these
        String getContent() { return content; }
        int getCursorPos() { return cursorPos; }
    }

    // ORIGINATOR: creates and restores mementos
    static class OriginalEditor {
        private String content = "";
        private int cursorPos = 0;

        public void type(String text) {
            content += text;
            cursorPos = content.length();
        }

        public EditorMemento save() {
            System.out.println("    [Editor] Saving state: '" + content + "' cursor=" + cursorPos);
            return new EditorMemento(content, cursorPos); // snapshot
        }

        public void restore(EditorMemento memento) {
            this.content = memento.getContent();
            this.cursorPos = memento.getCursorPos();
            System.out.println("    [Editor] Restored to: '" + content + "' cursor=" + cursorPos);
        }

        public String getContent() { return content; }
    }

    // CARETAKER: manages snapshots, doesn't interpret them
    static class HistoryManager {
        private final Deque<EditorMemento> history = new ArrayDeque<>();

        public void push(EditorMemento memento) { history.push(memento); }

        public EditorMemento pop() {
            return history.isEmpty() ? null : history.pop();
        }

        public int size() { return history.size(); }
    }

    // =========================================================
    // 6. STATE PATTERN
    // =========================================================
    //
    // CONCEPT: Allow an object to alter its behavior when its internal state
    //   changes. The object will appear to change its class.
    //   Essentially: represent each state as its own class.
    //
    // STATE vs SWITCH STATEMENT:
    //   switch (state) { case RED: ...; case GREEN: ...; } — hard to extend.
    //     Adding a new state requires modifying the switch (Open/Closed violation).
    //   State Pattern: each state is a class. Add new state = add new class.
    //     Open for extension, closed for modification.
    //
    // WHEN TO USE:
    //   - Object behaves differently based on its state
    //   - State transitions are explicit and numerous
    //   - Eliminate large state-dependent conditionals
    //
    // REAL JAVA EXAMPLE:
    //   Thread states: NEW, RUNNABLE, BLOCKED, WAITING, TERMINATED
    //   TCP connection states: CLOSED, LISTEN, ESTABLISHED, TIME_WAIT
    //   Order lifecycle: PENDING, PAID, SHIPPED, DELIVERED, CANCELLED

    interface TrafficLightState {
        void handle(TrafficLight context);
        String getColor();
    }

    static class TrafficLight {
        private TrafficLightState currentState;

        TrafficLight() {
            currentState = new RedState(); // initial state
        }

        // KEY INSIGHT: Context delegates behavior to current state object.
        //   State object decides the next state (or context could decide).
        public void change() { currentState.handle(this); }
        public void setState(TrafficLightState state) { this.currentState = state; }
        public String getColor() { return currentState.getColor(); }
    }

    // Each state class encapsulates: what to do NOW and what state comes NEXT
    static class RedState implements TrafficLightState {
        @Override public void handle(TrafficLight ctx) {
            System.out.println("    [RED] Stop! Changing to GREEN...");
            ctx.setState(new GreenState()); // transition
        }
        @Override public String getColor() { return "RED"; }
    }

    static class GreenState implements TrafficLightState {
        @Override public void handle(TrafficLight ctx) {
            System.out.println("    [GREEN] Go! Changing to YELLOW...");
            ctx.setState(new YellowState());
        }
        @Override public String getColor() { return "GREEN"; }
    }

    static class YellowState implements TrafficLightState {
        @Override public void handle(TrafficLight ctx) {
            System.out.println("    [YELLOW] Caution! Changing to RED...");
            ctx.setState(new RedState());
        }
        @Override public String getColor() { return "YELLOW"; }
    }

    // =========================================================
    // 7. STRATEGY PATTERN
    // =========================================================
    //
    // CONCEPT: Define a family of algorithms, encapsulate each one,
    //   and make them interchangeable. Strategy lets the algorithm vary
    //   independently from clients that use it.
    //
    // KEY CONNECTION: java.util.Comparator IS the Strategy pattern.
    //   Collections.sort(list, comparator) — you inject the sort strategy.
    //   The sorting algorithm uses the comparator strategy for comparisons.
    //
    // STRATEGY vs TEMPLATE METHOD:
    //   Strategy: algorithm is INJECTED (composition). Prefer for flexibility.
    //   Template Method: algorithm INHERITED (subclass overrides steps).
    //     Use Template when the algorithm skeleton is fixed and only a step varies.
    //
    // WHEN TO USE:
    //   - Multiple variants of an algorithm
    //   - Need to switch algorithms at runtime
    //   - Want to eliminate conditionals for algorithm selection
    //
    // REAL JAVA EXAMPLE:
    //   java.util.Comparator
    //   java.util.concurrent.RejectedExecutionHandler (thread pool policy)
    //   Spring's ResourceLoader, DataSource selection

    @FunctionalInterface // KEY: Strategy is naturally a functional interface
    interface SortStrategy {
        void sort(int[] arr);
    }

    // Concrete strategies — each is independently testable
    static class BubbleSortStrategy implements SortStrategy {
        @Override public void sort(int[] arr) {
            // O(n²) time, O(1) space — stable, good for nearly sorted data
            int n = arr.length;
            for (int i = 0; i < n - 1; i++) {
                for (int j = 0; j < n - i - 1; j++) {
                    if (arr[j] > arr[j + 1]) {
                        int tmp = arr[j]; arr[j] = arr[j+1]; arr[j+1] = tmp;
                    }
                }
            }
        }
    }

    static class InsertionSortStrategy implements SortStrategy {
        @Override public void sort(int[] arr) {
            // O(n²) worst, O(n) best (already sorted) — stable, great for small/nearly-sorted
            for (int i = 1; i < arr.length; i++) {
                int key = arr[i], j = i - 1;
                while (j >= 0 && arr[j] > key) { arr[j + 1] = arr[j]; j--; }
                arr[j + 1] = key;
            }
        }
    }

    // Context: uses whichever strategy is injected
    static class Sorter {
        private SortStrategy strategy; // inject via constructor or setter

        Sorter(SortStrategy strategy) { this.strategy = strategy; }

        // Strategy can be changed at RUNTIME (key differentiator from Template Method)
        public void setStrategy(SortStrategy strategy) { this.strategy = strategy; }

        public int[] sort(int[] arr) {
            int[] copy = Arrays.copyOf(arr, arr.length);
            strategy.sort(copy);
            return copy;
        }
    }

    // =========================================================
    // 8. TEMPLATE METHOD PATTERN
    // =========================================================
    //
    // CONCEPT: Define the skeleton of an algorithm in a method, deferring
    //   some steps to subclasses. Template Method lets subclasses redefine
    //   certain steps without changing the algorithm's structure.
    //
    // TEMPLATE METHOD vs STRATEGY:
    //   Template: uses INHERITANCE. The "template" method is final.
    //     - Rigid: algorithm structure fixed at compile time in the abstract class
    //     - Simpler: no need to pass strategy objects
    //   Strategy: uses COMPOSITION. The algorithm is a pluggable object.
    //     - Flexible: algorithm can change at runtime
    //     - More testable: can mock/stub the strategy
    //   RULE: Prefer Strategy (composition over inheritance).
    //     Use Template when: algorithm IS the type, and subclasses are few.
    //
    // REAL JAVA EXAMPLE:
    //   java.util.AbstractList (get(), size() abstract; add(), remove() have defaults)
    //   java.io.InputStream (read(byte[]) template uses abstract read())
    //   Spring's JdbcTemplate, RestTemplate, HibernateTemplate
    //   TestCase in JUnit 3 (setUp/tearDown hooks)

    static abstract class DataMiner {
        // TEMPLATE METHOD: final — subclasses can't change the algorithm structure
        public final void mine(String source) {
            System.out.println("    [DataMiner] Mining: " + source);
            String rawData = extractData(source);   // hook — subclass-specific
            String parsed = parseData(rawData);     // hook — subclass-specific
            String analyzed = analyzeData(parsed);  // hook — subclass-specific
            generateReport(analyzed);               // concrete — same for all
        }

        // ABSTRACT HOOKS: subclasses must implement these
        protected abstract String extractData(String source);
        protected abstract String parseData(String rawData);
        protected abstract String analyzeData(String parsed);

        // CONCRETE METHOD: shared algorithm step — not overridable
        private void generateReport(String data) {
            System.out.println("    [DataMiner] Report: " + data);
        }
    }

    static class CsvDataMiner extends DataMiner {
        @Override protected String extractData(String source) { return "CSV_RAW[" + source + "]"; }
        @Override protected String parseData(String raw) { return "CSV_PARSED[" + raw + "]"; }
        @Override protected String analyzeData(String parsed) { return "CSV_ANALYZED[" + parsed + "]"; }
    }

    static class JsonDataMiner extends DataMiner {
        @Override protected String extractData(String source) { return "JSON_RAW[" + source + "]"; }
        @Override protected String parseData(String raw) { return "JSON_PARSED[" + raw + "]"; }
        @Override protected String analyzeData(String parsed) { return "JSON_ANALYZED[" + parsed + "]"; }
    }

    // =========================================================
    // 9. VISITOR PATTERN
    // =========================================================
    //
    // CONCEPT: Let you add further operations to objects without modifying them.
    //   Separate algorithm from object structure by passing a "visitor" object.
    //
    // DOUBLE DISPATCH EXPLANATION:
    //   Java uses SINGLE dispatch: method resolution based on ONE type at runtime.
    //   Problem: area(shape) — if `shape` is Circle, we need CircleArea logic.
    //     Can't overload on runtime type (only static type is resolved).
    //   Solution: shape.accept(visitor) — dispatches on Shape type (1st dispatch).
    //     Inside accept(): visitor.visit(this) — `this` is Circle, dispatches on
    //     visitor type (2nd dispatch). = DOUBLE DISPATCH.
    //
    // WHEN TO USE:
    //   - Need to add operations to a class hierarchy without modifying it
    //   - Operations on an object structure depend on concrete types
    //   - Compiler AST traversal, XML DOM processing, dependency injection
    //
    // GOTCHA: Adding a new ELEMENT type requires updating ALL visitors.
    //   Adding a new VISITOR (operation) is easy — no existing code changes.
    //   So: Visitor is good when elements are STABLE, operations are VARIED.
    //   Composite/polymorphism is good when operations are STABLE, elements are VARIED.
    //
    // REAL JAVA EXAMPLE:
    //   javax.lang.model.element.ElementVisitor (Java annotation processing)
    //   java.nio.file.FileVisitor (walk file tree)
    //   ASM bytecode library uses visitors extensively

    // ELEMENT hierarchy (stable — we don't add new shapes often)
    interface VisitableShape {
        // KEY INSIGHT: accept() is the first dispatch — it knows the concrete type
        void accept(ShapeVisitor visitor);
    }

    static class VisitableCircle implements VisitableShape {
        final double radius;
        VisitableCircle(double radius) { this.radius = radius; }
        @Override public void accept(ShapeVisitor visitor) {
            visitor.visit(this); // 2nd dispatch: visitor.visit(Circle) resolved here
        }
    }

    static class VisitableRectangle implements VisitableShape {
        final double width, height;
        VisitableRectangle(double w, double h) { this.width = w; this.height = h; }
        @Override public void accept(ShapeVisitor visitor) {
            visitor.visit(this); // 2nd dispatch: visitor.visit(Rectangle)
        }
    }

    static class VisitableTriangle implements VisitableShape {
        final double base, height;
        VisitableTriangle(double base, double height) { this.base = base; this.height = height; }
        @Override public void accept(ShapeVisitor visitor) {
            visitor.visit(this);
        }
    }

    // VISITOR interface: one method per element type
    interface ShapeVisitor {
        void visit(VisitableCircle circle);
        void visit(VisitableRectangle rectangle);
        void visit(VisitableTriangle triangle);
    }

    // CONCRETE VISITORS: add operations WITHOUT modifying shape classes
    static class AreaCalculator implements ShapeVisitor {
        private double totalArea = 0;

        @Override public void visit(VisitableCircle c) {
            double area = Math.PI * c.radius * c.radius;
            System.out.printf("    Circle area: %.2f%n", area);
            totalArea += area;
        }
        @Override public void visit(VisitableRectangle r) {
            double area = r.width * r.height;
            System.out.printf("    Rectangle area: %.2f%n", area);
            totalArea += area;
        }
        @Override public void visit(VisitableTriangle t) {
            double area = 0.5 * t.base * t.height;
            System.out.printf("    Triangle area: %.2f%n", area);
            totalArea += area;
        }
        public double getTotalArea() { return totalArea; }
    }

    static class PerimeterCalculator implements ShapeVisitor {
        @Override public void visit(VisitableCircle c) {
            System.out.printf("    Circle perimeter: %.2f%n", 2 * Math.PI * c.radius);
        }
        @Override public void visit(VisitableRectangle r) {
            System.out.printf("    Rectangle perimeter: %.2f%n", 2 * (r.width + r.height));
        }
        @Override public void visit(VisitableTriangle t) {
            // Approximation for right triangle
            double hyp = Math.sqrt(t.base * t.base + t.height * t.height);
            System.out.printf("    Triangle perimeter (approx): %.2f%n", t.base + t.height + hyp);
        }
    }

    // =========================================================
    // MAIN: Demo all patterns
    // =========================================================
    public static void main(String[] args) {
        System.out.println("=== 1. CHAIN OF RESPONSIBILITY ===");
        {
            HttpHandler chain = new AuthMiddleware()
                .then(new RateLimitMiddleware(2))
                .then(new LoggingMiddleware())
                .then(new ActualHandler());

            System.out.println("-- Valid request --");
            HttpRequest req1 = new HttpRequest("/api/data", "Bearer xyz", "user1");
            chain.handle(req1, new HttpResponse());

            System.out.println("-- No token (blocked by AuthMiddleware) --");
            HttpRequest req2 = new HttpRequest("/api/data", "", "user2");
            chain.handle(req2, new HttpResponse());
        }

        System.out.println("\n=== 2. COMMAND PATTERN (undo/redo) ===");
        {
            TextEditor editor = new TextEditor();
            editor.executeCommand(new InsertCommand(editor.getBuffer(), "Hello", 0));
            editor.executeCommand(new InsertCommand(editor.getBuffer(), " World", 5));
            System.out.println("    Content: '" + editor.getContent() + "'");
            editor.undo();
            System.out.println("    After undo: '" + editor.getContent() + "'");
            editor.redo();
            System.out.println("    After redo: '" + editor.getContent() + "'");

            // Macro command
            System.out.println("    Macro:");
            editor.executeCommand(new MacroCommand(
                new InsertCommand(editor.getBuffer(), "!", 0),
                new InsertCommand(editor.getBuffer(), "!", 0)
            ));
            System.out.println("    Content: '" + editor.getContent() + "'");
            editor.undo(); // undoes both macro steps in reverse order
        }

        System.out.println("\n=== 3. INTERPRETER PATTERN ===");
        {
            // Parse: (3 + 4) * 2
            Expression expr = new MultiplyExpression(
                new AddExpression(new NumberExpression(3), new NumberExpression(4)),
                new NumberExpression(2)
            );
            System.out.println("  (3 + 4) * 2 = " + expr.interpret()); // 14
        }

        System.out.println("\n=== 4. MEDIATOR PATTERN ===");
        {
            ChatRoom room = new ChatRoom();
            ChatUser alice = new ChatUser("Alice", room);
            ChatUser bob = new ChatUser("Bob", room);
            ChatUser carol = new ChatUser("Carol", room);

            alice.send("Hi everyone!");
            bob.send("Hey Alice!");
            // KEY: Alice, Bob, Carol don't hold references to each other
        }

        System.out.println("\n=== 5. MEMENTO PATTERN ===");
        {
            OriginalEditor editor = new OriginalEditor();
            HistoryManager history = new HistoryManager();

            editor.type("Hello");
            history.push(editor.save()); // save state 1

            editor.type(" World");
            history.push(editor.save()); // save state 2

            editor.type("!!!");
            System.out.println("    Current: '" + editor.getContent() + "'");

            editor.restore(history.pop()); // restore state 2
            System.out.println("    After 1st undo: '" + editor.getContent() + "'");

            editor.restore(history.pop()); // restore state 1
            System.out.println("    After 2nd undo: '" + editor.getContent() + "'");
        }

        System.out.println("\n=== 6. STATE PATTERN ===");
        {
            TrafficLight light = new TrafficLight();
            for (int i = 0; i < 6; i++) {
                System.out.print("    Current: " + light.getColor() + " → ");
                light.change();
            }
        }

        System.out.println("\n=== 7. STRATEGY PATTERN ===");
        {
            int[] data = {5, 3, 8, 1, 9, 2};
            Sorter sorter = new Sorter(new BubbleSortStrategy());
            System.out.println("  Bubble: " + Arrays.toString(sorter.sort(data)));

            sorter.setStrategy(new InsertionSortStrategy()); // swap strategy at runtime
            System.out.println("  Insertion: " + Arrays.toString(sorter.sort(data)));

            // Java lambda as strategy (functional interface)
            sorter.setStrategy(arr -> Arrays.sort(arr)); // JDK sort as strategy
            System.out.println("  Arrays.sort: " + Arrays.toString(sorter.sort(data)));
        }

        System.out.println("\n=== 8. TEMPLATE METHOD PATTERN ===");
        {
            DataMiner csvMiner = new CsvDataMiner();
            csvMiner.mine("sales.csv");

            DataMiner jsonMiner = new JsonDataMiner();
            jsonMiner.mine("inventory.json");
        }

        System.out.println("\n=== 9. VISITOR PATTERN (double dispatch) ===");
        {
            List<VisitableShape> shapes = List.of(
                new VisitableCircle(5),
                new VisitableRectangle(4, 6),
                new VisitableTriangle(3, 4)
            );

            AreaCalculator areaCalc = new AreaCalculator();
            System.out.println("  Areas:");
            shapes.forEach(s -> s.accept(areaCalc));
            System.out.printf("  Total area: %.2f%n", areaCalc.getTotalArea());

            System.out.println("  Perimeters:");
            PerimeterCalculator perimCalc = new PerimeterCalculator();
            shapes.forEach(s -> s.accept(perimCalc));
            // KEY: Added area and perimeter operations WITHOUT modifying shape classes
        }

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: Strategy vs Template Method?");
        System.out.println("A: Template=inheritance (skeleton fixed, subclass overrides steps).");
        System.out.println("   Strategy=composition (algorithm injected, swappable at runtime).");
        System.out.println("   Prefer Strategy: more flexible, more testable.");
        System.out.println();
        System.out.println("Q: Why is double dispatch needed in Visitor?");
        System.out.println("A: Java has single dispatch. shape.accept(v) dispatches on shape's");
        System.out.println("   type; inside accept(), v.visit(this) dispatches on visitor type.");
        System.out.println("   Together: 2 type resolutions = double dispatch.");
        System.out.println();
        System.out.println("Q: Command vs Strategy?");
        System.out.println("A: Command encapsulates a REQUEST (can be stored, queued, undone).");
        System.out.println("   Strategy encapsulates an ALGORITHM. Commands often use Strategies.");
    }
}
