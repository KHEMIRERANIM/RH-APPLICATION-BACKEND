from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import pandas as pd
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="RH RSE - AI Microservice")

# Permet au Frontend Angular ou Backend Java de communiquer facilement (Local)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- 1. CHARGEMENT DU "CERVEAU" DE L'IA ---
print("Chargement du modèle XGBoost en mémoire...")
try:
    model_data = joblib.load("urgency_model.pkl")
    model = model_data["modele"]
    training_features = model_data["features"]
    print("✅ Modèle chargé avec succès.")
except Exception as e:
    print(f"⚠️ ERREUR: Impossible de charger 'urgency_model.pkl'. L'avez-vous bien placé dans ce dossier ? ({e})")

# --- 2. DÉFINITION DU FORMAT DE DONNÉES ATTENDU ---
class OffrePredictionRequest(BaseModel):
    prix: float
    mois_evenement: int
    jours_avant_debut: int
    places_initiales: int
    places_restantes: int
    categorie: str

# --- 3. CRÉATION DU ENDPOINT DE PRÉDICTION ---
@app.post("/predict-urgency")
def predict_urgency(request: OffrePredictionRequest):
    # Transformation de l'objet en DataFrame (1 ligne) pour XGBoost
    input_data = pd.DataFrame([{
        "prix": request.prix,
        "mois_evenement": request.mois_evenement,
        "jours_avant_debut": request.jours_avant_debut,
        "places_initiales": request.places_initiales,
        "places_restantes": request.places_restantes,
        "categorie": request.categorie
    }])
    
    # On transforme la catégorie en colonnes 1/0 comme durant l'entraînement
    input_data = pd.get_dummies(input_data, columns=['categorie'])
    
    # On reconstruit les colonnes vides (ex: s'il s'agit d'un HOTEL, les colonnes VOYAGE disparaissent, il faut repasser des 0)
    for col in training_features:
        if col not in input_data.columns:
            input_data[col] = 0
            
    # On aligne exactement l'ordre des colonnes avec celui du modèle
    input_features = input_data[training_features]
    
    # Appel de l'Intelligence Artificielle
    prediction = int(model.predict(input_features)[0])
    probability = float(model.predict_proba(input_features)[0][1])  # Confiance de rupture
    
    return {
        "urgence": bool(prediction == 1),
        "probabilite_rupture": round(probability, 4)
    }

if __name__ == "__main__":
    import uvicorn
    # Démarre le serveur local sur le port 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)
