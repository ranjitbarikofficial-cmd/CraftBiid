package com.craftbid.service;

import com.craftbid.dsa.LRUCache;
import com.craftbid.dsa.Trie;
import com.craftbid.entity.Category;
import com.craftbid.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    
    // DSA: In-Memory LRU Cache with 5-minute TTL for O(1) lookups
    private final LRUCache<Long, Category> categoryLruCache = new LRUCache<>(100, 300_000);
    
    // DSA: Prefix Trie for O(L) instantaneous category autocompletion
    private final Trie<Category> categoryTrie = new Trie<>();

    private volatile List<Category> cachedCategoriesList = null;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @PostConstruct
    public void initDefaultCategories() {
        if (categoryRepository.count() == 0) {
            List<String[]> defaultCategories = List.of(
                    new String[]{"Pottery & Ceramics", "Handcrafted clay pots, ceramic vases, stoneware bowls, and sculpted art"},
                    new String[]{"Woodworking", "Hand-carved wooden decor, custom furniture, utensils, and ornamental pieces"},
                    new String[]{"Handmade Jewelry", "Artisanal necklaces, gemstone rings, handcrafted bracelets, and beadwork"},
                    new String[]{"Paintings & Canvas", "Oil paintings, watercolors, acrylic artwork, and traditional folk paintings"},
                    new String[]{"Textiles & Weaving", "Handwoven rugs, tapestries, embroidered shawls, and organic cotton textiles"},
                    new String[]{"Leather Goods", "Handmade leather wallets, bags, belts, and bespoke leather craft"},
                    new String[]{"Glass Art", "Stained glass, blown glass ornaments, and sculpted glassware"},
                    new String[]{"Metalcraft & Sculptures", "Forged iron, brass decor, copper crafts, and handmade sculptures"}
            );

            for (String[] cat : defaultCategories) {
                Category category = new Category();
                category.setName(cat[0]);
                category.setDescription(cat[1]);
                categoryRepository.save(category);
            }
        }
        rebuildCacheAndTrie();
    }

    private synchronized void rebuildCacheAndTrie() {
        List<Category> all = categoryRepository.findAll();
        cachedCategoriesList = all;
        categoryLruCache.clear();
        categoryTrie.clear();

        for (Category c : all) {
            categoryLruCache.put(c.getId(), c);
            categoryTrie.insert(c.getName(), c);
            if (c.getName().contains(" ")) {
                for (String part : c.getName().split(" ")) {
                    if (part.length() > 2) {
                        categoryTrie.insert(part, c);
                    }
                }
            }
        }
    }

    public List<Category> getAllCategories() {
        if (cachedCategoriesList != null) {
            return cachedCategoriesList;
        }
        rebuildCacheAndTrie();
        return cachedCategoriesList;
    }

    /**
     * O(1) Category lookup via LRUCache
     */
    public Category getCategoryById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }
        Category cached = categoryLruCache.get(id);
        if (cached != null) {
            return cached;
        }

        Category fromDb = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        categoryLruCache.put(id, fromDb);
        return fromDb;
    }

    /**
     * O(L) Prefix Search across Category names
     */
    public List<Trie.SearchResult<Category>> searchCategories(String prefix, int limit) {
        return categoryTrie.searchPrefix(prefix, limit);
    }

    public Category createCategory(Category category) {
        Category saved = categoryRepository.save(category);
        rebuildCacheAndTrie();
        return saved;
    }

    public Category updateCategory(Long id, Category category) {
        Category existing = getCategoryById(id);
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setImageUrl(category.getImageUrl());
        Category saved = categoryRepository.save(existing);
        rebuildCacheAndTrie();
        return saved;
    }

    public void deleteCategory(Long id) {
        Category existing = getCategoryById(id);
        categoryRepository.delete(existing);
        rebuildCacheAndTrie();
    }
}