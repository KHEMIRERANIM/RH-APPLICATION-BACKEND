package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Commande;
import tn.esprit.rh_rse.entity.Fidelite;
import tn.esprit.rh_rse.entity.PaiementPlat;
import tn.esprit.rh_rse.repository.CommandeRepository;
import tn.esprit.rh_rse.repository.PaiementPlatRepository;
import tn.esprit.rh_rse.service.FideliteService;
import tn.esprit.rh_rse.service.PaiementPlatService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaiementPlatServiceImpl implements PaiementPlatService {

    private final PaiementPlatRepository paiementPlatRepository;
    private final CommandeRepository commandeRepository;
    private final FideliteService fideliteService;

    @Override
    public PaiementPlat payerCommande(String commandeId, String modePaiement) {

        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée : " + commandeId));

        double montantBrut = commande.getMontantTotal();
        double montantReduction = 0.0;
        double montantNet = montantBrut;

        // Appliquer la réduction fidélité si disponible — uniquement au paiement
        try {
            Fidelite fidelite = fideliteService.getOrCreate(commande.getUserId());
            if (fidelite.isReductionDisponible()) {
                montantReduction = fidelite.getMontantReduction();
                montantNet = Math.max(0, montantBrut - montantReduction);
                commande.setReductionAppliquee(true);
                commande.setMontantReduction(montantReduction);
                commande.setMontantTotal(montantNet);
                fideliteService.utiliserReduction(commande.getUserId());
                log.info("[Paiement] Réduction {} TND appliquée pour user {} : {} → {} TND",
                        montantReduction, commande.getUserId(), montantBrut, montantNet);
            }
        } catch (Exception e) {
            log.warn("[Paiement] Erreur vérification réduction pour user {}: {}",
                    commande.getUserId(), e.getMessage());
        }

        String statut = "salaire".equals(modePaiement) ? "en_attente_integration" : "paye";
        int pointsGagnes = (int)(montantNet * Fidelite.POINTS_PAR_TND);

        PaiementPlat paiement = PaiementPlat.builder()
                .commandeId(commandeId)
                .userId(commande.getUserId())
                .nomEmploye(commande.getNomEmploye() != null
                        ? commande.getNomEmploye() : commande.getUserId())
                .montantBrut(montantBrut)
                .montantReduction(montantReduction)
                .montantNet(montantNet)
                .reductionAppliquee(commande.isReductionAppliquee())
                .pointsGagnes(pointsGagnes)
                .modePaiement(modePaiement)
                .statut(statut)
                .datePaiement(LocalDateTime.now())
                .build();

        PaiementPlat saved = paiementPlatRepository.save(paiement);

        // Créditer les points uniquement au paiement réel
        try {
            fideliteService.ajouterPoints(commande.getUserId(), montantNet);
            log.info("[Paiement] {} points crédités pour user {} — commande {}",
                    pointsGagnes, commande.getUserId(), commandeId);
        } catch (Exception e) {
            log.warn("[Paiement] Erreur crédit points pour user {}: {}",
                    commande.getUserId(), e.getMessage());
        }

        // Passer la commande à livree
        commande.setStatut("livree");
        commandeRepository.save(commande);

        log.info("[Paiement] Commande {} livrée — mode: {} — net: {} TND — {} pts",
                commandeId, modePaiement, montantNet, pointsGagnes);

        return saved;
    }

    @Override
    public List<PaiementPlat> getPaiementsByUser(String userId) {
        return paiementPlatRepository.findByUserId(userId);
    }

    @Override
    public List<PaiementPlat> getPaiementsEnAttente() {
        return paiementPlatRepository.findByStatut("en_attente_integration");
    }
}