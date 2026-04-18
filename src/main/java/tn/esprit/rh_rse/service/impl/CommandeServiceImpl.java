package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.entity.Commande;
import tn.esprit.rh_rse.entity.Fidelite;
import tn.esprit.rh_rse.entity.Menu;
import tn.esprit.rh_rse.entity.Plat;
import tn.esprit.rh_rse.repository.CommandeRepository;
import tn.esprit.rh_rse.repository.MenuRepository;
import tn.esprit.rh_rse.service.CommandeService;
import tn.esprit.rh_rse.service.FideliteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandeServiceImpl implements CommandeService {

    private final CommandeRepository commandeRepository;
    private final MenuRepository menuRepository;
    private final FideliteService fideliteService;

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
    @Transactional
    public Commande save(Commande commande) {
        commande.setDateCommande(LocalDate.now());
        commande.setStatut("en_attente");

        Menu menu = menuRepository.findById(commande.getMenuId())
                .orElseThrow(() -> new RuntimeException("Menu non trouvé : " + commande.getMenuId()));

        if (commande.getPlats() == null) commande.setPlats(Collections.emptyList());
        if (menu.getPlats() == null) menu.setPlats(Collections.emptyList());

        // Vérification stock
        for (String platId : commande.getPlats()) {
            Plat plat = menu.getPlats().stream()
                    .filter(p -> platId.equals(p.getPlatId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Plat introuvable : " + platId));
            if (plat.getQuantite() == null || plat.getQuantite() <= 0) {
                throw new RuntimeException("Le plat '" + plat.getNom() + "' est en rupture de stock !");
            }
        }

        // Calcul montant brut
        double total = menu.getPlats().stream()
                .filter(p -> p.getPlatId() != null && commande.getPlats().contains(p.getPlatId()))
                .mapToDouble(p -> p.getPrix() != null ? p.getPrix() : 0.0)
                .sum();

        if (total == 0.0 && !commande.getPlats().isEmpty()) {
            throw new RuntimeException("Aucun plat valide sélectionné !");
        }

        // ✅ Appliquer réduction fidélité si disponible
        Fidelite fidelite = fideliteService.getOrCreate(commande.getUserId());
        double totalApresReduction = total;

        if (fidelite.isReductionDisponible()) {
            double reduction = fidelite.getMontantReduction();
            totalApresReduction = Math.max(0, total - reduction);
            commande.setReductionAppliquee(true);
            commande.setMontantReduction(reduction);
            fideliteService.utiliserReduction(commande.getUserId());
            log.info("[Fidélité] Réduction {} TND appliquée pour user {} : {} → {} TND",
                    reduction, commande.getUserId(), total, totalApresReduction);
        } else {
            commande.setReductionAppliquee(false);
            commande.setMontantReduction(0.0);
        }

        commande.setMontantTotal(totalApresReduction);
        commande.setMontantBrut(total);

        Commande saved = commandeRepository.save(commande);

        // Décrémenter stock
        for (String platId : commande.getPlats()) {
            menu.getPlats().stream()
                    .filter(p -> platId.equals(p.getPlatId()))
                    .findFirst()
                    .ifPresent(p -> p.setQuantite(Math.max(0, p.getQuantite() - 1)));
        }
        menuRepository.save(menu);

        log.info("[Commande] Créée — {} plat(s) — {} TND brut / {} TND net",
                commande.getPlats().size(), total, totalApresReduction);

        return saved;
    }

    @Override
    @Transactional
    public Commande updatePlats(String id, List<String> nouveauxPlats) {
        Commande commande = getById(id);

        if (!"en_attente".equals(commande.getStatut())) {
            throw new RuntimeException("Modification impossible : la commande n'est plus en attente.");
        }

        Menu menu = menuRepository.findById(commande.getMenuId())
                .orElseThrow(() -> new RuntimeException("Menu non trouvé : " + commande.getMenuId()));

        if (menu.getPlats() == null) menu.setPlats(Collections.emptyList());

        // 1. Remettre le stock des anciens plats
        for (String platId : commande.getPlats()) {
            menu.getPlats().stream()
                    .filter(p -> platId.equals(p.getPlatId()))
                    .findFirst()
                    .ifPresent(p -> p.setQuantite(p.getQuantite() + 1));
        }

        // 2. Vérifier le stock pour les nouveaux plats
        for (String platId : nouveauxPlats) {
            Plat plat = menu.getPlats().stream()
                    .filter(p -> platId.equals(p.getPlatId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Plat introuvable : " + platId));
            if (plat.getQuantite() == null || plat.getQuantite() <= 0) {
                throw new RuntimeException("Le plat '" + plat.getNom() + "' est en rupture de stock !");
            }
        }

        // 3. Décrémenter le stock des nouveaux plats
        for (String platId : nouveauxPlats) {
            menu.getPlats().stream()
                    .filter(p -> platId.equals(p.getPlatId()))
                    .findFirst()
                    .ifPresent(p -> p.setQuantite(Math.max(0, p.getQuantite() - 1)));
        }

        menuRepository.save(menu);

        // 4. Recalculer montant brut
        double nouveauTotal = nouveauxPlats.stream()
                .mapToDouble(platId -> menu.getPlats().stream()
                        .filter(p -> platId.equals(p.getPlatId()))
                        .mapToDouble(p -> p.getPrix() != null ? p.getPrix() : 0.0)
                        .findFirst().orElse(0.0))
                .sum();

        // 5. Réappliquer réduction si elle était déjà appliquée sur cette commande
        double totalNet = nouveauTotal;
        if (commande.isReductionAppliquee()) {
            totalNet = Math.max(0, nouveauTotal - commande.getMontantReduction());
        }

        commande.setPlats(nouveauxPlats);
        commande.setMontantTotal(totalNet);
        commande.setMontantBrut(nouveauTotal);

        log.info("[Commande] {} modifiée — {} plat(s) — {} TND brut / {} TND net",
                id, nouveauxPlats.size(), nouveauTotal, totalNet);

        return commandeRepository.save(commande);
    }

    @Override
    public Commande updateStatut(String id, String statut) {
        Commande commande = getById(id);
        commande.setStatut(statut);

        if ("prete".equals(statut)) {
            commande.setDatePrete(LocalDateTime.now());
            String code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 4)
                    .toUpperCase();
            commande.setCodeRetrait(code);
            log.info("[Commande] {} est prête — code retrait : {}", id, code);
        }

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