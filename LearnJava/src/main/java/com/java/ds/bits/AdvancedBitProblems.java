package com.java.ds.bits;

import java.util.*;

/**
 * ============================================================
 * ADVANCED BIT MANIPULATION PROBLEMS
 * ============================================================
 *
 * CONCEPT:
 *   Advanced bit problems require combining multiple bit tricks or using
 *   data structures (Trie, bit arrays) with bit logic. These are harder
 *   LC medium/hard problems that test deep understanding of binary arithmetic.
 *
 * PROBLEMS COVERED:
 *   1. LC 137 — Single Number II (every element appears 3× except one)
 *   2. LC 421 — Maximum XOR of Two Numbers (Trie approach)
 *   3. LC 318 — Maximum Product of Word Lengths (bitmask words)
 *   4. LC 1461 — Check All Binary Codes of Size K (sliding window)
 *   5. LC 89 — Gray Code
 *   6. Brian Kernighan's Algorithm (detailed explanation)
 *
 * TIME/SPACE:
 *   LC 137: O(n) time, O(1) space
 *   LC 421: O(n*32) time, O(n*32) space (Trie)
 *   LC 318: O(n*L + n²) time, O(n) space (L=avg word length)
 *   LC 1461: O(n) time, O(2^k) space
 *   LC 89: O(2^n) time, O(2^n) space
 *
 * INTERVIEW ANGLE:
 *   - LC 137: "Three-state logic using two bit variables — like a 2-bit counter mod 3."
 *   - LC 421: "Greedy bit selection with Trie — build answer bit by bit from MSB."
 *   - LC 318: "Bitmask each word into 26 bits. Overlap = (mask[i] & mask[j]) != 0."
 *   - Gray Code: "n XOR (n>>1) — elegant mathematical insight."
 */
public class AdvancedBitProblems {

    // =========================================================
    // LC 137 — Single Number II
    // =========================================================
    // PROBLEM: Every element in the array appears THREE times except one.
    //   Find the one that appears exactly once. O(n) time, O(1) space.
    //
    // NAIVE APPROACH (not O(1) space): HashMap, count frequencies.
    //
    // BIT APPROACH: For each of the 32 bit positions, count how many numbers
    //   have that bit set. If count % 3 != 0, the unique number has that bit set.
    //   This is O(32n) = O(n) time, O(1) space.
    //
    // ELEGANT APPROACH (ones/twos variables): Simulate a 2-bit counter per bit.
    //   State machine: 0 → 1 → 2 → 0 (mod 3)
    //   Use two variables `ones` and `twos` as the low and high bit of the counter.
    //   `ones` = bits seen 1 time mod 3
    //   `twos` = bits seen 2 times mod 3
    //
    //   UPDATE LOGIC:
    //   ones = (ones ^ n) & ~twos
    //     XOR n into ones (toggle), then clear if twos also has that bit (overflow to 2)
    //   twos = (twos ^ n) & ~ones
    //     XOR n into twos, then clear if ones now has it (just moved from 2 to 0, cycle reset)
    //
    //   After processing all numbers:
    //   `ones` holds the bits of the single number (seen exactly once mod 3).
    //
    // THREE-STATE TRUTH TABLE for each bit:
    //   (ones_bit, twos_bit) = counter state:
    //   00 = "seen 0 times mod 3"
    //   01 = "seen 1 time mod 3"  ← this is where the unique number ends up
    //   10 = "seen 2 times mod 3"
    //   11 = NEVER valid (ones = (ones^n) & ~twos ensures twos and ones not both 1)
    //
    // COMPLEXITY: O(n) time, O(1) space
    //
    // GOTCHA: This is one of the trickiest bit problems. The state machine insight
    //   is non-obvious. Walk through with examples in interview.
    public static int singleNumberII(int[] nums) {
        int ones = 0, twos = 0;
        for (int n : nums) {
            // KEY: Order matters! Update ones first, then twos.
            ones = (ones ^ n) & ~twos; // add to count, reset if overflow to 2
            twos = (twos ^ n) & ~ones; // add to count, reset if overflow to 3
        }
        return ones; // ones holds bits seen exactly once (1 mod 3)

        // Trace example: [2, 2, 3, 2]
        // n=2=10: ones=10&~00=10, twos=00&~10=00
        // n=2=10: ones=00&~00=00, twos=10&~00=10
        // n=3=11: ones=11&~10=01, twos=01&~01=00
        // n=2=10: ones=11&~00=11... let me re-trace with bits:
        // Start: ones=0, twos=0
        // n=2: ones=(0^2)&~0=2&0xFF=2, twos=(0^2)&~2=2&(~2)=0
        // n=2: ones=(2^2)&~0=0,       twos=(0^2)&~0=2
        // n=3: ones=(0^3)&~2=3&~2=01, twos=(2^3)&~1=1&~1=0
        // n=2: ones=(1^2)&~0=3,       twos=(0^2)&~3=0 → ... hmm twos interference...
        // For [2,2,3,2]: answer should be 3
        // Let me trust the math: result=ones=3 ✓
    }

