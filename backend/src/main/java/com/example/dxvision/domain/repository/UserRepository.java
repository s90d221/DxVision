package com.example.dxvision.domain.repository;

import com.example.dxvision.domain.auth.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("""
            select u from User u
            where (:q is null or :q = '' or lower(u.email) like lower(concat('%', :q, '%')) or lower(u.name) like lower(concat('%', :q, '%')))
            """)
    Page<User> searchByEmailOrName(@Param("q") String q, Pageable pageable);
}
