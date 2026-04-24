package tn.esprit.rh_rse.service.Formation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class QrCodeFormationService {

    // ✅ Version corrigée avec support LocalDateTime
    public String genererQRCodeFormation(Map<String, Object> qrData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // ✅ Ajouter le module pour supporter LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            String qrContent = objectMapper.writeValueAsString(qrData);
            log.info("📱 Contenu QR Code: {}", qrContent);

            return genererQRCode(qrContent, 400, 400);

        } catch (Exception e) {
            log.error("Erreur génération QR Code", e);
            return null;
        }
    }

    public String genererQRCode(String contenu, int largeur, int hauteur) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(contenu, BarcodeFormat.QR_CODE, largeur, hauteur, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrCodeBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (WriterException e) {
            log.error("Erreur lors de la génération du QR Code: {}", e.getMessage());
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String genererQRCodeFormation(String formationId, String titre, String description) {
        String url = "http://localhost:4200/formations/detail/" + formationId;
        String contenu = String.format("""
            Formation: %s
            Description: %s
            Plus d'infos: %s
            """, titre, description, url);
        return genererQRCode(contenu, 200, 200);
    }
}