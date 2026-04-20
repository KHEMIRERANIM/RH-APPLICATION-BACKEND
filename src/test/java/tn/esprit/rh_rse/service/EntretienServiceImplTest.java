package tn.esprit.rh_rse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh_rse.dto.request.CreateEntretienRequest;
import tn.esprit.rh_rse.dto.response.EntretienResponse;
import tn.esprit.rh_rse.entity.Entretien;
import tn.esprit.rh_rse.entity.enums.StatutEntretien;
import tn.esprit.rh_rse.entity.enums.TypeEntretien;
import tn.esprit.rh_rse.repository.EntretienRepository;
import tn.esprit.rh_rse.service.impl.EntretienServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntretienServiceImplTest {

    @Mock
    private EntretienRepository entretienRepository;

    @Mock
    private tn.esprit.rh_rse.repository.CandidatureRepository candidatureRepository;

    @Mock
    private tn.esprit.rh_rse.repository.UserRepository userRepository;

    @Mock
    private tn.esprit.rh_rse.repository.OffreRepository offreRepository;

    @Mock
    private tn.esprit.rh_rse.service.EmailService emailService;

    @Mock
    private tn.esprit.rh_rse.service.GoogleMeetService googleMeetService;

    @InjectMocks
    private EntretienServiceImpl entretienService;

    private Entretien entretien;
    private CreateEntretienRequest createRequest;

    @BeforeEach
    void setUp() {
        entretien = Entretien.builder()
                .id("ent123")
                .candidatureId("cand123")
                .recruteurId("rec123")
                .type(TypeEntretien.VISIO)
                .statut(StatutEntretien.PLANIFIE)
                .dateHeure(LocalDateTime.now().plusDays(2))
                .build();

        createRequest = new CreateEntretienRequest();
        createRequest.setCandidatureId("cand123");
        createRequest.setRecruteurId("rec123");
        createRequest.setType(TypeEntretien.VISIO);
        createRequest.setDateHeure(LocalDateTime.now().plusDays(2));
    }

    @Test
    void planifierEntretien_ShouldReturnSavedEntretien() {
        tn.esprit.rh_rse.entity.Candidature candidature = tn.esprit.rh_rse.entity.Candidature.builder()
                .id("cand123")
                .candidatId("candidat1")
                .offreId("offre1")
                .build();
                
        when(candidatureRepository.findById("cand123")).thenReturn(Optional.of(candidature));
        when(entretienRepository.save(any(Entretien.class))).thenReturn(entretien);

        EntretienResponse response = entretienService.planifierEntretien(createRequest);

        assertNotNull(response);
        assertEquals(StatutEntretien.PLANIFIE, response.getStatut());
        assertEquals(TypeEntretien.VISIO, response.getType());
        verify(entretienRepository, times(1)).save(any(Entretien.class));
    }

    @Test
    void annulerEntretien_ShouldChangeStatusToAnnule() {
        when(entretienRepository.findById("ent123")).thenReturn(Optional.of(entretien));
        when(entretienRepository.save(any(Entretien.class))).thenReturn(entretien);

        entretienService.annulerEntretien("ent123");

        assertEquals(StatutEntretien.ANNULE, entretien.getStatut());
        verify(entretienRepository, times(1)).save(entretien);
    }
}
