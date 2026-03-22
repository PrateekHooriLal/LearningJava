package com.interview.prep;

/**
 * NUMBERS TO WORDS — Recursive Integer-to-English Conversion
 *
 * PROBLEM:
 *   Convert an integer to its English word representation.
 *   Examples:
 *     123       → "One Hundred Twenty Three"
 *     1000      → "One Thousand"
 *     1000000   → "One Million"
 *     1000000000 → "One Billion"
 *     0         → "Zero"
 *
 * APPROACH — Recursive Decomposition:
 *   Numbers have natural groupings by scale: billions, millions, thousands, hundreds, tens, ones.
 *   We process from the largest scale down:
 *
 *   1. Numbers 1–19 are SPECIAL (unique words, no pattern): one, two, ..., nineteen
 *   2. Numbers 20–99 are TENS + ONES: twenty-one, thirty-five, etc.
 *   3. Numbers 100–999: "[hundreds word] Hundred [remainder]"
 *   4. Numbers 1000+:   "[thousands part] Thousand [remainder]"
 *   5. Numbers 1M+:     "[millions part] Million [remainder]"
 *   6. Numbers 1B+:     "[billions part] Billion [remainder]"
 *
 *   KEY INSIGHT: For scale groupings (thousands, millions, billions), divide by
 *   the scale to get the "group", recursively convert that group, append the
 *   scale name, then recursively convert the remainder.
 *
 * INTERVIEW FREQUENCY: Medium — asked at TCS, Infosys, Capgemini, and occasionally
 *   at product companies as a recursion/string manipulation problem.
 *
 * COMMON FOLLOW-UP QUESTIONS:
 *   1. "Handle negative numbers." → Check sign, prepend "Negative", negate.
 *   2. "Handle zero." → Special case: return "Zero".
 *   3. "What is the range?" → int range: -2,147,483,648 to 2,147,483,647
 *      Edge: Integer.MIN_VALUE can't be negated in int — use long internally.
 *
 * Time: O(log n) — number of recursive calls proportional to # of digit groups
 * Space: O(log n) — recursion stack depth
 */
public class NumbersToWords {

    // -------------------------------------------------------------------------
    // Lookup tables — hardcode the irregular English number words
    // -------------------------------------------------------------------------

    // 1–19: completely irregular (no pattern) → must be hardcoded
    private static final String[] ONES = {
        "",          // index 0 unused (we call with 1-based)
        "One",  "Two",      "Three",    "Four",     "Five",
        "Six",  "Seven",    "Eight",    "Nine",      "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
        "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    // Multiples of ten from 20–90 (indices 2–9 map to 20,30,...,90)
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"
    };

    // Scale groupings (US naming convention)
    private static final int BILLION  = 1_000_000_000;
    private static final int MILLION  = 1_000_000;
    private static final int THOUSAND = 1_000;
    private static final int HUNDRED  = 100;

    // -------------------------------------------------------------------------
    // main() — test cases covering all ranges
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== Numbers to Words ===");

        // --- Basic cases: 1–19 (special words) ---
        test(1);           // One
        test(13);          // Thirteen
        test(19);          // Nineteen

        // --- Tens: 20–99 ---
        test(20);          // Twenty
        test(21);          // Twenty One
        test(99);          // Ninety Nine

        // --- Hundreds ---
        test(100);         // One Hundred
        test(115);         // One Hundred Fifteen
        test(999);         // Nine Hundred Ninety Nine

        // --- Thousands ---
        test(1000);        // One Thousand
        test(1001);        // One Thousand One
        test(12345);       // Twelve Thousand Three Hundred Forty Five

        // --- Millions ---
        test(1_000_000);   // One Million
        test(1_234_567);   // One Million Two Hundred Thirty Four Thousand Five Hundred Sixty Seven

        // --- Billions ---
        test(1_000_000_000); // One Billion
        test(2_147_483_647); // Integer.MAX_VALUE

