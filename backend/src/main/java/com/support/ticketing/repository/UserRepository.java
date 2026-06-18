package com.support.ticketing.repository;

import com.support.ticketing.entity.User;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.support.ticketing.entity.Role;
import java.util.Collection;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(Role role);

    List<User> findAllByRoleIn(Collection<Role> roles);
}
