package com.sliit.helpdesk.knowledgebase.repository;

// Article Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepository extends JpaRepository<KbArticle, Long> {

    List<KbArticle> findAllByOrderByCreatedAtDesc();

    long countByCategory_Id(Long categoryId);

    boolean existsByTitleIgnoreCase(String title);

    @Query("""
            select a from KbArticle a
            where lower(a.title) like lower(concat('%', :q, '%'))
               or lower(a.content) like lower(concat('%', :q, '%'))
            order by a.createdAt desc
            """)
    List<KbArticle> search(@Param("q") String query);

    @Modifying(clearAutomatically = true)
    @Query("update KbArticle a set a.author = null where a.author.id = :userId")
    int detachAuthor(@Param("userId") Long userId);
}
