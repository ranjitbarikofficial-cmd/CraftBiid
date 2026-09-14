package com.craftbid.dsa;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-performance, thread-safe Prefix Tree (Trie) data structure.
 *
 * Complexities:
 * - Insert: O(L) where L is the length of the word
 * - Search / Prefix Lookup: O(L)
 * - Prefix Autocomplete: O(L + K) where K is the number of results
 * - Fuzzy Search: O(Sigma * L) bounded by max edit distance
 *
 * @param <V> The type of values attached to words in the Trie
 */
public class Trie<V> {

    private final TrieNode<V> root = new TrieNode<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private int size = 0;

    public static class TrieNode<V> {
        final Map<Character, TrieNode<V>> children = new HashMap<>();
        boolean isEndOfWord = false;
        final Set<V> values = new HashSet<>();
        String word = null;
        int frequency = 0;
    }

    public static class SearchResult<V> {
        private final String word;
        private final Set<V> values;
        private final int score;

        public SearchResult(String word, Set<V> values, int score) {
            this.word = word;
            this.values = values;
            this.score = score;
        }

        public String getWord() {
            return word;
        }

        public Set<V> getValues() {
            return values;
        }

        public int getScore() {
            return score;
        }
    }

    /**
     * Insert a word and an associated value into the Trie.
     */
    public void insert(String word, V value) {
        if (word == null || word.isBlank()) return;

        String normalized = word.trim().toLowerCase();
        rwLock.writeLock().lock();
        try {
            TrieNode<V> current = root;
            for (int i = 0; i < normalized.length(); i++) {
                char ch = normalized.charAt(i);
                current = current.children.computeIfAbsent(ch, c -> new TrieNode<>());
            }
            if (!current.isEndOfWord) {
                current.isEndOfWord = true;
                current.word = normalized;
                size++;
            }
            current.frequency++;
            if (value != null) {
                current.values.add(value);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Search for all values matching an exact word.
     */
    public Set<V> searchExact(String word) {
        if (word == null || word.isBlank()) return Collections.emptySet();

        String normalized = word.trim().toLowerCase();
        rwLock.readLock().lock();
        try {
            TrieNode<V> node = getNode(normalized);
            if (node != null && node.isEndOfWord) {
                return new HashSet<>(node.values);
            }
            return Collections.emptySet();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Check if a prefix exists in the Trie.
     */
    public boolean startsWith(String prefix) {
        if (prefix == null || prefix.isBlank()) return false;

        String normalized = prefix.trim().toLowerCase();
        rwLock.readLock().lock();
        try {
            return getNode(normalized) != null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Autocomplete suggestions for a given prefix, ranked by frequency and word length.
     * Time Complexity: O(L + K)
     */
    public List<SearchResult<V>> searchPrefix(String prefix, int limit) {
        if (prefix == null || prefix.isBlank() || limit <= 0) return Collections.emptyList();

        String normalized = prefix.trim().toLowerCase();
        rwLock.readLock().lock();
        try {
            TrieNode<V> node = getNode(normalized);
            if (node == null) {
                return Collections.emptyList();
            }

            List<SearchResult<V>> results = new ArrayList<>();
            collectWords(node, results);

            // Sort by frequency (descending), then shorter word length (ascending)
            results.sort((a, b) -> {
                if (b.getScore() != a.getScore()) {
                    return Integer.compare(b.getScore(), a.getScore());
                }
                return Integer.compare(a.getWord().length(), b.getWord().length());
            });

            if (results.size() > limit) {
                return results.subList(0, limit);
            }
            return results;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Fuzzy search using Levenshtein distance dynamic programming branch exploration.
     * Supports typo tolerance (e.g. "potry" -> "pottery").
     *
     * @param target The search term
     * @param maxDistance Maximum allowed edit distance (typically 1 or 2)
     * @param limit Maximum results to return
     */
    public List<SearchResult<V>> searchFuzzy(String target, int maxDistance, int limit) {
        if (target == null || target.isBlank() || maxDistance < 0 || limit <= 0) {
            return Collections.emptyList();
        }

        String normalized = target.trim().toLowerCase();
        rwLock.readLock().lock();
        try {
            int len = normalized.length();
            int[] initialRow = new int[len + 1];
            for (int i = 0; i <= len; i++) {
                initialRow[i] = i;
            }

            List<SearchResult<V>> results = new ArrayList<>();
            for (Map.Entry<Character, TrieNode<V>> entry : root.children.entrySet()) {
                fuzzySearchRecursive(entry.getValue(), entry.getKey(), normalized, initialRow, results, maxDistance);
            }

            results.sort(Comparator.comparingInt(SearchResult::getScore));
            if (results.size() > limit) {
                return results.subList(0, limit);
            }
            return results;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void fuzzySearchRecursive(
            TrieNode<V> node,
            char ch,
            String target,
            int[] previousRow,
            List<SearchResult<V>> results,
            int maxDistance) {

        int cols = target.length() + 1;
        int[] currentRow = new int[cols];
        currentRow[0] = previousRow[0] + 1;

        int minRowValue = currentRow[0];

        for (int c = 1; c < cols; c++) {
            int insertCost = currentRow[c - 1] + 1;
            int deleteCost = previousRow[c] + 1;
            int replaceCost = (target.charAt(c - 1) == ch) ? previousRow[c - 1] : previousRow[c - 1] + 1;

            currentRow[c] = Math.min(insertCost, Math.min(deleteCost, replaceCost));
            minRowValue = Math.min(minRowValue, currentRow[c]);
        }

        if (currentRow[cols - 1] <= maxDistance && node.isEndOfWord) {
            results.add(new SearchResult<>(node.word, new HashSet<>(node.values), currentRow[cols - 1]));
        }

        // Branch pruning: only proceed if minimum edit distance on this row is <= maxDistance
        if (minRowValue <= maxDistance) {
            for (Map.Entry<Character, TrieNode<V>> child : node.children.entrySet()) {
                fuzzySearchRecursive(child.getValue(), child.getKey(), target, currentRow, results, maxDistance);
            }
        }
    }

    /**
     * Remove a word and its value from the Trie.
     */
    public boolean delete(String word, V value) {
        if (word == null || word.isBlank()) return false;

        String normalized = word.trim().toLowerCase();
        rwLock.writeLock().lock();
        try {
            boolean removed = deleteRecursive(root, normalized, 0, value);
            if (removed) {
                size = Math.max(0, size - 1);
            }
            return removed;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private boolean deleteRecursive(TrieNode<V> current, String word, int index, V value) {
        if (index == word.length()) {
            if (!current.isEndOfWord) return false;

            if (value != null) {
                current.values.remove(value);
            }
            if (current.values.isEmpty() || value == null) {
                current.isEndOfWord = false;
                current.word = null;
                current.values.clear();
                return true;
            }
            return false;
        }

        char ch = word.charAt(index);
        TrieNode<V> child = current.children.get(ch);
        if (child == null) return false;

        boolean shouldDeleteChild = deleteRecursive(child, word, index + 1, value)
                && !child.isEndOfWord
                && child.children.isEmpty();

        if (shouldDeleteChild) {
            current.children.remove(ch);
        }
        return true;
    }

    /**
     * Clear all nodes from the Trie.
     */
    public void clear() {
        rwLock.writeLock().lock();
        try {
            root.children.clear();
            size = 0;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public int size() {
        rwLock.readLock().lock();
        try {
            return size;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private TrieNode<V> getNode(String prefix) {
        TrieNode<V> current = root;
        for (int i = 0; i < prefix.length(); i++) {
            char ch = prefix.charAt(i);
            current = current.children.get(ch);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void collectWords(TrieNode<V> node, List<SearchResult<V>> results) {
        if (node.isEndOfWord && node.word != null) {
            results.add(new SearchResult<>(node.word, new HashSet<>(node.values), node.frequency));
        }
        for (TrieNode<V> child : node.children.values()) {
            collectWords(child, results);
        }
    }
}
