package com.das.skillmatrix.config;

import com.das.skillmatrix.entity.GeneralStatus;
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
    private static final String ADMIN_FULL_NAME = "Admin";
    private static final String ADMIN_ROLE = "ADMIN";

    private static final String MANAGER_EMAIL = "manager_career@skillmatrix.com";
    private static final String MANAGER_PASSWORD = "123456";
    private static final String MANAGER_FULL_NAME = "Manager Career";
    private static final String MANAGER_ROLE = "Manager Career";

    @Bean
    CommandLineRunner seedAdminUser() {
        return args -> {
            User admin = userRepository.findUserByEmail(ADMIN_EMAIL);
            if (admin == null) {
                admin = new User();
                admin.setEmail(ADMIN_EMAIL);
                admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                admin.setFullName(ADMIN_FULL_NAME);
                admin.setRole(ADMIN_ROLE);
                admin.setStatus(GeneralStatus.ACTIVE);
                userRepository.save(admin);
            }

            User manager = userRepository.findUserByEmail(MANAGER_EMAIL);
            if (manager == null) {
                manager = new User();
                manager.setEmail(MANAGER_EMAIL);
                manager.setPasswordHash(passwordEncoder.encode(MANAGER_PASSWORD));
                manager.setFullName(MANAGER_FULL_NAME);
                manager.setRole(MANAGER_ROLE);
                manager.setStatus(GeneralStatus.ACTIVE);
                userRepository.save(manager);
            }
        };
    }
}