        // --- Edge cases ---
        test(0);             // Zero
        test(-42);           // Negative Forty Two
        test(Integer.MIN_VALUE); // Negative Two Billion One Hundred...
    }

    static void test(int n) {
        System.out.printf("  %-14d → %s%n", n, numberToWords(n));
    }

    // -------------------------------------------------------------------------
    // PUBLIC ENTRY POINT
    // -------------------------------------------------------------------------

    /**
     * Converts an integer to its English word representation.
     *
     * Handles: zero (special), negatives (prepend "Negative"), and the full
     * int range. Uses long internally to safely handle Integer.MIN_VALUE
     * (since Math.abs(Integer.MIN_VALUE) overflows int).
     */
    public static String numberToWords(int n) {
        if (n == 0) return "Zero";

        // Use long to safely handle Integer.MIN_VALUE = -2147483648
        // Math.abs(-2147483648) overflows to -2147483648 in int!
        long num = Math.abs((long) n);

        String words = convert(num);

        return n < 0 ? "Negative " + words : words;
    }

    // -------------------------------------------------------------------------
    // CORE RECURSIVE CONVERTER
    // -------------------------------------------------------------------------

    /**
     * Recursively converts a positive number to words.
     *
     * RECURSION STRUCTURE (largest scale first):
     *   if n >= BILLION  → convert(n/BILLION) + " Billion"  + convert(n%BILLION)
     *   if n >= MILLION  → convert(n/MILLION) + " Million"  + convert(n%MILLION)
     *   if n >= THOUSAND → convert(n/THOUSAND)+ " Thousand" + convert(n%THOUSAND)
     *   if n >= 100      → convert(n/100) + " Hundred"      + convert(n%100)
     *   if n >= 20       → TENS[n/10] + " " + ONES[n%10]
     *   else (1–19)      → ONES[n]  (direct lookup)
     *
     * IMPORTANT: Append the remainder with a leading space only if it's non-empty.
     */
    private static String convert(long n) {
        if (n == 0) {
            // Base case: nothing left to say — return empty string.
            // This is hit when a scale divides evenly, e.g., 1000 → "One Thousand" + ""
            return "";
        }

        if (n < 20) {
            // 1–19: direct lookup (irregular words — no computation possible)
            return ONES[(int) n];
        }

        if (n < 100) {
            // 20–99: TENS word + optional ONES word
            // e.g., 45 → TENS[4] + " " + ONES[5] = "Forty Five"
            //        30 → TENS[3] + ""            = "Thirty"
            String tensWord = TENS[(int) n / 10];
            String onesWord = (n % 10 != 0) ? " " + ONES[(int) n % 10] : "";
            return tensWord + onesWord;
        }

        if (n < THOUSAND) {
            // 100–999: "[X] Hundred [remainder]"
            // e.g., 345 → "Three Hundred Forty Five"
            String hundredsWord = ONES[(int) n / 100] + " Hundred";
            String remainder = (n % 100 != 0) ? " " + convert(n % 100) : "";
            return hundredsWord + remainder;
        }

        if (n < MILLION) {
            // 1,000–999,999: "[X] Thousand [remainder]"
            // e.g., 12345 → "Twelve Thousand Three Hundred Forty Five"
            String thousandsWord = convert(n / THOUSAND) + " Thousand";
            String remainder = (n % THOUSAND != 0) ? " " + convert(n % THOUSAND) : "";
            return thousandsWord + remainder;
        }

        if (n < BILLION) {
            // 1,000,000–999,999,999: "[X] Million [remainder]"
            String millionsWord = convert(n / MILLION) + " Million";
            String remainder = (n % MILLION != 0) ? " " + convert(n % MILLION) : "";
            return millionsWord + remainder;
        }

        // 1,000,000,000+: "[X] Billion [remainder]"
        // int max is ~2.1B so at most "Two Billion ..." for int input
        String billionsWord = convert(n / BILLION) + " Billion";
        String remainder = (n % BILLION != 0) ? " " + convert(n % BILLION) : "";
        return billionsWord + remainder;
    }
}
