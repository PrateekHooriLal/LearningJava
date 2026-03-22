package com.java.ds.bits;

import java.util.*;

/**
 * ============================================================
 * BIT MANIPULATION — Fundamentals + 9 LeetCode Problems
 * ============================================================
 *
 * CONCEPT:
 *   Bit manipulation operates directly on binary representations of integers.
 *   Operations are O(1) (or O(32) = O(1)) and extremely fast — no division,
 *   no multiplication overhead. Used in competitive programming, systems
 *   programming, cryptography, compression, and low-level optimization.
 *
 * WHY IT MATTERS FOR FAANG:
 *   - Google, Facebook, and Amazon frequently test bit manipulation.
 *   - Shows you understand computers at the hardware level.
 *   - Elegant solutions to problems that would otherwise require loops or hash tables.
 *
 * TIME/SPACE:
 *   Most bit operations: O(1) time, O(1) space.
 *   Exception: Counting Bits DP: O(n) time, O(n) space.
 *
 * INTERVIEW ANGLE:
 *   - "XOR is self-inverse: a^b^b=a. This cancels pairs."
 *   - "n & (n-1) removes the lowest set bit — key to efficient set-bit counting."
 *   - "n & (-n) isolates lowest set bit — used for Fenwick trees."
 *   - Always confirm: "Are we dealing with signed integers? 32-bit or 64-bit?"
 *   - In Java, >> is arithmetic shift (sign-extends), >>> is logical shift (zero-fills).
 *
 * ESSENTIAL BIT TRICKS REFERENCE:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ n & (n-1)     → clear lowest set bit / check if power of 2         │
 * │ n & (-n)      → isolate lowest set bit (Fenwick tree index)         │
 * │ n ^ n = 0     → XOR with self gives 0                               │
 * │ a ^ b ^ b = a → XOR is self-inverse (cancel pairs)                  │
 * │ n >> 1        → divide by 2 (arithmetic, preserves sign)            │
 * │ n << 1        → multiply by 2                                        │
 * │ x | (1<<i)    → set bit i                                           │
 * │ x & ~(1<<i)   → clear bit i                                         │
 * │ (x >> i) & 1  → get bit i (0 or 1)                                  │
 * │ x ^ (1<<i)    → toggle bit i                                         │
 * │ x & 0xFF      → extract lowest byte                                  │
 * │ x | (x-1)     → turn on all bits below lowest set bit               │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * JAVA SPECIFICS:
 *   int: 32-bit signed (-2^31 to 2^31-1)
 *   long: 64-bit signed (-2^63 to 2^63-1)
 *   >> arithmetic right shift (MSB preserved = sign extended)
 *   >>> logical right shift (MSB filled with 0 regardless of sign)
 *   Java has NO unsigned int. Use `& 0xFFFFFFFFL` to treat int as unsigned long.
 */
public class BitManipulation {

    // =========================================================
    // LC 136 — Single Number
    // =========================================================
    // PROBLEM: Array where every element appears twice except one. Find it.
    // CONSTRAINTS: Linear time, constant space.
    //
    // KEY INSIGHT: XOR properties:
    //   a ^ a = 0   (same values cancel)
    //   a ^ 0 = a   (identity)
    //   Commutative and associative: order doesn't matter.
    //   So: a ^ b ^ c ^ b ^ a = c (pairs cancel, unique survives)
    //
    // WHY NOT HASHMAP? O(n) space. XOR gives O(1) space.
    //
    // COMPLEXITY: O(n) time, O(1) space
    //
    // INTERVIEW FOLLOW-UP: "What if THREE copies instead of two?" → LC 137 (AdvancedBitProblems)
    public static int singleNumber(int[] nums) {
        int result = 0;
        for (int n : nums) {
            result ^= n; // accumulate XOR: pairs cancel, single survives
        }
        return result;
        // Example: [4,1,2,1,2]
        // 0^4=4, 4^1=5, 5^2=7, 7^1=6, 6^2=4 → 4 ✓
    }

