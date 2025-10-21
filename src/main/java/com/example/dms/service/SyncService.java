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
    private final MonitoringService monitoring;

    public SyncService(MultiVenteService multi, MonitoringService monitoring) {
        this.multi = multi;
        this.monitoring = monitoring;
    }

    // 🔹 Synchronisation automatique toutes les 60 s
    @Scheduled(fixedDelayString = "${sync.interval}")
    public void synchronize() {
        long startTime = monitoring.startSync();
        System.out.println("⏳ Synchronisation en cours...");
        
        try {
            // 1️⃣ Récupérer toutes les ventes de chaque base avec gestion d'erreurs
            Map<UUID, Vente> dakar = new HashMap<>();
            Map<UUID, Vente> thies = new HashMap<>();
            Map<UUID, Vente> stl = new HashMap<>();
            
            boolean hasErrors = false;
            
            // Récupération Dakar avec gestion d'erreur
            try {
                dakar = multi.findAllFromDakar().stream()
                        .collect(Collectors.toMap(Vente::getId, v -> v));
                monitoring.recordRegionAccess("Dakar");
                System.out.println("✅ Dakar : " + dakar.size() + " ventes récupérées");
            } catch (Exception e) {
                System.err.println("❌ Erreur connexion à Dakar : " + e.getMessage());
                monitoring.recordRegionError("Dakar");
                hasErrors = true;
            }
            
            // Récupération Thies avec gestion d'erreur
            try {
                thies = multi.findAllFromThies().stream()
                        .collect(Collectors.toMap(Vente::getId, v -> v));
                monitoring.recordRegionAccess("Thies");
                System.out.println("✅ Thies : " + thies.size() + " ventes récupérées");
            } catch (Exception e) {
                System.err.println("❌ Erreur connexion à Thies : " + e.getMessage());
                monitoring.recordRegionError("Thies");
                hasErrors = true;
            }
            
            // Récupération Saint-Louis avec gestion d'erreur
            try {
                stl = multi.findAllFromStl().stream()
                        .collect(Collectors.toMap(Vente::getId, v -> v));
                monitoring.recordRegionAccess("Saint-Louis");
                System.out.println("✅ Saint-Louis : " + stl.size() + " ventes récupérées");
            } catch (Exception e) {
                System.err.println("❌ Erreur connexion à Saint-Louis : " + e.getMessage());
                monitoring.recordRegionError("Saint-Louis");
                hasErrors = true;
            }

            // 2️⃣ Union de tous les IDs
            Set<UUID> allIds = new HashSet<>();
            allIds.addAll(dakar.keySet());
            allIds.addAll(thies.keySet());
            allIds.addAll(stl.keySet());
            
            System.out.println("📊 Total unique IDs : " + allIds.size());

            // 3️⃣ Pour chaque vente, choisir la plus récente (last-write-wins)
            int propagated = 0;
            for (UUID id : allIds) {
                Vente vD = dakar.get(id);
                Vente vT = thies.get(id);
                Vente vS = stl.get(id);

                // Trouver la version la plus récente
                Vente latest = Stream.of(vD, vT, vS)
                        .filter(Objects::nonNull)
                        .max(Comparator.comparing(Vente::getUpdatedAt))
                        .orElse(null);

                if (latest == null) continue;

                // 🔁 Propager la version la plus récente dans les autres bases
                try {
                    if (vD == null || vD.getUpdatedAt().isBefore(latest.getUpdatedAt())) {
                        multi.saveToDakar(cloneForRegion(latest, "Dakar"));
                        propagated++;
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur propagation vers Dakar : " + e.getMessage());
                    hasErrors = true;
                }
                
                try {
                    if (vT == null || vT.getUpdatedAt().isBefore(latest.getUpdatedAt())) {
                        multi.saveToThies(cloneForRegion(latest, "Thies"));
                        propagated++;
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur propagation vers Thies : " + e.getMessage());
                    hasErrors = true;
                }
                
                try {
                    if (vS == null || vS.getUpdatedAt().isBefore(latest.getUpdatedAt())) {
                        multi.saveToStl(cloneForRegion(latest, "Saint-Louis"));
                        propagated++;
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur propagation vers Saint-Louis : " + e.getMessage());
                    hasErrors = true;
                }
            }
            
            System.out.println("📤 " + propagated + " ventes propagées");
            
            // Mettre à jour les statistiques
            monitoring.setTotalVentes(allIds.size());

            if (hasErrors) {
                monitoring.endSyncFailure(startTime, "Synchronisation partielle avec erreurs");
                System.out.println("⚠️ Synchronisation terminée avec erreurs !");
            } else {
                monitoring.endSyncSuccess(startTime);
                System.out.println("✅ Synchronisation terminée avec succès !");
            }
            
        } catch (Exception e) {
            monitoring.endSyncFailure(startTime, e.getMessage());
            System.err.println("❌ Erreur critique lors de la synchronisation : " + e.getMessage());
            e.printStackTrace();
        }
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
