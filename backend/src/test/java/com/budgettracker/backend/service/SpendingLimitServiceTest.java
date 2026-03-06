package com.budgettracker.backend.service;

import com.budgettracker.backend.dto.SpendingLimitDTO;
import com.budgettracker.backend.entity.SpendingLimit;
import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.ExpenseRepository;
import com.budgettracker.backend.repository.SpendingLimitRepository;
import com.budgettracker.backend.repository.UserRepository;
import org.assertj.core.data.Offset;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpendingLimitService")
class SpendingLimitServiceTest {

    @Mock private SpendingLimitRepository spendingLimitRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;

    private SpendingLimitService service;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        service = new SpendingLimitService(spendingLimitRepository, expenseRepository, userRepository);

        alice = new User();
        alice.setId("alice-id");
        alice.setEmail("alice@example.com");
        alice.setName("Alice");
        alice.setPassword("hashed");
        alice.setRole(User.Role.USER);

        bob = new User();
        bob.setId("bob-id");
        bob.setEmail("bob@example.com");
    }

    private SpendingLimit buildLimit(String id, User owner, String category, double amount, SpendingLimit.Period period) {
        SpendingLimit limit = new SpendingLimit();
        limit.setId(id);
        limit.setUser(owner);
        limit.setCategory(category);
        limit.setLimitAmount(amount);
        limit.setPeriod(period);
        limit.setStartDate(LocalDate.now().withDayOfMonth(1));
        return limit;
    }

    @Nested
    @DisplayName("getLimitsWithProgress")
    class GetLimits {

        @Test
        @DisplayName("returns DTOs enriched with spending amounts from the database")
        void enrichesWithSpentAmount() {
            SpendingLimit limit = buildLimit("l1", alice, "Food", 400.0, SpendingLimit.Period.MONTHLY);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(spendingLimitRepository.findByUserId("alice-id")).thenReturn(List.of(limit));
            when(expenseRepository.sumAmountByUserIdAndCategoryAndDateBetween(
                    eq("alice-id"), eq("Food"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(250.0);

            List<SpendingLimitDTO> result = service.getLimitsWithProgress("alice@example.com");

            assertThat(result).hasSize(1);
            SpendingLimitDTO dto = result.get(0);
            assertThat(dto.getSpent()).isEqualTo(250.0);
            assertThat(dto.getRemaining()).isEqualTo(150.0);
            assertThat(dto.getPercentageUsed()).isCloseTo(62.5, Offset.offset(0.01));
            assertThat(dto.isExceeded()).isFalse();
        }

        @Test
        @DisplayName("flags exceeded=true when spending exceeds the limit")
        void flagsExceeded() {
            SpendingLimit limit = buildLimit("l1", alice, "Shopping", 100.0, SpendingLimit.Period.MONTHLY);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(spendingLimitRepository.findByUserId("alice-id")).thenReturn(List.of(limit));
            when(expenseRepository.sumAmountByUserIdAndCategoryAndDateBetween(
                    eq("alice-id"), eq("Shopping"), any(), any()))
                    .thenReturn(150.0);

            SpendingLimitDTO dto = service.getLimitsWithProgress("alice@example.com").get(0);

            assertThat(dto.isExceeded()).isTrue();
            assertThat(dto.getRemaining()).isEqualTo(-50.0);
        }

        @Test
        @DisplayName("does not divide by zero when limitAmount is zero")
        void zeroLimit_percentageIsZero() {
            SpendingLimit limit = buildLimit("l1", alice, "Food", 0.0, SpendingLimit.Period.MONTHLY);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(spendingLimitRepository.findByUserId("alice-id")).thenReturn(List.of(limit));
            when(expenseRepository.sumAmountByUserIdAndCategoryAndDateBetween(
                    any(), any(), any(), any())).thenReturn(50.0);

            SpendingLimitDTO dto = service.getLimitsWithProgress("alice@example.com").get(0);

            assertThat(dto.getPercentageUsed()).isZero();
        }

        @Test
        @DisplayName("returns window dates that cover the current period")
        void windowDatesSpanCurrentPeriod() {
            SpendingLimit limit = buildLimit("l1", alice, "Transport", 200.0, SpendingLimit.Period.MONTHLY);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(spendingLimitRepository.findByUserId("alice-id")).thenReturn(List.of(limit));
            when(expenseRepository.sumAmountByUserIdAndCategoryAndDateBetween(any(), any(), any(), any())).thenReturn(0.0);

            SpendingLimitDTO dto = service.getLimitsWithProgress("alice@example.com").get(0);

            assertThat(dto.getWindowStart()).isNotNull();
            assertThat(dto.getWindowEnd()).isNotNull();
            assertThat(dto.getWindowEnd()).isAfterOrEqualTo(dto.getWindowStart());
            assertThat(LocalDate.now()).isBetween(dto.getWindowStart(), dto.getWindowEnd());
        }
    }

    @Nested
    @DisplayName("createLimit")
    class Create {

        @Test
        @DisplayName("persists the limit and returns a DTO enriched with zero spending")
        void savesAndReturnsDto() {
            SpendingLimit input = buildLimit(null, null, "Food", 300.0, SpendingLimit.Period.MONTHLY);
            SpendingLimit saved = buildLimit("l-new", alice, "Food", 300.0, SpendingLimit.Period.MONTHLY);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
            when(spendingLimitRepository.save(any())).thenReturn(saved);
            when(expenseRepository.sumAmountByUserIdAndCategoryAndDateBetween(any(), any(), any(), any())).thenReturn(0.0);

            SpendingLimitDTO result = service.createLimit(input, "alice@example.com");

            assertThat(result.getId()).isEqualTo("l-new");
            assertThat(result.getCategory()).isEqualTo("Food");
            assertThat(result.getSpent()).isZero();
            verify(spendingLimitRepository).save(input);
        }
    }

    @Nested
    @DisplayName("updateLimit")
    class Update {

        @Test
        @DisplayName("updates mutable fields and returns a refreshed DTO")
        void updatesMutableFields() {
            SpendingLimit existing = buildLimit("l1", alice, "Food", 400.0, SpendingLimit.Period.MONTHLY);
            SpendingLimit updates = buildLimit(null, null, "Food", 600.0, SpendingLimit.Period.YEARLY);

            when(spendingLimitRepository.findById("l1")).thenReturn(Optional.of(existing));
            when(spendingLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(expenseRepository.sumAmountByUserIdAndCategoryAndDateBetween(any(), any(), any(), any())).thenReturn(100.0);

            SpendingLimitDTO result = service.updateLimit("l1", updates, "alice@example.com");

            assertThat(result.getLimitAmount()).isEqualTo(600.0);
            assertThat(result.getPeriod()).isEqualTo(SpendingLimit.Period.YEARLY);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the limit does not exist")
        void missingLimit_throws() {
            when(spendingLimitRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateLimit("bad-id", new SpendingLimit(), "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when user does not own the limit")
        void wrongOwner_throws() {
            SpendingLimit existing = buildLimit("l1", bob, "Food", 400.0, SpendingLimit.Period.MONTHLY);
            when(spendingLimitRepository.findById("l1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateLimit("l1", new SpendingLimit(), "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("deleteLimit")
    class Delete {

        @Test
        @DisplayName("removes the limit when the user owns it")
        void validOwner_deletes() {
            SpendingLimit existing = buildLimit("l1", alice, "Food", 400.0, SpendingLimit.Period.MONTHLY);
            when(spendingLimitRepository.findById("l1")).thenReturn(Optional.of(existing));

            service.deleteLimit("l1", "alice@example.com");

            verify(spendingLimitRepository).deleteById("l1");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when limit does not exist")
        void missingLimit_throws() {
            when(spendingLimitRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteLimit("bad-id", "alice@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(spendingLimitRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when user does not own the limit")
        void wrongOwner_throws() {
            SpendingLimit existing = buildLimit("l1", bob, "Food", 400.0, SpendingLimit.Period.MONTHLY);
            when(spendingLimitRepository.findById("l1")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.deleteLimit("l1", "alice@example.com"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(spendingLimitRepository, never()).deleteById(any());
        }
    }
}
