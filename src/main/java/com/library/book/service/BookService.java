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
import com.library.common.pagination.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;

    public BookService(
            BookRepository bookRepository,
            CategoryRepository categoryRepository,
            AuthorRepository authorRepository
    ) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        String cleanIsbn = request.isbn().replaceAll("[^0-9]", "");
        if (bookRepository.existsByIsbn(cleanIsbn)) {
            throw new ConflictException("Book with ISBN " + cleanIsbn + " already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        List<Author> authors = authorRepository.findAllById(request.authorIds());
        if (authors.isEmpty() || authors.size() != request.authorIds().size()) {
            throw new ResourceNotFoundException("One or more authors could not be found");
        }

        Book book = new Book();
        book.setIsbn(cleanIsbn);
        book.setTitle(request.title().trim());
        book.setDescription(request.description());
        book.setPublicationYear(request.publicationYear());
        book.setLanguage(request.language().trim());
        book.setCategory(category);
        book.setAuthors(authors);
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies());

        Book saved = bookRepository.save(book);
        return BookResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return BookResponse.from(book);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> searchBooks(
            String query,
            UUID authorId,
            UUID categoryId,
            boolean availableOnly,
            Pageable pageable
    ) {
        Page<Book> books = bookRepository.search(
                (query != null && !query.isBlank()) ? query.trim() : null,
                authorId,
                categoryId,
                availableOnly,
                pageable
        );
        return PageResponse.from(books.map(BookResponse::from));
    }

    @Transactional
    public BookResponse updateBook(UUID id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        String cleanIsbn = request.isbn().replaceAll("[^0-9]", "");
        if (!book.getIsbn().equals(cleanIsbn) && bookRepository.existsByIsbn(cleanIsbn)) {
            throw new ConflictException("Book with ISBN " + cleanIsbn + " already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        List<Author> authors = authorRepository.findAllById(request.authorIds());
        if (authors.size() != request.authorIds().size()) {
            throw new ResourceNotFoundException("One or more authors could not be found");
        }

        // Adjust available copies based on new total
        int activeLoansCount = book.getTotalCopies() - book.getAvailableCopies();
        if (request.totalCopies() < activeLoansCount) {
            throw new BusinessRuleException("Cannot reduce total copies below currently borrowed copies (" + activeLoansCount + ")");
        }
        int newAvailable = request.totalCopies() - activeLoansCount;

        book.setIsbn(cleanIsbn);
        book.setTitle(request.title().trim());
        book.setDescription(request.description());
        book.setPublicationYear(request.publicationYear());
        book.setLanguage(request.language().trim());
        book.setCategory(category);
        book.setAuthors(authors);
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(newAvailable);

        return BookResponse.from(book);
    }

    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        // If copies are borrowed, delete is prevented
        if (book.getAvailableCopies() < book.getTotalCopies()) {
            throw new BusinessRuleException("Cannot delete book while copies are currently on loan");
        }

        bookRepository.delete(book);
    }
}
