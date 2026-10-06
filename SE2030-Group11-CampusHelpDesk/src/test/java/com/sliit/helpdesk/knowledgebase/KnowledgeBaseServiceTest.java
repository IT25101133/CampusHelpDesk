package com.sliit.helpdesk.knowledgebase;

// Knowledge Base Service Test is part of the campus help desk knowledgebase code.

import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.knowledgebase.repository.KbCategoryRepository;
import com.sliit.helpdesk.knowledgebase.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private KbCategoryRepository categoryRepository;
    private KnowledgeBaseService knowledgeBaseService;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = new KnowledgeBaseService(articleRepository, categoryRepository);
    }

    @Test
    void viewIncrementsCountForArticle() {
        KbArticle article = new KbArticle();
        article.setId(3L);
        article.setViewCount(10);
        when(articleRepository.findById(3L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(KbArticle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KbArticle viewed = knowledgeBaseService.view(3L);

        assertThat(viewed.getViewCount()).isEqualTo(11);
    }

    @Test
    void viewRejectsUnknownArticle() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeBaseService.view(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
