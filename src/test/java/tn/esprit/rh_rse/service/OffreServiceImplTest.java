package tn.esprit.rh_rse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Avancés - Service des Offres")
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
                .id("off123")
                .titre("Architecte Cloud")
                .statut(StatutOffre.BROUILLON)
                .competencesRequises(List.of("AWS", "Kubernetes"))
                .build();

        createRequest = new CreateOffreRequest();
        createRequest.setTitre("Architecte Cloud");
    }

    @Test
    @DisplayName("Lecture d'une offre existante")
    void getOffreById_Success() {
        when(offreRepository.findById("off123")).thenReturn(Optional.of(offre));
        OffreResponse response = offreService.getOffreById("off123");
        
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("off123");
        verify(offreRepository).findById("off123");
    }

    @ParameterizedTest
    @DisplayName("Scénarios de Publication d'Offre")
    @CsvSource({
        "off123, true",
        "off456, false"
    })
    void publierOffre_Scenarios(String id, boolean exists) {
        if (exists) {
            when(offreRepository.findById(id)).thenReturn(Optional.of(offre));
            when(offreRepository.save(any(Offre.class))).thenReturn(offre);

            offreService.publierOffre(id);

            assertThat(offre.getStatut()).isEqualTo(StatutOffre.PUBLIEE);
            assertThat(offre.getDatePublication()).isNotNull();
        } else {
            when(offreRepository.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> offreService.publierOffre(id))
                    .isInstanceOf(RecrutementNotFoundException.class);
        }
    }

    @Test
    @DisplayName("Erreur : Offre introuvable")
    void getOffreById_NotFound() {
        when(offreRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> offreService.getOffreById("unknown"))
                .isInstanceOf(RecrutementNotFoundException.class);
    }
}
