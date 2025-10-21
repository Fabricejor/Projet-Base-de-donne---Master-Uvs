# Guide de Test - Scénario Last-Write-Wins

## 📋 Nouvelles Fonctionnalités Implémentées

### 1. **Modification de Ventes**
- Chaque vente peut être modifiée depuis l'interface web
- La modification est horodatée automatiquement avec `updatedAt`
- La modification se fait dans une région spécifique

### 2. **Suppression de Ventes**
- Suppression "soft delete" avec système de tombstones
- Les ventes supprimées sont marquées avec `deleted = true` et `deletedAt`
- La suppression est propagée lors de la synchronisation

### 3. **Synchronisation Last-Write-Wins Améliorée**
- Le système compare les timestamps `updatedAt`
- La version la plus récente est toujours conservée
- Les suppressions sont propagées si elles sont plus récentes que les modifications

---

## 🧪 Scénarios de Test

### **Scénario 1 : Test de Modification Simple**

#### Étapes :
1. Ajouter une vente dans **Dakar** (ex: Produit="Ordinateur", Montant=500000, Date=aujourd'hui)
2. Cliquer sur **"🔄 Synchroniser maintenant"**
3. Vérifier que la vente apparaît 3 fois (une dans chaque région)
4. Noter l'heure de "Dernière MAJ" pour chaque entrée
5. Modifier la vente dans **Thies** (changer Montant à 550000)
6. Observer que l'heure de "Dernière MAJ" est mise à jour pour Thies
7. Cliquer sur **"🔄 Synchroniser maintenant"**
8. **Résultat attendu** : Les 3 ventes affichent maintenant Montant=550000 avec la même heure de MAJ (celle de Thies)

---

### **Scénario 2 : Test de Conflit de Modification (Last-Write-Wins)**

#### Étapes :
1. Ajouter une vente dans **Dakar** (ex: Produit="Téléphone", Montant=200000)
2. Synchroniser
3. **SANS synchroniser**, modifier rapidement :
   - Dans **Dakar** : Montant=250000
   - Attendre 2-3 secondes
   - Dans **Thies** : Montant=300000
4. Cliquer sur **"🔄 Synchroniser maintenant"**
5. **Résultat attendu** : La dernière modification (Thies = 300000) gagne et s'applique partout

#### Vérification :
- Toutes les 3 entrées doivent afficher Montant=300000
- L'heure de "Dernière MAJ" doit être celle de la modification dans Thies

---

### **Scénario 3 : Test de Suppression Simple**

#### Étapes :
1. Ajouter une vente dans **Saint-Louis** (ex: Produit="Tablette", Montant=150000)
2. Synchroniser (la vente apparaît 3 fois)
3. Supprimer la vente depuis **Dakar** (cliquer sur 🗑️ Supprimer)
4. **Observation** : La vente Dakar disparaît immédiatement de l'affichage
5. Cliquer sur **"🔄 Synchroniser maintenant"**
6. **Résultat attendu** : Les 3 ventes disparaissent (suppression propagée)

---

### **Scénario 4 : Conflit Modification vs Suppression**

#### Étapes :
1. Ajouter une vente dans **Dakar** (ex: Produit="Souris", Montant=10000)
2. Synchroniser
3. **SANS synchroniser** :
   - Modifier dans **Thies** (Montant=15000)
   - Attendre 2-3 secondes
   - Supprimer dans **Saint-Louis**
4. Synchroniser
5. **Résultat attendu** : La suppression gagne si elle est la plus récente, sinon la modification gagne

#### Cas A : Suppression plus récente
- **Résultat** : Toutes les ventes disparaissent

#### Cas B : Modification plus récente
- Inverser l'ordre (supprimer d'abord, modifier ensuite)
- **Résultat** : La vente réapparaît avec Montant=15000 partout

---

### **Scénario 5 : Modifications Concurrentes Multiples**

#### Étapes :
1. Ajouter une vente dans **Dakar** (Produit="Clavier", Montant=25000)
2. Synchroniser
3. **SANS synchroniser**, effectuer dans l'ordre avec quelques secondes d'intervalle :
   - T+0s : Modifier Dakar → Montant=30000
   - T+3s : Modifier Thies → Montant=35000
   - T+6s : Modifier Saint-Louis → Montant=40000
4. Synchroniser
5. **Résultat attendu** : Montant=40000 partout (dernière modification)

---

## 🔍 Points de Vérification

### ✅ Checklist de Test

- [ ] Les modifications mettent à jour automatiquement `updatedAt`
- [ ] La synchronisation propage la version avec le `updatedAt` le plus récent
- [ ] Les suppressions marquent `deleted=true` et `deletedAt`
- [ ] Les ventes supprimées ne s'affichent plus dans l'interface
- [ ] Les suppressions sont propagées lors de la sync
- [ ] En cas de conflit, la dernière opération (modification ou suppression) gagne
- [ ] Le champ "Dernière MAJ" reflète correctement l'heure de la dernière opération

---

## 🛠️ Architecture Technique

### Mécanisme Last-Write-Wins

```java
// Dans SyncService.java
Vente latest = Stream.of(vD, vT, vS)
    .filter(Objects::nonNull)
    .max(Comparator.comparing(Vente::getUpdatedAt))
    .orElse(null);
```

- Compare les timestamps `updatedAt` de toutes les versions
- Sélectionne celle avec le timestamp le plus récent
- Propage cette version à toutes les bases

### Système de Tombstones

```java
// Dans Vente.java
public void markAsDeleted() {
    this.deleted = true;
    this.deletedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now(); // Mise à jour du timestamp
}
```

- Suppression logique (soft delete)
- Garde une trace de la suppression avec timestamp
- Permet de propager la suppression lors de la sync

---

## 📊 Base de Données

### Nouvelles Colonnes Ajoutées

| Colonne      | Type          | Description                           |
|--------------|---------------|---------------------------------------|
| `updated_at` | TIMESTAMP     | Date/heure de dernière modification  |
| `deleted`    | BOOLEAN       | Indicateur de suppression             |
| `deleted_at` | TIMESTAMP     | Date/heure de suppression             |

---

## 🚀 Lancement de l'Application

```bash
# Compiler et lancer
mvn spring-boot:run

# Accéder à l'interface
http://localhost:8080
```

---

## 📝 Notes Importantes

1. **Intervalle de Synchronisation** : Par défaut configuré dans `application.yml`
2. **Synchronisation Manuelle** : Bouton "🔄 Synchroniser maintenant" pour tests
3. **Timestamps Automatiques** : Gérés par JPA avec `@PrePersist` et `@PreUpdate`
4. **Interface Web** : Modal moderne pour modifier les ventes

---

## 🎯 Objectif du Test

Démontrer que le système gère correctement :
- ✅ Les conflits de modifications concurrentes
- ✅ La résolution Last-Write-Wins basée sur les timestamps
- ✅ La propagation des suppressions
- ✅ Les conflits entre modifications et suppressions
- ✅ La cohérence éventuelle des données entre toutes les régions

