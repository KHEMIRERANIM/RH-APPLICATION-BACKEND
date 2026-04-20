package tn.esprit.rh_rse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh_rse.dto.request.ChangerStatutRequest;
import tn.esprit.rh_rse.dto.response.CandidatureResponse;
import tn.esprit.rh_rse.entity.Candidature;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;
import tn.esprit.rh_rse.repository.CandidatureRepository;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.service.impl.CandidatureServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceImplTest {

    @Mock
    private CandidatureRepository candidatureRepository;

    @Mock
    private OffreRepository offreRepository;

    @InjectMocks
    private CandidatureServiceImpl candidatureService;

    private Candidature candidature;

    @BeforeEach
    void setUp() {
        candidature = Candidature.builder()
                .id("cand123")
                .candidatId("candUser123")
                .offreId("offre123")
                .statut(StatutCandidature.NOUVEAU)
                .scoreMatching(0.0)
                .build();
    }

    @Test
    void getCandidatureById_ShouldReturnCandidature() {
        when(candidatureRepository.findById("cand123")).thenReturn(Optional.of(candidature));

        CandidatureResponse response = candidatureService.getCandidatureById("cand123");

        assertNotNull(response);
        assertEquals("cand123", response.getId());
        verify(candidatureRepository, times(1)).findById("cand123");
    }

    @Test
    void changerStatut_ShouldUpdateStatutAndSave() {
        ChangerStatutRequest request = new ChangerStatutRequest();
        request.setNouveauStatut(StatutCandidature.ENTRETIEN_RH);

        when(candidatureRepository.findById("cand123")).thenReturn(Optional.of(candidature));
        when(candidatureRepository.save(any(Candidature.class))).thenReturn(candidature);

        CandidatureResponse response = candidatureService.changerStatut("cand123", request);

        assertNotNull(response);
        assertEquals(StatutCandidature.ENTRETIEN_RH, candidature.getStatut());
        verify(candidatureRepository, times(1)).save(candidature);
    }
}
