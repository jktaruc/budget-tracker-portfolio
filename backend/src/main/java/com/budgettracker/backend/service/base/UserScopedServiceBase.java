package com.budgettracker.backend.service.base;

import com.budgettracker.backend.entity.User;
import com.budgettracker.backend.exception.ResourceNotFoundException;
import com.budgettracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;

/**
 * Shared utilities for services that operate on user-owned entities.
 * Eliminates the duplicated getUser() / ensureOwnership() pattern
 * across ExpenseService, IncomeService, and similar services.
 */
@RequiredArgsConstructor
public abstract class UserScopedServiceBase {

    protected final UserRepository userRepository;

    /** Loads a User by email or throws ResourceNotFoundException. */
    protected User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    /**
     * Verifies that {@code owner} matches the given email.
     *
     * @throws AccessDeniedException if the owner is null or the email does not match
     */
    protected void ensureOwnership(User owner, String userEmail) {
        if (owner == null || !owner.getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You do not have permission to modify this resource");
        }
    }
}
