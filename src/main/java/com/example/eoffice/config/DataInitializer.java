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

import java.util.List;

@Configuration
public class DataInitializer {

    private static final List<String> ROLES = List.of("ADMIN", "CLERK", "DIRECTOR", "HEAD", "DEPUTY", "OFFICE_CHIEF", "STAFF");

    @Bean
    CommandLineRunner initData(UserRepository user, RoleRepository roles,
                               PasswordEncoder encoder) {
        return args -> {
            for (String name : ROLES) {
                if (roles.findByName(name).isEmpty()) {
                    roles.save(new Role(name));
                }
            }
        };
    }
}