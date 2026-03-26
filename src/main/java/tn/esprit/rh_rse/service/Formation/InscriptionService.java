package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.dto.Formation.InscriptionFormationDTO;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.entity.Formation.InscriptionFormation;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;
import tn.esprit.rh_rse.repository.Formation.InscriptionFormationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final InscriptionFormationRepository inscriptionRepository;
    private final FormationRepository formationRepository;
    private final NotificationService notificationService;

    @Transactional
    public InscriptionFormationDTO inscrireEmploye(String formationId, String employeId) {
        // Vérifier si déjà inscrit
        if (inscriptionRepository.existsByFormationIdAndEmployeId(formationId, employeId)) {
            throw new RuntimeException("Employé déjà inscrit à cette formation");
        }

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));

        // Vérifier les places disponibles
        if (formation.getPlacesDisponibles() <= 0) {
            throw new RuntimeException("Plus de places disponibles");
        }

        // Vérifier la date limite d'inscription
        if (LocalDateTime.now().isAfter(formation.getDateLimiteInscription())) {
            throw new RuntimeException("Date limite d'inscription dépassée");
        }

        InscriptionFormation inscription = new InscriptionFormation();
        inscription.setFormationId(formationId);
        inscription.setEmployeId(employeId);
        inscription.setStatut("CONFIRME");
        inscription.setDateInscription(LocalDateTime.now());

        InscriptionFormation savedInscription = inscriptionRepository.save(inscription);

        // Réduire les places disponibles
        formation.setPlacesDisponibles(formation.getPlacesDisponibles() - 1);
        formationRepository.save(formation);

        // Envoyer notification
        notificationService.envoyerConfirmationInscription(employeId, formation.getTitre());

        return convertToDTO(savedInscription);
    }

    @Transactional
    public void annulerInscription(String inscriptionId, String motif) {
        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        inscription.setStatut("ANNULE");
        inscription.setMotifAnnulation(motif);
        inscriptionRepository.save(inscription);

        // Réaugmenter les places disponibles
        Formation formation = formationRepository.findById(inscription.getFormationId())
                .orElseThrow(() -> new RuntimeException("Formation non trouvée"));
        formation.setPlacesDisponibles(formation.getPlacesDisponibles() + 1);
        formationRepository.save(formation);
    }

    @Transactional(readOnly = true)
    public List<InscriptionFormationDTO> getInscriptionsByEmploye(String employeId) {
        return inscriptionRepository.findByEmployeId(employeId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InscriptionFormationDTO> getInscriptionsByFormation(String formationId) {
        return inscriptionRepository.findByFormationId(formationId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmerPresence(String inscriptionId) {
        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        inscription.setPresenceConfirmee(true);
        inscription.setDatePresence(LocalDateTime.now());
        inscriptionRepository.save(inscription);
    }

    private InscriptionFormationDTO convertToDTO(InscriptionFormation inscription) {
        InscriptionFormationDTO dto = new InscriptionFormationDTO();
        dto.setId(inscription.getId());
        dto.setFormationId(inscription.getFormationId());
        dto.setEmployeId(inscription.getEmployeId());
        dto.setStatut(inscription.getStatut());
        dto.setDateInscription(inscription.getDateInscription());

        // Récupérer le titre de la formation
        formationRepository.findById(inscription.getFormationId())
                .ifPresent(f -> dto.setFormationTitre(f.getTitre()));

        return dto;
    }
}