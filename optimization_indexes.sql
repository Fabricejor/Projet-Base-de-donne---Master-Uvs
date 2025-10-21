-- Script d'Optimisation - Création d'Index sur la Table Vente
-- À exécuter dans CHAQUE base de données : ventes_dakar, ventes_thies, ventes_stlouis

-- ===============================================
-- INDEX POUR OPTIMISATION DES PERFORMANCES
-- ===============================================

-- 1. Index sur updated_at (utilisé pour Last-Write-Wins)
--    Accélère la comparaison des timestamps lors de la synchronisation
CREATE INDEX IF NOT EXISTS idx_vente_updated_at ON vente(updated_at DESC);
COMMENT ON INDEX idx_vente_updated_at IS 'Index pour optimiser les comparaisons Last-Write-Wins';

-- 2. Index sur deleted (filtre ventes actives/supprimées)
--    Accélère les requêtes WHERE deleted = false
CREATE INDEX IF NOT EXISTS idx_vente_deleted ON vente(deleted) WHERE deleted = false;
COMMENT ON INDEX idx_vente_deleted IS 'Index partiel pour ventes non supprimées';

-- 3. Index sur region (filtre par région)
--    Accélère les requêtes par région
CREATE INDEX IF NOT EXISTS idx_vente_region ON vente(region);
COMMENT ON INDEX idx_vente_region IS 'Index pour recherche par région';

-- 4. Index sur date_vente (filtres temporels, statistiques)
--    Accélère les requêtes de statistiques par période
CREATE INDEX IF NOT EXISTS idx_vente_date_vente ON vente(date_vente DESC);
COMMENT ON INDEX idx_vente_date_vente IS 'Index pour requêtes par date de vente';

-- 5. Index composite pour synchronisation efficace
--    Combine deleted et updated_at pour optimiser la sync
CREATE INDEX IF NOT EXISTS idx_vente_sync ON vente(deleted, updated_at DESC);
COMMENT ON INDEX idx_vente_sync IS 'Index composite pour synchronisation';

-- 6. Index sur produit (recherches textuelles)
--    Accélère les recherches par nom de produit
CREATE INDEX IF NOT EXISTS idx_vente_produit ON vente(produit);
COMMENT ON INDEX idx_vente_produit IS 'Index pour recherche par produit';

-- ===============================================
-- ANALYSE ET STATISTIQUES DE LA TABLE
-- ===============================================

-- Mise à jour des statistiques pour l'optimiseur de requêtes
ANALYZE vente;

-- ===============================================
-- VÉRIFICATION DES INDEX CRÉÉS
-- ===============================================

-- Voir tous les index sur la table vente
SELECT 
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'vente'
ORDER BY indexname;

-- ===============================================
-- STATISTIQUES D'UTILISATION DES INDEX
-- ===============================================

-- Afficher les statistiques d'utilisation des index
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as "Index Scans",
    idx_tup_read as "Tuples Read",
    idx_tup_fetch as "Tuples Fetched"
FROM pg_stat_user_indexes
WHERE tablename = 'vente'
ORDER BY idx_scan DESC;

-- ===============================================
-- TAILLE DES INDEX
-- ===============================================

-- Afficher la taille de chaque index
SELECT 
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as "Size"
FROM pg_indexes
WHERE tablename = 'vente';

-- ===============================================
-- OPTIMISATIONS SUPPLÉMENTAIRES
-- ===============================================

-- Activer l'auto-vacuum pour maintenir les performances
ALTER TABLE vente SET (autovacuum_enabled = true);

-- Configurer les statistiques pour l'optimiseur
ALTER TABLE vente SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE vente SET (autovacuum_analyze_scale_factor = 0.05);

-- ===============================================
-- NOTES ET RECOMMANDATIONS
-- ===============================================

/*
IMPACT DES INDEX :

1. idx_vente_updated_at :
   - Utilisé par : SyncService lors de la comparaison Last-Write-Wins
   - Gain estimé : 70-90% sur les requêtes de synchronisation
   
2. idx_vente_deleted :
   - Utilisé par : findAllFromAllRegions() pour filtrer les ventes actives
   - Gain estimé : 50-80% sur les lectures
   - Index partiel (prend moins de place)
   
3. idx_vente_region :
   - Utilisé par : Filtres par région dans l'interface
   - Gain estimé : 60-85% sur les filtres régionaux
   
4. idx_vente_date_vente :
   - Utilisé par : Statistiques et rapports temporels
   - Gain estimé : 50-70% sur les agrégations par date
   
5. idx_vente_sync :
   - Utilisé par : Synchronisation (filtre deleted + tri updated_at)
   - Gain estimé : 80-95% sur les opérations de sync complexes
   
6. idx_vente_produit :
   - Utilisé par : Recherches de produits spécifiques
   - Gain estimé : 40-60% sur les recherches textuelles

MAINTENANCE :

- Les index sont mis à jour automatiquement lors des INSERT/UPDATE/DELETE
- Auto-vacuum maintient les statistiques à jour
- Exécuter ANALYZE vente régulièrement (ou laisser autovacuum gérer)
- Surveiller l'utilisation avec pg_stat_user_indexes

COÛT EN ESPACE :

- Chaque index occupe de l'espace disque
- Compromis : Performances en lecture vs Espace disque
- Les index peuvent ralentir légèrement les écritures (INSERT/UPDATE)
- Pour ce système distribué, le gain en sync compense largement le coût

MONITORING :

-- Requête pour voir les index inutilisés (à exécuter après quelques jours)
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE tablename = 'vente'
AND idx_scan = 0
ORDER BY indexname;

-- Supprimer un index inutilisé (si nécessaire)
-- DROP INDEX IF EXISTS nom_index;
*/

