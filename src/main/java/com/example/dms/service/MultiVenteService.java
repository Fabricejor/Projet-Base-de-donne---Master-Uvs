package com.example.dms.service;

import com.example.dms.model.Vente;
import com.example.dms.dakar.repository.VenteRepositoryDakar;
import com.example.dms.thies.repository.VenteRepositoryThies;
import com.example.dms.stl.repository.VenteRepositoryStl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MultiVenteService {

    private final VenteRepositoryDakar dakarRepo;
    private final VenteRepositoryThies thiesRepo;
    private final VenteRepositoryStl stlRepo;

    public MultiVenteService(VenteRepositoryDakar dakarRepo,
                             VenteRepositoryThies thiesRepo,
                             VenteRepositoryStl stlRepo) {
        this.dakarRepo = dakarRepo;
        this.thiesRepo = thiesRepo;
        this.stlRepo = stlRepo;
    }

    // 🔹 Ajout d’une vente dans la base correspondant à la région
    @Transactional("dakarTransactionManager")
    public Vente saveToDakar(Vente vente) {
        vente.setRegion("Dakar");
        return dakarRepo.save(vente);
    }

    @Transactional("thiesTransactionManager")
    public Vente saveToThies(Vente vente) {
        vente.setRegion("Thies");
        return thiesRepo.save(vente);
    }

    @Transactional("stlTransactionManager")
    public Vente saveToStl(Vente vente) {
        vente.setRegion("Saint-Louis");
        return stlRepo.save(vente);
    }

    // 🔹 Récupérer toutes les ventes d'une région
    public List<Vente> findAllFromDakar() {
        return dakarRepo.findAll();
    }

    public List<Vente> findAllFromThies() {
        return thiesRepo.findAll();
    }

    public List<Vente> findAllFromStl() {
        return stlRepo.findAll();
    }

    // 🔹 Récupérer toutes les ventes de toutes les régions (seulement les non-supprimées)
    public List<Vente> findAllFromAllRegions() {
        List<Vente> all = new ArrayList<>();
        all.addAll(dakarRepo.findAll().stream().filter(v -> !Boolean.TRUE.equals(v.getDeleted())).toList());
        all.addAll(thiesRepo.findAll().stream().filter(v -> !Boolean.TRUE.equals(v.getDeleted())).toList());
        all.addAll(stlRepo.findAll().stream().filter(v -> !Boolean.TRUE.equals(v.getDeleted())).toList());
        return all;
    }

    // 🔹 Rechercher par ID dans toutes les bases
    public Optional<Vente> findById(UUID id) {
        Optional<Vente> v = dakarRepo.findById(id);
        if (v.isPresent()) return v;
        v = thiesRepo.findById(id);
        if (v.isPresent()) return v;
        return stlRepo.findById(id);
    }

    // 🔹 Mise à jour d'une vente dans une région spécifique
    @Transactional("dakarTransactionManager")
    public Vente updateInDakar(UUID id, Vente updatedVente) {
        return dakarRepo.findById(id).map(existing -> {
            existing.setProduit(updatedVente.getProduit());
            existing.setMontant(updatedVente.getMontant());
            existing.setDateVente(updatedVente.getDateVente());
            // updatedAt sera mis à jour automatiquement par @PreUpdate
            return dakarRepo.save(existing);
        }).orElseThrow(() -> new RuntimeException("Vente non trouvée dans Dakar"));
    }

    @Transactional("thiesTransactionManager")
    public Vente updateInThies(UUID id, Vente updatedVente) {
        return thiesRepo.findById(id).map(existing -> {
            existing.setProduit(updatedVente.getProduit());
            existing.setMontant(updatedVente.getMontant());
            existing.setDateVente(updatedVente.getDateVente());
            return thiesRepo.save(existing);
        }).orElseThrow(() -> new RuntimeException("Vente non trouvée dans Thies"));
    }

    @Transactional("stlTransactionManager")
    public Vente updateInStl(UUID id, Vente updatedVente) {
        return stlRepo.findById(id).map(existing -> {
            existing.setProduit(updatedVente.getProduit());
            existing.setMontant(updatedVente.getMontant());
            existing.setDateVente(updatedVente.getDateVente());
            return stlRepo.save(existing);
        }).orElseThrow(() -> new RuntimeException("Vente non trouvée dans Saint-Louis"));
    }

    // 🔹 Supprimer d'une région spécifique (soft delete avec tombstone)
    @Transactional("dakarTransactionManager")
    public void deleteFromDakar(UUID id) {
        dakarRepo.findById(id).ifPresent(vente -> {
            vente.markAsDeleted();
            dakarRepo.save(vente);
        });
    }

    @Transactional("thiesTransactionManager")
    public void deleteFromThies(UUID id) {
        thiesRepo.findById(id).ifPresent(vente -> {
            vente.markAsDeleted();
            thiesRepo.save(vente);
        });
    }

    @Transactional("stlTransactionManager")
    public void deleteFromStl(UUID id) {
        stlRepo.findById(id).ifPresent(vente -> {
            vente.markAsDeleted();
            stlRepo.save(vente);
        });
    }

    // 🔹 Supprimer dans toutes les bases (soft delete)
    @Transactional
    public void deleteById(UUID id) {
        dakarRepo.findById(id).ifPresent(v -> {
            v.markAsDeleted();
            dakarRepo.save(v);
        });
        thiesRepo.findById(id).ifPresent(v -> {
            v.markAsDeleted();
            thiesRepo.save(v);
        });
        stlRepo.findById(id).ifPresent(v -> {
            v.markAsDeleted();
            stlRepo.save(v);
        });
    }
}
