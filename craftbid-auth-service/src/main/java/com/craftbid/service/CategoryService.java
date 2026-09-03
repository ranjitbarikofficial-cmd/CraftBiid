package com.craftbid.service;

import com.craftbid.entity.Category;
import com.craftbid.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

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
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found with id: " + id)
                );
    }

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category category) {
        Category existing = getCategoryById(id);
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setImageUrl(category.getImageUrl());
        return categoryRepository.save(existing);
    }

    public void deleteCategory(Long id) {
        Category existing = getCategoryById(id);
        categoryRepository.delete(existing);
    }
}