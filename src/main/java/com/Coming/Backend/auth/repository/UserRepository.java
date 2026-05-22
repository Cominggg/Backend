package com.Coming.Backend.auth.repository;

import com.Coming.Backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    List<User> findAllByIdIn(Collection<Long> ids);
}
