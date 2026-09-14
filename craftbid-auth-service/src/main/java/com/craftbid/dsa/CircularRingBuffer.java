package com.craftbid.dsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-performance, thread-safe Circular Ring Buffer with fixed capacity.
 *
 * Complexities:
 * - push(item) -> O(1)
 * - getSnapshot() -> O(K) where K is current size (K <= capacity)
 * - getLatest(k) -> O(min(k, K))
 *
 * Pre-allocates a fixed array buffer, eliminating dynamic array resizing, heap allocations,
 * and Garbage Collection pauses during high-frequency live auction event streams.
 *
 * @param <T> Element type
 */
public class CircularRingBuffer<T> {

    private final Object[] buffer;
    private final int capacity;
    private int head = 0; // Points to oldest element
    private int tail = 0; // Points to next write slot
    private int size = 0;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public CircularRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * Push a new item into the ring buffer.
     * If the buffer is full, the oldest element is automatically overwritten.
     * Time Complexity: O(1)
     */
    public void push(T item) {
        if (item == null) return;

        rwLock.writeLock().lock();
        try {
            buffer[tail] = item;
            tail = (tail + 1) % capacity;

            if (size < capacity) {
                size++;
            } else {
                // Buffer was already full, advance head to drop oldest item
                head = (head + 1) % capacity;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Retrieve all current elements in chronological order (oldest to newest).
     * Time Complexity: O(K)
     */
    @SuppressWarnings("unchecked")
    public List<T> getSnapshot() {
        rwLock.readLock().lock();
        try {
            if (size == 0) return Collections.emptyList();

            List<T> snapshot = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int index = (head + i) % capacity;
                snapshot.add((T) buffer[index]);
            }
            return snapshot;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Retrieve the most recent K elements in reverse chronological order (newest first).
     * Time Complexity: O(min(k, K))
     */
    @SuppressWarnings("unchecked")
    public List<T> getLatest(int k) {
        if (k <= 0) return Collections.emptyList();

        rwLock.readLock().lock();
        try {
            if (size == 0) return Collections.emptyList();

            int count = Math.min(k, size);
            List<T> result = new ArrayList<>(count);

            // Start from newest: tail - 1
            for (int i = 0; i < count; i++) {
                int index = (tail - 1 - i + capacity) % capacity;
                result.add((T) buffer[index]);
            }
            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Peek at the most recently added item without removing.
     * Time Complexity: O(1)
     */
    @SuppressWarnings("unchecked")
    public T peekLatest() {
        rwLock.readLock().lock();
        try {
            if (size == 0) return null;
            int lastIndex = (tail - 1 + capacity) % capacity;
            return (T) buffer[lastIndex];
        } finally {
            rwLock.readLock().unlock();
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

    public int capacity() {
        return capacity;
    }

    public boolean isFull() {
        rwLock.readLock().lock();
        try {
            return size == capacity;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            for (int i = 0; i < capacity; i++) {
                buffer[i] = null;
            }
            head = 0;
            tail = 0;
            size = 0;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
