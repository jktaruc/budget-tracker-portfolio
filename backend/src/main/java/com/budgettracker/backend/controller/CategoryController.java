package com.budgettracker.backend.controller;

import com.budgettracker.backend.dto.CategoryCreateRequest;
import com.budgettracker.backend.entity.Category;
import com.budgettracker.backend.dto.CategoryDTO;
import com.budgettracker.backend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDTO> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Category.Type type) {
        return categoryService.getCategoriesForUser(type, userDetails.getUsername())
                .stream()
                .map(CategoryDTO::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CategoryDTO.from(
                        categoryService.createCategory(request, userDetails.getUsername())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        categoryService.deleteCategory(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}