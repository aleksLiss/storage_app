package com.storage.app.repository;

import com.storage.app.model.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<@NonNull User, @NonNull UUID> {

    boolean existsUserByUsername(String username);

    Optional<User> findByUsername(String username);
}
