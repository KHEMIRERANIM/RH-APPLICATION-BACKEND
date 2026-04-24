package tn.esprit.rh_rse.service.impl.conge;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.request.conge.BulletinSalaireRequest;
import tn.esprit.rh_rse.dto.response.conge.BulletinSalaireResponse;
import tn.esprit.rh_rse.entity.conge.BulletinSalaire;
import tn.esprit.rh_rse.repository.conge.BulletinSalaireRepository;
import tn.esprit.rh_rse.service.conge.SalaireService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaireServiceImpl implements SalaireService {

    private final BulletinSalaireRepository bulletinSalaireRepository;

    // Taux de cotisation Tunisie
    private static final double TAUX_CNSS     = 0.0918;  // 9.18%
    private static final double TAUX_IRPP_MIN = 0.15;    // 15% simplifié (tranche basse)

    @Override
    public BulletinSalaireResponse creerBulletin(BulletinSalaireRequest request) {
        // Vérifier qu'il n'existe pas déjà un bulletin pour ce mois/année
        bulletinSalaireRepository
                .findByEmployeIdAndMoisAndAnnee(request.getEmployeId(), request.getMois(), request.getAnnee())
                .ifPresent(b -> { throw new RuntimeException(
                        "Un bulletin existe déjà pour ce mois: " + request.getMois() + "/" + request.getAnnee()); });

        BulletinSalaire bulletin = buildBulletin(null, request);
        BulletinSalaire saved = bulletinSalaireRepository.save(bulletin);
        return toResponse(saved);
    }

    @Override
    public List<BulletinSalaireResponse> getBulletinsByEmploye(String employeId) {
        return bulletinSalaireRepository.findByEmployeId(employeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BulletinSalaireResponse getBulletinById(String id) {
        BulletinSalaire bulletin = bulletinSalaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bulletin introuvable avec id: " + id));
        return toResponse(bulletin);
    }

    @Override
    public BulletinSalaireResponse getBulletinByMois(String employeId, int mois, int annee) {
        BulletinSalaire bulletin = bulletinSalaireRepository
                .findByEmployeIdAndMoisAndAnnee(employeId, mois, annee)
                .orElseThrow(() -> new RuntimeException(
                        "Aucun bulletin pour " + employeId + " en " + mois + "/" + annee));
        return toResponse(bulletin);
    }

    @Override
    public List<BulletinSalaireResponse> getBulletinsByAnnee(String employeId, int annee) {
        return bulletinSalaireRepository.findByEmployeIdAndAnnee(employeId, annee)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BulletinSalaireResponse modifierBulletin(String id, BulletinSalaireRequest request) {
        bulletinSalaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bulletin introuvable avec id: " + id));

        BulletinSalaire updated = buildBulletin(id, request);
        BulletinSalaire saved = bulletinSalaireRepository.save(updated);
        return toResponse(saved);
    }
    @Override
    public List<BulletinSalaireResponse> getAllBulletins() {
        return bulletinSalaireRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    @Override
    public void supprimerBulletin(String id) {
        if (!bulletinSalaireRepository.existsById(id)) {
            throw new RuntimeException("Bulletin introuvable avec id: " + id);
        }
        bulletinSalaireRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTHODES PRIVÉES
    // ─────────────────────────────────────────────────────────────

    /**
     * Construit un BulletinSalaire en calculant automatiquement
     * les cotisations CNSS, l'IRPP et le salaire net.
     */
    private BulletinSalaire buildBulletin(String id, BulletinSalaireRequest req) {
        double brut      = req.getSalaireBrut();
        double primes    = req.getPrimes();
        double hs        = req.getHeuresSupplementaires();
        double retenues  = req.getAutresRetenues();

        double cnss  = brut * TAUX_CNSS;
        double irpp  = (brut + primes + hs - cnss) * TAUX_IRPP_MIN;
        double net   = brut + primes + hs - cnss - irpp - retenues;

        return BulletinSalaire.builder()
                .id(id)
                .employeId(req.getEmployeId())
                .mois(req.getMois())
                .annee(req.getAnnee())
                .salaireBrut(brut)
                .primes(primes)
                .heuresSupplementaires(hs)
                .cotisationsCNSS(Math.round(cnss * 100.0) / 100.0)
                .irpp(Math.round(irpp * 100.0) / 100.0)
                .autresRetenues(retenues)
                .salaireNet(Math.round(net * 100.0) / 100.0)
                .dateGeneration(LocalDateTime.now())
                .build();
    }

    private BulletinSalaireResponse toResponse(BulletinSalaire b) {
        return BulletinSalaireResponse.builder()
                .id(b.getId())
                .employeId(b.getEmployeId())
                .mois(b.getMois())
                .annee(b.getAnnee())
                .salaireBrut(b.getSalaireBrut())
                .primes(b.getPrimes())
                .heuresSupplementaires(b.getHeuresSupplementaires())
                .cotisationsCNSS(b.getCotisationsCNSS())
                .irpp(b.getIrpp())
                .autresRetenues(b.getAutresRetenues())
                .salaireNet(b.getSalaireNet())
                .dateGeneration(b.getDateGeneration())
                .pdfUrl(b.getPdfUrl())
                .build();
    }
}
