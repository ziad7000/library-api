package com.library.book.service;

import com.library.book.dto.CategoryRequest;
import com.library.book.dto.CategoryResponse;
import com.library.book.model.Category;
import com.library.book.repository.CategoryRepository;
import com.library.common.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Successfully creates new category")
    void shouldCreateCategory() {
        CategoryRequest request = new CategoryRequest("Science Fiction");
        when(categoryRepository.existsByName("Science Fiction")).thenReturn(false);

        Category saved = new Category(UUID.randomUUID(), "Science Fiction");
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.name()).isEqualTo("Science Fiction");
    }

    @Test
    @DisplayName("Throws ConflictException on duplicate category name")
    void shouldThrowWhenCategoryExists() {
        CategoryRequest request = new CategoryRequest("History");
        when(categoryRepository.existsByName("History")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ConflictException.class);
    }
}
