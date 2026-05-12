package tn.esprit.rh_rse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.ChangerStatutRequest;
import tn.esprit.rh_rse.dto.response.CandidatureResponse;
import tn.esprit.rh_rse.dto.response.CVAiDTO;
import tn.esprit.rh_rse.entity.Candidature;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;
import tn.esprit.rh_rse.repository.CandidatureRepository;
import tn.esprit.rh_rse.repository.OffreRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.impl.CandidatureServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceImplTest {

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private OffreRepository offreRepository;
    @Mock private UserRepository userRepository;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private EmailService emailService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private CandidatureServiceImpl candidatureService;

    private Candidature candidature;
    private Offre offre;
    private User user;

    @BeforeEach
    void setUp() {
        candidature = Candidature.builder()
                .id("cand123").candidatId("user123").offreId("offre123")
                .statut(StatutCandidature.NOUVEAU)
                .historiqueStatuts(new ArrayList<>())
                .scoreMatching(0.0)
                .build();
        
        offre = Offre.builder()
                .id("offre123").titre("Développeur Java")
                .competencesRequises(List.of("Java", "Spring Boot"))
                .niveauExperience("3 ans")
                .nombreCandidatures(0)
                .build();
                
        user = User.builder().id("user123").nom("Doe").prenom("John").email("john.doe@test.com").build();
    }

    @Test
    void getCandidatureById_ShouldReturnCandidature() {
        when(candidatureRepository.findById("cand123")).thenReturn(Optional.of(candidature));
        CandidatureResponse response = candidatureService.getCandidatureById("cand123");
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("cand123");
        verify(candidatureRepository).findById("cand123");
    }

    @Test
    void changerStatut_ShouldUpdateStatutAndSave() {
        ChangerStatutRequest request = new ChangerStatutRequest();
        request.setNouveauStatut(StatutCandidature.ENTRETIEN_RH);

        when(candidatureRepository.findById("cand123")).thenReturn(Optional.of(candidature));
        when(candidatureRepository.save(any(Candidature.class))).thenReturn(candidature);

        CandidatureResponse response = candidatureService.changerStatut("cand123", request);

        assertThat(response).isNotNull();
        assertThat(candidature.getStatut()).isEqualTo(StatutCandidature.ENTRETIEN_RH);
        assertThat(candidature.getHistoriqueStatuts()).isNotEmpty();
        verify(candidatureRepository).save(candidature);
    }
    
    @Test
    void postuler_ShouldThrowException_WhenAlreadyApplied() {
        MultipartFile cv = mock(MultipartFile.class);
        MultipartFile lettre = mock(MultipartFile.class);
        
        when(offreRepository.findById("offre123")).thenReturn(Optional.of(offre));
        when(candidatureRepository.findByCandidatIdAndOffreId("user123", "offre123"))
                .thenReturn(Optional.of(candidature));
                
        assertThatThrownBy(() -> candidatureService.postuler("user123", "offre123", cv, lettre))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vous avez déjà postulé à cette offre.");
    }
    
    @Test
    void postuler_ShouldProcessSuccessfully_WhenIAResultsAreGood() throws Exception {
        // Arrange
        MultipartFile cv = mock(MultipartFile.class);
        MultipartFile lettre = mock(MultipartFile.class);
        InputStream dummyStream = new ByteArrayInputStream("dummy content".getBytes());
        
        when(cv.isEmpty()).thenReturn(false);
        when(cv.getInputStream()).thenReturn(dummyStream);
        when(cv.getContentType()).thenReturn("application/pdf");
        when(cv.getResource()).thenReturn(mock(org.springframework.core.io.Resource.class));
        
        when(lettre.isEmpty()).thenReturn(true);
        when(offreRepository.findById("offre123")).thenReturn(Optional.of(offre));
        when(candidatureRepository.findByCandidatIdAndOffreId("user123", "offre123")).thenReturn(Optional.empty());
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        org.bson.types.ObjectId fakeId = new org.bson.types.ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString())).thenReturn(fakeId);
        
        // Mock Python AI Response
        CVAiDTO aiResponse = new CVAiDTO();
        CVAiDTO.ProfileDTO profile = new CVAiDTO.ProfileDTO();
        profile.setNomComplet("John Doe");
        profile.setEmail("john.doe@test.com");
        profile.setSkills(List.of("Java", "Spring Boot", "Docker"));
        profile.setAnneesExperience(4);
        aiResponse.setProfile(profile);
        
        ResponseEntity<CVAiDTO> responseEntity = new ResponseEntity<>(aiResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(CVAiDTO.class)))
                .thenReturn(responseEntity);
                
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(i -> {
            Candidature saved = i.getArgument(0);
            saved.setId("cand123");
            return saved;
        });
        
        // Act
        CandidatureResponse result = candidatureService.postuler("user123", "offre123", cv, lettre);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScoreMatching()).isGreaterThan(80.0);
        
        ArgumentCaptor<Candidature> candidatureCaptor = ArgumentCaptor.forClass(Candidature.class);
        verify(candidatureRepository).save(candidatureCaptor.capture());
        Candidature savedCandidature = candidatureCaptor.getValue();
        
        assertThat(savedCandidature.getCompetencesExtraites()).contains("Java", "Spring Boot", "Docker");
        assertThat(savedCandidature.getFormationRequise()).isFalse(); // all required skills matched
        
        verify(emailService).envoyerConfirmationCandidature(eq("john.doe@test.com"), eq("John Doe"), eq("Développeur Java"));
        verify(offreRepository).save(offre);
        assertThat(offre.getNombreCandidatures()).isEqualTo(1);
    }
}
