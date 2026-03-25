package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Menu;
import tn.esprit.rh_rse.entity.Plat;

import java.util.List;

public interface MenuService {
    List<Menu> getAll();
    Menu getById(String id);
    Menu save(Menu menu);
    Menu update(String id, Menu menu);
    void delete(String id);
    Menu ajouterPlat(String menuId, Plat plat);
    Menu supprimerPlat(String menuId, String platId);
    List<Menu> getMenusPublies();

    // Nouveaux endpoints
    List<Plat> getPlatsByRegime(String menuId, String regime);
    List<Menu> getMenusSuggestion(String userId);
}