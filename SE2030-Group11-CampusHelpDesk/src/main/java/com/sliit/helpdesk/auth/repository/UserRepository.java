package com.sliit.helpdesk.auth.repository;

import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleInOrderByFullNameAsc(List<Role> roles);

    List<User> findByDepartmentIgnoreCaseAndRoleInOrderByFullNameAsc(String department, List<Role> roles);
}
