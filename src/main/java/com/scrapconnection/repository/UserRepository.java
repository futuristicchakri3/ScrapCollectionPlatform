package com.scrapconnection.repository;

import com.scrapconnection.entity.User;
import com.scrapconnection.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobileNumber(String mobileNumber);
    boolean existsByMobileNumber(String mobileNumber);
    long countByRole(Role role);
    List<User> findByRole(Role role);
}
