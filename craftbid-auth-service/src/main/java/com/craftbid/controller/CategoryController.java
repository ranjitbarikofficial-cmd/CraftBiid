package com.craftbid.controller;

import com.craftbid.entity.Category;
import com.craftbid.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:4200")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // GET all categories
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(
                categoryService.getAllCategories()
        );
    }

    // GET category by ID
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(
            @PathVariable @Positive(message = "Category ID must be positive") Long id) {

        return ResponseEntity.ok(
                categoryService.getCategoryById(id)
        );
    }

    // CREATE category
    @PostMapping
    public ResponseEntity<Category> createCategory(
            @Valid @RequestBody Category category) {

        return ResponseEntity.ok(
                categoryService.createCategory(category)
        );
    }

    // UPDATE category
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable @Positive(message = "Category ID must be positive") Long id,
            @Valid @RequestBody Category category) {

        return ResponseEntity.ok(
                categoryService.updateCategory(id, category)
        );
    }

    // DELETE category
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(
            @PathVariable @Positive(message = "Category ID must be positive") Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
                "Category deleted successfully"
        );
    }
}