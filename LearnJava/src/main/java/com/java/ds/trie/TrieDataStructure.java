package com.java.ds.trie;

import java.util.*;

/**
 * TRIE DATA STRUCTURE — Prefix Tree
 *
 * CONCEPT:
 *   A Trie (pronounced "try", from re-TRIE-val) is a tree where each node
 *   represents a CHARACTER, and paths from root to leaf spell out words.
 *   Every node has up to 26 children (for lowercase English letters).
 *
 * WHY TRIE OVER HASHMAP FOR PREFIX QUERIES:
 *   HashMap<String, Boolean> for exact lookup: O(L) per word where L = word length.
 *   Trie for prefix search: O(L) to find ALL words sharing a prefix — O(1) per word
 *   after traversing to the prefix node. HashMap requires iterating all keys O(N*L).
 *   For autocomplete on 1M words with prefix "app": Trie jumps to "app" node,
 *   DFS collects descendants. HashMap must scan all 1M keys.
 *
 * TIME COMPLEXITY:
 *   insert:     O(L) — L = length of word
 *   search:     O(L)
 *   startsWith: O(L)
 *   delete:     O(L)
 *
 * SPACE COMPLEXITY: O(N * L * 26) worst case — N words, each L chars, each node 26 children
 *   But shared prefixes save space: "apple" and "application" share "appl" nodes.
 *
 * INTERVIEW ANGLE:
 *   Trie problems test your ability to design multi-branch recursive structures.
 *   Follow-up questions: "Add frequency count per word" (for autocomplete ranking),
 *   "Support delete" (careful with shared prefixes), "Support wildcard `.`" (DFS).
 *
 * REAL-WORLD USE:
 *   - Autocomplete systems (Google search suggestions, IDE code completion)
 *   - Spell checkers
 *   - IP routing tables (prefix matching)
 *   - DNA sequence databases
 */
public class TrieDataStructure {

    // =========================================================================
    // TRIE NODE
    // =========================================================================

    /**
     * TrieNode: each node represents ONE character (the character is implicit
     * in the parent's children array index).
     *
     * children[0] = child for 'a', children[1] = child for 'b', ..., children[25] = 'z'
     * isEnd = true means a valid word ends at this node
     *
     * Alternative: use Map<Character, TrieNode> children for non-ASCII chars.
     * Array is faster (O(1) index vs O(1) amortized HashMap) but uses 26x more space.
     */
    static class TrieNode {
        TrieNode[] children = new TrieNode[26]; // null = child doesn't exist
        boolean isEnd;                          // true = word ends here
        int count;                              // optional: word frequency for autocomplete
    }

    private final TrieNode root = new TrieNode(); // root has no char, represents ""

    // =========================================================================
    // CORE OPERATIONS
    // =========================================================================

