package com.example.mvc_default.repository;

import com.example.mvc_default.entity.Instruction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructionRepository extends JpaRepository<Instruction, Long> {
}
