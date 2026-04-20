package tn.esprit.rh_rse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh_rse.dto.request.CreateOffreRequest;
import tn.esprit.rh_rse.dto.response.OffreResponse;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.enums.StatutOffre;
import tn.esprit.rh_rse.exception.RecrutementNotFoundException;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.service.impl.OffreServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffreServiceImplTest {

    @Mock
    private OffreRepository offreRepository;

    @InjectMocks
    private OffreServiceImpl offreService;

    private Offre offre;
    private CreateOffreRequest createRequest;

    @BeforeEach
    void setUp() {
        offre = Offre.builder()
                .id("offre123")
                .titre("Développeur Java")
                .statut(StatutOffre.BROUILLON)
                .nombreCandidatures(0)
                .build();

        createRequest = new CreateOffreRequest();
        createRequest.setTitre("Développeur Java");
    }

    @Test
    void createOffre_ShouldReturnOffreResponse() {
        // Arrange
        when(offreRepository.save(any(Offre.class))).thenReturn(offre);

        // Act
        OffreResponse response = offreService.createOffre(createRequest, "admin123");

        // Assert
        assertNotNull(response);
        assertEquals("Développeur Java", response.getTitre());
        assertEquals(StatutOffre.BROUILLON, response.getStatut());
        verify(offreRepository, times(1)).save(any(Offre.class));
    }

    @Test
    void getOffreById_WhenExists_ShouldReturnOffreResponse() {
        when(offreRepository.findById("offre123")).thenReturn(Optional.of(offre));

        OffreResponse response = offreService.getOffreById("offre123");

        assertNotNull(response);
        assertEquals("offre123", response.getId());
        verify(offreRepository, times(1)).findById("offre123");
    }

    @Test
    void getOffreById_WhenNotExists_ShouldThrowException() {
        when(offreRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(RecrutementNotFoundException.class, () -> offreService.getOffreById("invalid"));
        verify(offreRepository, times(1)).findById("invalid");
    }

    @Test
    void publierOffre_ShouldUpdateStatut() {
        when(offreRepository.findById("offre123")).thenReturn(Optional.of(offre));
        when(offreRepository.save(any(Offre.class))).thenReturn(offre);

        offreService.publierOffre("offre123");

        assertEquals(StatutOffre.PUBLIEE, offre.getStatut());
        assertNotNull(offre.getDatePublication());
        verify(offreRepository, times(1)).save(offre);
    }
}
