from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import pandas as pd
import numpy as np
import joblib
from fastapi.middleware.cors import CORSMiddleware
app = FastAPI(title="Mobility AI API")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:5173",
        "http://127.0.0.1:5173"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# Charger les fichiers .pkl
model = joblib.load("models/mobility_model.pkl")
current_job_encoder = joblib.load("models/current_job_encoder.pkl")
target_job_encoder = joblib.load("models/target_job_encoder.pkl")
feature_columns = joblib.load("models/feature_columns.pkl")


JOB_REQUIREMENTS = {
    "backend developer": {
        "java_years": 4,
        "sql_years": 2,
        "python_years": 1
    },
    "cloud engineer": {
        "cloud_years": 4,
        "devops_years": 3,
        "security_years": 1
    },
    "cybersecurity analyst": {
        "security_years": 4,
        "cloud_years": 2,
        "testing_years": 2
    },
    "data analyst": {
        "python_years": 3,
        "sql_years": 4,
        "ml_years": 1
    },
    "devops engineer": {
        "devops_years": 4,
        "cloud_years": 3,
        "python_years": 1
    },
    "frontend developer": {
        "angular_years": 3,
        "react_years": 3,
        "testing_years": 1
    },
    "ml engineer": {
        "python_years": 4,
        "ml_years": 4,
        "sql_years": 2
    },
    "qa engineer": {
        "testing_years": 4,
        "sql_years": 1
    }
}

SKILL_RECOMMENDATIONS = {
    "python_years": [
        "Renforcer Python avancé",
        "Pratiquer sur des projets réels en Python"
    ],
    "sql_years": [
        "Améliorer SQL avancé",
        "Travailler les requêtes complexes"
    ],
    "java_years": [
        "Approfondir Java orienté objet",
        "Réaliser un projet backend avec Java"
    ],
    "angular_years": [
        "Renforcer Angular",
        "Créer une interface front moderne avec Angular"
    ],
    "react_years": [
        "Approfondir React",
        "Construire une application frontend complète"
    ],
    "cloud_years": [
        "Développer les compétences Cloud",
        "Se former sur AWS, Azure ou GCP"
    ],
    "devops_years": [
        "Renforcer les pratiques DevOps",
        "Travailler CI/CD, Docker et automatisation"
    ],
    "security_years": [
        "Approfondir la cybersécurité",
        "Se former sur sécurité applicative et réseau"
    ],
    "ml_years": [
        "Suivre une formation en Machine Learning",
        "Réaliser un projet IA concret"
    ],
    "testing_years": [
        "Renforcer les tests logiciels",
        "Pratiquer tests unitaires et intégration"
    ]
}


class MobilityRequest(BaseModel):
    current_job: str
    experience_years: float
    python_years: float
    sql_years: float
    java_years: float
    angular_years: float
    react_years: float
    cloud_years: float
    devops_years: float
    security_years: float
    ml_years: float
    testing_years: float


class TopJob(BaseModel):
    job: str
    score: float


class SkillGapItem(BaseModel):
    skill: str
    current: float
    required: float
    gap: float


class MobilityResponse(BaseModel):
    recommended_job: str
    confidence_score: float
    top_3_jobs: List[TopJob]
    skills_gap: List[SkillGapItem]
    evolution_plan: List[str]


@app.get("/")
def root():
    return {"message": "Mobility AI API is running"}


def compute_skills_gap(user_data: dict, target_job: str):
    requirements = JOB_REQUIREMENTS.get(target_job, {})
    gaps = []

    for skill, required_value in requirements.items():
        current_value = float(user_data.get(skill, 0))
        if current_value < required_value:
            gaps.append({
                "skill": skill,
                "current": current_value,
                "required": float(required_value),
                "gap": float(required_value - current_value)
            })

    return gaps


def generate_evolution_plan(gaps: list):
    plan = []

    for gap in gaps:
        skill = gap["skill"]
        recommendations = SKILL_RECOMMENDATIONS.get(skill, [])
        for rec in recommendations:
            if rec not in plan:
                plan.append(rec)

    if not plan:
        plan.append("Le profil est déjà bien aligné avec le poste recommandé.")
        plan.append("Continuer à renforcer les compétences actuelles par la pratique.")
    else:
        plan.append("Mettre en place un plan d’apprentissage sur 3 à 6 mois.")

    return plan


@app.post("/predict-mobility", response_model=MobilityResponse)
def predict_mobility(data: MobilityRequest):
    current_job_value = data.current_job.strip().lower()

    if current_job_value not in current_job_encoder.classes_:
        raise HTTPException(
            status_code=400,
            detail=f"current_job inconnu. Valeurs possibles: {list(current_job_encoder.classes_)}"
        )

    encoded_current_job = current_job_encoder.transform([current_job_value])[0]

    row_dict = {
        "current_job": encoded_current_job,
        "experience_years": data.experience_years,
        "python_years": data.python_years,
        "sql_years": data.sql_years,
        "java_years": data.java_years,
        "angular_years": data.angular_years,
        "react_years": data.react_years,
        "cloud_years": data.cloud_years,
        "devops_years": data.devops_years,
        "security_years": data.security_years,
        "ml_years": data.ml_years,
        "testing_years": data.testing_years
    }

    row = pd.DataFrame([row_dict])
    row = row[feature_columns]

    pred = model.predict(row)[0]
    probabilities = model.predict_proba(row)[0]

    recommended_job = target_job_encoder.inverse_transform([pred])[0]

    top_indices = np.argsort(probabilities)[::-1][:3]
    top_3_jobs = [
        TopJob(
            job=target_job_encoder.inverse_transform([idx])[0],
            score=round(float(probabilities[idx]), 4)
        )
        for idx in top_indices
    ]

    user_dict = data.dict()
    skills_gap_raw = compute_skills_gap(user_dict, recommended_job)
    evolution_plan = generate_evolution_plan(skills_gap_raw)

    skills_gap = [
        SkillGapItem(
            skill=item["skill"],
            current=item["current"],
            required=item["required"],
            gap=item["gap"]
        )
        for item in skills_gap_raw
    ]

    return MobilityResponse(
        recommended_job=recommended_job,
        confidence_score=round(float(np.max(probabilities)), 4),
        top_3_jobs=top_3_jobs,
        skills_gap=skills_gap,
        evolution_plan=evolution_plan
    )