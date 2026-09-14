/**
 * Client-side Trie (Prefix Tree) data structure for sub-millisecond instant autocomplete.
 *
 * Time Complexities:
 * - insert(word, value): O(L)
 * - searchPrefix(prefix, limit): O(L + K)
 * - searchFuzzy(term, maxDist): O(Sigma * L)
 */
export interface TrieSearchResult<T> {
  word: string;
  values: T[];
  score: number;
}

class TrieNode<T> {
  children: Map<string, TrieNode<T>> = new Map();
  isEndOfWord = false;
  word?: string;
  values: Set<T> = new Set();
  frequency = 0;
}

export class Trie<T = any> {
  private root = new TrieNode<T>();
  private _size = 0;

  public get size(): number {
    return this._size;
  }

  public insert(word: string, value?: T): void {
    if (!word || !word.trim()) return;

    const normalized = word.trim().toLowerCase();
    let current = this.root;

    for (let i = 0; i < normalized.length; i++) {
      const ch = normalized[i];
      let child = current.children.get(ch);
      if (!child) {
        child = new TrieNode<T>();
        current.children.set(ch, child);
      }
      current = child;
    }

    if (!current.isEndOfWord) {
      current.isEndOfWord = true;
      current.word = normalized;
      this._size++;
    }
    current.frequency++;
    if (value !== undefined) {
      current.values.add(value);
    }
  }

  public startsWith(prefix: string): boolean {
    if (!prefix) return false;
    const node = this.getNode(prefix.trim().toLowerCase());
    return node !== null;
  }

  public searchPrefix(prefix: string, limit = 8): TrieSearchResult<T>[] {
    if (!prefix || !prefix.trim() || limit <= 0) return [];

    const normalized = prefix.trim().toLowerCase();
    const node = this.getNode(normalized);
    if (!node) return [];

    const results: TrieSearchResult<T>[] = [];
    this.collectWords(node, results);

    results.sort((a, b) => {
      if (b.score !== a.score) {
        return b.score - a.score;
      }
      return a.word.length - b.word.length;
    });

    return results.slice(0, limit);
  }

  public searchFuzzy(target: string, maxDistance = 1, limit = 5): TrieSearchResult<T>[] {
    if (!target || !target.trim()) return [];

    const normalized = target.trim().toLowerCase();
    const len = normalized.length;
    const initialRow: number[] = Array.from({ length: len + 1 }, (_, i) => i);

    const results: TrieSearchResult<T>[] = [];
    for (const [ch, child] of this.root.children.entries()) {
      this.fuzzyRecursive(child, ch, normalized, initialRow, results, maxDistance);
    }

    results.sort((a, b) => a.score - b.score);
    return results.slice(0, limit);
  }

  private fuzzyRecursive(
    node: TrieNode<T>,
    ch: string,
    target: string,
    previousRow: number[],
    results: TrieSearchResult<T>[],
    maxDistance: number
  ): void {
    const cols = target.length + 1;
    const currentRow: number[] = new Array(cols);
    currentRow[0] = previousRow[0] + 1;

    let minRowValue = currentRow[0];

    for (let c = 1; c < cols; c++) {
      const insertCost = currentRow[c - 1] + 1;
      const deleteCost = previousRow[c] + 1;
      const replaceCost = target[c - 1] === ch ? previousRow[c - 1] : previousRow[c - 1] + 1;

      currentRow[c] = Math.min(insertCost, deleteCost, replaceCost);
      minRowValue = Math.min(minRowValue, currentRow[c]);
    }

    if (currentRow[cols - 1] <= maxDistance && node.isEndOfWord && node.word) {
      results.push({
        word: node.word,
        values: Array.from(node.values),
        score: currentRow[cols - 1],
      });
    }

    if (minRowValue <= maxDistance) {
      for (const [childChar, child] of node.children.entries()) {
        this.fuzzyRecursive(child, childChar, target, currentRow, results, maxDistance);
      }
    }
  }

  public clear(): void {
    this.root = new TrieNode<T>();
    this._size = 0;
  }

  private getNode(prefix: string): TrieNode<T> | null {
    let current = this.root;
    for (let i = 0; i < prefix.length; i++) {
      const ch = prefix[i];
      const child = current.children.get(ch);
      if (!child) return null;
      current = child;
    }
    return current;
  }

  private collectWords(node: TrieNode<T>, results: TrieSearchResult<T>[]): void {
    if (node.isEndOfWord && node.word) {
      results.push({
        word: node.word,
        values: Array.from(node.values),
        score: node.frequency,
      });
    }
    for (const child of node.children.values()) {
      this.collectWords(child, results);
    }
  }
}
