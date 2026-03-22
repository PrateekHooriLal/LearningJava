// Run with: --enable-preview --source 21
package com.java.java21;

/**
 * CONCEPT: String Templates (Java 21 Preview — JEP 430)
 *
 * CORE IDEA: A type-safe, extensible mechanism for embedding expressions
 * inside string literals. Unlike String.format() or concatenation, templates
 * are processed by a "template processor" that can validate, transform, or
 * construct any type from the template — not just strings.
 *
 * WHEN TO USE:
 *   - Building SQL queries (with safe parameterization — prevents SQL injection!)
 *   - Constructing JSON/XML/HTML snippets
 *   - Log message formatting with structured data
 *   - Any place you currently use String.format(), MessageFormat, or + concatenation
 *
 * TIME/SPACE: O(n) where n = total length of template output
 *
 * INTERVIEW ANGLE:
 *   Q: Why not just use String.format()?
 *   A: String.format() is:
 *      1. Not type-safe: "%d" with a String → runtime exception
 *      2. Fragile: argument order must match placeholder order manually
 *      3. Not composable: can't intercept and transform the template
 *      String Templates are:
 *      1. Type-safe: \{expression} is type-checked at compile time
 *      2. Readable: expression is inline, not separated from its placeholder
 *      3. Extensible: custom processors can build ANY type, not just String
 *      4. Injection-safe: SQL processor can escape parameters by design
 *
 *   Q: What is a template processor?
 *   A: An object implementing StringTemplate.Processor<R, E> where R is the
 *      return type. STR produces String. FMT produces String with formatting.
 *      RAW produces StringTemplate (deferred processing). You can write your
 *      own to produce PreparedStatement, JsonNode, SafeHtml, etc.
 *
 *   Q: Can you use String Templates with dynamic strings?
 *   A: NO. The template must be a COMPILE-TIME LITERAL. You cannot write:
 *      String template = "Hello \{name}"; // ERROR: not a template literal
 *      This is intentional — the compile-time literal enables static analysis,
 *      type checking, and security guarantees.
 *
 * REAL-WORLD USE:
 *   - ORM frameworks: type-safe query builders
 *   - Web frameworks: safe HTML/JS generation
 *   - Logging frameworks: structured log templates
 *   - Configuration: safe interpolation in config files
 *
 * GOTCHA:
 *   - PREVIEW in Java 21 — may change before final release
 *   - NOT for dynamic templates (must be compile-time literals)
 *   - STR processor eagerly evaluates — no lazy evaluation
 *   - Custom processors throw checked exceptions — must handle in calling code
 */

import java.util.*;

public class StringTemplatesDemo {

    // -------------------------------------------------------------------------
    // SECTION 1: STR Processor — Basic String Interpolation
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: STR is the simplest template processor.
     * It interpolates expressions inline, replacing \{expr} with expr.toString().
     *
     * COMPARISON:
     *   Old way (concatenation):  "Hello " + name + ", you are " + age + " years old"
     *   Old way (format):         String.format("Hello %s, you are %d years old", name, age)
     *   New way (STR):            STR."Hello \{name}, you are \{age} years old"
     *
     * ADVANTAGES of STR over format:
     *   1. Expression is ADJACENT to its placeholder — no counting %s vs args
     *   2. Any Java expression works: \{user.getName()}, \{list.size()}, \{a + b}
     *   3. Type-safe: compiler checks the expression, not a format string
     *   4. Multi-line templates work naturally (text blocks + STR)
     */
    public static void demonstrateSTRProcessor() {
        System.out.println("=== STR Processor Demo ===");

        String name = "Alice";
        int age = 30;
        double score = 98.6;
        List<String> items = List.of("apple", "banana", "cherry");

        /*
         * PREVIEW CODE (requires --enable-preview):
         *
         * // Simple variable interpolation
         * String greeting = STR."Hello \{name}, you are \{age} years old";
         * // → "Hello Alice, you are 30 years old"
         *
         * // Arbitrary expressions — method calls, arithmetic, ternary
         * String msg = STR."Score: \{score}, Grade: \{score >= 90 ? "A" : "B"}";
         * // → "Score: 98.6, Grade: A"
         *
         * // Multi-line (text block + template)
         * String json = STR."""
         *     {
         *         "name": "\{name}",
         *         "age": \{age},
         *         "items": \{items.size()}
         *     }
         *     """;
         *
         * // Method calls inline
         * String upper = STR."Hello \{name.toUpperCase()}!";
         * // → "Hello ALICE!"
         *
         * // Conditional logic inline
         * String list = STR."You have \{items.size()} item\{items.size() == 1 ? "" : "s"}";
         * // → "You have 3 items"
         */

        // Equivalent without templates:
        String greeting = "Hello " + name + ", you are " + age + " years old";
        String msg = String.format("Score: %.1f, Grade: %s", score, score >= 90 ? "A" : "B");
        System.out.println("Equivalent to STR template: " + greeting);
        System.out.println("Formatted: " + msg);

        System.out.println("(STR template code shown in comments — requires preview flag)");
    }

