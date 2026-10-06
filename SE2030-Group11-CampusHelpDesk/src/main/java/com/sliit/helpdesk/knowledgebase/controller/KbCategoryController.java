package com.sliit.helpdesk.knowledgebase.controller;

// Kb Category Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.knowledgebase.dto.KbCategoryRequest;
import com.sliit.helpdesk.knowledgebase.dto.KbCategoryResponse;
import com.sliit.helpdesk.knowledgebase.service.KbCategoryService;
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
@RequestMapping("/api/kb/categories")
public class KbCategoryController {

    private final KbCategoryService kbCategoryService;

    public KbCategoryController(KbCategoryService kbCategoryService) {
        this.kbCategoryService = kbCategoryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<KbCategoryResponse> list() {
        return kbCategoryService.list().stream().map(KbCategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public KbCategoryResponse detail(@PathVariable Long id) {
        return KbCategoryResponse.from(kbCategoryService.require(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Transactional
    public ResponseEntity<KbCategoryResponse> create(@Valid @RequestBody KbCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(KbCategoryResponse.from(kbCategoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Transactional
    public KbCategoryResponse update(@PathVariable Long id, @Valid @RequestBody KbCategoryRequest request) {
        return KbCategoryResponse.from(kbCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        kbCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
