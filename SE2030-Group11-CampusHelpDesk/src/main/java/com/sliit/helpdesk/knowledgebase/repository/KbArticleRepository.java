package com.sliit.helpdesk.knowledgebase.repository;

import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    List<KbArticle> findAllByOrderByCreatedAtDesc();

    @Query("""
            select a from KbArticle a
            where lower(a.title) like lower(concat('%', :q, '%'))
               or lower(a.content) like lower(concat('%', :q, '%'))
            order by a.createdAt desc
            """)
    List<KbArticle> search(@Param("q") String query);
}