    // =========================================================
    // LC 191 — Number of 1 Bits (Hamming Weight)
    // =========================================================
    // PROBLEM: Return the number of '1' bits in an unsigned integer.
    //
    // APPROACH 1 (naive): shift and check each of 32 bits → O(32) always
    // APPROACH 2 (Kernighan's): n &= n-1 removes lowest set bit → O(k) where k=set bits
    //
    // KEY INSIGHT on n & (n-1):
    //   n     = ...1010 1000  (lowest set bit is the trailing 1000)
    //   n-1   = ...1010 0111  (borrow propagates through trailing zeros)
    //   n&(n-1)=...1010 0000  (lowest set bit CLEARED, lower bits also cleared)
    //   Each iteration removes exactly one 1-bit. So iterations = count of 1s.
    //
    // COMPLEXITY: O(k) time where k = number of set bits, O(1) space
    //   Best case O(1) for n=0, worst O(32) for n=all-ones
    //
    // ALTERNATIVE: Integer.bitCount(n) — built-in, uses hardware POPCNT instruction
    public static int hammingWeight(int n) {
        int count = 0;
        while (n != 0) {
            n &= (n - 1); // clear lowest set bit
            count++;
        }
        return count;
        // Example: n=11 = 1011b
        // iter1: 1011 & 1010 = 1010 (count=1)
        // iter2: 1010 & 1001 = 1000 (count=2)
        // iter3: 1000 & 0111 = 0000 (count=3) → return 3 ✓
    }

    // Approach 1 for comparison: always 32 iterations
    public static int hammingWeightNaive(int n) {
        int count = 0;
        for (int i = 0; i < 32; i++) {
            count += (n >>> i) & 1; // >>> logical shift: works correctly for negative numbers
        }
        return count;
        // GOTCHA: Use >>> not >> here!
        //   n=Integer.MIN_VALUE = 10000...0 (MSB=1, i.e., negative)
        //   >> 1 would give 11000...0 (sign-extended) → wrong
        //   >>> 1 gives 01000...0 → correct (treats as unsigned)
    }

    // =========================================================
    // LC 338 — Counting Bits
    // =========================================================
    // PROBLEM: Given n, return array ans[0..n] where ans[i] = # of 1-bits in i.
    //
    // KEY INSIGHT: dp[i] = dp[i >> 1] + (i & 1)
    //   i >> 1 is i with LSB removed (we already computed this).
    //   (i & 1) is the LSB (0 or 1).
    //   Example: i=7=111b, i>>1=3=11b, dp[7]=dp[3]+1 (LSB of 7 is 1)
    //   Example: i=6=110b, i>>1=3=11b, dp[6]=dp[3]+0 (LSB of 6 is 0)
    //
    // ALTERNATIVE DP: dp[i] = dp[i & (i-1)] + 1
    //   i & (i-1) removes lowest set bit → already computed
    //
    // COMPLEXITY: O(n) time, O(n) space
    //
    // INTERVIEW TALKING POINT: "This is DP using bit structure of integers — very elegant."
    public static int[] countBits(int n) {
        int[] dp = new int[n + 1];
        dp[0] = 0; // base case: 0 has zero 1-bits
        for (int i = 1; i <= n; i++) {
            dp[i] = dp[i >> 1] + (i & 1);
            // i >> 1: drop LSB (already computed)
            // i & 1: add back LSB contribution
        }
        return dp;
    }

    // =========================================================
    // LC 190 — Reverse Bits
    // =========================================================
    // PROBLEM: Reverse bits of a 32-bit unsigned integer.
    //
    // APPROACH: Process one bit at a time from LSB to MSB of input,
    //   building result from MSB to LSB.
    //
    // KEY STEPS each iteration:
    //   1. result <<= 1         — make room for next bit
    //   2. result |= (n & 1)    — place LSB of n into LSB of result
    //   3. n >>>= 1             — shift n right (unsigned! use >>> not >>)
    //
    // COMPLEXITY: O(32) = O(1) time, O(1) space
    //
    // GOTCHA: Java has no unsigned int. Use long if result > Integer.MAX_VALUE.
    //   LeetCode expects treating int as unsigned, so reversing 1 (32-bit) gives
    //   10000000 00000000 00000000 00000000 = 2147483648 (not representable as int).
    //   Return as long or trust the problem's constraint that result fits int.
    public static int reverseBits(int n) {
        int result = 0;
        for (int i = 0; i < 32; i++) {
            result <<= 1;          // make room
            result |= (n & 1);     // grab LSB of n
            n >>>= 1;              // logical right shift (MUST use >>> for unsigned behavior)
        }
        return result;
        // Example: n=43261596=00000010100101000001111010011100
        // reversed =  00111001011110000010100101000000 = 964176192
    }

