package com.loantrackr.repository;

import com.loantrackr.enums.Role;
import com.loantrackr.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.username = :identifier OR LOWER(u.email) = LOWER(:identifier)")
    Optional<User> findUserByUsernameOrEmail(@Param("identifier") String identifier);


    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findUserById(Long id);

    Optional<User> findUserByUsername(String username);

    boolean existsByRole(Role role);

    List<User> findAllByRole(Role role);
}