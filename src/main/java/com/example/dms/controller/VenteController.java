package com.example.dms.controller;

import com.example.dms.model.Vente;
import com.example.dms.service.MultiVenteService;
import com.example.dms.service.SyncService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/sync")
    public String sync() {
        sync.manualSync();
        return "redirect:/";
    }
}