    // =========================================================
    // LC 231 — Power of Two
    // =========================================================
    // PROBLEM: Return true if n is a power of two.
    //
    // KEY INSIGHT: Powers of 2 in binary have EXACTLY ONE set bit:
    //   1  = 00001
    //   2  = 00010
    //   4  = 00100
    //   8  = 01000
    //   16 = 10000
    //   n & (n-1) clears the only set bit → result is 0 iff exactly one bit set.
    //
    // EDGE CASE: n=0 → 0 & (-1) = 0, but 0 is NOT a power of 2.
    //            n<0 → signed, has high bit set, n & (n-1) might be 0 for MIN_VALUE.
    //   Always check n > 0 first!
    //
    // COMPLEXITY: O(1) time, O(1) space
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
        // n=1:  0001 & 0000 = 0 → true ✓
        // n=4:  0100 & 0011 = 0 → true ✓
        // n=6:  0110 & 0101 = 0100 ≠ 0 → false ✓
        // n=0:  n > 0 fails → false ✓
    }

    // =========================================================
    // LC 268 — Missing Number
    // =========================================================
    // PROBLEM: Array [0,n] with one missing number. Find it.
    //   Guaranteed: range [0, n], no duplicates, one missing.
    //
    // XOR APPROACH:
    //   KEY INSIGHT: XOR all values [0..n] with all elements.
    //   Pairs cancel: if i appears in both indices AND values, it XORs to 0.
    //   The MISSING value appears only in the index XOR, not in values → survives.
    //
    // FORMULA: result = 0 ^ 0 ^ 1 ^ 1 ^ ... ^ missing (only one) ^ n ^ n ...
    //   = missing
    //
    // ALTERNATIVE: Math approach: n*(n+1)/2 - sum(nums)
    //   Risk: integer overflow for large n. XOR is overflow-safe.
    //
    // COMPLEXITY: O(n) time, O(1) space
    public static int missingNumber(int[] nums) {
        int result = nums.length; // start with n (highest index that has no array element)
        for (int i = 0; i < nums.length; i++) {
            result ^= i ^ nums[i]; // XOR both index and value at that index
        }
        return result;
        // Example: [3,0,1], n=3
        // result = 3
        // i=0: result ^= 0 ^ 3 = 3^0^3 = 0
        // i=1: result ^= 1 ^ 0 = 0^1^0 = 1
        // i=2: result ^= 2 ^ 1 = 1^2^1 = 2 → missing=2 ✓
    }

    // =========================================================
    // LC 371 — Sum of Two Integers Without + Operator
    // =========================================================
    // PROBLEM: Add two integers without using + or -.
    //
    // KEY INSIGHT: Binary addition = XOR (sum without carry) + AND<<1 (carries)
    //   a ^ b: adds bits at each position WITHOUT carry (0+0=0, 1+0=1, 1+1=0)
    //   (a & b) << 1: produces carry bits shifted left by 1
    //   Repeat until no carry remains.
    //
    // EXAMPLE: 5+3 = 0101 + 0011
    //   sum=0110, carry=0010<<1=0100 (4)
    //   sum=0010, carry=0100<<1... wait let me trace:
    //   iter1: sum=0110, carry=0010 (no shift yet)... actually:
    //   a=5=0101, b=3=0011
    //   XOR=0110=6, AND=0001, carry=0010
    //   a=6, b=2: XOR=0100=4, AND=0010, carry=0100
    //   a=4, b=4: XOR=0000=0, AND=0100, carry=1000
    //   a=0, b=8: XOR=1000=8, AND=0000, carry=0 → done → 8 ✓
    //
    // COMPLEXITY: O(1) — at most 32 iterations (bit width)
    //
    // GOTCHA: Java int overflow with left shift. Works correctly for 32-bit int
    //   because Java wraps on overflow. For LC, this is expected behavior.
    public static int getSum(int a, int b) {
        while (b != 0) {
            int carry = (a & b) << 1; // compute carry positions, shift left
            a = a ^ b;               // sum without carry
            b = carry;               // next iteration: add the carry
        }
        return a;
    }

    // =========================================================
    // LC 260 — Single Number III (two unique numbers)
    // =========================================================
    // PROBLEM: Array where every element appears twice except TWO.
    //   Find both unique numbers.
    //
    // APPROACH:
    //   Step 1: XOR all elements → xorAll = a ^ b (where a, b are the two uniques)
    //   Step 2: Find any bit that differs between a and b.
    //     Use xorAll & (-xorAll) to get the LOWEST SET BIT of xorAll.
    //     This bit is 1 in a and 0 in b (or vice versa) — guaranteed since a≠b.
    //   Step 3: Partition all numbers by that bit: group1 has bit set, group2 doesn't.
    //     XOR group1: all pairs cancel → one unique survives.
    //     XOR group2: all pairs cancel → other unique survives.
    //
    // KEY INSIGHT: The "differing bit" as a partition key separates the two uniques
    //   into different groups, while keeping duplicate pairs together in the SAME group.
    //
    // COMPLEXITY: O(n) time, O(1) space
    public static int[] singleNumberIII(int[] nums) {
        // Step 1: XOR everything → xorAll = a ^ b
        int xorAll = 0;
        for (int n : nums) xorAll ^= n;

        // Step 2: isolate a differing bit between a and b
        // n & (-n): two's complement isolates lowest set bit
        // -n = ~n + 1 (in two's complement)
        int diffBit = xorAll & (-xorAll);

        // Step 3: partition and XOR each group
        int a = 0, b = 0;
        for (int n : nums) {
            if ((n & diffBit) != 0) {
                a ^= n; // group with bit set
            } else {
                b ^= n; // group with bit clear
            }
        }
        return new int[]{a, b};
        // Example: [1,2,1,3,2,5] → unique: {3,5}
        // xorAll = 3^5 = 011^101 = 110
        // diffBit = 110 & 010 = 010 (bit 1)
        // Group {bit1=1}: 2,3,2 → XOR=3
        // Group {bit1=0}: 1,1,5 → XOR=5 → [3,5] ✓
    }

    // =========================================================
    // LC 201 — Bitwise AND of Numbers Range [left, right]
    // =========================================================
    // PROBLEM: Return the bitwise AND of all numbers in range [left, right].
    //
    // KEY INSIGHT: If left < right, there exist two consecutive numbers whose
    //   lowest bit flips (one is even, next is odd → last bit alternates 0,1 → AND=0).
    //   So the result = the COMMON PREFIX of left and right in binary.
    //
    // APPROACH: Right-shift both until they're equal.
    //   Count shifts. Result = (left == right value) << shifts.
    //   The shifted-out bits contributed 0 to the AND (they flipped somewhere in range).
    //
    // COMPLEXITY: O(log n) = O(32) = O(1) time, O(1) space
    //
    // EXAMPLE: left=5=101, right=7=111
    //   shift once: left=10=2, right=11=3 (differ)
    //   shift twice: left=01=1, right=01=1 (equal!) → 1 << 2 = 4
    //   Verify: 5&6&7=100=4 ✓
    public static int rangeBitwiseAnd(int left, int right) {
        int shifts = 0;
        while (left != right) {
            left >>= 1;  // trim LSB
            right >>= 1; // trim LSB
            shifts++;
        }
        return left << shifts; // restore the common prefix to its original position
    }

    // =========================================================
    // BONUS: Bit Trick — Check if integer is even/odd
    // =========================================================
    public static boolean isEven(int n) { return (n & 1) == 0; }

    // BONUS: Swap two integers without temp variable
    public static void swapBits(int[] a, int i, int j) {
        if (i == j) return; // GOTCHA: a[i]^=a[i] would zero it! Must check same index.
        a[i] ^= a[j];
        a[j] ^= a[i];
        a[i] ^= a[j];
        // After: a[i]=old a[j], a[j]=old a[i]
    }

    // BONUS: Count trailing zeros (useful for Fenwick tree, power of 2 checks)
    public static int countTrailingZeros(int n) {
        if (n == 0) return 32;
        return Integer.numberOfTrailingZeros(n); // built-in uses BSF hardware instruction
        // Manual: return hammingWeight((n & (-n)) - 1);
    }

    // BONUS: Set/Clear/Toggle/Get bit
    public static int setBit(int n, int i)    { return n | (1 << i); }
    public static int clearBit(int n, int i)  { return n & ~(1 << i); }
    public static int toggleBit(int n, int i) { return n ^ (1 << i); }
    public static int getBit(int n, int i)    { return (n >> i) & 1; }

    // =========================================================
    // MAIN: Demo + test all problems
    // =========================================================
    public static void main(String[] args) {
        System.out.println("=== LC 136: Single Number ===");
        System.out.println(singleNumber(new int[]{2, 2, 1}));          // 1
        System.out.println(singleNumber(new int[]{4, 1, 2, 1, 2}));    // 4

        System.out.println("\n=== LC 191: Hamming Weight ===");
        System.out.println(hammingWeight(11));    // 3 (1011 → three 1s)
        System.out.println(hammingWeight(128));   // 1 (10000000)
        System.out.println(hammingWeightNaive(11)); // 3 (same result, different approach)
        System.out.println("Java built-in: " + Integer.bitCount(11)); // 3

        System.out.println("\n=== LC 338: Counting Bits ===");
        System.out.println(Arrays.toString(countBits(5)));  // [0,1,1,2,1,2]
        System.out.println(Arrays.toString(countBits(8)));  // [0,1,1,2,1,2,2,3,1]

        System.out.println("\n=== LC 190: Reverse Bits ===");
        System.out.printf("reverseBits(43261596) = %d%n", reverseBits(43261596)); // 964176192

        System.out.println("\n=== LC 231: Power of Two ===");
        System.out.println(isPowerOfTwo(1));   // true
        System.out.println(isPowerOfTwo(16));  // true
        System.out.println(isPowerOfTwo(3));   // false
        System.out.println(isPowerOfTwo(0));   // false (EDGE CASE!)
        System.out.println(isPowerOfTwo(-4));  // false (EDGE CASE!)

        System.out.println("\n=== LC 268: Missing Number ===");
        System.out.println(missingNumber(new int[]{3, 0, 1}));        // 2
        System.out.println(missingNumber(new int[]{9,6,4,2,3,5,7,0,1})); // 8

        System.out.println("\n=== LC 371: Sum Without + ===");
        System.out.println(getSum(1, 2));     // 3
        System.out.println(getSum(-1, 2));    // 1 (negative numbers work too)
        System.out.println(getSum(5, 3));     // 8

        System.out.println("\n=== LC 260: Single Number III ===");
        System.out.println(Arrays.toString(singleNumberIII(new int[]{1, 2, 1, 3, 2, 5}))); // [3,5]
        System.out.println(Arrays.toString(singleNumberIII(new int[]{-1, 0}))); // [-1, 0]

        System.out.println("\n=== LC 201: Bitwise AND of Range ===");
        System.out.println(rangeBitwiseAnd(5, 7));   // 4
        System.out.println(rangeBitwiseAnd(0, 0));   // 0
        System.out.println(rangeBitwiseAnd(1, 2147483647)); // 0 (huge range, all bits flip)

        System.out.println("\n=== Bit Utility Functions ===");
        System.out.println("isEven(4)=" + isEven(4));           // true
        System.out.println("isEven(7)=" + isEven(7));           // false
        int[] arr = {5, 10};
        swapBits(arr, 0, 1);
        System.out.println("After swap: " + Arrays.toString(arr)); // [10, 5]
        System.out.println("setBit(5,1)=" + Integer.toBinaryString(setBit(5,1)));   // 111=7
        System.out.println("clearBit(7,1)=" + Integer.toBinaryString(clearBit(7,1))); // 101=5
        System.out.println("toggleBit(5,0)=" + Integer.toBinaryString(toggleBit(5,0))); // 100=4
        System.out.println("getBit(5,2)=" + getBit(5,2)); // 1 (5=101b, bit2=1)

        System.out.println("\n=== INTERVIEW Q&A ===");
        System.out.println("Q: What does n & (-n) do?");
        System.out.println("A: Isolates the lowest set bit. -n = two's complement = ~n+1.");
        System.out.println("   Example: n=12=1100, -n=0100, n&(-n)=0100 (lowest set bit).");
        System.out.println("   Used in Fenwick trees (BIT) for index arithmetic.");
        System.out.println();
        System.out.println("Q: Java >> vs >>> ?");
        System.out.println("A: >> arithmetic shift: fills with sign bit (MSB). For negatives,");
        System.out.println("   fills with 1. >>> logical shift: always fills with 0.");
        System.out.println("   Use >>> when treating int as unsigned (e.g., reverseBits, rangeBitwiseAnd).");
    }
}
