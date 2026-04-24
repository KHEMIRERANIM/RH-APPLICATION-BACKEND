package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.NotifCarriere;
import tn.esprit.rh_rse.repository.NotifCarriereRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotifCarriereService {

    private final NotifCarriereRepository notifRepo;

    public void send(String userId, String title, String message, String type) {
        NotifCarriere notif = NotifCarriere.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        notifRepo.save(notif);
    }

    public List<NotifCarriere> getMyNotifications(String userId) {
        return notifRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(String userId) {
        return notifRepo.countByUserIdAndReadFalse(userId);
    }

    public void markAllRead(String userId) {
        List<NotifCarriere> notifs = notifRepo.findByUserIdOrderByCreatedAtDesc(userId);
        notifs.forEach(n -> n.setRead(true));
        notifRepo.saveAll(notifs);
    }

    public void markRead(String notifId) {
        notifRepo.findById(notifId).ifPresent(n -> {
            n.setRead(true);
            notifRepo.save(n);
        });
    }
}