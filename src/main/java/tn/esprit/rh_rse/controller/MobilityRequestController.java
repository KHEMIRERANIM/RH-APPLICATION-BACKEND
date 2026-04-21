package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.MobilityRequestDTO;
import tn.esprit.rh_rse.dto.request.MobilityReviewDTO;
import tn.esprit.rh_rse.dto.response.AnalysisResult;
import tn.esprit.rh_rse.entity.Career;
import tn.esprit.rh_rse.entity.MobilityRequest;
import tn.esprit.rh_rse.entity.enums.MobilityStatus;
import tn.esprit.rh_rse.repository.CareerRepository;
import tn.esprit.rh_rse.service.MobilityRequestService;
import tn.esprit.rh_rse.service.MotivationAnalysisService;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobility")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MobilityRequestController {

    private final MobilityRequestService mobilityService;
    private final CareerRepository careerRepo;
    private final MotivationAnalysisService analysisService;

    @PostMapping
    public ResponseEntity<MobilityRequest> submit(@RequestBody MobilityRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mobilityService.submitRequest(dto));
    }

    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MobilityRequest> submitWithFile(
            @RequestParam("targetCareerId") String targetCareerId,
            @RequestParam("motivationFile") MultipartFile motivationFile
    ) throws IOException {

        MobilityRequestDTO dto = new MobilityRequestDTO();
        dto.setTargetCareerId(targetCareerId);
        dto.setMotivationLetter(motivationFile.getOriginalFilename());
        dto.setMotivationFileName(motivationFile.getOriginalFilename());
        dto.setMotivationFileBase64(Base64.getEncoder().encodeToString(motivationFile.getBytes()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mobilityService.submitRequest(dto));
    }

    // ── GET MY REQUESTS (employé connecté) ────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<List<MobilityRequest>> getMyRequests() {
        return ResponseEntity.ok(mobilityService.getMyRequests());
    }

    // ── GET ALL (admin) ───────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<MobilityRequest>> getAll() {
        return ResponseEntity.ok(mobilityService.getAll());
    }

    // ── GET BY EMPLOYEE ID ────────────────────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<MobilityRequest>> getByEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(mobilityService.getByEmployee(employeeId));
    }

    // ── GET BY STATUS ─────────────────────────────────────────────────────
    @GetMapping("/status/{status}")
    public ResponseEntity<List<MobilityRequest>> getByStatus(@PathVariable MobilityStatus status) {
        return ResponseEntity.ok(mobilityService.getByStatus(status));
    }

    // ── GET ONE ───────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<MobilityRequest> getById(@PathVariable String id) {
        return ResponseEntity.ok(mobilityService.getById(id));
    }

    // ── REVIEW ────────────────────────────────────────────────────────────
    @PatchMapping("/{id}/review")
    public ResponseEntity<MobilityRequest> review(
            @PathVariable String id,
            @RequestBody MobilityReviewDTO dto) {
        return ResponseEntity.ok(mobilityService.reviewRequest(id, dto));
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        mobilityService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }

    // ── PREVIEW ───────────────────────────────────────────────────────────
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable String id) {
        MobilityRequest request = mobilityService.getById(id);
        if (request.getMotivationFileBase64() == null || request.getMotivationFileBase64().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        byte[] fileBytes = Base64.getDecoder().decode(cleanBase64(request.getMotivationFileBase64()));
        return ResponseEntity.ok()
                .contentType(detectType(request.getMotivationFileName()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + request.getMotivationFileName() + "\"")
                .body(fileBytes);
    }

    // ── DOWNLOAD ──────────────────────────────────────────────────────────
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        MobilityRequest request = mobilityService.getById(id);
        if (request.getMotivationFileBase64() == null || request.getMotivationFileBase64().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        byte[] fileBytes = Base64.getDecoder().decode(cleanBase64(request.getMotivationFileBase64()));
        String fileName = request.getMotivationFileName() != null
                ? request.getMotivationFileName() : "motivation.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(fileBytes);
    }

    // ── ANALYZE MOTIVATION LETTER ─────────────────────────────────────────
    @GetMapping("/{id}/analyze")
    public ResponseEntity<AnalysisResult> analyzeMotivationLetter(@PathVariable String id) {
        MobilityRequest request = mobilityService.getById(id);

        if (request.getMotivationFileBase64() == null || request.getMotivationFileBase64().isBlank()) {
            // Solution propre sans constructeur problématique
            AnalysisResult errorResult = AnalysisResult.empty();
            // On enrichit l'objet pour indiquer l'erreur (si les setters existent)
            // Sinon, vous pouvez créer une méthode static error() dans AnalysisResult
            return ResponseEntity.badRequest().body(errorResult);
        }

        Career career = careerRepo.findById(request.getTargetCareerId())
                .orElseThrow(() -> new RuntimeException("Career not found"));

        // Décodage du Base64 en bytes
        byte[] fileBytes = Base64.getDecoder().decode(cleanBase64(request.getMotivationFileBase64()));

        AnalysisResult result = analysisService.analyzeLetterForJob(
                fileBytes,
                request.getMotivationFileName(),
                career.getTitle(),
                career.getDescription()
        );

        return ResponseEntity.ok(result);
    }

    private String cleanBase64(String base64) {
        return base64.contains(",") ? base64.split(",")[1] : base64;
    }

    private MediaType detectType(String fileName) {
        if (fileName == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lowerName.endsWith(".docx"))
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (lowerName.endsWith(".txt")) return MediaType.TEXT_PLAIN;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}