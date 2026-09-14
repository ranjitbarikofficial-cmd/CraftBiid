/**
 * Client-side Least Recently Used (LRU) Cache in TypeScript.
 *
 * Complexities:
 * - get(key): O(1)
 * - put(key, value, ttlMs): O(1)
 * - delete(key): O(1)
 */
class LruNode<K, V> {
  key?: K;
  value?: V;
  expiresAt?: number;
  prev?: LruNode<K, V>;
  next?: LruNode<K, V>;

  constructor(key?: K, value?: V, expiresAt?: number) {
    this.key = key;
    this.value = value;
    this.expiresAt = expiresAt;
  }

  isExpired(): boolean {
    return !!this.expiresAt && Date.now() > this.expiresAt;
  }
}

export class LRUCache<K = any, V = any> {
  private map: Map<K, LruNode<K, V>> = new Map();
  private head: LruNode<K, V>;
  private tail: LruNode<K, V>;

  constructor(
    public readonly capacity: number = 50,
    private readonly defaultTtlMs: number = 0
  ) {
    if (capacity <= 0) {
      throw new Error('LRUCache capacity must be greater than 0');
    }
    this.head = new LruNode<K, V>();
    this.tail = new LruNode<K, V>();
    this.head.next = this.tail;
    this.tail.prev = this.head;
  }

  public get(key: K): V | null {
    const node = this.map.get(key);
    if (!node) return null;

    if (node.isExpired()) {
      this.removeNode(node);
      this.map.delete(key);
      return null;
    }

    this.moveToHead(node);
    return node.value ?? null;
  }

  public put(key: K, value: V, ttlMs: number = this.defaultTtlMs): void {
    const expiresAt = ttlMs > 0 ? Date.now() + ttlMs : 0;
    const existing = this.map.get(key);

    if (existing) {
      existing.value = value;
      existing.expiresAt = expiresAt;
      this.moveToHead(existing);
      return;
    }

    if (this.map.size >= this.capacity) {
      this.evictLRU();
    }

    const newNode = new LruNode<K, V>(key, value, expiresAt);
    this.addToHead(newNode);
    this.map.set(key, newNode);
  }

  public delete(key: K): boolean {
    const node = this.map.get(key);
    if (!node) return false;
    this.removeNode(node);
    this.map.delete(key);
    return true;
  }

  public clear(): void {
    this.map.clear();
    this.head.next = this.tail;
    this.tail.prev = this.head;
  }

  public get size(): number {
    return this.map.size;
  }

  private addToHead(node: LruNode<K, V>): void {
    node.prev = this.head;
    node.next = this.head.next;
    if (this.head.next) {
      this.head.next.prev = node;
    }
    this.head.next = node;
  }

  private removeNode(node: LruNode<K, V>): void {
    if (node.prev) {
      node.prev.next = node.next;
    }
    if (node.next) {
      node.next.prev = node.prev;
    }
  }

  private moveToHead(node: LruNode<K, V>): void {
    this.removeNode(node);
    this.addToHead(node);
  }

  private evictLRU(): void {
    const lru = this.tail.prev;
    if (lru && lru !== this.head && lru.key !== undefined) {
      this.removeNode(lru);
      this.map.delete(lru.key);
    }
  }
}
