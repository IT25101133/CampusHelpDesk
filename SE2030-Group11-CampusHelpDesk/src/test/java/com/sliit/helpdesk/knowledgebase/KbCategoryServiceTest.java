package com.sliit.helpdesk.knowledgebase;

// Kb Category Service Test is part of the campus help desk knowledgebase code.

import com.sliit.helpdesk.knowledgebase.dto.KbCategoryRequest;
import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.knowledgebase.repository.KbCategoryRepository;
import com.sliit.helpdesk.knowledgebase.service.KbCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KbCategoryServiceTest {

    @Mock private KbCategoryRepository categoryRepository;
    @Mock private ArticleRepository articleRepository;
    private KbCategoryService kbCategoryService;

    @BeforeEach
    void setUp() {
        kbCategoryService = new KbCategoryService(categoryRepository, articleRepository);
    }

    @Test
    void createPersistsNamedCategory() {
        KbCategoryRequest request = new KbCategoryRequest();
        request.setName(" Exam support ");
        when(categoryRepository.existsByNameIgnoreCase("Exam support")).thenReturn(false);
        when(categoryRepository.save(any(KbCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KbCategory saved = kbCategoryService.create(request);

        assertThat(saved.getName()).isEqualTo("Exam support");
    }

    @Test
    void deleteRejectsCategoryWithArticles() {
        KbCategory category = new KbCategory();
        category.setId(4L);
        category.setName("IT FAQ");
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(articleRepository.countByCategory_Id(4L)).thenReturn(2L);

        assertThatThrownBy(() -> kbCategoryService.delete(4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existing articles");
    }

    @Test
    void deleteRemovesEmptyCategory() {
        KbCategory category = new KbCategory();
        category.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(articleRepository.countByCategory_Id(5L)).thenReturn(0L);

        kbCategoryService.delete(5L);

        verify(categoryRepository).delete(category);
    }
}
