package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Commande;
import tn.esprit.rh_rse.entity.Menu;
import tn.esprit.rh_rse.entity.Plat;
import tn.esprit.rh_rse.repository.CommandeRepository;
import tn.esprit.rh_rse.repository.MenuRepository;
import tn.esprit.rh_rse.service.CommandeService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandeServiceImpl implements CommandeService {

    private final CommandeRepository commandeRepository;
    private final MenuRepository menuRepository;

    @Override
    public List<Commande> getAll() {
        return commandeRepository.findAll();
    }

    @Override
    public Commande getById(String id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée : " + id));
    }

    @Override
    public Commande save(Commande commande) {
        if (commandeRepository.existsByUserIdAndMenuId(commande.getUserId(), commande.getMenuId())) {
            throw new RuntimeException("Vous avez déjà commandé ce menu !");
        }
        Menu menu = menuRepository.findById(commande.getMenuId())
                .orElseThrow(() -> new RuntimeException("Menu non trouvé"));

        double total = menu.getPlats().stream()
                .filter(p -> commande.getPlats().contains(p.getPlatId()))
                .mapToDouble(Plat::getPrix)
                .sum();
        commande.setMontantTotal(total);
        return commandeRepository.save(commande);
    }

    @Override
    public Commande updateStatut(String id, String statut) {
        Commande commande = getById(id);
        commande.setStatut(statut);
        return commandeRepository.save(commande);
    }

    @Override
    public void delete(String id) {
        getById(id);
        commandeRepository.deleteById(id);
    }

    @Override
    public List<Commande> getByUser(String userId) {
        return commandeRepository.findByUserId(userId);
    }

    @Override
    public Map<String, Long> getNombreCommandesParJour() {
        return commandeRepository.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getDateCommande().toString(), Collectors.counting()));
    }

    @Override
    public Map<String, Long> getPlatsPlusCommandes() {
        return commandeRepository.findAll().stream()
                .flatMap(c -> c.getPlats().stream())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
    }
}