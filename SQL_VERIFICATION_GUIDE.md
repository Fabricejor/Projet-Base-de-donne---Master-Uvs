# Guide SQL - Vérification des Tests Last-Write-Wins

## 🗄️ Requêtes de Vérification pour les Tests

Ce guide contient les requêtes SQL utiles pour vérifier directement dans les bases de données le comportement du système pendant les tests.

---

## 🔌 Connexion aux Bases de Données

### PostgreSQL (exemple)

```bash
# Base Dakar
psql -h localhost -p 5432 -U postgres -d db_dakar

# Base Thies
psql -h localhost -p 5433 -U postgres -d db_thies

# Base Saint-Louis
psql -h localhost -p 5434 -U postgres -d db_stl
```

---

## 📊 Requêtes de Consultation

### 1. Voir Toutes les Ventes (y compris supprimées)

```sql
SELECT 
    id,
    produit,
    montant,
    region,
    date_vente,
    updated_at,
    deleted,
    deleted_at
FROM vente
ORDER BY updated_at DESC;
```

### 2. Voir Uniquement les Ventes Actives (non supprimées)

```sql
SELECT 
    id,
    produit,
    montant,
    region,
    date_vente,
    updated_at
FROM vente
WHERE deleted = false OR deleted IS NULL
ORDER BY updated_at DESC;
```

### 3. Voir Uniquement les Ventes Supprimées (Tombstones)

```sql
SELECT 
    id,
    produit,
    montant,
    region,
    deleted_at,
    updated_at
FROM vente
WHERE deleted = true
ORDER BY deleted_at DESC;
```

### 4. Comparer une Vente Spécifique dans Toutes les Bases

```sql
-- Remplacer 'VOTRE-UUID-ICI' par l'ID réel
-- Exécuter dans chaque base (Dakar, Thies, StL)

SELECT 
    region,
    produit,
    montant,
    updated_at,
    deleted,
    deleted_at
FROM vente
WHERE id = 'VOTRE-UUID-ICI';
```

### 5. Voir les Ventes Récemment Modifiées (dernières 5 minutes)

```sql
SELECT 
    id,
    produit,
    montant,
    region,
    updated_at,
    deleted
FROM vente
WHERE updated_at > NOW() - INTERVAL '5 minutes'
ORDER BY updated_at DESC;
```

---

## 🧪 Vérifications pour Scénarios de Test

### Scénario 1 : Vérifier Modification Simple

**Après modification dans Thies, avant sync :**

```sql
-- Dans base Dakar
SELECT produit, montant, updated_at FROM vente WHERE id = 'UUID';
-- Montant devrait être l'ancien, updated_at ancien

-- Dans base Thies
SELECT produit, montant, updated_at FROM vente WHERE id = 'UUID';
-- Montant devrait être le nouveau, updated_at récent

-- Dans base StL
SELECT produit, montant, updated_at FROM vente WHERE id = 'UUID';
-- Montant devrait être l'ancien, updated_at ancien
```

**Après sync :**

```sql
-- Dans les 3 bases
SELECT produit, montant, updated_at FROM vente WHERE id = 'UUID';
-- Les 3 devraient avoir le même montant et updated_at
```

### Scénario 2 : Vérifier Conflit de Modification

**Voir quelle version gagne :**

```sql
-- Exécuter dans les 3 bases avant sync
SELECT region, montant, updated_at 
FROM vente 
WHERE id = 'UUID'
ORDER BY updated_at DESC;
```

**Identifier la plus récente :**

```sql
-- Cette requête simule ce que fait le code Java
SELECT 
    region,
    montant,
    updated_at,
    ROW_NUMBER() OVER (ORDER BY updated_at DESC) as rang
FROM (
    -- Simuler l'union des 3 bases
    SELECT 'Dakar' as region, montant, updated_at FROM vente WHERE id = 'UUID'
    UNION ALL
    SELECT 'Thies' as region, montant, updated_at FROM vente WHERE id = 'UUID'
    UNION ALL
    SELECT 'Saint-Louis' as region, montant, updated_at FROM vente WHERE id = 'UUID'
) combined
WHERE rang = 1;
```

