package com.craftbid.dsa;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * High-performance array algorithms:
 * - QuickSelect: O(N) expected time selection for median / percentile calculation
 * - BinarySearch Range Finder: O(log N) for lower/upper bound price partitioning
 */
public class ArrayAlgorithms {

    private static final Random RANDOM = new Random();

    /**
     * QuickSelect algorithm to find the k-th smallest element in expected O(N) time.
     *
     * @param list Mutable list of items
     * @param k 0-indexed rank (e.g. size/2 for median)
     * @param comparator Item comparator
     * @param <T> Item type
     * @return The k-th smallest item
     */
    public static <T> T quickSelect(List<T> list, int k, Comparator<T> comparator) {
        if (list == null || list.isEmpty() || k < 0 || k >= list.size()) {
            throw new IllegalArgumentException("Invalid arguments for quickSelect");
        }
        return quickSelectRecursive(list, 0, list.size() - 1, k, comparator);
    }

    private static <T> T quickSelectRecursive(List<T> list, int left, int right, int k, Comparator<T> comparator) {
        if (left == right) {
            return list.get(left);
        }

        int pivotIndex = left + RANDOM.nextInt(right - left + 1);
        pivotIndex = partition(list, left, right, pivotIndex, comparator);

        if (k == pivotIndex) {
            return list.get(k);
        } else if (k < pivotIndex) {
            return quickSelectRecursive(list, left, pivotIndex - 1, k, comparator);
        } else {
            return quickSelectRecursive(list, pivotIndex + 1, right, k, comparator);
        }
    }

    private static <T> int partition(List<T> list, int left, int right, int pivotIndex, Comparator<T> comparator) {
        T pivotValue = list.get(pivotIndex);
        swap(list, pivotIndex, right);
        int storeIndex = left;

        for (int i = left; i < right; i++) {
            if (comparator.compare(list.get(i), pivotValue) < 0) {
                swap(list, storeIndex, i);
                storeIndex++;
            }
        }
        swap(list, right, storeIndex);
        return storeIndex;
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    /**
     * Calculate median value of a numerical list in O(N) expected time.
     */
    public static BigDecimal findMedianPrice(List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int n = prices.size();
        if (n % 2 != 0) {
            return quickSelect(prices, n / 2, Comparator.naturalOrder());
        } else {
            BigDecimal mid1 = quickSelect(prices, n / 2 - 1, Comparator.naturalOrder());
            BigDecimal mid2 = quickSelect(prices, n / 2, Comparator.naturalOrder());
            return mid1.add(mid2).divide(BigDecimal.valueOf(2));
        }
    }

    /**
     * Binary Search: Lower bound (first index with value >= target).
     * Time Complexity: O(log N) on sorted array.
     */
    public static <T> int lowerBound(List<T> sortedList, T target, Comparator<T> comparator) {
        int low = 0;
        int high = sortedList.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (comparator.compare(sortedList.get(mid), target) >= 0) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    /**
     * Binary Search: Upper bound (first index with value > target).
     * Time Complexity: O(log N) on sorted array.
     */
    public static <T> int upperBound(List<T> sortedList, T target, Comparator<T> comparator) {
        int low = 0;
        int high = sortedList.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (comparator.compare(sortedList.get(mid), target) > 0) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }
}
