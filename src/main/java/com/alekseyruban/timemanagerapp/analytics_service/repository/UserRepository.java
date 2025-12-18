package com.alekseyruban.timemanagerapp.analytics_service.repository;

import com.alekseyruban.timemanagerapp.analytics_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDomainId(Long domainId);
}
