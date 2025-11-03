package com.example.demo.repository;

import com.example.demo.model.Campanha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampanhaRepo extends JpaRepository<Campanha, Long> {
}
