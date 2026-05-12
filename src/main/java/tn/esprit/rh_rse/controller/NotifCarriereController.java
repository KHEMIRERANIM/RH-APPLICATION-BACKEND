package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.NotifCarriere;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.NotifCarriereService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications/carriere")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotifCarriereController {

    private final NotifCarriereService notifService;
    private final UserRepository userRepository;

    private String getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<NotifCarriere>> getMyNotifications() {
        return ResponseEntity.ok(notifService.getMyNotifications(getCurrentUserId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = notifService.countUnread(getCurrentUserId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead() {
        notifService.markAllRead(getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        notifService.markRead(id);
        return ResponseEntity.ok().build();
    }
}