package com.careerpilot.repository;

import com.careerpilot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * No implementation class needed: Spring Data generates one at startup.
 * JpaRepository already gives you save, findById, findAll, delete, count and more.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Derived queries: Spring Data reads the method name and writes the SQL
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}