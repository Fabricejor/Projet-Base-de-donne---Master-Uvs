# 🔧 Guide de Test - Simulation de Panne de Base de Données

## 🎯 Objectif du Scénario

Tester la résilience du système distribué en simulant :
1. La panne d'une base de données (ex: Thies)
2. Le fonctionnement du système avec 2 bases sur 3
3. La récupération automatique après redémarrage de la base

---

## 📋 Prérequis

- Application Spring Boot en cours d'exécution
- Les 3 bases de données PostgreSQL actives
- Accès administrateur sur PostgreSQL

---

## 🧪 Scénario Complet de Test

### Phase 1 : État Initial (Système Sain)

#### 1.1 Ajouter des données de test

1. Accéder à http://localhost:8080
2. Ajouter une vente dans **Dakar** :
   - Produit : "Ordinateur HP"
   - Montant : 500000
   - Date : aujourd'hui
3. Cliquer sur **🔄 Synchroniser**
4. Vérifier que la vente apparaît **3 fois** (une dans chaque région)

#### 1.2 Vérification dans les bases

```sql
-- Dans chaque base (Dakar, Thies, StL)
SELECT id, produit, montant, region, updated_at 
FROM vente 
WHERE produit = 'Ordinateur HP';

-- Résultat attendu : 1 ligne dans chaque base avec le même ID
```

---

### Phase 2 : Simulation de Panne (Arrêt d'une Base)

#### 2.1 Identifier le service PostgreSQL de la base Thies

**Sur Windows :**

```powershell
# Voir tous les services PostgreSQL
Get-Service | Where-Object {$_.Name -like "*postgres*"}

# Ou via services.msc
services.msc
```

**Identifier le service correspondant à la base Thies** (dépend de votre installation) :
- Si installation séparée : `postgresql-x64-14-thies` (ou similaire)
- Si même instance PostgreSQL : voir méthode alternative ci-dessous

#### 2.2 Méthode 1 : Arrêter le service PostgreSQL (si instances séparées)

**PowerShell (Administrateur) :**

```powershell
# Arrêter le service de la base Thies
Stop-Service -Name "postgresql-x64-14-thies"

# Vérifier l'arrêt
Get-Service -Name "postgresql-x64-14-thies"
```

#### 2.3 Méthode 2 : Bloquer la connexion (si même instance PostgreSQL)

Si les 3 bases sont sur la même instance PostgreSQL, bloquez l'accès à la base :

**Option A - Via pg_hba.conf :**

1. Éditer `pg_hba.conf` :
```bash
# Ajouter cette ligne en haut du fichier
host    ventes_thies    all    127.0.0.1/32    reject
```

2. Recharger PostgreSQL :
```powershell
# PowerShell (Administrateur)
Restart-Service postgresql-x64-14
```

**Option B - Supprimer temporairement la base :**

```sql
-- Se connecter en tant que postgres
DROP DATABASE ventes_thies;
```

**Option C - Fermer toutes les connexions et renommer :**

```sql
-- Se connecter à postgres (pas à ventes_thies)
-- Terminer toutes les connexions
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'ventes_thies';

-- Renommer la base (simuler qu'elle est inaccessible)
ALTER DATABASE ventes_thies RENAME TO ventes_thies_DOWN;
```

---

### Phase 3 : Observation du Comportement (Base en Panne)

#### 3.1 Observer les logs de l'application

Dans la console Spring Boot, vous devriez voir des erreurs :

```
⏳ Synchronisation en cours...
WARN ... - Exception lors de l'accès à la base Thies
ERROR ... - Connection refused: ventes_thies
✅ Synchronisation terminée ! (avec erreurs)
```

#### 3.2 Tester l'ajout de ventes

**Cas 1 : Ajouter dans Dakar (base fonctionnelle)**

1. Ajouter une vente dans **Dakar** :
   - Produit : "Souris Logitech"
   - Montant : 15000
2. Cliquer sur **🔄 Synchroniser**
3. **Résultat attendu** :
   - ✅ Vente ajoutée dans Dakar
   - ✅ Vente synchronisée dans Saint-Louis
   - ❌ Erreur pour Thies (normal, base en panne)

**Cas 2 : Tenter d'ajouter dans Thies (base en panne)**

1. Essayer d'ajouter une vente dans **Thies**
2. **Résultat attendu** :
   - ❌ Erreur 500 (la base est inaccessible)

#### 3.3 Vérifier dans les bases actives

```sql
-- Base Dakar
SELECT produit, region FROM vente WHERE produit = 'Souris Logitech';
-- Résultat : 1 ligne (region = Dakar)

-- Base Saint-Louis
SELECT produit, region FROM vente WHERE produit = 'Souris Logitech';
-- Résultat : 1 ligne (region = Saint-Louis)

-- Base Thies : INACCESSIBLE (panne simulée)
```

---

### Phase 4 : Récupération (Redémarrage de la Base)

#### 4.1 Redémarrer le service PostgreSQL

**Méthode 1 - Si service arrêté :**

```powershell
# PowerShell (Administrateur)
Start-Service -Name "postgresql-x64-14-thies"

# Vérifier le démarrage
Get-Service -Name "postgresql-x64-14-thies"
```

**Méthode 2 - Si base renommée :**

```sql
-- Renommer la base pour la rendre accessible
ALTER DATABASE ventes_thies_DOWN RENAME TO ventes_thies;
```

**Méthode 3 - Si base supprimée, la recréer :**

```sql
-- Recréer la base
CREATE DATABASE ventes_thies OWNER dsms_user;

-- Se connecter et recréer la table
\c ventes_thies

CREATE TABLE vente (
    id UUID PRIMARY KEY,
    produit VARCHAR(255),
    montant DOUBLE PRECISION,
    date_vente DATE,
    region VARCHAR(100),
    updated_at TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP
);
```

