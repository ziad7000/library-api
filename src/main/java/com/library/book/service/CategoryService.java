package com.library.book.service;

import com.library.book.dto.CategoryRequest;
import com.library.book.dto.CategoryResponse;
import com.library.book.model.Category;
import com.library.book.repository.CategoryRepository;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.name().trim();
        if (categoryRepository.existsByName(trimmedName)) {
            throw new ConflictException("Category already exists: " + trimmedName);
        }
        Category category = new Category();
        category.setName(trimmedName);
        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
