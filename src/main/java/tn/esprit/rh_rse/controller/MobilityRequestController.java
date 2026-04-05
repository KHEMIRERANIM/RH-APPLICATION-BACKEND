package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
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

    // =========================
    // CREATE REQUEST (JSON)
    // =========================
    @PostMapping
    public ResponseEntity<MobilityRequest> submit(@RequestBody MobilityRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mobilityService.submitRequest(dto));
    }

    // =========================
    // CREATE WITH FILE
    // =========================
    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MobilityRequest> submitWithFile(
            @RequestParam("targetCareerId") String targetCareerId,
            @RequestParam("motivationFile") MultipartFile motivationFile
    ) throws IOException {

        MobilityRequestDTO dto = new MobilityRequestDTO();

        dto.setTargetCareerId(targetCareerId);
        dto.setMotivationLetter(motivationFile.getOriginalFilename());
        dto.setMotivationFileName(motivationFile.getOriginalFilename());

        String base64 = Base64.getEncoder().encodeToString(motivationFile.getBytes());
        dto.setMotivationFileBase64(base64);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mobilityService.submitRequest(dto));
    }

    // =========================
    // GET ALL
    // =========================
    @GetMapping
    public ResponseEntity<List<MobilityRequest>> getAll() {
        return ResponseEntity.ok(mobilityService.getAll());
    }

    // =========================
    // GET ONE (IMPORTANT)
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<MobilityRequest> getById(@PathVariable String id) {
        return ResponseEntity.ok(mobilityService.getById(id));
    }

    // =========================
    // REVIEW
    // =========================
    @PatchMapping("/{id}/review")
    public ResponseEntity<MobilityRequest> review(
            @PathVariable String id,
            @RequestBody MobilityReviewDTO dto) {
        return ResponseEntity.ok(mobilityService.reviewRequest(id, dto));
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        mobilityService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable String id) {

        MobilityRequest request = mobilityService.getById(id);

        if (request == null || request.getMotivationFileBase64() == null) {
            return ResponseEntity.notFound().build();
        }

        String base64 = cleanBase64(request.getMotivationFileBase64());
        byte[] fileBytes = Base64.getDecoder().decode(base64);

        MediaType type = detectType(request.getMotivationFileName());

        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + request.getMotivationFileName() + "\"")
                .body(fileBytes);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {

        MobilityRequest request = mobilityService.getById(id);

        if (request == null || request.getMotivationFileBase64() == null) {
            return ResponseEntity.notFound().build();
        }

        String base64 = cleanBase64(request.getMotivationFileBase64());
        byte[] fileBytes = Base64.getDecoder().decode(base64);

        String fileName = request.getMotivationFileName();
        if (fileName == null) fileName = "motivation.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(fileBytes);
    }
    private String cleanBase64(String base64) {
        if (base64.contains(",")) {
            return base64.split(",")[1];
        }
        return base64;
    }

    private MediaType detectType(String fileName) {
        if (fileName == null) return MediaType.APPLICATION_OCTET_STREAM;

        if (fileName.endsWith(".pdf")) return MediaType.APPLICATION_PDF;

        if (fileName.endsWith(".docx")) {
            return MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        }

        if (fileName.endsWith(".txt")) return MediaType.TEXT_PLAIN;

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}