#### 4.2 Vérifier l'état de la base après redémarrage

```sql
-- Se connecter à ventes_thies
SELECT * FROM vente;

-- Si la base était simplement arrêtée : données intactes
-- Si la base était supprimée/recréée : table vide
```

---

### Phase 5 : Synchronisation Automatique (Récupération des Données)

#### 5.1 Lancer une synchronisation manuelle

1. Dans l'interface web : **🔄 Synchroniser maintenant**
2. Observer les logs :

```
⏳ Synchronisation en cours...
[INFO] Connexion à ventes_dakar : OK
[INFO] Connexion à ventes_thies : OK ← Reconnectée !
[INFO] Connexion à ventes_stlouis : OK
✅ Synchronisation terminée !
```

#### 5.2 Vérifier la récupération des données

```sql
-- Base Thies (après sync)
SELECT produit, montant, region, updated_at 
FROM vente 
ORDER BY updated_at DESC;

-- Résultat attendu : toutes les ventes ajoutées pendant la panne
-- sont maintenant présentes dans Thies
```

**Exemple de résultat :**

| produit | montant | region | updated_at |
|---------|---------|--------|------------|
| Souris Logitech | 15000 | Thies | 2025-10-21 15:30:00 |
| Ordinateur HP | 500000 | Thies | 2025-10-21 15:00:00 |

---

## 📊 Vérifications Complètes

### Checklist de Validation

- [ ] Base en panne détectée dans les logs
- [ ] Application continue de fonctionner avec les 2 autres bases
- [ ] Données ajoutées pendant la panne sont dans les 2 bases actives
- [ ] Après redémarrage, la base se reconnecte automatiquement
- [ ] Synchronisation propage les données manquantes
- [ ] Toutes les bases ont les mêmes données après récupération

### Script SQL de Comparaison

```sql
-- Exécuter dans les 3 bases et comparer
SELECT 
    COUNT(*) as total_ventes,
    SUM(montant) as total_montant,
    MAX(updated_at) as derniere_maj
FROM vente
WHERE deleted = false;

-- Les 3 bases doivent avoir les mêmes résultats après sync
```

---

## 🔍 Comportements Attendus

### Pendant la Panne

| Action | Base Saine (Dakar/StL) | Base en Panne (Thies) |
|--------|------------------------|------------------------|
| Ajouter vente | ✅ Succès | ❌ Erreur 500 |
| Lire ventes | ✅ Données complètes | ❌ Inaccessible |
| Synchroniser | ⚠️ Erreur partielle | ❌ Échec |
| Modifier vente | ✅ Succès (si dans base saine) | ❌ Échec |

### Après Récupération

| Action | Résultat |
|--------|----------|
| Synchronisation auto | ✅ Rattrape les données manquantes |
| Ventes ajoutées pendant panne | ✅ Propagées à la base récupérée |
| Modifications pendant panne | ✅ Propagées selon Last-Write-Wins |
| Cohérence des données | ✅ Les 3 bases identiques |

---

## 💡 Améliorations Possibles du Code

### 1. Gestion des Erreurs dans SyncService

Actuellement, si une base est en panne, la sync peut échouer. Amélioration :

```java
@Scheduled(fixedDelayString = "${sync.interval}")
public void synchronize() {
    System.out.println("⏳ Synchronisation en cours...");
    
    Map<UUID, Vente> dakar = new HashMap<>();
    Map<UUID, Vente> thies = new HashMap<>();
    Map<UUID, Vente> stl = new HashMap<>();
    
    // Récupération avec gestion d'erreur
    try {
        dakar = multi.findAllFromDakar().stream()
            .collect(Collectors.toMap(Vente::getId, v -> v));
    } catch (Exception e) {
        System.err.println("❌ Erreur connexion à Dakar : " + e.getMessage());
    }
    
    try {
        thies = multi.findAllFromThies().stream()
            .collect(Collectors.toMap(Vente::getId, v -> v));
    } catch (Exception e) {
        System.err.println("❌ Erreur connexion à Thies : " + e.getMessage());
    }
    
    try {
        stl = multi.findAllFromStl().stream()
            .collect(Collectors.toMap(Vente::getId, v -> v));
    } catch (Exception e) {
        System.err.println("❌ Erreur connexion à Saint-Louis : " + e.getMessage());
    }
    
    // Continuer la sync avec les bases disponibles
    // ...
}
```

### 2. Retry Automatique

Configurer un retry automatique pour les bases temporairement indisponibles :

```yaml
spring:
  datasource:
    thies:
      hikari:
        connection-timeout: 5000
        maximum-pool-size: 5
        validation-timeout: 3000
```

---

## 🎯 Points Clés pour le Rapport

1. **Résilience** : Le système continue de fonctionner avec N-1 bases
2. **Auto-récupération** : Après redémarrage, synchronisation automatique
3. **Cohérence Éventuelle** : Les données finissent par converger
4. **Last-Write-Wins** : Les conflits pendant la panne sont résolus par timestamp

---

## 🆘 Troubleshooting

### Problème : La base ne se reconnecte pas après redémarrage

**Solution :**
1. Vérifier que le service PostgreSQL est bien démarré
2. Tester la connexion manuellement :
```bash
psql -h localhost -p 5432 -U dsms_user -d ventes_thies
```
3. Redémarrer l'application Spring Boot

### Problème : Les données ne se synchronisent pas après récupération

**Solution :**
1. Vérifier les logs de synchronisation
2. Lancer manuellement : **🔄 Synchroniser maintenant**
3. Vérifier que `sync.interval` est configuré dans `application.yml`

---

**Ce scénario démontre la robustesse de votre architecture distribuée ! 🎉**

