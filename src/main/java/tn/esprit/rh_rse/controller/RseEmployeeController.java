package tn.esprit.rh_rse.controller;

import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.RseActionRequest;
import tn.esprit.rh_rse.dto.response.RsePendingActionResponse;
import tn.esprit.rh_rse.dto.response.RseUserDataResponse;
import tn.esprit.rh_rse.entity.RseAction;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.RseActionRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.BadgeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rse/employee")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:5173",
        "http://127.0.0.1:5173"
})
public class RseEmployeeController {

    private final RseActionRepository actionRepo;
    private final UserRepository userRepo;
    private final BadgeService badgeService;

    public RseEmployeeController(RseActionRepository actionRepo,
                                 UserRepository userRepo,
                                 BadgeService badgeService) {
        this.actionRepo = actionRepo;
        this.userRepo = userRepo;
        this.badgeService = badgeService;
    }

    @PostMapping("/action")
    public RseAction submitAction(@RequestBody RseActionRequest request) {

        User user = userRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        RseAction action = RseAction.builder()
                .type(request.getType())
                .description(request.getDescription())
                .status("PENDING")
                .employee(user)
                .createdAt(LocalDateTime.now())
                .build();

        return actionRepo.save(action);
    }

    @GetMapping("/user/{id}")
    public RseUserDataResponse getUserRseData(@PathVariable String id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        List<String> badges = badgeService.assignBadges(user);

        return new RseUserDataResponse(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRsePoints(),
                user.getRseLevel(),
                badges
        );
    }

    @GetMapping("/actions/{employeeId}")
    public List<RsePendingActionResponse> getEmployeeActions(@PathVariable String employeeId) {
        return actionRepo.findByEmployee_IdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(action -> {
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
                })
                .collect(Collectors.toList());
    }
}