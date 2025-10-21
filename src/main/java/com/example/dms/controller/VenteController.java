package com.example.dms.controller;

import com.example.dms.model.Vente;
import com.example.dms.service.MultiVenteService;
import com.example.dms.service.SyncService;
import com.example.dms.service.MonitoringService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
public class VenteController {

    private final MultiVenteService multi;
    private final SyncService sync;
    private final MonitoringService monitoring;

    public VenteController(MultiVenteService multi, SyncService sync, MonitoringService monitoring) {
        this.multi = multi;
        this.sync = sync;
        this.monitoring = monitoring;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("ventes", multi.findAllFromAllRegions());
        model.addAttribute("vente", new Vente());
        model.addAttribute("stats", monitoring.getStatistics());
        return "index";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Vente vente, @RequestParam String region) {
        switch (region) {
            case "Dakar" -> multi.saveToDakar(vente);
            case "Thies" -> multi.saveToThies(vente);
            case "Saint-Louis" -> multi.saveToStl(vente);
        }
        monitoring.recordVenteCreated();
        return "redirect:/";
    }

    @PostMapping("/update")
    public String update(@RequestParam String id, 
                        @ModelAttribute Vente vente, 
                        @RequestParam String region) {
        UUID uuid = UUID.fromString(id);
        switch (region) {
            case "Dakar" -> multi.updateInDakar(uuid, vente);
            case "Thies" -> multi.updateInThies(uuid, vente);
            case "Saint-Louis" -> multi.updateInStl(uuid, vente);
        }
        monitoring.recordVenteUpdated();
        return "redirect:/";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String id, @RequestParam String region) {
        UUID uuid = UUID.fromString(id);
        switch (region) {
            case "Dakar" -> multi.deleteFromDakar(uuid);
            case "Thies" -> multi.deleteFromThies(uuid);
            case "Saint-Louis" -> multi.deleteFromStl(uuid);
        }
        monitoring.recordVenteDeleted();
        return "redirect:/";
    }

    @PostMapping("/sync")
    public String sync() {
        sync.manualSync();
        return "redirect:/";
    }
    
    // 📊 Endpoint REST pour les statistiques (API JSON)
    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getStats() {
        return monitoring.getStatistics();
    }
    
    // 📊 Page dédiée aux statistiques
    @GetMapping("/stats")
    public String statsPage(Model model) {
        model.addAttribute("stats", monitoring.getStatistics());
        return "stats";
    }
}
