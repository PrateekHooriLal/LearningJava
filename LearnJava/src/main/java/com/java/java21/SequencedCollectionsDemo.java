package com.java.java21;

import java.util.*;

/**
 * =====================================================================
 * JAVA 21 FEATURE: SEQUENCED COLLECTIONS
 * =====================================================================
 *
 * WHAT IS IT? (Java 21)
 * -------------------------------------------------------
 * Java 21 introduced three new interfaces in the collections framework:
 *   1. SequencedCollection<E>  — ordered collection with first/last access
 *   2. SequencedSet<E>         — SequencedCollection with no duplicates
 *   3. SequencedMap<K,V>       — ordered map with first/last entry access
 *
 * These interfaces were added to EXISTING collection classes — it's a
 * backwards-compatible change that UNIFIES the API for ordered collections.
 *
 * THE PROBLEM THEY SOLVE:
 * -------------------------------------------------------
 * Before Java 21, getting the first or last element of different collections
 * required DIFFERENT, INCONSISTENT APIs:
 *
 *   Collection Type     | Get First              | Get Last
 *   --------------------|------------------------|------------------------
 *   List                | list.get(0)            | list.get(list.size()-1)
 *   Deque               | deque.peekFirst()      | deque.peekLast()
 *   SortedSet           | sortedSet.first()      | sortedSet.last()
 *   LinkedHashSet       | NO easy API!           | NO easy API!
 *   LinkedHashMap       | NO easy API!           | NO easy API!
 *
 * With Java 21 Sequenced Collections:
 *   ANY of the above → collection.getFirst() / collection.getLast()
 *   UNIFORM API everywhere.
 *
 * SEQUENCED COLLECTION INTERFACE adds:
 *   - getFirst()    → first element (throws NoSuchElementException if empty)
 *   - getLast()     → last element
 *   - addFirst(e)   → insert at head
 *   - addLast(e)    → append at tail
 *   - removeFirst() → remove and return first element
 *   - removeLast()  → remove and return last element
 *   - reversed()    → returns a REVERSED VIEW (not a copy) of the collection
 *
 * WHICH COLLECTIONS IMPLEMENT THESE INTERFACES?
 *   SequencedCollection: List, Deque, LinkedHashSet (and its subtypes)
 *   SequencedSet:        LinkedHashSet, SortedSet (TreeSet)
 *   SequencedMap:        LinkedHashMap, SortedMap (TreeMap)
 *
 *   NOTE: HashSet and HashMap are NOT sequenced — they have no order.
 *
 * INTERVIEW MUST-KNOW QUESTIONS:
 *   Q: "What is SequencedCollection in Java 21?"
 *   A: A new interface that unifies first/last element access across List, Deque,
 *      LinkedHashSet. Adds getFirst(), getLast(), addFirst(), addLast(), reversed().
 *
 *   Q: "What does reversed() return?"
 *   A: A VIEW of the collection in reverse order. NOT a copy — mutations to the
 *      reversed view affect the original. Like Collections.unmodifiableList() — it's a wrapper.
 *
 *   Q: "Does HashMap implement SequencedMap?"
 *   A: No. HashMap has no guaranteed order, so it can't have meaningful first/last.
 *      LinkedHashMap (insertion-ordered) and TreeMap (sorted) do implement SequencedMap.
 *
 *   Q: "What is the difference between getFirst() and get(0) on a List?"
 *   A: Functionally the same for List. But getFirst() is semantically cleaner and
 *      throws NoSuchElementException on empty (same as Deque.getFirst()).
 *      get(0) throws IndexOutOfBoundsException. The exception type differs.
 *
 * =====================================================================
 */
public class SequencedCollectionsDemo {

    // =====================================================================
    // 1. LIST — already had get(0) but now has uniform getFirst()/getLast()
    // =====================================================================
    static void demo1_List() {
        System.out.println("=== 1. SequencedCollection on List ===");

        List<String> list = new ArrayList<>(List.of("Alice", "Bob", "Charlie", "Diana"));

        // Old way:
        System.out.println("  Old way first: " + list.get(0));
        System.out.println("  Old way last:  " + list.get(list.size() - 1));

        // New Java 21 way:
        System.out.println("  New way first: " + list.getFirst());
        System.out.println("  New way last:  " + list.getLast());

        // addFirst / addLast
        list.addFirst("Zara");     // insert at position 0
        list.addLast("Edward");   // append at end
        System.out.println("  After addFirst('Zara') + addLast('Edward'): " + list);

        // removeFirst / removeLast
        String removed = list.removeFirst();
        System.out.println("  Removed first: " + removed + ", list: " + list);

        // reversed() — returns a VIEW (backed by original list)
        // IMPORTANT: Mutations via the reversed view affect the original!
        List<String> reversed = list.reversed();
        System.out.println("  Reversed view: " + reversed);
        System.out.println("  Original (unchanged): " + list); // original is still forward order
    }

    // =====================================================================
    // 2. DEQUE — had peekFirst()/peekLast() — now unified with getFirst()/getLast()
    // =====================================================================
    static void demo2_Deque() {
        System.out.println("\n=== 2. SequencedCollection on Deque ===");

        Deque<Integer> deque = new ArrayDeque<>(List.of(10, 20, 30, 40));

        // Old Deque API (still works):
        System.out.println("  Old peekFirst: " + deque.peekFirst());
        System.out.println("  Old peekLast:  " + deque.peekLast());

        // New unified API:
        System.out.println("  New getFirst:  " + deque.getFirst());
        System.out.println("  New getLast:   " + deque.getLast());

        // Both APIs coexist — getFirst() is now the universal way
        Deque<Integer> reversed = deque.reversed(); // SequencedCollection.reversed()
        System.out.println("  Reversed deque: " + reversed);
    }