### Scénario 3 : Vérifier Suppression

**Avant sync (après suppression dans Dakar) :**

```sql
-- Base Dakar
SELECT id, deleted, deleted_at, updated_at 
FROM vente 
WHERE id = 'UUID';
-- deleted = true, deleted_at et updated_at récents

-- Base Thies et StL
SELECT id, deleted, deleted_at, updated_at 
FROM vente 
WHERE id = 'UUID';
-- deleted = false ou NULL
```

**Après sync :**

```sql
-- Dans les 3 bases
SELECT id, deleted, deleted_at, updated_at 
FROM vente 
WHERE id = 'UUID';
-- deleted = true partout, même deleted_at et updated_at
```

---

## 🔍 Requêtes de Diagnostic

### Compter les Ventes par Statut

```sql
SELECT 
    CASE 
        WHEN deleted = true THEN 'Supprimée'
        ELSE 'Active'
    END as statut,
    COUNT(*) as nombre
FROM vente
GROUP BY deleted;
```

### Voir les Incohérences entre Bases (à exécuter après sync)

```sql
-- Cette requête nécessite dblink ou exécution manuelle dans chaque base
-- Comparer le nombre de ventes actives

-- Base Dakar
SELECT 'Dakar' as base, COUNT(*) as nb_ventes 
FROM vente 
WHERE deleted = false OR deleted IS NULL;

-- Base Thies
SELECT 'Thies' as base, COUNT(*) as nb_ventes 
FROM vente 
WHERE deleted = false OR deleted IS NULL;

-- Base StL
SELECT 'Saint-Louis' as base, COUNT(*) as nb_ventes 
FROM vente 
WHERE deleted = false OR deleted IS NULL;

-- Les 3 devraient avoir le même nombre après sync
```

### Historique des Modifications

```sql
SELECT 
    produit,
    montant,
    updated_at,
    deleted,
    CASE 
        WHEN deleted = true THEN 'Supprimée le ' || deleted_at::text
        ELSE 'Active'
    END as statut
FROM vente
ORDER BY updated_at DESC
LIMIT 20;
```

### Ventes avec Conflits Potentiels

```sql
-- Ventes modifiées récemment (possibles conflits)
SELECT 
    id,
    produit,
    region,
    updated_at,
    deleted
FROM vente
WHERE updated_at > NOW() - INTERVAL '10 minutes'
ORDER BY id, updated_at DESC;
```

---

## 🛠️ Requêtes de Maintenance

### Initialiser updated_at pour Anciennes Données

```sql
-- Si des ventes existantes n'ont pas de updated_at
UPDATE vente 
SET updated_at = date_vente::timestamp 
WHERE updated_at IS NULL;
```

### Initialiser deleted pour Anciennes Données

```sql
UPDATE vente 
SET deleted = false 
WHERE deleted IS NULL;
```

### Nettoyer les Tombstones Anciens (> 30 jours)

```sql
-- ATTENTION : Suppression physique définitive
DELETE FROM vente 
WHERE deleted = true 
AND deleted_at < NOW() - INTERVAL '30 days';
```

### Récupérer une Vente Supprimée par Erreur

```sql
-- "Ressusciter" une vente supprimée
UPDATE vente 
SET 
    deleted = false,
    deleted_at = NULL,
    updated_at = NOW()
WHERE id = 'UUID-A-RECUPERER';

-- Puis synchroniser pour propager aux autres bases
```

---

## 📈 Requêtes de Statistiques

### Statistiques Globales

```sql
SELECT 
    COUNT(*) as total_ventes,
    SUM(CASE WHEN deleted = true THEN 1 ELSE 0 END) as supprimees,
    SUM(CASE WHEN deleted = false OR deleted IS NULL THEN 1 ELSE 0 END) as actives,
    MAX(updated_at) as derniere_modification
FROM vente;
```

### Ventes par Région

