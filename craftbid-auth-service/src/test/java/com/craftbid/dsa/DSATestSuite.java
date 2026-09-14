package com.craftbid.dsa;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class DSATestSuite {

    // ==========================================
    // 1. TRIE TESTS
    // ==========================================

    @Test
    public void testTriePrefixAndAutocomplete() {
        Trie<String> trie = new Trie<>();

        trie.insert("Pottery", "craft-1");
        trie.insert("Pottery Bowl", "craft-2");
        trie.insert("Pots & Clay", "craft-3");
        trie.insert("Painting", "craft-4");
        trie.insert("Woodcraft", "craft-5");

        assertTrue(trie.startsWith("pot"));
        assertTrue(trie.startsWith("wood"));
        assertFalse(trie.startsWith("glass"));

        // Exact match
        Set<String> potteryResults = trie.searchExact("pottery");
        assertEquals(1, potteryResults.size());
        assertTrue(potteryResults.contains("craft-1"));

        // Prefix autocomplete
        List<Trie.SearchResult<String>> potMatches = trie.searchPrefix("pot", 10);
        assertEquals(3, potMatches.size());

        // Fuzzy search with typo tolerance (edit distance <= 2)
        List<Trie.SearchResult<String>> fuzzy = trie.searchFuzzy("potry", 2, 5);
        assertFalse(fuzzy.isEmpty());
        assertEquals("pottery", fuzzy.get(0).getWord());

        // Delete test
        trie.delete("pottery", "craft-1");
        assertFalse(trie.searchExact("pottery").contains("craft-1"));
    }

    // ==========================================
    // 2. LRU CACHE TESTS
    // ==========================================

    @Test
    public void testLRUCacheCapacityAndEviction() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);

        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);

        assertEquals(3, cache.size());
        assertEquals(1, cache.get("A")); // A becomes most recently used

        // Insert D -> should evict B (least recently used)
        cache.put("D", 4);

        assertEquals(3, cache.size());
        assertNull(cache.get("B")); // Evicted
        assertEquals(1, cache.get("A"));
        assertEquals(3, cache.get("C"));
        assertEquals(4, cache.get("D"));
    }

    @Test
    public void testLRUCacheTTLExpiration() throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(10, 50); // 50ms TTL

        cache.put("key1", "val1");
        assertEquals("val1", cache.get("key1"));

        Thread.sleep(70);
        assertNull(cache.get("key1")); // Expired
    }

    @Test
    public void testLRUCacheConcurrentAccess() throws InterruptedException {
        int threads = 8;
        int operationsPerThread = 500;
        LRUCache<Integer, String> cache = new LRUCache<>(50);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        int key = (threadId * 1000) + (i % 100);
                        cache.put(key, "Val-" + key);
                        cache.get(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(cache.size() <= 50);
    }

    // ==========================================
    // 3. LIVE AUCTION HEAP TESTS
    // ==========================================

    @Test
    public void testLiveAuctionHeapMaxBidAndLeaderboard() {
        LiveAuctionHeap heap = new LiveAuctionHeap(101L);

        heap.recordBid(1L, "Alice", "Bhubaneswar", BigDecimal.valueOf(500), LocalDateTime.now());
        heap.recordBid(2L, "Bob", "Cuttack", BigDecimal.valueOf(650), LocalDateTime.now());
        heap.recordBid(3L, "Charlie", "Puri", BigDecimal.valueOf(600), LocalDateTime.now());

        // Peek highest bid -> O(1)
        LiveAuctionHeap.BidNode highest = heap.peekHighestBid();
        assertNotNull(highest);
        assertEquals(2L, highest.getBidderId());
        assertEquals(BigDecimal.valueOf(650), highest.getAmount());

        // Alice updates her bid with higher differential
        heap.recordBid(1L, "Alice", "Bhubaneswar", BigDecimal.valueOf(800), LocalDateTime.now());

        highest = heap.peekHighestBid();
        assertEquals(1L, highest.getBidderId());
        assertEquals(BigDecimal.valueOf(800), highest.getAmount());

        // Top-2 Leaderboard
        List<LiveAuctionHeap.BidNode> top2 = heap.getTopBidders(2);
        assertEquals(2, top2.size());
        assertEquals(1L, top2.get(0).getBidderId()); // 800
        assertEquals(2L, top2.get(1).getBidderId()); // 650
        assertEquals("Alice • Bhubaneswar", top2.get(0).getMaskedIdentity());
    }

    // ==========================================
    // 4. CIRCULAR RING BUFFER TESTS
    // ==========================================

    @Test
    public void testCircularRingBufferWrappingAndSnapshot() {
        CircularRingBuffer<String> ring = new CircularRingBuffer<>(3);

        ring.push("Event-1");
        ring.push("Event-2");
        ring.push("Event-3");

        assertEquals(3, ring.size());
        assertTrue(ring.isFull());

        List<String> snapshot = ring.getSnapshot();
        assertEquals(List.of("Event-1", "Event-2", "Event-3"), snapshot);

        // Push 4th -> overwrites Event-1
        ring.push("Event-4");
        assertEquals(3, ring.size());
        assertEquals(List.of("Event-2", "Event-3", "Event-4"), ring.getSnapshot());

        // Reverse latest query
        assertEquals(List.of("Event-4", "Event-3"), ring.getLatest(2));
        assertEquals("Event-4", ring.peekLatest());
    }

    // ==========================================
    // 5. ARRAY ALGORITHMS TESTS (QUICKSELECT & BINARY SEARCH)
    // ==========================================

    @Test
    public void testQuickSelectMedian() {
        List<BigDecimal> prices = new ArrayList<>(List.of(
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(300),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(100)
        ));

        BigDecimal median = ArrayAlgorithms.findMedianPrice(prices);
        assertEquals(BigDecimal.valueOf(150), median);
    }

    @Test
    public void testBinarySearchBounds() {
        List<Integer> sorted = List.of(100, 200, 200, 300, 400, 500);

        int lower = ArrayAlgorithms.lowerBound(sorted, 200, Integer::compareTo);
        int upper = ArrayAlgorithms.upperBound(sorted, 200, Integer::compareTo);

        assertEquals(1, lower); // First index >= 200
        assertEquals(3, upper); // First index > 200
    }
}