    // =====================================================================
    // 3. LINKEDHASHSET — HAD NO first/last API before Java 21
    // =====================================================================
    static void demo3_LinkedHashSet() {
        System.out.println("\n=== 3. SequencedSet on LinkedHashSet ===");

        // LinkedHashSet preserves insertion order (unlike HashSet which is unordered)
        // Before Java 21: NO way to get first/last without iterating
        // Java 21: now implements SequencedSet (which extends SequencedCollection)

        LinkedHashSet<String> lhs = new LinkedHashSet<>();
        lhs.add("one");
        lhs.add("two");
        lhs.add("three");
        lhs.add("four");

        // Old way (UGLY — had to iterate or convert to list):
        // String first = lhs.iterator().next();                          // only first
        // String last = lhs.stream().reduce((a, b) -> b).orElseThrow(); // last — O(n)

        // New Java 21 way:
        System.out.println("  First: " + lhs.getFirst());  // "one"
        System.out.println("  Last:  " + lhs.getLast());   // "four"

        // addFirst / addLast — if element already exists, it's MOVED to first/last position
        lhs.addFirst("zero");
        System.out.println("  After addFirst('zero'): " + lhs);

        // reversed() — reverse view of insertion order
        SequencedSet<String> reversed = lhs.reversed();
        System.out.println("  Reversed: " + reversed);
    }

    // =====================================================================
    // 4. LINKEDHASHMAP — SequencedMap for first/last entries
    // =====================================================================
    static void demo4_LinkedHashMap() {
        System.out.println("\n=== 4. SequencedMap on LinkedHashMap ===");

        // LinkedHashMap preserves insertion order
        // Java 21: implements SequencedMap — adds firstEntry(), lastEntry(), reversed()

        LinkedHashMap<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 82);
        scores.put("Charlie", 91);
        scores.put("Diana", 78);

        // New SequencedMap methods:
        Map.Entry<String, Integer> first = scores.firstEntry();
        Map.Entry<String, Integer> last  = scores.lastEntry();

        System.out.println("  First entry: " + first.getKey() + " = " + first.getValue());
        System.out.println("  Last entry:  " + last.getKey()  + " = " + last.getValue());

        // pollFirstEntry / pollLastEntry — remove and return
        Map.Entry<String, Integer> polled = scores.pollFirstEntry();
        System.out.println("  Polled first: " + polled.getKey() + " = " + polled.getValue());
        System.out.println("  Map after poll: " + scores);

        // reversed() — returns a SequencedMap view in reverse insertion order
        SequencedMap<String, Integer> reversed = scores.reversed();
        System.out.println("  Reversed map entries:");
        reversed.forEach((k, v) -> System.out.println("    " + k + " = " + v));
    }

    // =====================================================================
    // 5. REVERSED() IS A VIEW — Not a copy
    // =====================================================================
    static void demo5_ReversedIsAView() {
        System.out.println("\n=== 5. reversed() returns a VIEW, not a copy ===");

        List<Integer> original = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> reversedView = original.reversed();

        System.out.println("  Original: " + original);
        System.out.println("  Reversed view: " + reversedView);

        // Mutate the ORIGINAL — the reversed view reflects the change
        original.add(6);
        System.out.println("  After adding 6 to original:");
        System.out.println("  Original: " + original);
        System.out.println("  Reversed view (reflects change): " + reversedView);

        // Mutate through the REVERSED VIEW — original is affected
        reversedView.addFirst(0); // addFirst on reversed = addLast on original
        System.out.println("  After addFirst(0) on reversed view:");
        System.out.println("  Original (reflects change): " + original);

        // Interview key point: reversed() is like a mirror — mutations are bidirectional
        System.out.println("  KEY POINT: reversed() is a live VIEW, not a snapshot.");
        System.out.println("  To get an independent copy: new ArrayList<>(original.reversed())");
    }

    // =====================================================================
    // 6. TREEMAP — Also implements SequencedMap (sorted order)
    // =====================================================================
    static void demo6_TreeMap() {
        System.out.println("\n=== 6. SequencedMap on TreeMap (sorted) ===");

        // TreeMap is sorted by natural key order (or Comparator)
        // Java 21: TreeMap implements SequencedMap
        TreeMap<String, Integer> sorted = new TreeMap<>();
        sorted.put("Banana", 2);
        sorted.put("Apple", 5);
        sorted.put("Cherry", 1);
        sorted.put("Date", 3);

        System.out.println("  TreeMap (sorted): " + sorted);
        System.out.println("  First entry (alphabetically): " + sorted.firstEntry());
        System.out.println("  Last entry:  " + sorted.lastEntry());

        // Note: SortedMap already had firstKey()/lastKey() — SequencedMap adds firstEntry()/lastEntry()
    }

    // =====================================================================
    // MAIN — Run all demos
    // =====================================================================
    public static void main(String[] args) {
        demo1_List();
        demo2_Deque();
        demo3_LinkedHashSet();
        demo4_LinkedHashMap();
        demo5_ReversedIsAView();
        demo6_TreeMap();

        System.out.println("\n=== Summary: Which Collections are Sequenced? ===");
        System.out.println("  SequencedCollection: List (ArrayList, LinkedList), Deque, LinkedHashSet");
        System.out.println("  SequencedSet:        LinkedHashSet, TreeSet (SortedSet)");
        System.out.println("  SequencedMap:        LinkedHashMap, TreeMap (SortedMap)");
        System.out.println("  NOT Sequenced:       HashSet, HashMap (no defined order)");
    }
}