    // -------------------------------------------------------------------------
    // SECTION 2: FMT Processor — Formatted String Interpolation
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: FMT processor supports printf-style format specifiers
     * WITHIN the template. Each \{expr} can be preceded by a format spec.
     * This combines the readability of STR with the precision of String.format().
     *
     * SYNTAX: FMT."The value is %.2f\{myDouble}"
     *   The format spec comes just before the \{ — like C's printf but inline.
     *
     * USE CASES:
     *   - Scientific output: "Pi = %.10f\{Math.PI}"
     *   - Financial: "$%,.2f\{amount}"  → "$1,234.56"
     *   - Tables: "%-20s\{name} | %5d\{count}"  (left-aligned padding)
     */
    public static void demonstrateFMTProcessor() {
        System.out.println("\n=== FMT Processor Demo ===");

        double pi = Math.PI;
        double salary = 85_000.50;
        String productName = "Widget";
        int quantity = 1500;

        /*
         * PREVIEW CODE:
         *
         * // Numeric formatting
         * String piStr = FMT."Pi is approximately %.5f\{pi}";
         * // → "Pi is approximately 3.14159"
         *
         * // Currency formatting
         * String salaryStr = FMT."Annual salary: $%,.2f\{salary}";
         * // → "Annual salary: $85,000.50"
         *
         * // Table row with mixed types
         * String row = FMT."%-20s\{productName} | %,6d\{quantity} units";
         * // → "Widget               |  1,500 units"
         *
         * // Composing with multi-line for tables:
         * String report = FMT."""
         *     Product    : %-20s\{productName}
         *     Quantity   : %,6d\{quantity}
         *     Unit Price : $%.2f\{salary/quantity}
         *     Total      : $%,.2f\{salary}
         *     """;
         */

        // Equivalent using String.format():
        String piStr = String.format("Pi is approximately %.5f", pi);
        String salaryStr = String.format("Annual salary: $%,.2f", salary);
        System.out.println(piStr);
        System.out.println(salaryStr);
        System.out.println("(FMT template code shown in comments — requires preview flag)");
    }

    // -------------------------------------------------------------------------
    // SECTION 3: RAW Processor — Deferred Processing
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: RAW processor returns a StringTemplate OBJECT instead of a String.
     * This lets you inspect the template's structure: its fragments (literal parts)
     * and its values (expression results) separately.
     *
     * WHY USE RAW:
     *   - Build custom processors that need to see fragments and values separately
     *   - Deferred evaluation: pass the template around, process it later
     *   - Debugging templates: inspect what fragments and values were captured
     *
     * STRINGTEMPLATE STRUCTURE:
     *   Template: "Hello \{name}, you scored \{score}!"
     *   Fragments: ["Hello ", ", you scored ", "!"]    (3 parts, always len = values+1)
     *   Values:    [name, score]                        (2 expressions evaluated)
     *
     * INTERVIEW INSIGHT: The RAW processor is what you use as the foundation
     * for building custom template processors (like the SQL processor below).
     * Custom processors receive a StringTemplate and return any type R.
     */
    public static void demonstrateRAWProcessor() {
        System.out.println("\n=== RAW Processor Demo ===");

        String name = "Bob";
        int score = 95;

        /*
         * PREVIEW CODE:
         *
         * // RAW returns StringTemplate, not String
         * StringTemplate st = RAW."Hello \{name}, you scored \{score}!";
         *
         * // Inspect structure
         * List<String> fragments = st.fragments();
         * // → ["Hello ", ", you scored ", "!"]
         * List<Object> values = st.values();
         * // → ["Bob", 95]
         *
         * // You can reconstruct the String manually:
         * // String result = StringTemplate.interpolate(fragments, values);
         * // Or use STR.process(st) to convert to string
         *
         * System.out.println("Fragments: " + fragments);
         * System.out.println("Values: " + values);
         */

        System.out.println("RAW processor returns StringTemplate with fragments + values");
        System.out.println("Used as building block for custom processors");
    }

    // -------------------------------------------------------------------------
    // SECTION 4: Custom Processor — SQL Injection Prevention
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: This is the KILLER USE CASE for String Templates.
     * A custom SQL processor can NEVER produce a SQL injection vulnerability
     * because it sees literal parts (SQL structure) and value parts (user data)
     * SEPARATELY, and can parameterize the values automatically.
     *
     * CRITICAL SECURITY INSIGHT:
     *   Bad (SQL injection possible):
     *     String query = "SELECT * FROM users WHERE name = '" + userInput + "'";
     *     // If userInput = "'; DROP TABLE users; --"  → DISASTER
     *
     *   Good with String Templates (preview):
     *     PreparedStatement ps = SQL."SELECT * FROM users WHERE name = \{userInput}";
     *     // The SQL processor creates a PreparedStatement with userInput as parameter.
     *     // userInput is NEVER concatenated into the SQL string.
     *     // SQL injection is STRUCTURALLY IMPOSSIBLE.
     *
     * HOW THE CUSTOM PROCESSOR WORKS (preview):
     *
     *   public class SqlProcessor implements StringTemplate.Processor<PreparedStatement, SQLException> {
     *
     *       private final Connection conn;
     *
     *       public SqlProcessor(Connection conn) { this.conn = conn; }
     *
     *       @Override
     *       public PreparedStatement process(StringTemplate st) throws SQLException {
     *           // fragments() = the SQL structure (safe — came from source code)
     *           // values()    = the user data (must be parameterized, never concatenated)
     *
     *           // Build SQL with ? placeholders
     *           StringBuilder sql = new StringBuilder();
     *           List<String> fragments = st.fragments();
     *           for (int i = 0; i < fragments.size() - 1; i++) {
     *               sql.append(fragments.get(i));
     *               sql.append("?");  // placeholder for value[i]
     *           }
     *           sql.append(fragments.getLast()); // final fragment
     *
     *           // Create PreparedStatement and bind values
     *           PreparedStatement ps = conn.prepareStatement(sql.toString());
     *           List<Object> values = st.values();
     *           for (int i = 0; i < values.size(); i++) {
     *               ps.setObject(i + 1, values.get(i));
     *           }
     *           return ps;  // Safe! User data never concatenated into SQL.
     *       }
     *   }
     *
     * USAGE (preview):
     *   var SQL = new SqlProcessor(connection);
     *   String maliciousInput = "'; DROP TABLE users; --";
     *   PreparedStatement ps = SQL."SELECT * FROM users WHERE name = \{maliciousInput}";
     *   // ps is a PreparedStatement with maliciousInput safely bound as parameter.
     *   // SQL INJECTION IS IMPOSSIBLE by construction.
     */
    public static void demonstrateSQLProcessor() {
        System.out.println("\n=== SQL Processor Demo (Injection Prevention) ===");
        System.out.println("Custom StringTemplate.Processor<PreparedStatement, SQLException>");
        System.out.println("fragments() = SQL structure (compile-time literal, safe)");
        System.out.println("values()    = user data (always parameterized, never concatenated)");
        System.out.println("Result: SQL injection is STRUCTURALLY IMPOSSIBLE with this processor");

        // Demonstrate the concept with a simple simulation:
        String userInput = "'; DROP TABLE users; --";  // classic SQL injection attempt
        String badQuery = "SELECT * FROM users WHERE name = '" + userInput + "'";
        System.out.println("\nBAD (injection vulnerability): " + badQuery);

        // With custom processor, this would produce a PreparedStatement
        // with "SELECT * FROM users WHERE name = ?" and userInput as parameter.
        System.out.println("GOOD (with SQL template processor): PreparedStatement");
        System.out.println("  SQL: SELECT * FROM users WHERE name = ?");
        System.out.println("  Params: [" + userInput + "] (safely bound, never concatenated)");
    }

    // -------------------------------------------------------------------------
    // SECTION 5: Custom JSON Processor
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: Custom processors can return ANY type, not just String.
     * A JSON processor could return a JsonNode, a Map, or a JSONObject
     * with values already properly escaped and typed.
     *
     * PREVIEW CODE:
     *
     *   public class JsonProcessor implements StringTemplate.Processor<Map<String,Object>, RuntimeException> {
     *
     *       public static final JsonProcessor JSON = new JsonProcessor();
     *
     *       @Override
     *       public Map<String, Object> process(StringTemplate st) {
     *           // Reconstruct the JSON string with proper escaping
     *           // Then parse it into a Map
     *           StringBuilder sb = new StringBuilder();
     *           Iterator<Object> values = st.values().iterator();
     *           for (String fragment : st.fragments()) {
     *               sb.append(fragment);
     *               if (values.hasNext()) {
     *                   Object val = values.next();
     *                   if (val instanceof String s) {
     *                       // Escape special JSON characters
     *                       sb.append('"').append(escapeJson(s)).append('"');
     *                   } else {
     *                       sb.append(val); // numbers, booleans as-is
     *                   }
     *               }
     *           }
     *           return parseJson(sb.toString()); // returns structured data
     *       }
     *   }
     *
     * USAGE (preview):
     *   String userName = "O'Reilly \"Test\"";  // has quotes and apostrophes
     *   Map<String,Object> data = JSON."""
     *       {
     *           "name": \{userName},
     *           "age": \{42}
     *       }
     *       """;
     *   // userName is auto-escaped. No manual escaping. No injection possible.
     */
    public static void demonstrateJSONProcessor() {
        System.out.println("\n=== JSON Processor Demo ===");
        System.out.println("Custom processor auto-escapes string values in JSON context");
        System.out.println("Returns Map<String,Object> — structured data, not raw string");

        // Simulation without preview:
        String userName = "O'Reilly \"Test\"";
        // Manual approach (error-prone):
        String manualJson = "{\"name\": \"" + userName.replace("\"", "\\\"") + "\", \"age\": 42}";
        System.out.println("Manual (error-prone): " + manualJson);
        System.out.println("JSON template processor would auto-escape and return typed Map");
    }

    // -------------------------------------------------------------------------
    // SECTION 6: Why Compile-Time Literals Matter (Security + Performance)
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: The requirement that templates be compile-time literals is
     * a FEATURE, not a limitation. It provides:
     *
     * 1. SECURITY: The "structure" (fragments) of a query always comes from source
     *    code, never from user input. Only the "values" come from runtime.
     *    → SQL injection via template structure is impossible
     *
     * 2. PERFORMANCE: The compiler can optimize template processing at compile time.
     *    Fragment strings are interned; structure is constant.
     *
     * 3. STATIC ANALYSIS: IDEs and linters can validate templates at edit time.
     *    Highlighting, type checking, format spec validation.
     *
     * 4. EXPLICITNESS: You can't accidentally use a template processor on a
     *    dynamically-constructed string (which would bypass security guarantees).
     *
     * GOTCHA EXAMPLE:
     *   // This WILL NOT COMPILE (intentional design):
     *   String template = "Hello \{name}";  // Not a template literal — just a string
     *   String result = STR.process(template); // ERROR: template is a String, not StringTemplate
     *
     *   // You MUST write:
     *   String result = STR."Hello \{name}";  // compile-time template literal
     */
    public static void demonstrateCompileTimeLiteralImportance() {
        System.out.println("\n=== Compile-Time Literal Safety ===");
        System.out.println("Templates MUST be compile-time literals (by design, not limitation)");
        System.out.println("This guarantees: fragments come from source code, values from runtime");
        System.out.println("Security implication: structure of SQL/JSON always trusted, data always parameterized");
        System.out.println("Performance implication: fragment strings are constant, can be optimized");
    }

    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== String Templates Demo ===");
        System.out.println("(Note: String Templates require --enable-preview in Java 21)");
        System.out.println();

        demonstrateSTRProcessor();
        demonstrateFMTProcessor();
        demonstrateRAWProcessor();
        demonstrateSQLProcessor();
        demonstrateJSONProcessor();
        demonstrateCompileTimeLiteralImportance();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("STR: basic interpolation, any expression, type-safe");
        System.out.println("FMT: printf-style formatting inline with expressions");
        System.out.println("RAW: returns StringTemplate object for custom processing");
        System.out.println("Custom processors: build any type — PreparedStatement, JsonNode, etc.");
        System.out.println("KEY FEATURE: compile-time literal requirement enables injection safety");
        System.out.println("KEY IMPROVEMENT over format(): expressions adjacent to placeholders, type-safe");
        System.out.println("KEY IMPROVEMENT over concatenation(): readable single expression, no + chains");
    }
}
