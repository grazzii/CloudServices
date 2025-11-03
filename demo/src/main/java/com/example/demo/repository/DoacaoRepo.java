package com.example.demo.repository;

import com.example.demo.model.Doacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoacaoRepo extends JpaRepository<Doacao, Long> {
}
