package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.dto.AlternativeDTO;
import tn.esprit.rh_rse.entity.*;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;      // ← AJOUTER
import tn.esprit.rh_rse.entity.enums.TypeNotification;   // ← AJOUTER
import tn.esprit.rh_rse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AlternativeTransportService {

    private final TrajetRepository trajetRepository;
    private final ReservationRepository reservationRepository;
    private final BusRepository busRepository;
    private final NotificationService notificationService;

    @Transactional
    public void annulerTrajetConducteur(String trajetId) {

        // 1. Récupérer le trajet
        Trajet trajet = trajetRepository.findById(trajetId)
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé"));

        // 2. Récupérer les réservations
        List<Reservation> reservations = reservationRepository.findByTrajetId(trajetId);

        // 3. Envoyer notifications
        for (Reservation reservation : reservations) {
            notificationService.envoyerNotification(
                    reservation.getEmployeId(),
                    trajet.getEmployeId(),
                    trajetId,
                    TypeNotification.ALTERNATIVES_DISPONIBLES,
                    "Le conducteur a annulé son trajet. Des alternatives sont disponibles."
            );
        }

        // 4. Changer statut (avec Enum, pas String)
        trajet.setStatut(StatutTrajet.INACTIF);  // ✅ Enum
        trajetRepository.save(trajet);
    }

    public List<AlternativeDTO> findAlternatives(String trajetId, String employeId) {

        Trajet trajetAnnule = trajetRepository.findById(trajetId)
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé"));

        List<AlternativeDTO> alternatives = new ArrayList<>();

        // 1. Chercher covoiturages disponibles
        List<Trajet> covoiturages = trajetRepository.findByStatutAndPlacesRestantesGreaterThan(
                StatutTrajet.ACTIF, 0  // ✅ Enum
        );

        for (Trajet t : covoiturages) {
            if (t.getId().equals(trajetId)) continue;
            if (t.getEmployeId().equals(trajetAnnule.getEmployeId())) continue;

            AlternativeDTO dto = new AlternativeDTO();
            dto.setId(t.getId());
            dto.setAdresseDepart(t.getAdresseDepart());
            dto.setAdresseArrivee(t.getAdresseArrivee());
            dto.setHeureDepart(t.getHeureDepart().toString());
            dto.setPlacesRestantes(t.getPlacesRestantes());
            dto.setType("COVOITURAGE");
            dto.setPriorite(1);
            alternatives.add(dto);
        }

        // 2. Chercher bus disponibles
        List<Bus> bus = busRepository.findByPlacesRestantesGreaterThan(0);
        for (Bus b : bus) {
            AlternativeDTO dto = new AlternativeDTO();
            dto.setId(b.getId());
            dto.setAdresseDepart(b.getLigne());
            dto.setHeureDepart(b.getHeureDepart());
            dto.setPlacesRestantes(b.getPlacesRestantes());
            dto.setType("BUS");
            dto.setPriorite(2);
            alternatives.add(dto);
        }

        alternatives.sort(Comparator.comparingInt(AlternativeDTO::getPriorite));
        return alternatives;
    }
}