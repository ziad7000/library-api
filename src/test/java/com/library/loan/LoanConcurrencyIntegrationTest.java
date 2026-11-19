package com.library.loan;

import com.library.BaseIntegrationTest;
import com.library.book.model.Author;
import com.library.book.model.Book;
import com.library.book.model.Category;
import com.library.book.repository.AuthorRepository;
import com.library.book.repository.BookRepository;
import com.library.book.repository.CategoryRepository;
import com.library.common.exception.ConflictException;
import com.library.loan.dto.BorrowRequest;
import com.library.loan.dto.LoanResponse;
import com.library.loan.model.LoanStatus;
import com.library.loan.repository.LoanRepository;
import com.library.loan.service.LoanService;
import com.library.user.model.Role;
import com.library.user.model.User;
import com.library.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class LoanConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    @DisplayName("Concurrency Proof: Exactly one member succeeds when concurrently borrowing the last available copy")
    void whenTwoMembersBorrowLastCopy_exactlyOneSucceeds() throws InterruptedException {
        // 1. Arrange: Create Category, Author, and Book with exactly 1 available copy
        Category category = categoryRepository.save(new Category(UUID.randomUUID(), "Computer Science"));
        Author author = authorRepository.save(new Author(UUID.randomUUID(), "Robert C. Martin"));

        Book book = new Book(UUID.randomUUID(), "9780132350884", "Clean Code", "A Handbook of Agile Software Craftsmanship",
                2008, "English", category, 1, 1);
        book.setAuthors(List.of(author));
        Book savedBook = bookRepository.save(book);

        // Create two distinct members
        User member1 = userRepository.save(new User(UUID.randomUUID(), "member1@library.com", "hash1", "Member One", Role.MEMBER, true));
        User member2 = userRepository.save(new User(UUID.randomUUID(), "member2@library.com", "hash2", "Member Two", Role.MEMBER, true));

        // 2. Act: Prepare concurrent borrow execution
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Boolean> task1 = () -> {
            readyLatch.countDown();
            startLatch.await(); // wait for simultaneous trigger
            try {
                loanService.borrowBook(member1.getId(), new BorrowRequest(savedBook.getId()));
                return true;
            } catch (ConflictException e) {
                return false;
            }
        };

        Callable<Boolean> task2 = () -> {
            readyLatch.countDown();
            startLatch.await(); // wait for simultaneous trigger
            try {
                loanService.borrowBook(member2.getId(), new BorrowRequest(savedBook.getId()));
                return true;
            } catch (ConflictException e) {
                return false;
            }
        };

        Future<Boolean> future1 = executor.submit(task1);
        Future<Boolean> future2 = executor.submit(task2);

        readyLatch.await(); // ensure both threads are waiting at startLatch
        startLatch.countDown(); // fire both requests concurrently

        boolean result1 = false;
        boolean result2 = false;
        try {
            result1 = future1.get();
            result2 = future2.get();
        } catch (ExecutionException e) {
            // Execution exception wrapper
        } finally {
            executor.shutdown();
        }

        // 3. Assert: Exactly one operation succeeded, and one failed with ConflictException
        assertThat(result1 ^ result2)
                .as("Exactly one member must succeed and the other must receive ConflictException")
                .isTrue();

        // Verify database state: available copies must be exactly 0, never negative
        Book updatedBook = bookRepository.findById(savedBook.getId()).orElseThrow();
        assertThat(updatedBook.getAvailableCopies())
                .as("Available copies must be exactly 0 after borrowing the last copy")
                .isZero();

        // Verify exactly one active loan exists
        long activeLoans = loanRepository.countByUserIdAndStatus(member1.getId(), LoanStatus.ACTIVE)
                + loanRepository.countByUserIdAndStatus(member2.getId(), LoanStatus.ACTIVE);
        assertThat(activeLoans)
                .as("Total active loans created must be exactly 1")
                .isEqualTo(1);
    }
}