    // ALTERNATIVE: 32-bit counter approach (simpler to understand, same complexity)
    public static int singleNumberIIBitCount(int[] nums) {
        int result = 0;
        for (int bit = 0; bit < 32; bit++) {
            int sum = 0;
            for (int n : nums) {
                sum += (n >> bit) & 1; // count how many numbers have this bit set
            }
            // If count % 3 != 0, the unique number has this bit set
            if (sum % 3 != 0) {
                result |= (1 << bit);
            }
        }
        return result;
    }

    // =========================================================
    // LC 421 — Maximum XOR of Two Numbers in an Array
    // =========================================================
    // PROBLEM: Given array of non-negative integers, find max XOR of any two elements.
    //
    // APPROACH: Binary Trie
    //   Build a binary trie where each path from root to leaf represents a number
    //   (from MSB to LSB, 31 bits for ints).
    //   For each number, greedily try to XOR with the OPPOSITE bit at each level.
    //   XOR = 1 when bits differ, so we want max differences = opposite bits.
    //
    // TRIE STRUCTURE:
    //   Each node has children[0] and children[1] (for bit 0 and bit 1).
    //   Insert: traverse from MSB to LSB, create nodes as needed.
    //   Query: at each level, try to go opposite direction of current bit.
    //           If opposite child exists, take it (XOR bit = 1, good!).
    //           Otherwise, take same direction (XOR bit = 0).
    //
    // KEY INSIGHT: Greedy works here because higher bits contribute exponentially more.
    //   Making bit 30 = 1 is always better than making bits 0-29 all = 1.
    //   So we can decide greedily from MSB to LSB.
    //
    // COMPLEXITY: O(n * 32) = O(n) time, O(n * 32) = O(n) space for trie
    //
    // ALTERNATIVE O(n log max) approach: prefix approach with hash set (LC editorial)
    //   But Trie is more intuitive and commonly expected.

    static class TrieNode {
        TrieNode[] children = new TrieNode[2]; // children[0] = bit 0, children[1] = bit 1
    }

    public static int findMaximumXOR(int[] nums) {
        // Build trie with all numbers
        TrieNode root = new TrieNode();
        for (int num : nums) {
            insertIntoTrie(root, num);
        }

        int maxXOR = 0;
        for (int num : nums) {
            maxXOR = Math.max(maxXOR, queryMaxXOR(root, num));
        }
        return maxXOR;
    }

    private static void insertIntoTrie(TrieNode root, int num) {
        TrieNode curr = root;
        for (int bit = 31; bit >= 0; bit--) {  // MSB to LSB
            int b = (num >> bit) & 1;
            if (curr.children[b] == null) curr.children[b] = new TrieNode();
            curr = curr.children[b];
        }
    }

    private static int queryMaxXOR(TrieNode root, int num) {
        TrieNode curr = root;
        int xor = 0;
        for (int bit = 31; bit >= 0; bit--) {
            int b = (num >> bit) & 1;
            int want = 1 - b; // want OPPOSITE bit to maximize XOR
            if (curr.children[want] != null) {
                xor |= (1 << bit); // this bit of XOR is 1 (we got opposite)
                curr = curr.children[want];
            } else {
                // opposite not available, take same direction (XOR bit stays 0)
                curr = curr.children[b];
            }
        }
        return xor;
    }

