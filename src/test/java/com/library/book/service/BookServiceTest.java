package com.library.book.service;

import com.library.book.dto.BookRequest;
import com.library.book.dto.BookResponse;
import com.library.book.model.Author;
import com.library.book.model.Book;
import com.library.book.model.Category;
import com.library.book.repository.AuthorRepository;
import com.library.book.repository.BookRepository;
import com.library.book.repository.CategoryRepository;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("Successfully create a new book with available copies equal to total copies")
    void shouldCreateBook() {
        UUID categoryId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        BookRequest request = new BookRequest(
                "9780134685991", "Effective Java", "Great book", 2018, "English",
                categoryId, List.of(authorId), 5
        );

        Category category = new Category(categoryId, "Software Engineering");
        Author author = new Author(authorId, "Joshua Bloch");

        when(bookRepository.existsByIsbn("9780134685991")).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(authorRepository.findAllById(List.of(authorId))).thenReturn(List.of(author));

        Book savedBook = new Book(UUID.randomUUID(), "9780134685991", "Effective Java", "Great book",
                2018, "English", category, 5, 5);
        savedBook.setAuthors(List.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        BookResponse response = bookService.createBook(request);

        assertThat(response).isNotNull();
        assertThat(response.isbn()).isEqualTo("9780134685991");
        assertThat(response.availableCopies()).isEqualTo(5);
        assertThat(response.totalCopies()).isEqualTo(5);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("Throws ConflictException when ISBN already exists")
    void shouldThrowWhenIsbnExists() {
        UUID catId = UUID.randomUUID();
        BookRequest request = new BookRequest(
                "9780134685991", "Duplicate Book", "Desc", 2020, "English",
                catId, List.of(UUID.randomUUID()), 3
        );
        when(bookRepository.existsByIsbn("9780134685991")).thenReturn(true);

        assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Throws BusinessRuleException when attempting to delete a book with copies on loan")
    void shouldPreventDeletingBookWithActiveLoans() {
        UUID bookId = UUID.randomUUID();
        Book book = new Book(bookId, "9780134685991", "Borrowed Book", "Desc",
                2020, "English", new Category(UUID.randomUUID(), "Tech"), 5, 3); // 2 copies borrowed

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.deleteBook(bookId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("on loan");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when book does not exist")
    void shouldThrowWhenBookNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(bookRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
