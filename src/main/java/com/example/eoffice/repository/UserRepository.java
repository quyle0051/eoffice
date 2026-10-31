package com.example.eoffice.repository;

import com.example.eoffice.entity.User;
import com.example.eoffice.entity.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    @Query("select distinct u from User u join u.roles r where r.name = :roleName and u.status = :status order by u.id asc")
    List<User> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") UserStatus status);

    @EntityGraph(attributePaths = {"department", "roles"})
    @Query("select distinct u from User u join u.roles r where r.name in :roleNames and u.status = :status order by u.fullName asc")
    List<User> findByRoleNamesAndStatus(@Param("roleNames") Collection<String> roleNames, @Param("status") UserStatus status);

    @EntityGraph(attributePaths = {"department", "roles"})
    List<User> findAllByStatusOrderByFullNameAsc(UserStatus status);
}