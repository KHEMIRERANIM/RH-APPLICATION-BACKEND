package tn.esprit.rh_rse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Menu;
import tn.esprit.rh_rse.entity.Plat;
import tn.esprit.rh_rse.service.MenuService;

import java.util.List;

@RestController
@RequestMapping("/api/menus")

@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<List<Menu>> getAll() {
        return ResponseEntity.ok(menuService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Menu> getById(@PathVariable String id) {
        return ResponseEntity.ok(menuService.getById(id));
    }

    @GetMapping("/publies")
    public ResponseEntity<List<Menu>> getMenusPublies() {
        return ResponseEntity.ok(menuService.getMenusPublies());
    }

    @PostMapping
    public ResponseEntity<Menu> create(@Valid @RequestBody Menu menu) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.save(menu));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Menu> update(@PathVariable String id,
                                       @Valid @RequestBody Menu menu) {
        return ResponseEntity.ok(menuService.update(id, menu));
    }

    // ✅ AJOUTER PLAT
    @PostMapping("/{menuId}/plats")
    public ResponseEntity<Menu> ajouterPlat(@PathVariable String menuId,
                                            @Valid @RequestBody Plat plat) {
        return ResponseEntity.ok(menuService.ajouterPlat(menuId, plat));
    }

    // ✅ NOUVEAU : UPDATE PLAT (CORRECTION)
    @PutMapping("/{menuId}/plats/{platId}")
    public ResponseEntity<Menu> updatePlat(@PathVariable String menuId,
                                           @PathVariable String platId,
                                           @Valid @RequestBody Plat plat) {
        return ResponseEntity.ok(menuService.updatePlat(menuId, platId, plat));
    }

    // ✅ SUPPRIMER PLAT
    @DeleteMapping("/{menuId}/plats/{platId}")
    public ResponseEntity<Menu> supprimerPlat(@PathVariable String menuId,
                                              @PathVariable String platId) {
        return ResponseEntity.ok(menuService.supprimerPlat(menuId, platId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --------- Nouveaux endpoints ---------
    @GetMapping("/{id}/plats/regime")
    public ResponseEntity<List<Plat>> getPlatsByRegime(@PathVariable String id,
                                                       @RequestParam String regime) {
        return ResponseEntity.ok(menuService.getPlatsByRegime(id, regime));
    }

    @GetMapping("/suggestion/{userId}")
    public ResponseEntity<List<Menu>> getMenusSuggestion(@PathVariable String userId) {
        return ResponseEntity.ok(menuService.getMenusSuggestion(userId));
    }
}