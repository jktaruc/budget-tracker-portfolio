package com.budgettracker.backend.controller;

import com.budgettracker.backend.entity.Expense;
import com.budgettracker.backend.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@DisplayName("ExpenseController")
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    private Expense sampleExpense() {
        Expense e = new Expense();
        e.setId("e-1");
        e.setTitle("Groceries");
        e.setCategory("Food");
        e.setAmount(85.0);
        e.setDate(LocalDate.of(2025, 3, 10));
        return e;
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("GET /api/expenses returns 200 with expense list")
    void getExpenses_returns200() throws Exception {
        when(expenseService.getAllExpensesByUser(eq("alice@example.com"), isNull(), isNull()))
                .thenReturn(List.of(sampleExpense()));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Groceries"))
                .andExpect(jsonPath("$[0].amount").value(85.0));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("GET /api/expenses?month=2025-03 filters by month")
    void getExpenses_withMonthFilter_returns200() throws Exception {
        when(expenseService.getAllExpensesByUser("alice@example.com", "2025-03", null))
                .thenReturn(List.of(sampleExpense()));

        mockMvc.perform(get("/api/expenses").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/expenses without auth returns 401")
    void getExpenses_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("POST /api/expenses creates and returns 201")
    void createExpense_returns201() throws Exception {
        Expense saved = sampleExpense();
        when(expenseService.createExpense(any(Expense.class), eq("alice@example.com"))).thenReturn(saved);

        mockMvc.perform(post("/api/expenses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Groceries",
                                "category": "Food",
                                "amount": 85.0,
                                "date": "2025-03-10"
                            }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("e-1"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("POST /api/expenses with missing title returns 400")
    void createExpense_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "category": "Food",
                                "amount": 85.0,
                                "date": "2025-03-10"
                            }
                        """))
                .andExpect(status().isBadRequest());

        verify(expenseService, never()).createExpense(any(), any());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("DELETE /api/expenses/{id} returns 204")
    void deleteExpense_returns204() throws Exception {
        doNothing().when(expenseService).deleteExpense("e-1", "alice@example.com");

        mockMvc.perform(delete("/api/expenses/e-1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(expenseService).deleteExpense("e-1", "alice@example.com");
    }
}
