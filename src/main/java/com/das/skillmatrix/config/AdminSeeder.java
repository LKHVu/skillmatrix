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

    private static final String MD_EMAIL = "manager_department@skillmatrix.com";
    private static final String MD_PASSWORD = "123456";
    private static final String MD_FULL_NAME = "Manager Department";
    private static final String MD_ROLE = "Manager Department";

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

            User managerDept = userRepository.findUserByEmail(MD_EMAIL);
            if (managerDept == null) {
                managerDept = new User();
                managerDept.setEmail(MD_EMAIL);
                managerDept.setPasswordHash(passwordEncoder.encode(MD_PASSWORD));
                managerDept.setFullName(MD_FULL_NAME);
                managerDept.setRole(MD_ROLE);
                managerDept.setStatus(GeneralStatus.ACTIVE);
                userRepository.save(managerDept);
            }
        };
    }
}