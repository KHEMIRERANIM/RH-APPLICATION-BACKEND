package tn.esprit.rh_rse.controller;

import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.response.RsePendingActionResponse;
import tn.esprit.rh_rse.dto.response.ValidationResponse;
import tn.esprit.rh_rse.entity.RseAction;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.RseActionRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.BadgeService;
import tn.esprit.rh_rse.service.LevelService;
import tn.esprit.rh_rse.service.RseService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rse/admin")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:5173",
        "http://127.0.0.1:5173"
})
public class RseAdminController {

    private final RseActionRepository actionRepo;
    private final RseService rseService;
    private final BadgeService badgeService;
    private final LevelService levelService;
    private final UserRepository userRepo;

    public RseAdminController(RseActionRepository actionRepo,
                              RseService rseService,
                              BadgeService badgeService,
                              LevelService levelService,
                              UserRepository userRepo) {
        this.actionRepo = actionRepo;
        this.rseService = rseService;
        this.badgeService = badgeService;
        this.levelService = levelService;
        this.userRepo = userRepo;
    }

    @PutMapping("/validate/{id}")
    public ValidationResponse validateAction(@PathVariable String id) {
        RseAction action = actionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Action introuvable"));

        action.setStatus("VALIDATED");

        User user = action.getEmployee();
        if (user == null) {
            throw new RuntimeException("Aucun employé lié à cette action");
        }

        int points = rseService.calculatePoints(action.getType());
        user.setRsePoints(user.getRsePoints() + points);

        String level = levelService.getLevel(user.getRsePoints());
        user.setRseLevel(level);

        List<String> badges = badgeService.assignBadges(user);

        userRepo.save(user);
        actionRepo.save(action);

        return new ValidationResponse(
                user.getId(),
                true,
                points,
                user.getRsePoints(),
                level,
                badges,
                user.getNom(),
                user.getPrenom(),
                user.getEmail()
        );
    }

    @GetMapping("/pending")
    public List<RsePendingActionResponse> getPendingActions() {
        return actionRepo.findByStatusOrderByCreatedAtDesc("PENDING")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/validated")
    public List<RsePendingActionResponse> getValidatedActions() {
        return actionRepo.findByStatusOrderByCreatedAtDesc("VALIDATED")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RsePendingActionResponse mapToResponse(RseAction action) {
        User user = action.getEmployee();
        List<String> badges = user != null ? badgeService.assignBadges(user) : List.of();

        return new RsePendingActionResponse(
                action.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getNom() : null,
                user != null ? user.getPrenom() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhotoUrl() : null,
                action.getType(),
                action.getDescription(),
                action.getStatus(),
                action.getCreatedAt(),
                user != null ? user.getRsePoints() : 0,
                user != null ? user.getRseLevel() : "—",
                badges
        );
    }
}