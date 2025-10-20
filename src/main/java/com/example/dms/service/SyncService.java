package com.example.dms.service;

import com.example.dms.dakar.repository.VenteRepositoryDakar;
import com.example.dms.thies.repository.VenteRepositoryThies;
import com.example.dms.stl.repository.VenteRepositoryStl;
import com.example.dms.model.Vente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

@Service
public class SyncService {
    
    private static final Logger logger = Logger.getLogger(SyncService.class.getName());
    
    @Autowired
    private VenteRepositoryDakar venteRepositoryDakar;
    
    @Autowired
    private VenteRepositoryThies venteRepositoryThies;
    
    @Autowired
    private VenteRepositoryStl venteRepositoryStl;
    
    /**
     * Synchronise les données de Dakar vers les autres sites
     */
    @Transactional
    public void syncFromDakar() {
        logger.info("Début de la synchronisation depuis Dakar...");
        List<Vente> ventesDakar = venteRepositoryDakar.findAll();
        
        // Synchroniser vers Thiès
        syncVentesToSite(ventesDakar, "thies");
        
        // Synchroniser vers STL
        syncVentesToSite(ventesDakar, "stl");
        
        logger.info("Synchronisation depuis Dakar terminée.");
    }
    
    /**
     * Synchronise les données de Thiès vers les autres sites
     */
    @Transactional
    public void syncFromThies() {
        logger.info("Début de la synchronisation depuis Thiès...");
        List<Vente> ventesThies = venteRepositoryThies.findAll();
        
        // Synchroniser vers Dakar
        syncVentesToSite(ventesThies, "dakar");
        
        // Synchroniser vers STL
        syncVentesToSite(ventesThies, "stl");
        
        logger.info("Synchronisation depuis Thiès terminée.");
    }
    
    /**
     * Synchronise les données de STL vers les autres sites
     */
    @Transactional
    public void syncFromStl() {
        logger.info("Début de la synchronisation depuis STL...");
        List<Vente> ventesStl = venteRepositoryStl.findAll();
        
        // Synchroniser vers Dakar
        syncVentesToSite(ventesStl, "dakar");
        
        // Synchroniser vers Thiès
        syncVentesToSite(ventesStl, "thies");
        
        logger.info("Synchronisation depuis STL terminée.");
    }
    
    /**
     * Synchronise toutes les données entre tous les sites
     */
    @Transactional
    public void syncAll() {
        logger.info("Début de la synchronisation complète...");
        syncFromDakar();
        syncFromThies();
        syncFromStl();
        logger.info("Synchronisation complète terminée.");
    }
    
    /**
     * Synchronisation automatique programmée (toutes les heures)
     */
    @Scheduled(fixedRate = 3600000) // Toutes les heures
    public void scheduledSync() {
        logger.info("Exécution de la synchronisation programmée...");
        syncAll();
    }
    
    /**
     * Méthode privée pour synchroniser les ventes vers un site spécifique
     */
    private void syncVentesToSite(List<Vente> ventes, String targetSite) {
        for (Vente vente : ventes) {
            try {
                switch (targetSite.toLowerCase()) {
                    case "dakar":
                        if (!venteRepositoryDakar.existsById(vente.getId())) {
                            venteRepositoryDakar.save(vente);
                        }
                        break;
                    case "thies":
                        if (!venteRepositoryThies.existsById(vente.getId())) {
                            venteRepositoryThies.save(vente);
                        }
                        break;
                    case "stl":
                        if (!venteRepositoryStl.existsById(vente.getId())) {
                            venteRepositoryStl.save(vente);
                        }
                        break;
                    default:
                        logger.warning("Site inconnu: " + targetSite);
                }
            } catch (Exception e) {
                logger.severe("Erreur lors de la synchronisation de la vente " + vente.getId() + 
                             " vers " + targetSite + ": " + e.getMessage());
            }
        }
    }
}

