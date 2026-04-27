package com.example.mvc_default.repository;

import com.example.mvc_default.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByCategoryId(Long categoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Product p where p.id = :id")
    @Transactional
    int deleteHardById(@Param("id") Long id);
}
