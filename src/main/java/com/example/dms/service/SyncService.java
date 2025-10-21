package com.example.dms.service;

import com.example.dms.model.Vente;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SyncService {

    private final MultiVenteService multi;

    public SyncService(MultiVenteService multi) {
        this.multi = multi;
    }

    // 🔹 Synchronisation automatique toutes les 60 s
    @Scheduled(fixedDelayString = "${sync.interval}")
    public void synchronize() {
        System.out.println("⏳ Synchronisation en cours...");

        // 1️⃣ Récupérer toutes les ventes de chaque base (y compris les supprimées pour les tombstones)
        Map<UUID, Vente> dakar = multi.findAllFromDakar().stream()
                .collect(Collectors.toMap(Vente::getId, v -> v));
        Map<UUID, Vente> thies = multi.findAllFromThies().stream()
                .collect(Collectors.toMap(Vente::getId, v -> v));
        Map<UUID, Vente> stl = multi.findAllFromStl().stream()
                .collect(Collectors.toMap(Vente::getId, v -> v));

        // 2️⃣ Union de tous les IDs
        Set<UUID> allIds = new HashSet<>();
        allIds.addAll(dakar.keySet());
        allIds.addAll(thies.keySet());
        allIds.addAll(stl.keySet());

        // 3️⃣ Pour chaque vente, choisir la plus récente (last-write-wins) avec gestion des tombstones
        for (UUID id : allIds) {
            Vente vD = dakar.get(id);
            Vente vT = thies.get(id);
            Vente vS = stl.get(id);

            // Trouver la version la plus récente (même si supprimée)
            Vente latest = Stream.of(vD, vT, vS)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Vente::getUpdatedAt))
                    .orElse(null);

            if (latest == null) continue;

            // 🔁 Propager la version la plus récente dans les autres bases
            // Si la version la plus récente est supprimée, propager la suppression
            if (vD == null || vD.getUpdatedAt().isBefore(latest.getUpdatedAt())) {
                multi.saveToDakar(cloneForRegion(latest, "Dakar"));
            }
            if (vT == null || vT.getUpdatedAt().isBefore(latest.getUpdatedAt())) {
                multi.saveToThies(cloneForRegion(latest, "Thies"));
            }
            if (vS == null || vS.getUpdatedAt().isBefore(latest.getUpdatedAt())) {
                multi.saveToStl(cloneForRegion(latest, "Saint-Louis"));
            }
        }

        System.out.println("✅ Synchronisation terminée !");
    }

    private Vente cloneForRegion(Vente source, String region) {
        Vente clone = new Vente();
        clone.setId(source.getId());
        clone.setDateVente(source.getDateVente());
        clone.setMontant(source.getMontant());
        clone.setProduit(source.getProduit());
        clone.setRegion(region);
        clone.setUpdatedAt(source.getUpdatedAt());
        // Copier aussi les informations de suppression (tombstone)
        clone.setDeleted(source.getDeleted());
        clone.setDeletedAt(source.getDeletedAt());
        return clone;
    }

    // 🔹 Pour lancer la synchro manuellement depuis le contrôleur
    public void manualSync() {
        synchronize();
    }
}
