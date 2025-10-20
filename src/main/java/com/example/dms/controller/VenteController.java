// package com.example.dms.controller;

// import com.example.dms.model.Vente;
// import com.example.dms.service.MultiVenteService;
// import com.example.dms.service.SyncService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.List;

// @RestController
// @RequestMapping("/api/ventes")
// @CrossOrigin(origins = "*")
// public class VenteController {
    
//     @Autowired
//     private MultiVenteService multiVenteService;
    
//     @Autowired
//     private SyncService syncService;
    
//     /**
//      * Récupère toutes les ventes de tous les sites
//      */
//     @GetMapping
//     public ResponseEntity<List<Vente>> getAllVentes() {
//         List<Vente> ventes = multiVenteService.getAllVentes();
//         return ResponseEntity.ok(ventes);
//     }
    
//     /**
//      * Récupère les ventes d'un site spécifique
//      */
//     @GetMapping("/site/{site}")
//     public ResponseEntity<List<Vente>> getVentesBySite(@PathVariable String site) {
//         try {
//             List<Vente> ventes = multiVenteService.getVentesBySite(site);
//             return ResponseEntity.ok(ventes);
//         } catch (IllegalArgumentException e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }
    
//     /**
//      * Crée une nouvelle vente sur un site spécifique
//      */
//     @PostMapping("/site/{site}")
//     public ResponseEntity<Vente> createVente(@PathVariable String site, @RequestBody Vente vente) {
//         try {
//             vente.setSite(site);
//             vente.setDateVente(LocalDateTime.now());
//             Vente savedVente = multiVenteService.createVente(vente, site);
//             return ResponseEntity.status(HttpStatus.CREATED).body(savedVente);
//         } catch (IllegalArgumentException e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }
    
//     /**
//      * Récupère les ventes par période
//      */
//     @GetMapping("/period")
//     public ResponseEntity<List<Vente>> getVentesByPeriod(
//             @RequestParam String startDate, 
//             @RequestParam String endDate) {
//         LocalDateTime start = LocalDateTime.parse(startDate);
//         LocalDateTime end = LocalDateTime.parse(endDate);
//         List<Vente> ventes = multiVenteService.getVentesByPeriod(start, end);
//         return ResponseEntity.ok(ventes);
//     }
    
//     /**
//      * Récupère les ventes par client
//      */
//     @GetMapping("/client/{client}")
//     public ResponseEntity<List<Vente>> getVentesByClient(@PathVariable String client) {
//         List<Vente> ventes = multiVenteService.getVentesByClient(client);
//         return ResponseEntity.ok(ventes);
//     }
    
//     /**
//      * Récupère les ventes par produit
//      */
//     @GetMapping("/produit/{produit}")
//     public ResponseEntity<List<Vente>> getVentesByProduit(@PathVariable String produit) {
//         List<Vente> ventes = multiVenteService.getVentesByProduit(produit);
//         return ResponseEntity.ok(ventes);
//     }
    
//     /**
//      * Déclenche la synchronisation depuis un site spécifique
//      */
//     @PostMapping("/sync/{site}")
//     public ResponseEntity<String> syncFromSite(@PathVariable String site) {
//         try {
//             switch (site.toLowerCase()) {
//                 case "dakar":
//                     syncService.syncFromDakar();
//                     break;
//                 case "thies":
//                     syncService.syncFromThies();
//                     break;
//                 case "stl":
//                     syncService.syncFromStl();
//                     break;
//                 default:
//                     return ResponseEntity.badRequest().body("Site inconnu: " + site);
//             }
//             return ResponseEntity.ok("Synchronisation depuis " + site + " lancée avec succès");
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                     .body("Erreur lors de la synchronisation: " + e.getMessage());
//         }
//     }
    
//     /**
//      * Déclenche la synchronisation complète de tous les sites
//      */
//     @PostMapping("/sync/all")
//     public ResponseEntity<String> syncAll() {
//         try {
//             syncService.syncAll();
//             return ResponseEntity.ok("Synchronisation complète lancée avec succès");
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                     .body("Erreur lors de la synchronisation complète: " + e.getMessage());
//         }
//     }
// }

