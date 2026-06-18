package com.support.ticketing.config;

import com.support.ticketing.entity.Role;
import com.support.ticketing.entity.User;
import com.support.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedAdminUser() {
        return args -> {
            String adminEmail = "admin@support.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                userRepository.save(User.builder()
                        .name("System Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("Admin@123"))
                        .role(Role.ROLE_ADMIN)
                        .active(true)
                        .build());
            } else {
                userRepository.findByEmail(adminEmail).ifPresent(user -> {
                    user.setRole(Role.ROLE_ADMIN);
                    user.setActive(true);
                    userRepository.save(user);
                });
            }
            seedExtraAdmin("support.lead@support.com", "Support Lead");
            seedExtraAdmin("ops.manager@support.com", "Ops Manager");
            seedAgent("agent.one@support.com", "Support Agent One");
            seedAgent("agent.two@support.com", "Support Agent Two");
        };
    }

    private void seedExtraAdmin(String email, String name) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ROLE_ADMIN)
                    .active(true)
                    .build());
        }
    }

    private void seedAgent(String email, String name) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode("Agent@123"))
                    .role(Role.ROLE_AGENT)
                    .active(true)
                    .build());
        }
    }
}
