package com.java.ds.trie;

import java.util.*;

/**
 * TRIE PROBLEMS — Intermediate to Advanced Applications
 *
 * CONCEPT:
 *   These problems use the Trie as a core data structure but require
 *   additional logic: DFS for collection, frequency tracking, prefix matching.
 *
 * FILES COVERED:
 *   1. LC 720  — Longest Word in Dictionary (all prefixes must exist)
 *   2. LC 648  — Replace Words (find shortest root in Trie)
 *   3. LC 642  — Design Search Autocomplete System (top-3 by frequency + DFS)
 *
 * INTERVIEW ANGLE:
 *   These test whether you can EXTEND the Trie beyond simple insert/search.
 *   Common follow-ups: "Rank by frequency", "Add delete support",
 *   "Support case-insensitive search", "Handle Unicode".
 */
public class TrieProblems {

    // =========================================================================
    // SHARED TRIE INFRASTRUCTURE
    // =========================================================================

    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEnd;
        String word;       // store the full word at the leaf (avoids rebuilding from path)
        int freq;          // frequency for autocomplete ranking
    }

    private TrieNode newNode() { return new TrieNode(); }

    // =========================================================================
    // LC 720 — LONGEST WORD WITH ALL PREFIXES IN DICTIONARY
    // =========================================================================

    /**
     * Find the longest word in words[] such that every prefix of it
     * also exists in words[]. If tie, return lexicographically smallest.
     *
     * APPROACH: insert all words into Trie. Then DFS the Trie — we can only
     * traverse through nodes where isEnd == true (prefix is a word).
     * The deepest reachable word is the answer.
     *
     * EXAMPLE: words = ["a","banana","app","appl","ap","apply","apple"]
     *   "apple" has all prefixes: "a"✓ "ap"✓ "app"✓ "appl"✓ "apple"✓
     *   "banana" has prefix "b" NOT in words → skip
     *   Answer: "apple"
     *
     * TIME: O(N*L) build + O(N*L) DFS = O(N*L)
     * SPACE: O(N*L) trie
     *
     * ALTERNATIVE: sort words, add to HashSet. For each word, check if
     * word[0..len-2] is in HashSet. O(N*L log N). Trie approach is more elegant.
     */
    public String longestWord(String[] words) {
        TrieNode root = newNode();

        // build trie
        for (String word : words) {
            TrieNode cur = root;
            for (char c : word.toCharArray()) {
                int idx = c - 'a';
                if (cur.children[idx] == null) cur.children[idx] = newNode();
                cur = cur.children[idx];
            }
            cur.isEnd = true;
            cur.word = word;
        }

        // DFS: only traverse through end-of-word nodes
        // Use iterative DFS with stack (or recursive — both fine)
        String[] best = {""};
        dfsLongest(root, best);
        return best[0];
    }

    private void dfsLongest(TrieNode node, String[] best) {
        for (TrieNode child : node.children) {
            if (child != null && child.isEnd) {
                // only proceed if this prefix is a complete word
                if (child.word.length() > best[0].length() ||
                    (child.word.length() == best[0].length() && child.word.compareTo(best[0]) < 0)) {
                    best[0] = child.word;
                }
                dfsLongest(child, best); // recurse only into valid paths
            }
        }
    }
    // GOTCHA: the root node itself has no 'isEnd'. Start DFS from root's children.
    // GOTCHA: tie-breaking requires lexicographic comparison — use String.compareTo.

    // =========================================================================
    // LC 648 — REPLACE WORDS
    // =========================================================================

    /**
     * Given a dictionary of "roots" and a sentence, replace each word in the
     * sentence with the shortest matching root. If no root matches, keep original.
     *
     * EXAMPLE: roots = ["cat","bat","rat"], sentence = "the cattle was rattled by the battery"
     *   "cattle" → "cat" (root "cat" matches at start)
     *   "rattled" → "rat"
     *   "battery" → "bat"
     *   Answer: "the cat was rat by the bat"
     *
     * APPROACH:
     *   Build Trie from roots. For each word, walk Trie until we hit an isEnd node
     *   (found a root) or run out of Trie (no root → keep original word).
     *
     * ALTERNATIVE: HashSet of roots, for each word check all prefixes O(L²).
     *   Trie gives O(L) per word which is better when many roots share prefixes.
     *
     * TIME: O(D) build trie where D = total chars in dictionary
     *       O(S) process sentence where S = total chars in sentence
     * SPACE: O(D) for trie
     */
    public String replaceWords(List<String> dictionary, String sentence) {
        TrieNode root = newNode();

        // build trie from roots
        for (String root2 : dictionary) {
            TrieNode cur = root;
            for (char c : root2.toCharArray()) {
                int idx = c - 'a';
                if (cur.children[idx] == null) cur.children[idx] = newNode();
                cur = cur.children[idx];
            }
            cur.isEnd = true;
            cur.word = root2;
        }

        // process each word in sentence
        StringBuilder sb = new StringBuilder();
        for (String word : sentence.split(" ")) {
            if (sb.length() > 0) sb.append(' ');

            // find shortest matching root
            TrieNode cur = root;
            String replacement = null;
            for (char c : word.toCharArray()) {
                int idx = c - 'a';
                if (cur.children[idx] == null) break; // no root matches this prefix
                cur = cur.children[idx];
                if (cur.isEnd) {
                    replacement = cur.word; // found a root — this is the shortest match
                    break;                  // STOP: we want SHORTEST root, not longest
                }
            }
            sb.append(replacement != null ? replacement : word);
        }
        return sb.toString();
    }
    // WHY SHORTEST ROOT: the Trie traversal hits the shallowest isEnd first.
    // Once we find it, we stop. This naturally gives the shortest matching root.

    // =========================================================================
    // LC 642 — DESIGN SEARCH AUTOCOMPLETE SYSTEM
    // =========================================================================

    /**
     * Autocomplete system: given historical sentences with frequencies,
     * input characters one at a time, return top-3 hottest (most frequent)
     * sentences that match the current prefix. '#' ends current input.
     *
     * APPROACH:
     *   Trie where each node stores Map<String, Integer> of sentences with their frequencies
     *   that pass through this node. On input(c), walk to the current Trie node,
     *   then return the top-3 from that node's map.
     *
     * ALTERNATIVE TRIE: each leaf stores (sentence, freq), DFS from current prefix
     * node to collect all candidates. We use the "store at each node" approach
     * for O(1) retrieval after traversal.
     *
     * TIME: input(c): O(L + k log k) where L = prefix length, k = candidates
     * SPACE: O(N * L) where N = sentences, L = avg sentence length
     */
    static class AutocompleteSystem {

        private static class AutoNode {
            AutoNode[] children = new AutoNode[128]; // all ASCII (includes space)
            // Store all sentences passing through this node with their frequencies
            Map<String, Integer> counts = new HashMap<>();
        }

        private final AutoNode root = new AutoNode();
        private AutoNode curNode = root;
        private final StringBuilder input = new StringBuilder();

        public AutocompleteSystem(String[] sentences, int[] times) {
            for (int i = 0; i < sentences.length; i++) {
                addSentence(sentences[i], times[i]);
            }
        }

        private void addSentence(String sentence, int count) {
            AutoNode cur = root;
            for (char c : sentence.toCharArray()) {
                if (cur.children[c] == null) cur.children[c] = new AutoNode();
                cur = cur.children[c];
                // store at EVERY node on the path (so prefix lookup is O(1))
                cur.counts.merge(sentence, count, Integer::sum);
            }
        }

        public List<String> input(char c) {
            if (c == '#') {
                // end of input: save current input as a new sentence
                String sentence = input.toString();
                addSentence(sentence, 1);
                // reset
                input.setLength(0);
                curNode = root;
                return Collections.emptyList();
            }

            input.append(c);

            if (curNode == null || curNode.children[c] == null) {
                curNode = null; // dead end — no sentences match this prefix
                return Collections.emptyList();
            }

            curNode = curNode.children[c];

            // get top-3 by frequency (tie: lexicographic order)
            return curNode.counts.entrySet().stream()
                .sorted((a, b) -> a.getValue().equals(b.getValue())
                    ? a.getKey().compareTo(b.getKey())   // tie: alphabetical
                    : b.getValue() - a.getValue())        // more frequent first
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        }
    }
    // TIME TRADE-OFF: storing at every node is O(N*L) space but O(L + k log k) per query.
    // DFS-based approach is O(N*L) space + O(L + all_matches * log all_matches) per query.
    // The "store at every node" approach is faster for large autocomplete systems.

    // Q: Why store the full sentence at every node, not just the leaf?
    // A: Without it, we'd need to DFS from the prefix node to collect all sentences —
    //    O(subtree size) per query. Storing at every node makes retrieval O(k log k)
    //    where k = sentences through that node. Classic space-time tradeoff.

    public static void main(String[] args) {
        TrieProblems tp = new TrieProblems();

        // LC 720
        System.out.println("=== Longest Word With All Prefixes ===");
        System.out.println(tp.longestWord(new String[]{"a","banana","app","appl","ap","apply","apple"}));
        // "apple"
        System.out.println(tp.longestWord(new String[]{"w","wo","wor","worl","world"}));
        // "world"

        // LC 648
        System.out.println("\n=== Replace Words ===");
        System.out.println(tp.replaceWords(
            Arrays.asList("cat","bat","rat"),
            "the cattle was rattled by the battery"));
        // "the cat was rat by the bat"

        // LC 642
        System.out.println("\n=== Autocomplete System ===");
        AutocompleteSystem ac = new AutocompleteSystem(
            new String[]{"i love you","island","ironman","i love leetcode"},
            new int[]{5, 3, 2, 2}
        );
        System.out.println(ac.input('i'));  // [i love you, island, i love leetcode]
        System.out.println(ac.input(' ')); // [i love you, i love leetcode]
        System.out.println(ac.input('a')); // []
        System.out.println(ac.input('#')); // [] — saves "i a"
    }
}
