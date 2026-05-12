import os
import io

base_dir = r'c:\Users\INFOKOM\Desktop\RH-APPLICATION\rh_rse\src\main\java\tn\esprit\rh_rse'

transport_entity = '''package tn.esprit.rh_rse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.time.LocalDateTime;

@Document(collection = "notification_transports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTransport {

    @Id
    private String id;

    private String destinataireId;
    private String expediteurId;
    private String trajetId;
    private String reservationId;
    private String contenu;

    private TypeNotification type;

    @Builder.Default
    private boolean lu = false;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
'''

transport_repo = '''package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.util.List;

@Repository
public interface NotificationTransportRepository extends MongoRepository<NotificationTransport, String> {
    List<NotificationTransport> findByDestinataireId(String destinataireId);
    List<NotificationTransport> findByDestinataireIdAndLu(String destinataireId, Boolean lu);
    List<NotificationTransport> findByDestinataireIdAndType(String destinataireId, TypeNotification type);
    List<NotificationTransport> findByTrajetId(String trajetId);
    long countByDestinataireIdAndLu(String destinataireId, Boolean lu);
}
'''

transport_service = '''package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.util.List;

public interface NotificationTransportService {
    List<NotificationTransport> getAll();
    NotificationTransport getById(String id);
    NotificationTransport create(NotificationTransport notification);
    void delete(String id);
    List<NotificationTransport> getByDestinataire(String destinataireId);
    List<NotificationTransport> getNonLues(String destinataireId);
    NotificationTransport marquerCommeLu(String id);
    void marquerToutesCommeLues(String destinataireId);
    long countNonLues(String destinataireId);
    NotificationTransport envoyerNotification(String destinataireId, String expediteurId,
                                     String trajetId, TypeNotification type,
                                     String contenu);
    NotificationTransport envoyerDemandeConfirmation(String conducteurId, String passagerId,
                                            String trajetId, String reservationId);
}
'''

transport_impl = '''package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.NotificationTransportRepository;
import tn.esprit.rh_rse.service.NotificationTransportService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationTransportServiceImpl implements NotificationTransportService {

    private final NotificationTransportRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<NotificationTransport> getAll() {
        return notificationRepository.findAll();
    }

    @Override
    public NotificationTransport getById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée : " + id));
    }

    @Override
    public NotificationTransport create(NotificationTransport notification) {
        notification.setLu(false);
        notification.setDateCreation(LocalDateTime.now());
        NotificationTransport saved = notificationRepository.save(notification);
        envoyerWebSocket(saved);
        return saved;
    }

    @Override
    public void delete(String id) {
        notificationRepository.deleteById(id);
    }

    @Override
    public List<NotificationTransport> getByDestinataire(String destinataireId) {
        return notificationRepository.findByDestinataireId(destinataireId);
    }

    @Override
    public List<NotificationTransport> getNonLues(String destinataireId) {
        return notificationRepository.findByDestinataireIdAndLu(destinataireId, false);
    }

    @Override
    public NotificationTransport marquerCommeLu(String id) {
        NotificationTransport notification = getById(id);
        notification.setLu(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void marquerToutesCommeLues(String destinataireId) {
        List<NotificationTransport> nonLues = notificationRepository
                .findByDestinataireIdAndLu(destinataireId, false);
        nonLues.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(nonLues);
    }

    @Override
    public long countNonLues(String destinataireId) {
        return notificationRepository.countByDestinataireIdAndLu(destinataireId, false);
    }

    @Override
    public NotificationTransport envoyerNotification(String destinataireId, String expediteurId,
                                            String trajetId, TypeNotification type,
                                            String contenu) {
        NotificationTransport notification = NotificationTransport.builder()
                .destinataireId(destinataireId)
                .expediteurId(expediteurId)
                .trajetId(trajetId)
                .type(type)
                .contenu(contenu)
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();
        NotificationTransport saved = notificationRepository.save(notification);
        envoyerWebSocket(saved);
        return saved;
    }

    @Override
    public NotificationTransport envoyerDemandeConfirmation(String conducteurId, String passagerId,
                                                   String trajetId, String reservationId) {
        NotificationTransport notification = NotificationTransport.builder()
                .destinataireId(conducteurId)
                .expediteurId(passagerId)
                .trajetId(trajetId)
                .reservationId(reservationId)
                .type(TypeNotification.DEMANDE_CONFIRMATION)
                .contenu("Un employé demande à rejoindre votre trajet. Confirmez ou refusez.")
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();
        NotificationTransport saved = notificationRepository.save(notification);
        envoyerWebSocket(saved);
        return saved;
    }

    private void envoyerWebSocket(NotificationTransport notification) {
        try {
            if (notification.getDestinataireId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + notification.getDestinataireId(),
                        notification
                );
            }
        } catch (Exception e) {
            System.err.println("WebSocket non disponible pour: " + notification.getDestinataireId());
        }
    }
}
'''

