package com.example.dms.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MonitoringService {

    // Statistiques de synchronisation
    private final AtomicInteger totalSyncs = new AtomicInteger(0);
    private final AtomicInteger successfulSyncs = new AtomicInteger(0);
    private final AtomicInteger failedSyncs = new AtomicInteger(0);
    private LocalDateTime lastSyncTime;
    private LocalDateTime lastSuccessfulSync;
    private Long lastSyncDuration;
    
    // Statistiques par région
    private final Map<String, AtomicInteger> regionErrors = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> regionLastAccess = new ConcurrentHashMap<>();
    
    // Statistiques d'opérations
    private final AtomicLong totalVentes = new AtomicLong(0);
    private final AtomicInteger ventesCrees = new AtomicInteger(0);
    private final AtomicInteger ventesModifiees = new AtomicInteger(0);
    private final AtomicInteger ventesSupprimees = new AtomicInteger(0);
    
    // Historique des synchronisations (dernières 10)
    private final List<SyncRecord> syncHistory = Collections.synchronizedList(new LinkedList<>());
    
    public MonitoringService() {
        regionErrors.put("Dakar", new AtomicInteger(0));
        regionErrors.put("Thies", new AtomicInteger(0));
        regionErrors.put("Saint-Louis", new AtomicInteger(0));
    }
    
    // 🔹 Enregistrer début de sync
    public long startSync() {
        totalSyncs.incrementAndGet();
        lastSyncTime = LocalDateTime.now();
        return System.currentTimeMillis();
    }
    
    // 🔹 Enregistrer fin de sync réussie
    public void endSyncSuccess(long startTime) {
        successfulSyncs.incrementAndGet();
        lastSuccessfulSync = LocalDateTime.now();
        lastSyncDuration = System.currentTimeMillis() - startTime;
        
        addSyncRecord(true, lastSyncDuration, null);
    }
    
    // 🔹 Enregistrer échec de sync
    public void endSyncFailure(long startTime, String error) {
        failedSyncs.incrementAndGet();
        lastSyncDuration = System.currentTimeMillis() - startTime;
        
        addSyncRecord(false, lastSyncDuration, error);
    }
    
    // 🔹 Enregistrer erreur par région
    public void recordRegionError(String region) {
        regionErrors.computeIfAbsent(region, r -> new AtomicInteger(0)).incrementAndGet();
    }
    
    // 🔹 Enregistrer accès réussi à une région
    public void recordRegionAccess(String region) {
        regionLastAccess.put(region, LocalDateTime.now());
    }
    
    // 🔹 Enregistrer opérations CRUD
    public void recordVenteCreated() {
        ventesCrees.incrementAndGet();
    }
    
    public void recordVenteUpdated() {
        ventesModifiees.incrementAndGet();
    }
    
    public void recordVenteDeleted() {
        ventesSupprimees.incrementAndGet();
    }
    
    public void setTotalVentes(long count) {
        totalVentes.set(count);
    }
    
    // 🔹 Ajouter un enregistrement d'historique
    private void addSyncRecord(boolean success, long duration, String error) {
        synchronized (syncHistory) {
            syncHistory.add(0, new SyncRecord(LocalDateTime.now(), success, duration, error));
            if (syncHistory.size() > 10) {
                syncHistory.remove(10);
            }
        }
    }
    
    // 🔹 Obtenir les statistiques complètes
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Stats de synchronisation
        stats.put("totalSyncs", totalSyncs.get());
        stats.put("successfulSyncs", successfulSyncs.get());
        stats.put("failedSyncs", failedSyncs.get());
        stats.put("successRate", calculateSuccessRate());
        stats.put("lastSyncTime", lastSyncTime);
        stats.put("lastSuccessfulSync", lastSuccessfulSync);
        stats.put("lastSyncDuration", lastSyncDuration);
        
        // Stats par région
        Map<String, Object> regionStats = new HashMap<>();
        for (String region : Arrays.asList("Dakar", "Thies", "Saint-Louis")) {
            Map<String, Object> regionInfo = new HashMap<>();
            regionInfo.put("errors", regionErrors.getOrDefault(region, new AtomicInteger(0)).get());
            regionInfo.put("lastAccess", regionLastAccess.get(region));
            regionInfo.put("status", getRegionStatus(region));
            regionStats.put(region, regionInfo);
        }
        stats.put("regions", regionStats);
        
        // Stats d'opérations
        stats.put("totalVentes", totalVentes.get());
        stats.put("ventesCrees", ventesCrees.get());
        stats.put("ventesModifiees", ventesModifiees.get());
        stats.put("ventesSupprimees", ventesSupprimees.get());
        
        // Historique
        stats.put("syncHistory", new ArrayList<>(syncHistory));
        
        return stats;
    }
    
    // 🔹 Calculer taux de succès
    private double calculateSuccessRate() {
        int total = totalSyncs.get();
        if (total == 0) return 0.0;
        return (successfulSyncs.get() * 100.0) / total;
    }
    
    // 🔹 Déterminer le statut d'une région
    private String getRegionStatus(String region) {
        LocalDateTime lastAccess = regionLastAccess.get(region);
        if (lastAccess == null) return "UNKNOWN";
        
        long minutesSinceAccess = java.time.Duration.between(lastAccess, LocalDateTime.now()).toMinutes();
        if (minutesSinceAccess < 2) return "ONLINE";
        if (minutesSinceAccess < 10) return "WARNING";
        return "OFFLINE";
    }
    
    // 🔹 Réinitialiser les statistiques
    public void resetStats() {
        totalSyncs.set(0);
        successfulSyncs.set(0);
        failedSyncs.set(0);
        regionErrors.values().forEach(counter -> counter.set(0));
        ventesCrees.set(0);
        ventesModifiees.set(0);
        ventesSupprimees.set(0);
        syncHistory.clear();
    }
    
    // Classe interne pour l'historique
    public static class SyncRecord {
        private final LocalDateTime timestamp;
        private final boolean success;
        private final long duration;
        private final String error;
        
        public SyncRecord(LocalDateTime timestamp, boolean success, long duration, String error) {
            this.timestamp = timestamp;
            this.success = success;
            this.duration = duration;
            this.error = error;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
        public long getDuration() { return duration; }
        public String getError() { return error; }
    }
}

