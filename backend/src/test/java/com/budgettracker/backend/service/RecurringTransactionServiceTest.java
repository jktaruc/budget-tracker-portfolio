package com.budgettracker.backend.service;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.entity.Income;
import com.budgettracker.backend.entity.RecurringTransaction;
import com.budgettracker.backend.entity.RecurringTransaction.Frequency;
import com.budgettracker.backend.entity.RecurringTransaction.Type;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.ExpenseRepository;
import com.budgettracker.backend.repository.IncomeRepository;
import com.budgettracker.backend.repository.RecurringTransactionRepository;
import com.budgettracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("RecurringTransactionService")
class RecurringTransactionServiceTest {

    @Mock private RecurringTransactionRepository recurringRepo;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private UserRepository userRepository;

    private RecurringTransactionService service;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        service = new RecurringTransactionService(recurringRepo, expenseRepository, incomeRepository, userRepository);

        alice = new User();
        alice.setId("alice-id");
        alice.setEmail("alice@example.com");

        bob = new User();
        bob.setId("bob-id");
        bob.setEmail("bob@example.com");
    }

    private RecurringTransaction buildRt(String id, User owner, Type type, Frequency freq) {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setId(id);
        rt.setUser(owner);
        rt.setTitle("Netflix");
        rt.setCategory("Entertainment");
        rt.setAmount(22.99);
        rt.setType(type);
        rt.setFrequency(freq);
        rt.setStartDate(LocalDate.now().minusMonths(2));
        rt.setActive(true);
        return rt;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("assigns the authenticated user and saves")
        void assignsUserAndPersists() {
            RecurringTransaction input = buildRt(null, null, Type.EXPENSE, Frequency.MONTHLY);
            RecurringTransaction saved = buildRt("rt-1", alice, Type.EXPENSE, Frequency.MONTHLY);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(recurringRepo.save(any())).thenReturn(saved);

            RecurringTransaction result = service.create(input, "alice@example.com");

            assertThat(result.getUser()).isEqualTo(alice);
            verify(recurringRepo).save(input);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown user")
        void unknownUser_throws() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(buildRt(null, null, Type.EXPENSE, Frequency.MONTHLY), "ghost@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getByUser")
    class GetByUser {

        @Test
        @DisplayName("returns all recurring transactions for the user")
        void returnsUserTransactions() {
            RecurringTransaction rt = buildRt("rt-1", alice, Type.INCOME, Frequency.MONTHLY);
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(recurringRepo.findByUserId("alice-id")).thenReturn(List.of(rt));

            List<RecurringTransaction> results = service.getByUser("alice@example.com");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Netflix");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates mutable fields and returns the saved record")
        void updatesMutableFields() {
            RecurringTransaction existing = buildRt("rt-1", alice, Type.EXPENSE, Frequency.MONTHLY);
            RecurringTransaction updates = buildRt(null, null, Type.EXPENSE, Frequency.WEEKLY);
            updates.setTitle("Spotify");
            updates.setAmount(11.99);
            updates.setActive(false);

            when(recurringRepo.findById("rt-1")).thenReturn(Optional.of(existing));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RecurringTransaction result = service.update("rt-1", updates, "alice@example.com");

            assertThat(result.getTitle()).isEqualTo("Spotify");
            assertThat(result.getAmount()).isEqualTo(11.99);
            assertThat(result.getFrequency()).isEqualTo(Frequency.WEEKLY);
            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id does not exist")
        void missingRt_throws() {
            when(recurringRepo.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update("bad-id", new RecurringTransaction(), "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when user does not own the record")
        void wrongOwner_throws() {
            RecurringTransaction existing = buildRt("rt-1", bob, Type.EXPENSE, Frequency.MONTHLY);
            when(recurringRepo.findById("rt-1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update("rt-1", new RecurringTransaction(), "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("removes the record when the user owns it")
        void validOwner_deletes() {
            RecurringTransaction existing = buildRt("rt-1", alice, Type.EXPENSE, Frequency.MONTHLY);
            when(recurringRepo.findById("rt-1")).thenReturn(Optional.of(existing));

            service.delete("rt-1", "alice@example.com");

            verify(recurringRepo).deleteById("rt-1");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown id")
        void missingRt_throws() {
            when(recurringRepo.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete("bad-id", "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(recurringRepo, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when user does not own the record")
        void wrongOwner_throws() {
            RecurringTransaction existing = buildRt("rt-1", bob, Type.EXPENSE, Frequency.MONTHLY);
            when(recurringRepo.findById("rt-1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.delete("rt-1", "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("processAll")
    class ProcessAll {

        @Test
        @DisplayName("creates an income entry when a monthly rule is overdue")
        void overdueMonthly_createsIncome() {
            RecurringTransaction rt = buildRt("rt-1", alice, Type.INCOME, Frequency.MONTHLY);
            rt.setLastProcessed(LocalDate.now().minusMonths(1).minusDays(1));

            when(recurringRepo.findByActiveTrue()).thenReturn(List.of(rt));
            when(recurringRepo.save(any())).thenReturn(rt);

            service.processAll();

            ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
            verify(incomeRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).contains("Recurring");
            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates an expense entry when a weekly expense rule is overdue")
        void overdueWeekly_createsExpense() {
            RecurringTransaction rt = buildRt("rt-2", alice, Type.EXPENSE, Frequency.WEEKLY);
            rt.setLastProcessed(LocalDate.now().minusWeeks(1).minusDays(1));

            when(recurringRepo.findByActiveTrue()).thenReturn(List.of(rt));
            when(recurringRepo.save(any())).thenReturn(rt);

            service.processAll();

            verify(expenseRepository).save(any(Expense.class));
            verify(incomeRepository, never()).save(any());
        }

        @Test
        @DisplayName("deactivates a rule when it has passed its end date")
        void expiredRule_deactivated() {
            RecurringTransaction rt = buildRt("rt-1", alice, Type.INCOME, Frequency.MONTHLY);
            rt.setEndDate(LocalDate.now().minusDays(1));

            when(recurringRepo.findByActiveTrue()).thenReturn(List.of(rt));
            when(recurringRepo.save(any())).thenReturn(rt);

            service.processAll();

            assertThat(rt.isActive()).isFalse();
            verify(recurringRepo).save(rt);
            verify(incomeRepository, never()).save(any());
            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("does not create a transaction when the rule is not yet due")
        void notYetDue_skips() {
            RecurringTransaction rt = buildRt("rt-1", alice, Type.EXPENSE, Frequency.MONTHLY);
            rt.setLastProcessed(LocalDate.now().minusDays(5));

            when(recurringRepo.findByActiveTrue()).thenReturn(List.of(rt));

            service.processAll();

            verify(expenseRepository, never()).save(any());
            verify(incomeRepository, never()).save(any());
        }

        @Test
        @DisplayName("updates lastProcessed after creating a transaction")
        void updatesLastProcessed() {
            RecurringTransaction rt = buildRt("rt-1", alice, Type.EXPENSE, Frequency.MONTHLY);
            rt.setLastProcessed(LocalDate.now().minusMonths(1).minusDays(1));

            when(recurringRepo.findByActiveTrue()).thenReturn(List.of(rt));
            when(recurringRepo.save(any())).thenReturn(rt);

            service.processAll();

            assertThat(rt.getLastProcessed()).isEqualTo(LocalDate.now());
        }
    }
}
