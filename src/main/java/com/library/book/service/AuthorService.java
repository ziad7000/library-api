package com.library.book.service;

import com.library.book.dto.AuthorRequest;
import com.library.book.dto.AuthorResponse;
import com.library.book.model.Author;
import com.library.book.repository.AuthorRepository;
import com.library.common.exception.ResourceNotFoundException;
import com.library.common.pagination.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorRequest request) {
        Author author = new Author();
        author.setName(request.name().trim());
        Author saved = authorRepository.save(author);
        return AuthorResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AuthorResponse getAuthorById(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));
        return AuthorResponse.from(author);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuthorResponse> getAllAuthors(String query, Pageable pageable) {
        var page = (query != null && !query.isBlank())
                ? authorRepository.findByNameContainingIgnoreCase(query.trim(), pageable)
                : authorRepository.findAll(pageable);
        return PageResponse.from(page.map(AuthorResponse::from));
    }
}
