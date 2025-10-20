package com.example.dms.dakar.repository;

import com.example.dms.model.Vente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenteRepositoryDakar extends JpaRepository<Vente, UUID> {
}
