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

    // 🔹 Récupérer toutes les ventes de toutes les régions
    public List<Vente> findAllFromAllRegions() {
        List<Vente> all = new ArrayList<>();
        all.addAll(dakarRepo.findAll());
        all.addAll(thiesRepo.findAll());
        all.addAll(stlRepo.findAll());
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

    // 🔹 Supprimer dans toutes les bases
    @Transactional
    public void deleteById(UUID id) {
        dakarRepo.deleteById(id);
        thiesRepo.deleteById(id);
        stlRepo.deleteById(id);
    }
}
