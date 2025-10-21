package com.example.dms.controller;

import com.example.dms.model.Vente;
import com.example.dms.service.MultiVenteService;
import com.example.dms.service.SyncService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
public class VenteController {

    private final MultiVenteService multi;
    private final SyncService sync;

    public VenteController(MultiVenteService multi, SyncService sync) {
        this.multi = multi;
        this.sync = sync;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("ventes", multi.findAllFromAllRegions());
        model.addAttribute("vente", new Vente());
        return "index";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Vente vente, @RequestParam String region) {
        switch (region) {
            case "Dakar" -> multi.saveToDakar(vente);
            case "Thies" -> multi.saveToThies(vente);
            case "Saint-Louis" -> multi.saveToStl(vente);
        }
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
        return "redirect:/";
    }

    @PostMapping("/sync")
    public String sync() {
        sync.manualSync();
        return "redirect:/";
    }
}
