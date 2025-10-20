package com.example.dms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vente")
public class Vente {

    @Id
    private UUID id;

    private LocalDate dateVente;
    private Double montant;
    private String produit;
    private String region;

    private LocalDateTime updatedAt;

    // 🔹 Constructeur par défaut
    public Vente() {
        this.id = UUID.randomUUID();
    }

    // 🔹 Méthode pour mettre à jour automatiquement le timestamp
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ Getters et Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDateVente() {
        return dateVente;
    }

    public void setDateVente(LocalDate dateVente) {
        this.dateVente = dateVente;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public String getProduit() {
        return produit;
    }

    public void setProduit(String produit) {
        this.produit = produit;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