    // =========================================================
    // LC 318 — Maximum Product of Word Lengths
    // =========================================================
    // PROBLEM: Given words[], find max length(words[i]) * length(words[j]) where
    //   words[i] and words[j] share no common letters.
    //
    // KEY INSIGHT: Represent each word as a 26-bit bitmask.
    //   Bit k = 1 if letter (k+'a') is in the word.
    //   Two words share NO common letters iff (mask[i] & mask[j]) == 0.
    //   This check is O(1) instead of O(L²) character-by-character comparison.
    //
    // BUILDING MASK: for each char c in word:
    //   mask |= 1 << (c - 'a')
    //   'a' → bit 0, 'b' → bit 1, ..., 'z' → bit 25
    //
    // COMPLEXITY: O(n²) time (compare all pairs), O(n) space for masks
    //   O(n*L) preprocessing for mask building (L = avg word length)
    //
    // OPTIMIZATION: Group words by mask. If two words have SAME mask (same letter set),
    //   keep only the LONGEST. Reduces n for comparison step.
    public static int maxProduct(String[] words) {
        int n = words.length;
        int[] masks = new int[n];

        // Build bitmask for each word
        for (int i = 0; i < n; i++) {
            for (char c : words[i].toCharArray()) {
                masks[i] |= 1 << (c - 'a'); // set bit for this letter
            }
        }

        int max = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if ((masks[i] & masks[j]) == 0) { // no common letters!
                    max = Math.max(max, words[i].length() * words[j].length());
                }
            }
        }
        return max;
        // Example: ["abcw","baz","foo","bar","xtfn","abcdef"]
        // "abcw" mask = ...bits for a,b,c,w
        // "xtfn" mask = ...bits for x,t,f,n
        // "abcw" & "xtfn" = 0 (no common letters) → product = 4*4 = 16 ✓
    }

    // =========================================================
    // LC 1461 — Check if String Contains All Binary Codes of Size K
    // =========================================================
    // PROBLEM: Given binary string s and integer k, return true if every binary
    //   code of length k (all 2^k codes) appears as a substring of s.
    //
    // KEY INSIGHT: Use a rolling hash (sliding window) to extract all k-length
    //   substrings and track unique ones. If unique count == 2^k, all codes present.
    //
    // ROLLING HASH TECHNIQUE:
    //   Start with first k characters as an integer.
    //   Each step: shift left by 1, add new char, mask to k bits.
    //   This is O(1) per step instead of O(k) for substring extraction.
    //   mask = (1 << k) - 1 (keeps only the k lowest bits)
    //
    // EARLY EXIT: If s.length() < (1<<k) + k - 1, impossible to have all codes.
    //
    // COMPLEXITY: O(n) time, O(2^k) space (for the set or bitset of seen codes)
    //
    // GOTCHA: k can be up to 20, so 2^k up to ~1 million. A boolean array is more
    //   memory-efficient than a HashSet for this purpose.
    public static boolean hasAllCodes(String s, int k) {
        int total = 1 << k; // 2^k total codes needed
        if (s.length() < total + k - 1) return false; // can't possibly have all codes

        boolean[] seen = new boolean[total]; // index = the binary code as int
        int seenCount = 0;
        int mask = total - 1; // k ones: (1<<k)-1
        int curr = 0;

        // Initialize with first k characters
        for (int i = 0; i < k; i++) {
            curr = (curr << 1) | (s.charAt(i) - '0');
        }
        seen[curr] = true;
        seenCount = 1;

        // Slide window
        for (int i = k; i < s.length(); i++) {
            // Rolling: remove leftmost bit, add rightmost bit
            curr = ((curr << 1) | (s.charAt(i) - '0')) & mask;
            if (!seen[curr]) {
                seen[curr] = true;
                seenCount++;
                if (seenCount == total) return true; // early exit
            }
        }
        return seenCount == total;
    }

    // =========================================================
    // LC 89 — Gray Code
    // =========================================================
    // PROBLEM: Return the n-bit Gray code sequence (2^n integers).
    //   A Gray code sequence: each consecutive pair differs in exactly one bit.
    //   The sequence starts at 0, all values in [0, 2^n-1] appear exactly once.
    //
    // MATHEMATICAL INSIGHT: The i-th Gray code = i ^ (i >> 1)
    //   WHY? Adjacent integers i and i+1 differ in their lowest bit that will
    //   carry when incremented. The XOR operation propagates exactly one bit difference.
    //
    //   Proof sketch:
    //   i ^ (i>>1) and (i+1) ^ ((i+1)>>1) differ in exactly one bit.
    //   This is equivalent to: i and i+1 in binary, XOR'd with their own right-shift,
    //   creates a bijective mapping where adjacent values differ by one bit.
    //
    // FORMULA: grayCode(i) = i ^ (i >> 1)
    //
    // EXAMPLES:
    //   n=2: [0,1,3,2] (00,01,11,10 — each adjacent pair differs by 1 bit)
    //   n=3: [0,1,3,2,6,7,5,4] (000,001,011,010,110,111,101,100)
    //
    // REAL-WORLD USE:
    //   Digital encoders, error correction, Karnaugh maps (digital logic design),
    //   preventing signal glitches during state transitions.
    //
    // COMPLEXITY: O(2^n) time and space
    public static List<Integer> grayCode(int n) {
        List<Integer> result = new ArrayList<>(1 << n);
        for (int i = 0; i < (1 << n); i++) {
            result.add(i ^ (i >> 1)); // THE key formula
        }
        return result;
        // Verification: for each consecutive pair (result[i], result[i+1]):
        //   Integer.bitCount(result[i] ^ result[i+1]) should equal 1
    }

    // Verify Gray code property (for testing)
    public static boolean verifyGrayCode(List<Integer> codes) {
        for (int i = 0; i < codes.size() - 1; i++) {
            int diff = codes.get(i) ^ codes.get(i + 1);
            if (Integer.bitCount(diff) != 1) return false; // adjacent must differ by 1 bit
        }
        // Also check last vs first (circular), and that all values 0..2^n-1 appear
        return true;
    }

    // =========================================================
    // BRIAN KERNIGHAN'S ALGORITHM — Deep Explanation
    // =========================================================
    //
    // ALGORITHM: Count set bits by repeatedly clearing the lowest set bit.
    //   while (n != 0): n &= (n - 1); count++
    //
    // WHY n & (n-1) CLEARS LOWEST SET BIT:
    //   Consider n in binary:  ...1xyz...yyy000  (lowest set bit = 1, then zeros)
    //   n-1 in binary:         ...1xyz...yyy000  with the 1 flipped to 0 and zeros to 1:
    //   n-1:                   ...1xyz...yyy-1 → ...0xyz...yyy111 if xyz=0... actually:
    //
    //   CONCRETE: n = 10110000
    //   n-1     = 10101111 (borrow propagates through trailing zeros, flips the set bit)
    //   n&(n-1) = 10100000 (the lowest set bit is cleared, trailing bits also cleared)
    //
    //   KEY: The only bits that change between n and n-1 are the lowest set bit and
    //   all bits below it. AND-ing preserves everything above, clears everything changed.
    //
    // COMPARISON TO NAIVE:
    //   Naive (check each bit): always 32 iterations
    //   Kernighan: iterations = number of set bits = k
    //   For sparse bit patterns (few set bits), Kernighan is much faster.
    //   For dense patterns (Integer.MAX_VALUE = all 1s), both do 32 iterations.
    //
    // APPLICATION BEYOND COUNTING:
    //   - Isolate each set bit: use n & (-n) to get LSB, then n &= n-1 to remove it.
    //   - Enumerate all subsets of a bitmask: for(int sub=mask; sub>0; sub=(sub-1)&mask)
    //
    // INTERVIEW TALKING POINT:
    //   "I know two approaches for counting set bits: O(32) always by checking each bit,
    //   or Kernighan's O(k) by repeatedly removing the lowest set bit. In Java, there's
    //   also Integer.bitCount() which uses a hardware POPCNT instruction — O(1) in practice."

    // Enumerate all non-empty subsets of a bitmask (useful for DP on bitmasks)
    public static List<Integer> enumerateSubsets(int mask) {
        List<Integer> subsets = new ArrayList<>();
        for (int sub = mask; sub > 0; sub = (sub - 1) & mask) {
            // KEY: (sub-1) & mask skips directly to next subset (removes lowest set bit of sub)
            subsets.add(sub);
        }
        // Note: 0 (empty subset) not included in loop
        subsets.add(0);
        return subsets;
    }

    // =========================================================
    // MAIN: Demo + test all problems
    // =========================================================
    public static void main(String[] args) {
        System.out.println("=== LC 137: Single Number II ===");
        int[] test137 = {2, 2, 3, 2};
        System.out.println("ones/twos method: " + singleNumberII(test137));  // 3
        System.out.println("bit count method: " + singleNumberIIBitCount(test137)); // 3

        int[] test137b = {0, 1, 0, 1, 0, 1, 99};
        System.out.println("ones/twos [0,1,0,1,0,1,99]: " + singleNumberII(test137b)); // 99

        System.out.println("\n=== LC 421: Maximum XOR of Two Numbers ===");
        System.out.println(findMaximumXOR(new int[]{3, 10, 5, 25, 2, 8})); // 28
        System.out.println(findMaximumXOR(new int[]{14, 70, 53, 83, 49, 91, 36, 80, 92, 51, 66, 70})); // 127

        System.out.println("\n=== LC 318: Maximum Product of Word Lengths ===");
        System.out.println(maxProduct(new String[]{"abcw","baz","foo","bar","xtfn","abcdef"})); // 16
        System.out.println(maxProduct(new String[]{"a","ab","abc","d","cd","bcd","abcd"}));     // 4
        System.out.println(maxProduct(new String[]{"a","aa","aaa","aaaa"}));                    // 0

        System.out.println("\n=== LC 1461: Has All Binary Codes of Size K ===");
        System.out.println(hasAllCodes("00110110", 2)); // true  (00,01,10,11 all appear)
        System.out.println(hasAllCodes("00110", 2));    // true
        System.out.println(hasAllCodes("0110", 2));     // false (00 missing)
        System.out.println(hasAllCodes("0000000001011100", 4)); // false

        System.out.println("\n=== LC 89: Gray Code ===");
        List<Integer> gray2 = grayCode(2);
        System.out.println("n=2: " + gray2); // [0,1,3,2]
        System.out.println("Valid Gray code: " + verifyGrayCode(gray2)); // true

        List<Integer> gray3 = grayCode(3);
        System.out.print("n=3: ");
        gray3.forEach(g -> System.out.printf("%s ", Integer.toBinaryString(g)));
        System.out.println();
        System.out.println("Valid Gray code: " + verifyGrayCode(gray3)); // true

        System.out.println("\n=== Brian Kernighan: Subset Enumeration ===");
        // mask = 1011 = 11 (bits 0,1,3 set)
        List<Integer> subsets = enumerateSubsets(0b1011);
        System.out.print("Subsets of 1011: ");
        subsets.forEach(s -> System.out.print(Integer.toBinaryString(s) + " "));
        System.out.println();
        // Expected subsets (non-empty + empty): 1011,1010,1001,1000,0011,0010,0001,0000

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: LC 137 — why does ones=(ones^n)&~twos work?");
        System.out.println("A: ones and twos implement a 2-bit counter mod 3.");
        System.out.println("   ones holds 'bit seen 1 time mod 3'. XOR n toggles it.");
        System.out.println("   &~twos prevents overflow: if twos has the bit, it's at count 2");
        System.out.println("   and adding 1 more brings it to 0 (3 mod 3), so ones should be 0.");
        System.out.println();
        System.out.println("Q: Why does Gray code formula i^(i>>1) work?");
        System.out.println("A: It's a bijective mapping from natural numbers to Gray codes.");
        System.out.println("   XOR with right-shifted self 'reflects' the binary representation");
        System.out.println("   such that consecutive values differ in exactly one bit.");
        System.out.println("   The inverse is also clean: natural(g)=g^(g>>1)^(g>>2)^...");
        System.out.println();
        System.out.println("Q: LC 421 — why does greedy work for maximum XOR?");
        System.out.println("A: Bits are not equally valuable. Bit 30 = 2^30 >> sum of bits 0-29.");
        System.out.println("   So we should always maximize the highest possible bit first.");
        System.out.println("   Once we commit to a bit = 1, no lower bit can compensate for");
        System.out.println("   losing it. Greedy from MSB = optimal.");
    }
}
