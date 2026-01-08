package com.example.bumil_backend.repository;

import com.example.bumil_backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmailAndIsDeletedFalse(String email);

    Optional<Users> findByIdAndIsDeletedFalse(Long id);
    boolean existsByEmailAndIsDeletedFalse(String email);

    List<Users> findAllByIsDeletedFalse();
}