    /**
     * INSERT a word into the trie.
     *
     * Walk down the trie creating nodes as needed.
     * Mark the last node as a word ending.
     *
     * Time: O(L)  Space: O(L) new nodes in worst case
     */
    public void insert(String word) {
        TrieNode cur = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';                                // 'a'=0, 'b'=1, ..., 'z'=25
            if (cur.children[idx] == null) {
                cur.children[idx] = new TrieNode();           // create node if missing
            }
            cur = cur.children[idx];
        }
        cur.isEnd = true;   // mark end of word
        cur.count++;        // track frequency (for autocomplete ranking)
    }

    /**
     * SEARCH for an exact word.
     *
     * Walk down the trie following each character.
     * Return true only if path exists AND last node is word ending.
     *
     * GOTCHA: "app" is in trie if we inserted "app", NOT just because "apple" is there.
     * The isEnd flag distinguishes "prefix that is also a word" from "prefix only".
     *
     * Time: O(L)
     */
    public boolean search(String word) {
        TrieNode node = findNode(word);
        return node != null && node.isEnd; // path must exist AND end flag must be set
    }

    /**
     * STARTSWITH — check if any word has this prefix.
     *
     * Same as search but don't require isEnd — just need path to exist.
     *
     * Time: O(L)
     */
    public boolean startsWith(String prefix) {
        return findNode(prefix) != null; // just need the path to exist
    }

    /**
     * Helper: walk trie to end of string, return last node (or null if path doesn't exist).
     */
    private TrieNode findNode(String s) {
        TrieNode cur = root;
        for (char c : s.toCharArray()) {
            int idx = c - 'a';
            if (cur.children[idx] == null) return null; // prefix not in trie
            cur = cur.children[idx];
        }
        return cur; // last node of the prefix path
    }

    /**
     * DELETE a word from the trie.
     *
     * Tricky: we can only delete a node if it has NO other children
     * and is not an end-of-word for another word.
     *
     * Example: inserting "app" and "apple", then deleting "app":
     *   → just unset isEnd on 'p' node, keep nodes (they're shared with "apple")
     *
     * Example: only "app" in trie, deleting "app":
     *   → unset isEnd AND delete 'a', 'p', 'p' nodes (all are leaf/dead-end now)
     *
     * Approach: recursive DFS, delete on the way back up.
     * A node can be deleted if: it's a leaf (no children) AND not an end-of-word.
     *
     * Time: O(L)
     */
    public void delete(String word) {
        deleteHelper(root, word, 0);
    }

    private boolean deleteHelper(TrieNode cur, String word, int depth) {
        if (cur == null) return false; // word not in trie

        if (depth == word.length()) {
            if (!cur.isEnd) return false; // word not in trie
            cur.isEnd = false;            // unmark end of word
            cur.count = 0;
            // delete this node only if it has no children
            return isLeaf(cur);           // true = can be deleted by parent
        }

        int idx = word.charAt(depth) - 'a';
        boolean shouldDeleteChild = deleteHelper(cur.children[idx], word, depth + 1);

        if (shouldDeleteChild) {
            cur.children[idx] = null;     // delete the child
            // delete this node if it's now a childless non-word node
            return isLeaf(cur) && !cur.isEnd;
        }
        return false;
    }

    private boolean isLeaf(TrieNode node) {
        for (TrieNode child : node.children) {
            if (child != null) return false;
        }
        return true;
    }

    // =========================================================================
    // LC 211 — ADD AND SEARCH WORD (Wildcard '.' via DFS)
    // =========================================================================

    /**
     * WordDictionary supports '.' which matches ANY single character.
     *
     * KEY INSIGHT: '.' at position i means we must explore ALL non-null children
     * at depth i (not just one path). → DFS with branching on '.'.
     *
     * Time: O(L) for no wildcards, O(26^k * L) worst case for k wildcards.
     *   (In practice, only a few '.' in any query.)
     *
     * INTERVIEW FOLLOW-UP: "What if '.*' means match zero or more chars?"
     * → More complex backtracking, similar to regex matching (LC 10).
     */
    static class WordDictionary {
        private final TrieNode root = new TrieNode();

        public void addWord(String word) {
            TrieNode cur = root;
            for (char c : word.toCharArray()) {
                int idx = c - 'a';
                if (cur.children[idx] == null) cur.children[idx] = new TrieNode();
                cur = cur.children[idx];
            }
            cur.isEnd = true;
        }

        public boolean search(String word) {
            return searchHelper(root, word, 0);
        }

        private boolean searchHelper(TrieNode cur, String word, int depth) {
            if (cur == null) return false;
            if (depth == word.length()) return cur.isEnd;

            char c = word.charAt(depth);

            if (c == '.') {
                // wildcard: try ALL possible children
                for (TrieNode child : cur.children) {
                    if (searchHelper(child, word, depth + 1)) return true; // any match
                }
                return false;
            } else {
                // regular char: only one path
                return searchHelper(cur.children[c - 'a'], word, depth + 1);
            }
        }
    }

    // =========================================================================
    // LC 212 — WORD SEARCH II (Trie + Backtracking on Grid)
    // =========================================================================

    /**
     * Given a board and list of words, find all words present in the board.
     * Characters must be connected horizontally or vertically, no cell reused.
     *
     * NAIVE: run Word Search I (LC 79) for each word → O(words * 4^L * M*N)
     * OPTIMIZED with Trie:
     *   1. Insert all words into a Trie
     *   2. DFS from each cell, traverse the Trie simultaneously
     *   3. When we reach a Trie node with isEnd=true → found a word
     *   4. Prune: if no Trie child exists for current char → stop DFS branch
     *
     * KEY OPTIMIZATION: remove word from Trie once found (avoid duplicates + prune empty branches).
     *
     * Time: O(M*N * 4^L) where L = max word length (Trie pruning makes it much faster in practice)
     * Space: O(total chars in all words) for Trie
     */
    private static final int[][] DIRS = {{0,1},{0,-1},{1,0},{-1,0}};

    public List<String> findWords(char[][] board, String[] words) {
        // Step 1: build Trie from all words
        TrieNode trieRoot = new TrieNode();
        for (String word : words) {
            TrieNode cur = trieRoot;
            for (char c : word.toCharArray()) {
                int idx = c - 'a';
                if (cur.children[idx] == null) cur.children[idx] = new TrieNode();
                cur = cur.children[idx];
            }
            cur.isEnd = true;
        }

        int m = board.length, n = board[0].length;
        List<String> result = new ArrayList<>();

        // Step 2: DFS from every cell
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                dfsBoard(board, r, c, trieRoot, new StringBuilder(), result);
            }
        }
        return result;
    }

    private void dfsBoard(char[][] board, int r, int c, TrieNode node,
                          StringBuilder path, List<String> result) {
        if (r < 0 || r >= board.length || c < 0 || c >= board[0].length) return;
        char ch = board[r][c];
        if (ch == '#') return;                    // visited — avoid reuse
        int idx = ch - 'a';
        if (node.children[idx] == null) return;   // Trie prunes this branch

        TrieNode next = node.children[idx];
        path.append(ch);

        if (next.isEnd) {
            result.add(path.toString());          // found a word
            next.isEnd = false;                   // remove to avoid duplicates
        }

        board[r][c] = '#';                        // mark visited
        for (int[] d : DIRS) {
            dfsBoard(board, r + d[0], c + d[1], next, path, result);
        }
        board[r][c] = ch;                         // restore (backtrack)
        path.deleteCharAt(path.length() - 1);

        // Optimization: prune dead Trie nodes (no words under them anymore)
        if (isLeaf(next) && !next.isEnd) {
            node.children[idx] = null; // prune empty branch from Trie
        }
    }

    // Q: Why put words in a Trie instead of a HashSet for LC 212?
    // A: Trie enables EARLY PRUNING during DFS. If the current board path
    //    doesn't match any Trie prefix, we stop immediately. HashSet can only
    //    check complete words, so we'd need to complete every path first.
    //    For 10,000 words and a 12x12 board, Trie is orders of magnitude faster.

    public static void main(String[] args) {
        TrieDataStructure trie = new TrieDataStructure();

        // Basic operations
        trie.insert("apple");
        trie.insert("app");
        trie.insert("application");
        System.out.println(trie.search("apple"));       // true
        System.out.println(trie.search("app"));         // true
        System.out.println(trie.search("ap"));          // false (not inserted)
        System.out.println(trie.startsWith("ap"));      // true
        System.out.println(trie.startsWith("xyz"));     // false

        // Delete
        trie.delete("app");
        System.out.println(trie.search("app"));         // false (deleted)
        System.out.println(trie.search("apple"));       // true (still there)

        // WordDictionary with wildcard
        WordDictionary wd = new WordDictionary();
        wd.addWord("bad"); wd.addWord("dad"); wd.addWord("mad");
        System.out.println(wd.search("pad")); // false
        System.out.println(wd.search("bad")); // true
        System.out.println(wd.search(".ad")); // true (matches bad, dad, mad)
        System.out.println(wd.search("b..")); // true (matches bad)

        // Word Search II
        TrieDataStructure ts = new TrieDataStructure();
        char[][] board = {
            {'o','a','a','n'},
            {'e','t','a','e'},
            {'i','h','k','r'},
            {'i','f','l','v'}
        };
        System.out.println(ts.findWords(board, new String[]{"oath","pea","eat","rain"}));
        // [eat, oath]
    }
}
