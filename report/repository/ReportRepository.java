package com.sliit.helpdesk.report.repository;

// Report Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.report.model.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = "createdBy")
    List<Report> findAllByOrderByCreatedAtDesc();

    @Query("select r from Report r left join fetch r.createdBy where r.id = :id")
    Optional<Report> findDetailById(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Report r set r.createdBy = null where r.createdBy.id = :userId")
    int detachCreator(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update Report r set r.updatedBy = null where r.updatedBy.id = :userId")
    int detachEditor(@Param("userId") Long userId);
}
