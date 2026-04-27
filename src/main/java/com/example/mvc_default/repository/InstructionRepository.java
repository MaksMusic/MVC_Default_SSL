package com.example.mvc_default.repository;

import com.example.mvc_default.entity.Instruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InstructionRepository extends JpaRepository<Instruction, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Instruction i where i.product.id = :productId")
    @Transactional
    int deleteByProductId(@Param("productId") Long productId);
}
