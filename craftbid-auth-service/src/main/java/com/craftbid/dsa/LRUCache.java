package com.craftbid.dsa;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Custom high-throughput Least Recently Used (LRU) Cache combining a Doubly Linked List
 * and a Hash Map with optional Time-To-Live (TTL) expiration.
 *
 * Complexities:
 * - get(key): O(1)
 * - put(key, value): O(1)
 * - remove(key): O(1)
 * - Eviction of LRU tail: O(1)
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class LRUCache<K, V> {

    private final int maxCapacity;
    private final long defaultTtlMillis;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head;
    private final Node<K, V> tail;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public static class Node<K, V> {
        K key;
        V value;
        long expiresAt;
        Node<K, V> prev;
        Node<K, V> next;

        Node() {}

        Node(K key, V value, long expiresAt) {
            this.key = key;
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
        }
    }

    public LRUCache(int maxCapacity) {
        this(maxCapacity, 0); // 0 = no expiration
    }

    public LRUCache(int maxCapacity, long defaultTtlMillis) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.maxCapacity = maxCapacity;
        this.defaultTtlMillis = defaultTtlMillis;
        this.map = new ConcurrentHashMap<>(maxCapacity);

        // Dummy sentinel nodes
        this.head = new Node<>();
        this.tail = new Node<>();
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    /**
     * Get a value from the cache. Moves accessed element to the head (most recently used).
     * Time Complexity: O(1)
     */
    public V get(K key) {
        if (key == null) return null;

        lock.writeLock().lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                return null;
            }

            if (node.isExpired()) {
                removeNode(node);
                map.remove(key);
                return null;
            }

            moveToHead(node);
            return node.value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Put a value into the cache with default TTL.
     * Time Complexity: O(1)
     */
    public void put(K key, V value) {
        put(key, value, defaultTtlMillis);
    }

    /**
     * Put a value into the cache with explicit TTL millis.
     * Time Complexity: O(1)
     */
    public void put(K key, V value, long ttlMillis) {
        if (key == null || value == null) return;

        long expiresAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0;

        lock.writeLock().lock();
        try {
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                existing.expiresAt = expiresAt;
                moveToHead(existing);
                return;
            }

            if (map.size() >= maxCapacity) {
                evictLRU();
            }

            Node<K, V> newNode = new Node<>(key, value, expiresAt);
            addToHead(newNode);
            map.put(key, newNode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove an item from the cache.
     * Time Complexity: O(1)
     */
    public V remove(K key) {
        if (key == null) return null;

        lock.writeLock().lock();
        try {
            Node<K, V> node = map.remove(key);
            if (node != null) {
                removeNode(node);
                return node.value;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if key is present and not expired.
     */
    public boolean containsKey(K key) {
        if (key == null) return false;

        lock.readLock().lock();
        try {
            Node<K, V> node = map.get(key);
            return node != null && !node.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all non-expired cached values.
     */
    public List<V> values() {
        lock.readLock().lock();
        try {
            List<V> list = new ArrayList<>();
            Node<K, V> current = head.next;
            while (current != tail) {
                if (!current.isExpired()) {
                    list.add(current.value);
                }
                current = current.next;
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clear all cache entries.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
            head.next = tail;
            tail.prev = head;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return map.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    // ==========================================
    // INTERNAL DOUBLY LINKED LIST PRIMITIVES
    // ==========================================

    private void addToHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }

    private void evictLRU() {
        Node<K, V> lru = tail.prev;
        if (lru != head) {
            removeNode(lru);
            map.remove(lru.key);
        }
    }
}
