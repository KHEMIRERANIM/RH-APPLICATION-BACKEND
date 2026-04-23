package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Menu;
import tn.esprit.rh_rse.entity.Plat;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.MenuRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.MenuService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

    @Override
    public List<Menu> getAll() {
        return menuRepository.findAll();
    }

    @Override
    public Menu getById(String id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu non trouvé : " + id));
    }

    @Override
    public Menu save(Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    public Menu update(String id, Menu menu) {
        getById(id);
        menu.setId(id);
        return menuRepository.save(menu);
    }

    @Override
    public void delete(String id) {
        getById(id);
        menuRepository.deleteById(id);
    }

    @Override
    public Menu ajouterPlat(String menuId, Plat plat) {
        Menu menu = getById(menuId);
        plat.setPlatId(UUID.randomUUID().toString());
        menu.getPlats().add(plat);
        return menuRepository.save(menu);
    }

    @Override
    public Menu updatePlat(String menuId, String platId, Plat platUpdated) {
        Menu menu = getById(menuId);

        for (Plat plat : menu.getPlats()) {
            if (plat.getPlatId().equals(platId)) {

                plat.setNom(platUpdated.getNom());
                plat.setDescription(platUpdated.getDescription());
                plat.setPrix(platUpdated.getPrix());
                plat.setTags(platUpdated.getTags());
                plat.setImage(platUpdated.getImage());
                plat.setDisponible(platUpdated.getDisponible());
                plat.setQuantite(platUpdated.getQuantite());
                plat.setIngredients(platUpdated.getIngredients());
                plat.setCalories(platUpdated.getCalories());
                plat.setProteines(platUpdated.getProteines());
                plat.setGlucides(platUpdated.getGlucides());
                plat.setLipides(platUpdated.getLipides());
                plat.setSucres(platUpdated.getSucres());
                plat.setFibres(platUpdated.getFibres());
                plat.setPctProteines(platUpdated.getPctProteines());
                plat.setPctGlucides(platUpdated.getPctGlucides());
                plat.setPctLipides(platUpdated.getPctLipides());
                plat.setPmrAdapte(platUpdated.getPmrAdapte());
                plat.setPmrRaison(platUpdated.getPmrRaison());
                plat.setNiveauCalories(platUpdated.getNiveauCalories());

                break;
            }
        }

        return menuRepository.save(menu);
    }

    @Override
    public Menu supprimerPlat(String menuId, String platId) {
        Menu menu = getById(menuId);
        menu.getPlats().removeIf(p -> p.getPlatId().equals(platId));
        return menuRepository.save(menu);
    }

    @Override
    public List<Menu> getMenusPublies() {
        return menuRepository.findByStatut("publie");
    }


    @Override
    public List<Plat> getPlatsByRegime(String menuId, String regime) {
        Menu menu = getById(menuId);
        return menu.getPlats().stream()
                .filter(p -> p.getTags() != null && p.getTags().contains(regime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> getMenusSuggestion(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Menu> menus = menuRepository.findByStatut("publie");

        for (Menu menu : menus) {
            menu.setPlats(menu.getPlats().stream()
                    .filter(plat -> plat.getTags() != null
                            && plat.getTags().stream().anyMatch(tag -> user.getRegime().contains(tag))
                            && plat.getTags().stream().noneMatch(tag -> user.getAllergies().contains(tag)))
                    .collect(Collectors.toList()));
        }
        return menus;
    }
}