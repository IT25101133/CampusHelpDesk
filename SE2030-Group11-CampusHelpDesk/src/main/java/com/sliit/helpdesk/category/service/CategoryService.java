package com.sliit.helpdesk.category.service;

import com.sliit.helpdesk.category.dto.CategoryForm;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> activeCategories() {
        return allCategories();
    }

    public List<Category> allCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category require(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public Category requireActive(Long id) {
        return require(id);
    }

    @Transactional
    public Category save(CategoryForm form, Long existingId) {
        Category category = existingId == null ? new Category() : require(existingId);
        boolean nameTaken = categoryRepository.existsByNameIgnoreCase(form.getName());
        if (nameTaken && (existingId == null || !form.getName().equalsIgnoreCase(category.getName()))) {
            throw new IllegalArgumentException("A category with that name already exists.");
        }
        category.setName(form.getName().trim());
        category.setDepartment(form.getDepartment() == null || form.getDepartment().isBlank()
                ? null
                : form.getDepartment().trim());
        category.setSlaHours(form.getSlaHours());
        return categoryRepository.save(category);
    }
}
