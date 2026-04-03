package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Commande;
import tn.esprit.rh_rse.entity.Menu;
import tn.esprit.rh_rse.entity.Plat;
import tn.esprit.rh_rse.repository.CommandeRepository;
import tn.esprit.rh_rse.repository.MenuRepository;
import tn.esprit.rh_rse.service.CommandeService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
        commande.setDateCommande(LocalDate.now());

        Menu menu = menuRepository.findById(commande.getMenuId())
                .orElseThrow(() -> new RuntimeException("Menu non trouvé : " + commande.getMenuId()));

        if (commande.getPlats() == null) commande.setPlats(Collections.emptyList());
        if (menu.getPlats() == null) menu.setPlats(Collections.emptyList());

        // Vérification stock avant commande
        for (String platId : commande.getPlats()) {
            Plat plat = menu.getPlats().stream()
                    .filter(p -> platId.equals(p.getPlatId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Plat introuvable : " + platId));
            if (plat.getQuantite() == null || plat.getQuantite() <= 0) {
                throw new RuntimeException("Le plat '" + plat.getNom() + "' est en rupture de stock !");
            }
        }

        // ✅ Calcul montant
        double total = menu.getPlats().stream()
                .filter(p -> p.getPlatId() != null && commande.getPlats().contains(p.getPlatId()))
                .mapToDouble(p -> p.getPrix() != null ? p.getPrix() : 0.0)
                .sum();

        if (total == 0.0 && !commande.getPlats().isEmpty()) {
            throw new RuntimeException("Aucun plat valide sélectionné !");
        }
        commande.setMontantTotal(total);

        Commande saved = commandeRepository.save(commande);

        // Décrémentation du stock pour chaque platId commandé (gère les doublons pour quantité > 1)
        for (String platId : commande.getPlats()) {
            menu.getPlats().stream()
                    .filter(p -> platId.equals(p.getPlatId()))
                    .findFirst()
                    .ifPresent(p -> p.setQuantite(Math.max(0, p.getQuantite() - 1)));
        }
        menuRepository.save(menu);

        return saved;
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
                .collect(Collectors.groupingBy(
                        c -> c.getDateCommande().toString(),
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getPlatsPlusCommandes() {
        return commandeRepository.findAll().stream()
                .flatMap(c -> c.getPlats().stream())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
    }
}