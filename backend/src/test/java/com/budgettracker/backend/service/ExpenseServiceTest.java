package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.ExpenseRepository;
import com.budgettracker.backend.repository.IncomeRepository;
import com.budgettracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService")
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private UserRepository userRepository;

    private TransactionService transactionService;
    private ExpenseService expenseService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(expenseRepository, incomeRepository, userRepository);
        expenseService = new ExpenseService(transactionService, userRepository);

        alice = new User();
        alice.setId("alice-id");
        alice.setEmail("alice@example.com");

        bob = new User();
        bob.setId("bob-id");
        bob.setEmail("bob@example.com");
    }

    private Expense buildExpense(String id, User owner, double amount) {
        Expense e = new Expense();
        e.setId(id);
        e.setTitle("Groceries");
        e.setCategory("Food");
        e.setAmount(amount);
        e.setDate(LocalDate.of(2025, 3, 10));
        e.setUser(owner);
        return e;
    }

    @Nested
    @DisplayName("getAllExpensesByUser")
    class GetAll {

        @Test
        @DisplayName("returns all expenses when no filters applied")
        void noFilters_returnsAllForUser() {
            Expense e = buildExpense("e1", alice, 55.0);
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(expenseRepository.findByUserIdOrderByDateDesc("alice-id")).thenReturn(List.of(e));

            List<Expense> result = expenseService.getAllExpensesByUser("alice@example.com", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Groceries");
            verify(expenseRepository).findByUserIdOrderByDateDesc("alice-id");
        }

        @Test
        @DisplayName("filters by month when month param supplied")
        void monthFilter_queriesCorrectDateRange() {
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    eq("alice-id"), eq(LocalDate.of(2025, 3, 1)), eq(LocalDate.of(2025, 3, 31))))
                    .thenReturn(List.of(buildExpense("e1", alice, 55.0)));

            List<Expense> result = expenseService.getAllExpensesByUser("alice@example.com", "2025-03", null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown user")
        void unknownUser_throws() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.getAllExpensesByUser("nobody@example.com", null, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createExpense")
    class Create {

        @Test
        @DisplayName("assigns user and persists the expense")
        void assignsUserAndSaves() {
            Expense input = buildExpense(null, null, 99.0);
            Expense saved = buildExpense("e-new", alice, 99.0);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(expenseRepository.save(any(Expense.class))).thenReturn(saved);

            Expense result = expenseService.createExpense(input, "alice@example.com");

            assertThat(result.getId()).isEqualTo("e-new");
            assertThat(result.getUser()).isEqualTo(alice);
            verify(expenseRepository).save(input);
        }
    }

    @Nested
    @DisplayName("updateExpense")
    class Update {

        @Test
        @DisplayName("updates fields and returns the saved expense")
        void updatesAllFields() {
            Expense existing = buildExpense("e1", alice, 50.0);
            Expense updates = new Expense();
            updates.setTitle("Supermarket");
            updates.setCategory("Food");
            updates.setAmount(80.0);
            updates.setDate(LocalDate.of(2025, 4, 1));

            when(expenseRepository.findById("e1")).thenReturn(Optional.of(existing));
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Expense result = expenseService.updateExpense("e1", updates, "alice@example.com");

            assertThat(result.getTitle()).isEqualTo("Supermarket");
            assertThat(result.getAmount()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when expense does not exist")
        void missingExpense_throws() {
            when(expenseRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.updateExpense("bad-id", new Expense(), "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("bad-id");
        }

        @Test
        @DisplayName("throws AccessDeniedException when user does not own the expense")
        void wrongOwner_throwsAccessDenied() {
            Expense existing = buildExpense("e1", bob, 50.0);
            when(expenseRepository.findById("e1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> expenseService.updateExpense("e1", new Expense(), "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("deleteExpense")
    class Delete {

        @Test
        @DisplayName("deletes the expense when the user owns it")
        void validOwner_deletes() {
            Expense existing = buildExpense("e1", alice, 50.0);
            when(expenseRepository.findById("e1")).thenReturn(Optional.of(existing));

            expenseService.deleteExpense("e1", "alice@example.com");

            verify(expenseRepository).deleteById("e1");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when expense does not exist")
        void missingExpense_throws() {
            when(expenseRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.deleteExpense("bad-id", "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(expenseRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when user does not own the expense")
        void wrongOwner_throwsAccessDenied() {
            Expense existing = buildExpense("e1", bob, 50.0);
            when(expenseRepository.findById("e1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> expenseService.deleteExpense("e1", "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(expenseRepository, never()).deleteById(any());
        }
    }
}
