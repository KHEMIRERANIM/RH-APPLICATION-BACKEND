package tn.esprit.rh_rse.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
@Slf4j
public class QrCodeService {

    // ⚠️ Assure-toi que cette URL est accessible depuis ton téléphone
    // Ex: http://192.168.1.10:4200 (IP locale de ton PC sur le réseau)
    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Génère un QR code Base64 qui ouvre directement la page de signature
     *
     * @param candidatureId l'ID du candidat
     * @return image QR code en Base64
     */
    public String genererQrCodeBase64(String candidatureId) {
        try {
            // URL complète vers la page de signature
            String url = frontendUrl + "/recrutement/charte/" + candidatureId;

            // Création du QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 300, 300);

            // Conversion en image PNG puis en Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            log.error("Erreur génération QR Code : {}", e.getMessage());
            return null;
        }
    }
}