# ia-service/hf_classifier.py
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import requests
import uvicorn
import logging
from datetime import datetime

app = FastAPI(title="Document Classifier - Hugging Face")

# Configuration CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:8081"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Configuration Hugging Face
HF_API_KEY = "hf_xxxxxxxxxxxxxxxxxxxxx"  # REMPLACE PAR TA VRAIE CLÉ
HF_MODEL = "openai/clip-vit-large-patch14-336"

# Types de documents avec seuils d'auto-approbation
DOCUMENT_TYPES = {
    "certificat medical": {"auto_approve_threshold": 80, "label": "🏥 Certificat médical"},
    "prescription": {"auto_approve_threshold": 75, "label": "💊 Prescription médicale"},
    "carte identite": {"auto_approve_threshold": 0, "label": "🪪 Carte d'identité"},
    "autre": {"auto_approve_threshold": 0, "label": "📄 Autre document"}
}

@app.get("/health")
async def health_check():
    """Vérifier que le service fonctionne"""
    return {"status": "ok", "service": "Hugging Face Classifier", "timestamp": datetime.now()}

@app.post("/api/analyze-certificate")
async def analyze_certificate(file: UploadFile = File(...)):
    """
    Analyse un certificat médical et retourne le type avec pourcentage de confiance
    """
    # Vérifier le type de fichier
    if not file.content_type.startswith("image/"):
        raise HTTPException(400, "Le fichier doit être une image (JPEG, PNG)")

    try:
        # Lire l'image
        image_bytes = await file.read()

        if len(image_bytes) > 5 * 1024 * 1024:  # 5MB max
            raise HTTPException(400, "Fichier trop volumineux (max 5MB)")

        # Appel à l'API Hugging Face
        response = requests.post(
            f"https://api-inference.huggingface.co/models/{HF_MODEL}",
            headers={"Authorization": f"Bearer {HF_API_KEY}"},
            data=image_bytes,
            params={
                "parameters": {
                    "candidate_labels": list(DOCUMENT_TYPES.keys())
                }
            },
            timeout=30
        )

        if response.status_code != 200:
            logger.error(f"Erreur Hugging Face: {response.status_code} - {response.text}")
            raise HTTPException(502, "Service d'analyse temporairement indisponible")

        result = response.json()

        # Extraire le meilleur résultat
        best_label = result["labels"][0]
        best_score = result["scores"][0] * 100  # Convertir en pourcentage

        doc_info = DOCUMENT_TYPES.get(best_label, DOCUMENT_TYPES["autre"])

        # Déterminer si auto-approbation
        auto_approve = best_score >= doc_info["auto_approve_threshold"] and best_label != "autre"

        return {
            "success": True,
            "document_type": best_label,
            "document_label": doc_info["label"],
            "confidence": round(best_score, 2),
            "auto_approve": auto_approve,
            "all_scores": {
                label: round(score * 100, 2)
                for label, score in zip(result["labels"], result["scores"])
            },
            "message": get_message(best_label, best_score, auto_approve),
            "filename": file.filename
        }

    except requests.exceptions.Timeout:
        logger.error("Timeout Hugging Face API")
        raise HTTPException(504, "Le service d'analyse a pris trop de temps")
    except Exception as e:
        logger.error(f"Erreur inattendue: {str(e)}")
        raise HTTPException(500, f"Erreur technique: {str(e)}")

def get_message(doc_type, confidence, auto_approve):
    """Génère un message adapté"""
    if auto_approve:
        return f"✅ {DOCUMENT_TYPES[doc_type]['label']} détecté avec {confidence}% de confiance - Demande auto-approuvée"
    elif confidence >= 60:
        return f"📄 {DOCUMENT_TYPES[doc_type]['label']} détecté avec {confidence}% de confiance - Vérification RH recommandée"
    elif confidence >= 40:
        return f"⚠️ Document difficile à classifier ({confidence}%) - Vérification manuelle requise"
    else:
        return f"❌ Document non reconnu ({confidence}%) - Merci de fournir un certificat médical clair"

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8002)