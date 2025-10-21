-- Migration SQL pour ajouter les colonnes deleted et deleted_at
-- À exécuter dans CHAQUE base de données : ventes_dakar, ventes_thies, ventes_stlouis

-- 1. Ajouter la colonne deleted (false par défaut)
ALTER TABLE vente ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;

-- 2. Ajouter la colonne deleted_at (nullable)
ALTER TABLE vente ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 3. Initialiser deleted à false pour les données existantes (si NULL)
UPDATE vente SET deleted = false WHERE deleted IS NULL;

-- 4. Vérification - afficher la structure de la table
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'vente'
ORDER BY ordinal_position;

