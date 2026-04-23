package tn.esprit.rh_rse.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

@Service
public class PredictionService {

    public String getWeeklyPrediction() {
        try {
            // Utilisation du chemin absolu pour être sûr que le script est trouvé
            String pythonPath = "python"; 
            File workingDir = new File("c:/Users/INFOKOM/Desktop/RH-APPLICATION/rh_rse/src/main/resources/Python");
            String scriptName = "predict.py";

            if (!workingDir.exists()) {
                System.err.println("CRITICAL: Dossier Python non trouvé à: " + workingDir.getAbsolutePath());
                return getFallback();
            }

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptName);
            pb.directory(workingDir);
            pb.redirectErrorStream(true); 

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String lastLine = "";
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lastLine = line;
                }
                System.out.println("Python: " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Le script Python a échoué avec le code: " + exitCode);
                return getFallback();
            }

            return lastLine;

        } catch (Exception e) {
            e.printStackTrace();
            return getFallback();
        }
    }

    private String getFallback() {
        return "{\"predictions\":[], \"total\":0, \"status\":\"error\"}";
    }
}