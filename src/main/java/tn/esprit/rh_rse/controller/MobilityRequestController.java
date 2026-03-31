package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.MobilityRequestDTO;
import tn.esprit.rh_rse.dto.request.MobilityReviewDTO;
import tn.esprit.rh_rse.entity.MobilityRequest;
import tn.esprit.rh_rse.entity.enums.MobilityStatus;
import tn.esprit.rh_rse.service.MobilityRequestService;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/mobility")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MobilityRequestController {

    private final MobilityRequestService mobilityService;

    @PostMapping
    public ResponseEntity<MobilityRequest> submit(@RequestBody MobilityRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mobilityService.submitRequest(dto));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<MobilityRequest> review(
            @PathVariable String id,
            @RequestBody MobilityReviewDTO dto) {
        return ResponseEntity.ok(mobilityService.reviewRequest(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<MobilityRequest>> getAll() {
        return ResponseEntity.ok(mobilityService.getAll());
    }

    // ✅ Employé connecté — utilise le token JWT
    @GetMapping("/me")
    public ResponseEntity<List<MobilityRequest>> getMyRequests() {
        return ResponseEntity.ok(mobilityService.getMyRequests());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MobilityRequest>> getByStatus(
            @PathVariable MobilityStatus status) {
        return ResponseEntity.ok(mobilityService.getByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        mobilityService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MobilityRequest> submitWithFile(
            @RequestParam("targetCareerId") String targetCareerId,
            @RequestParam("motivationFile") MultipartFile motivationFile) throws IOException {

        // Convertir le fichier en base64 pour stockage
        String fileBase64 = Base64.getEncoder()
                .encodeToString(motivationFile.getBytes());
        String fileName = motivationFile.getOriginalFilename();

        MobilityRequestDTO dto = new MobilityRequestDTO();
        dto.setTargetCareerId(targetCareerId);
        dto.setMotivationLetter(fileName); // stocker le nom
        dto.setMotivationFileBase64(fileBase64); // stocker le contenu

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mobilityService.submitRequest(dto));
    }
}