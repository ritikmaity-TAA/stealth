package ai.theaware.stealth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.theaware.stealth.entity.Users;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
}