```sql
SELECT 
    region,
    COUNT(*) as nb_ventes,
    SUM(montant) as total_montant,
    AVG(montant) as montant_moyen
FROM vente
WHERE deleted = false OR deleted IS NULL
GROUP BY region
ORDER BY total_montant DESC;
```

### Activité Récente (dernière heure)

```sql
SELECT 
    DATE_TRUNC('minute', updated_at) as minute,
    COUNT(*) as nb_operations
FROM vente
WHERE updated_at > NOW() - INTERVAL '1 hour'
GROUP BY DATE_TRUNC('minute', updated_at)
ORDER BY minute DESC;
```

---

## 🔧 Scripts de Test

### Script 1 : Créer Données de Test

```sql
-- À exécuter dans une seule base (sera synchronisé)
INSERT INTO vente (id, produit, montant, date_vente, region, updated_at, deleted)
VALUES 
    (gen_random_uuid(), 'Test Produit 1', 100000, CURRENT_DATE, 'Dakar', NOW(), false),
    (gen_random_uuid(), 'Test Produit 2', 200000, CURRENT_DATE, 'Dakar', NOW(), false),
    (gen_random_uuid(), 'Test Produit 3', 300000, CURRENT_DATE, 'Dakar', NOW(), false);
```

### Script 2 : Simuler un Conflit

```sql
-- 1. Créer une vente avec UUID fixe
INSERT INTO vente (id, produit, montant, date_vente, region, updated_at, deleted)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'Produit Test', 100000, CURRENT_DATE, 'Dakar', NOW(), false);

-- 2. Dans base Dakar - modifier immédiatement
UPDATE vente 
SET montant = 150000, updated_at = NOW() 
WHERE id = '550e8400-e29b-41d4-a716-446655440000';

-- 3. Attendre 5 secondes, puis dans base Thies
UPDATE vente 
SET montant = 200000, updated_at = NOW() 
WHERE id = '550e8400-e29b-41d4-a716-446655440000';

-- 4. Synchroniser et vérifier
```

---

## 📝 Notes de Débogage

### Vérifier que JPA a créé les colonnes

```sql
-- PostgreSQL
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'vente'
ORDER BY ordinal_position;
```

### Voir les Timestamps avec Précision

```sql
SELECT 
    id,
    produit,
    updated_at::timestamp(6) as updated_at_precise,
    deleted_at::timestamp(6) as deleted_at_precise
FROM vente
ORDER BY updated_at DESC
LIMIT 10;
```

### Différence de Temps entre Modifications

```sql
SELECT 
    id,
    produit,
    updated_at,
    LAG(updated_at) OVER (PARTITION BY id ORDER BY updated_at) as previous_update,
    updated_at - LAG(updated_at) OVER (PARTITION BY id ORDER BY updated_at) as time_diff
FROM vente
ORDER BY id, updated_at DESC;
```

---

## ⚠️ Précautions

1. **NEVER** : Ne jamais modifier manuellement `updated_at` en production
2. **Backup** : Toujours sauvegarder avant les opérations de nettoyage
3. **Sync** : Après modification manuelle, toujours lancer une synchronisation
4. **Test** : Tester toutes les requêtes sur données de test d'abord

---

## 🎯 Checklist de Vérification Post-Sync

```sql
-- 1. Même nombre de ventes dans les 3 bases
-- 2. Même updated_at pour chaque UUID
-- 3. Même statut deleted pour chaque UUID
-- 4. Même montant pour chaque UUID

-- Requête de vérification globale
SELECT 
    id,
    COUNT(DISTINCT montant) as versions_montant,
    COUNT(DISTINCT updated_at) as versions_timestamp,
    COUNT(DISTINCT deleted) as versions_statut
FROM (
    -- Union manuelle des 3 bases
    SELECT * FROM vente
) combined
GROUP BY id
HAVING 
    COUNT(DISTINCT montant) > 1 
    OR COUNT(DISTINCT updated_at) > 1 
    OR COUNT(DISTINCT deleted) > 1;

-- Si cette requête retourne des résultats, il y a incohérence
```

---

**📌 Conseil** : Garder ce fichier ouvert dans un terminal pendant les tests pour vérification en temps réel !

