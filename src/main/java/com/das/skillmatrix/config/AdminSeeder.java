package com.das.skillmatrix.config;

import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin@skillmatrix.com";
    private static final String ADMIN_PASSWORD = "123456";
    private static final String ADMIN_FULL_NAME = "Nguyễn Văn Admin";
    private static final String ADMIN_ROLE = "ADMIN";

    @Bean
    CommandLineRunner seedAdminUser() {
        return args -> {
            User existing = userRepository.findUserByEmail(ADMIN_EMAIL);
            if (existing != null) {
                return;
            }

            User admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setFullName(ADMIN_FULL_NAME);
            admin.setRole(ADMIN_ROLE);

            userRepository.save(admin);

            System.out.println("✅ Seeded ADMIN user: " + ADMIN_EMAIL + " / " + ADMIN_PASSWORD);
        };
    }
}