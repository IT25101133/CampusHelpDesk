package com.sliit.helpdesk.auth.repository;

// User Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleInOrderByFullNameAsc(List<Role> roles);

    java.util.Optional<User> findFirstByRoleOrderByIdDesc(Role role);

    List<User> findByEnabledTrueAndRoleInOrderByIdAsc(List<Role> roles);

    List<User> findByDepartmentIgnoreCaseAndRoleInOrderByFullNameAsc(String department, List<Role> roles);

    @Query("select distinct u.department from User u"
            + " where u.department is not null and u.department <> '' order by u.department")
    List<String> findDistinctDepartments();
}
