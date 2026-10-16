package com.example.eoffice.config;

import com.example.eoffice.entity.Role;
import com.example.eoffice.entity.User;
import com.example.eoffice.entity.UserStatus;
import com.example.eoffice.repository.RoleRepository;
import com.example.eoffice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository,
                               PasswordEncoder encoder) {
        return args -> {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

            if (userRepository.findByEmail("admin@eoffice.com").isEmpty()) {
                User u = new User();
                u.setFullName("Quản trị viên");
                u.setEmail("admin@eoffice.com");
                u.setPassword(encoder.encode("123456"));
                u.setPhone("0000000000");
                u.setStatus(UserStatus.ACTIVE);
                u.getRoles().add(adminRole);
                userRepository.save(u);
            }
        };
    }
}