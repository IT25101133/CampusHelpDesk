package com.sliit.helpdesk.category.controller;

// Category Api Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.category.dto.CategoryForm;
import com.sliit.helpdesk.category.service.CategoryService;
import com.sliit.helpdesk.ticket.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryService.allCategories().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CategoryResponse detail(@PathVariable Long id) {
        return CategoryResponse.from(categoryService.require(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(categoryService.save(form, null)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryForm form) {
        return CategoryResponse.from(categoryService.save(form, id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