transport_ctrl = '''package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.service.NotificationTransportService;

import java.util.List;

@RestController
@RequestMapping("/api/transport-notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationTransportController {

    private final NotificationTransportService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationTransport>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationTransport> getById(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @GetMapping("/destinataire/{destinataireId}")
    public ResponseEntity<List<NotificationTransport>> getByDestinataire(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getByDestinataire(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/non-lues")
    public ResponseEntity<List<NotificationTransport>> getNonLues(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getNonLues(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/count")
    public ResponseEntity<Long> countNonLues(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.countNonLues(destinataireId));
    }

    @PostMapping
    public ResponseEntity<NotificationTransport> create(@RequestBody NotificationTransport notification) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(notification));
    }

    @PutMapping("/{id}/lire")
    public ResponseEntity<NotificationTransport> marquerCommeLu(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.marquerCommeLu(id));
    }

    @PutMapping("/destinataire/{destinataireId}/lire-tout")
    public ResponseEntity<Void> marquerToutesCommeLues(@PathVariable String destinataireId) {
        notificationService.marquerToutesCommeLues(destinataireId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
'''

mutuelle_entity = '''package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    private String idUser;

    private TypeNotification type;

    private String idOffreAvantage;

    private String titreOffreAvantage;

    private String message;

    @Builder.Default
    private boolean lu = false;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
'''

mutuelle_service = '''package tn.esprit.rh_rse.service;

import tn.esprit.rh_rse.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> getMesNotifications(String idUser);
    Notification marquerCommeLu(String idNotification, String idUser);
    void marquerToutCommeLu(String idUser);
    long getNbNonLues(String idUser);
    void supprimerNotification(String idNotification, String idUser);
}
'''

mutuelle_impl = '''package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.repository.NotificationRepository;
import tn.esprit.rh_rse.service.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<Notification> getMesNotifications(String idUser) {
        return notificationRepository.findByIdUserOrderByDateCreationDesc(idUser);
    }

    @Override
    public Notification marquerCommeLu(String idNotification, String idUser) {
        Notification notification = notificationRepository.findById(idNotification)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        
        if (notification.getIdUser() != null && !notification.getIdUser().equals(idUser)) {
            throw new RuntimeException("Non autorisé");
        }
        
        notification.setLu(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void marquerToutCommeLu(String idUser) {
        List<Notification> notifications = notificationRepository.findByIdUserOrderByDateCreationDesc(idUser);
        for (Notification notif : notifications) {
            if (!notif.isLu()) {
                notif.setLu(true);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    public long getNbNonLues(String idUser) {
        return notificationRepository.countByIdUserAndLuFalse(idUser);
    }

    @Override
    public void supprimerNotification(String idNotification, String idUser) {
        Notification notification = notificationRepository.findById(idNotification)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        
        if (notification.getIdUser() != null && !notification.getIdUser().equals(idUser)) {
            throw new RuntimeException("Non autorisé");
        }
        
        notificationRepository.delete(notification);
    }
}
'''

mutuelle_repo = '''package tn.esprit.rh_rse.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.rh_rse.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByIdUserOrderByDateCreationDesc(String idUser);
    long countByIdUserAndLuFalse(String idUser);
}
'''

