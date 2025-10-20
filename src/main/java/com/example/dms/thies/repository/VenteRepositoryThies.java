package com.example.dms.thies.repository;

import com.example.dms.model.Vente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenteRepositoryThies extends JpaRepository<Vente, UUID> {
}
