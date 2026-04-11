package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import tn.esprit.rh_rse.config.jwt.JwtUtil;
import tn.esprit.rh_rse.dto.request.MobilityRequestDTO;
import tn.esprit.rh_rse.dto.request.MobilityReviewDTO;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.entity.MobilityRequest;
import tn.esprit.rh_rse.entity.NotifCarriere;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.MobilityStatus;
import tn.esprit.rh_rse.repository.CareerRepository;
import tn.esprit.rh_rse.repository.MobilityRequestRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MobilityRequestService {

    private final MobilityRequestRepository mobilityRepo;
    private final UserRepository userRepo;
    private final CareerRepository careerRepo;
    private final JwtUtil jwtUtil;
    private final NotifCarriereService notificationService;

    private String getCurrentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null) {
            return auth.getCredentials().toString();
        }
        return null;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        if (email != null && !email.isBlank()) {
            return userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + email));
        }
        throw new RuntimeException("Impossible d'identifier l'utilisateur connecté");
    }

    public MobilityRequest submitRequest(MobilityRequestDTO dto) {
        User employee = getCurrentUser();

        if (mobilityRepo.existsByEmployeeIdAndTargetCareerIdAndStatus(
                employee.getId(), dto.getTargetCareerId(), MobilityStatus.PENDING)) {
            throw new RuntimeException("Vous avez déjà une demande en attente pour ce poste.");
        }

        Career targetCareer = careerRepo.findById(dto.getTargetCareerId())
                .orElseThrow(() -> new RuntimeException("Poste cible introuvable"));

        MobilityRequest request = MobilityRequest.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getPrenom() + " " + employee.getNom())
                .currentCareerTitle(employee.getPoste())
                .currentDepartement(employee.getDepartement())
                .targetCareerId(targetCareer.getId())
                .targetCareerTitle(targetCareer.getTitle())
                .targetDepartement(targetCareer.getDepartement())
                .motivationLetter(dto.getMotivationLetter())
                .motivationFileName(dto.getMotivationFileName())
                .motivationFileBase64(dto.getMotivationFileBase64())
                .status(MobilityStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return mobilityRepo.save(request);
    }

    public List<MobilityRequest> getMyRequests() {
        User employee = getCurrentUser();
        return mobilityRepo.findByEmployeeId(employee.getId());
    }

    public MobilityRequest getById(String id) {
        return mobilityRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));
    }

    public MobilityRequest reviewRequest(String requestId, MobilityReviewDTO dto) {
        MobilityRequest request = mobilityRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));

        request.setStatus(dto.getStatus());
        request.setReviewedBy(dto.getReviewedBy());
        request.setReviewComment(dto.getReviewComment());
        request.setReviewedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        if (dto.getStatus() == MobilityStatus.APPROVED) {
            userRepo.findById(request.getEmployeeId()).ifPresent(user -> {
                user.setPoste(request.getTargetCareerTitle());
                user.setDepartement(request.getTargetDepartement());
                user.setUpdatedAt(LocalDateTime.now());
                userRepo.save(user);
            });

            // ✅ Notification approbation
            notificationService.send(
                    request.getEmployeeId(),
                    "🎉 Demande de mobilité approuvée !",
                    "Votre demande pour le poste \""
                            + request.getTargetCareerTitle()
                            + "\" a été approuvée. Votre profil a été mis à jour.",
                    "MOBILITY_APPROVED"
            );

        } else if (dto.getStatus() == MobilityStatus.REJECTED) {
            // ✅ Notification refus
            notificationService.send(
                    request.getEmployeeId(),
                    "❌ Demande de mobilité refusée",
                    "Votre demande pour le poste \""
                            + request.getTargetCareerTitle() + "\" a été refusée."
                            + (dto.getReviewComment() != null
                            ? " Motif : " + dto.getReviewComment() : ""),
                    "MOBILITY_REJECTED"
            );

        } else if (dto.getStatus() == MobilityStatus.ON_HOLD) {
            // ✅ Notification suspension
            notificationService.send(
                    request.getEmployeeId(),
                    "⏸️ Demande de mobilité en suspens",
                    "Votre demande pour le poste \""
                            + request.getTargetCareerTitle() + "\" est mise en attente."
                            + (dto.getReviewComment() != null
                            ? " Motif : " + dto.getReviewComment() : ""),
                    "MOBILITY_ON_HOLD"
            );
        }

        return mobilityRepo.save(request);
    }

    public List<MobilityRequest> getAll() { return mobilityRepo.findAll(); }
    public List<MobilityRequest> getByEmployee(String employeeId) { return mobilityRepo.findByEmployeeId(employeeId); }
    public List<MobilityRequest> getByStatus(MobilityStatus status) { return mobilityRepo.findByStatus(status); }
    public void deleteRequest(String id) { mobilityRepo.deleteById(id); }
}