mutuelle_ctrl = '''package tn.esprit.rh_rse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.service.NotificationService;
import tn.esprit.rh_rse.config.jwt.JwtUtil;

import java.util.List;

@RestController
@RequestMapping("/api/mutuelle-notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    private String extraireIdUser(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            String jwt = headerAuth.substring(7);
            return jwtUtil.extractUserId(jwt);
        }
        throw new RuntimeException("Utilisateur non authentifié");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<List<Notification>> getMesNotifications(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        return ResponseEntity.ok(notificationService.getMesNotifications(idUser));
    }

    @PatchMapping("/{id}/lire")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Notification> marquerCommeLu(@PathVariable String id, HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        return ResponseEntity.ok(notificationService.marquerCommeLu(id, idUser));
    }

    @PatchMapping("/tout-lire")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Void> marquerToutCommeLu(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        notificationService.marquerToutCommeLu(idUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Long> getNbNonLues(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        return ResponseEntity.ok(notificationService.getNbNonLues(idUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<Void> supprimerNotification(@PathVariable String id, HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        notificationService.supprimerNotification(id, idUser);
        return ResponseEntity.noContent().build();
    }
}
'''

with io.open(os.path.join(base_dir, 'entity', 'NotificationTransport.java'), 'w', encoding='utf8') as f: f.write(transport_entity)
with io.open(os.path.join(base_dir, 'repository', 'NotificationTransportRepository.java'), 'w', encoding='utf8') as f: f.write(transport_repo)
with io.open(os.path.join(base_dir, 'service', 'NotificationTransportService.java'), 'w', encoding='utf8') as f: f.write(transport_service)
with io.open(os.path.join(base_dir, 'service', 'impl', 'NotificationTransportServiceImpl.java'), 'w', encoding='utf8') as f: f.write(transport_impl)
with io.open(os.path.join(base_dir, 'controller', 'NotificationTransportController.java'), 'w', encoding='utf8') as f: f.write(transport_ctrl)

with io.open(os.path.join(base_dir, 'entity', 'Notification.java'), 'w', encoding='utf8') as f: f.write(mutuelle_entity)
with io.open(os.path.join(base_dir, 'repository', 'NotificationRepository.java'), 'w', encoding='utf8') as f: f.write(mutuelle_repo)
with io.open(os.path.join(base_dir, 'service', 'NotificationService.java'), 'w', encoding='utf8') as f: f.write(mutuelle_service)
with io.open(os.path.join(base_dir, 'service', 'impl', 'NotificationServiceImpl.java'), 'w', encoding='utf8') as f: f.write(mutuelle_impl)
with io.open(os.path.join(base_dir, 'controller', 'NotificationController.java'), 'w', encoding='utf8') as f: f.write(mutuelle_ctrl)

transport_dependent_files = [
    os.path.join(base_dir, 'service', 'impl', 'TrajetServiceImpl.java'),
    os.path.join(base_dir, 'service', 'impl', 'ReservationServiceImpl.java'),
    os.path.join(base_dir, 'service', 'impl', 'ReservationNavetteServiceImpl.java'),
    os.path.join(base_dir, 'service', 'impl', 'BusServiceImpl.java'),
    os.path.join(base_dir, 'service', 'AlternativeTransportService.java'),
    os.path.join(base_dir, 'service', 'RemplaceReservationService.java'),
    os.path.join(base_dir, 'service', 'CareerPlanService.java')
]

for file_path in transport_dependent_files:
    if os.path.exists(file_path):
        with io.open(file_path, 'r', encoding='utf8') as f: content = f.read()
        content = content.replace('import tn.esprit.rh_rse.service.NotificationService;', 'import tn.esprit.rh_rse.service.NotificationTransportService;')
        content = content.replace('NotificationService notificationService', 'NotificationTransportService notificationService')
        content = content.replace('private final NotificationService notificationService;', 'private final NotificationTransportService notificationService;')
        with io.open(file_path, 'w', encoding='utf8') as f: f.write(content)

print('Done writing all files and updating transport dependencies!')
