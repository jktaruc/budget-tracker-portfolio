package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.Income;
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
@DisplayName("IncomeService")
class IncomeServiceTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;

    private TransactionService transactionService;
    private IncomeService incomeService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(expenseRepository, incomeRepository, userRepository);
        incomeService = new IncomeService(transactionService, userRepository);

        alice = new User();
        alice.setId("alice-id");
        alice.setEmail("alice@example.com");

        bob = new User();
        bob.setId("bob-id");
        bob.setEmail("bob@example.com");
    }

    private Income buildIncome(String id, User owner, String title, double amount) {
        Income i = new Income();
        i.setId(id);
        i.setTitle(title);
        i.setCategory("Salary");
        i.setAmount(amount);
        i.setDate(LocalDate.of(2025, 3, 1));
        i.setUser(owner);
        return i;
    }

    @Nested
    @DisplayName("getAllIncomesByUser")
    class GetAll {

        @Test
        @DisplayName("returns all income for the user when no filters applied")
        void noFilters_returnsAll() {
            Income i = buildIncome("i1", alice, "Salary", 5000.0);
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(incomeRepository.findByUserIdOrderByDateDesc("alice-id")).thenReturn(List.of(i));

            List<Income> result = incomeService.getAllIncomesByUser("alice@example.com", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Salary");
        }

        @Test
        @DisplayName("filters by month when month param is provided")
        void monthFilter_queriesDateRange() {
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(incomeRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    eq("alice-id"), eq(LocalDate.of(2025, 3, 1)), eq(LocalDate.of(2025, 3, 31))))
                    .thenReturn(List.of(buildIncome("i1", alice, "Salary", 5000.0)));

            List<Income> result = incomeService.getAllIncomesByUser("alice@example.com", "2025-03", null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown user")
        void unknownUser_throws() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incomeService.getAllIncomesByUser("ghost@example.com", null, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createIncome")
    class Create {

        @Test
        @DisplayName("assigns user and persists the income entry")
        void assignsUserAndSaves() {
            Income input = buildIncome(null, null, "Freelance", 1200.0);
            Income saved = buildIncome("i-new", alice, "Freelance", 1200.0);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(incomeRepository.save(any(Income.class))).thenReturn(saved);

            Income result = incomeService.createIncome(input, "alice@example.com");

            assertThat(result.getId()).isEqualTo("i-new");
            assertThat(result.getUser()).isEqualTo(alice);
            verify(incomeRepository).save(input);
        }
    }

    @Nested
    @DisplayName("updateIncome")
    class Update {

        @Test
        @DisplayName("updates all fields and returns the saved record")
        void updatesAllFields() {
            Income existing = buildIncome("i1", alice, "Salary", 5000.0);
            Income updates = new Income();
            updates.setTitle("Freelance Work");
            updates.setCategory("Freelance");
            updates.setAmount(1500.0);
            updates.setDate(LocalDate.of(2025, 4, 15));

            when(incomeRepository.findById("i1")).thenReturn(Optional.of(existing));
            when(incomeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Income result = incomeService.updateIncome("i1", updates, "alice@example.com");

            assertThat(result.getTitle()).isEqualTo("Freelance Work");
            assertThat(result.getAmount()).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the income record does not exist")
        void missingIncome_throws() {
            when(incomeRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incomeService.updateIncome("bad-id", new Income(), "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("bad-id");
        }

        @Test
        @DisplayName("throws AccessDeniedException when another user tries to update the record")
        void wrongOwner_throwsAccessDenied() {
            Income existing = buildIncome("i1", bob, "Salary", 5000.0);
            when(incomeRepository.findById("i1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> incomeService.updateIncome("i1", new Income(), "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("deleteIncome")
    class Delete {

        @Test
        @DisplayName("deletes the record when the user owns it")
        void validOwner_deletes() {
            Income existing = buildIncome("i1", alice, "Salary", 5000.0);
            when(incomeRepository.findById("i1")).thenReturn(Optional.of(existing));

            incomeService.deleteIncome("i1", "alice@example.com");

            verify(incomeRepository).deleteById("i1");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when income record does not exist")
        void missingIncome_throws() {
            when(incomeRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incomeService.deleteIncome("bad-id", "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(incomeRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when user does not own the record")
        void wrongOwner_throwsAccessDenied() {
            Income existing = buildIncome("i1", bob, "Salary", 5000.0);
            when(incomeRepository.findById("i1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> incomeService.deleteIncome("i1", "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(incomeRepository, never()).deleteById(any());
        }
    }
}
