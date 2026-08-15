package com.sliit.sparepartshub.repository;

import com.sliit.sparepartshub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    // Used by CustomUserDetailsService during login - staff log in with
    // their email, not a separate username field.
    Optional<User> findByEmail(String email);
}
