import sys
import traceback
import io
import os
import re
import json
import random
import urllib.request
from flask import Flask, request, jsonify
from flask_cors import CORS
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from PyPDF2 import PdfReader
from google import genai
from google.genai import types

# For Windows console encoding
if sys.platform == "win32":
    try:
        import io
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
    except Exception:
        pass

# --- LOADING KNOWLEDGE BASE ---
BASE_DIR = os.path.dirname(__file__)
def load_json(filename):
    try:
        with open(os.path.join(BASE_DIR, filename), 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        print(f"Error loading {filename}: {e}")
        return [] if filename.endswith('library.json') else {}

KNOWLEDGE_BASE = load_json('knowledge.json')
TRAINING_LIBRARY = load_json('training_library.json')
ENGLISH_REFERENCE = load_json('english_reference.json')
AMBASSADEURS = load_json('ambassadeurs.json')

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}}) # Autorise Angular et autres
MAX_CV_BYTES = 5 * 1024 * 1024

# --- CONFIGURATION GEMINI AI ---
GEMINI_API_KEY = "AIzaSyATWH3G3mB8NyyfDSpGii5jc7UKst_4iM4"
client = genai.Client(api_key=GEMINI_API_KEY)

# Modèles triés par priorité (Noms exacts détectés dans votre terminal)
MODELS_PRIORITY = [
    'models/gemini-1.5-flash',
    'models/gemini-2.0-flash',
    'models/gemini-flash-latest',
    'models/gemini-1.5-pro',
    'models/gemini-pro-latest'
]

# VRAI DATASET (CORPUS D'ENTRAINEMENT) : Ce que cherche l'entreprise
IDEAL_BUSINESS_CORPUS = [
    "I have extensive experience in software development and managing agile projects.",
    "My skills include designing strong architectures, leading technical teams, and deploying cloud systems.",
    "I am highly motivated to learn new technologies, solve complex data problems, and contribute to the company's growth.",
    "Professional communication, teamwork, and a passion for continuous learning in computer science."
]

vectorizer = TfidfVectorizer(stop_words='english')
vectorizer.fit(IDEAL_BUSINESS_CORPUS)


# --- MOTEUR NLU (INTENTIONS) AMELIORÉ ---
COACH_INTENTS = {
    "salutation": ["bonjour", "salut", "hello", "coucou", "hey", "bonsoir", "ça va"],
    "amelioration": ["comment m'améliorer", "je veux m'améliorer", "j'ai des lacunes", "compétences manquantes", "que dois-je apprendre", "comment progresser", "améliorer", "conseils", "conseil", "aide"],
    "culture_rse": ["c'est quoi la rse", "environnement", "écologie", "valeurs de l'entreprise", "culture d'entreprise", "inclusion", "diversité", "éthique", "entreprise", "valeurs"],
    "preparation_entretien": ["comment me préparer", "conseil pour l'entretien", "aide entretien", "que dire à l'entretien", "questions fréquentes", "stress", "entrainement", "simuler"],
    "salaire": ["salaire", "rémunération", "combien je vais gagner", "smic", "argent", "paye", "budget"],
    "trouver_offre": ["quelles sont les offres", "offre pour moi", "quel poste", "trouver un travail", "opportunités", "matcher", "compatibilité", "qui marche avec moi", "conseiller offre"],
    "equipe": ["qui travaille", "futur collègue", "ambassadeur", "équipe", "parler à", "collaborateur", "insider", "contact"]
}

# --- FONCTION DE NORMALISATION POUR LA ROBUSTESSE ---
def normalize_text(text):
    import re
    # Lowercase, remove special chars, and very basic plural handling (strip 's' at end of words)
    text = text.lower()
    text = re.sub(r'[^\w\s]', '', text)
    words = text.split()
    normalized = [w[:-1] if (w.endswith('s') and len(w) > 3) else w for w in words]
    return " ".join(normalized)

intent_labels = list(COACH_INTENTS.keys())
intent_corpus = [normalize_text(" ".join(COACH_INTENTS[label])) for label in intent_labels]

nlu_vectorizer = TfidfVectorizer()
nlu_vectorizer.fit(intent_corpus)
nlu_matrix = nlu_vectorizer.transform(intent_corpus)

INTERVIEW_QUESTIONS = [
    "Pouvez-vous nous parler d'une expérience où vous avez dû faire preuve d'adaptabilité ?",
    "Comment gérez-vous le stress lors d'une deadline importante ?",
    "Que signifie pour vous l'inclusion au sein d'une équipe technique ?",
    "Racontez-nous un projet dont vous êtes particulièrement fier.",
    "Comment réagissez-vous face à un conflit d'opinion avec un collègue ?",
    "Quelles sont vos méthodes pour rester à jour technologiquement ?"
]

def analyze_sentiment(text):
    positive_words = ['bien', 'super', 'génial', 'content', 'heureux', 'passion', 'motivé', 'excellent', 'merci', 'top', 'love', 'great', 'happy', 'joyeux', 'joyeuse', 'fier', 'fière', 'fiére', 'ravi', 'ravie', 'extra', 'cool', 'bravo']
    negative_words = ['stress', 'peur', 'difficile', 'nul', 'triste', 'mauvais', 'problème', 'inquiétude', 'fatigué', 'hard', 'bad', 'sad', 'déçu', 'angoisse', 'échec']
    
    text = text.lower()
    # Utilisation de regex pour ne matcher que des mots entiers (évite les faux positifs comme 'sad' dans 'ambassadeur')
    words = re.findall(r'\b\w+\b', text)
    
    pos_score = sum(1 for word in positive_words if word in words)
    neg_score = sum(1 for word in negative_words if word in words)
    
    if pos_score > neg_score:
        return "positive"
    elif neg_score > pos_score:
        return "negative"
    return "neutral"

def _normalize_spaces(text):
    return re.sub(r"\s+", " ", text or "").strip()

def extract_cv_text(file_storage):
    if not file_storage:
        return ""
    raw = file_storage.read()
    file_storage.stream.seek(0)
    if len(raw) > MAX_CV_BYTES:
        raise ValueError("CV trop volumineux (max 5MB).")
    try:
        reader = PdfReader(io.BytesIO(raw))
        pages = [page.extract_text() or "" for page in reader.pages]
        return "\n".join(pages).strip()
    except Exception:
        try:
            return raw.decode("utf-8", errors="ignore").strip()
        except Exception:
            return ""

def _extract_first_match(pattern, text):
    match = re.search(pattern, text, flags=re.IGNORECASE)
    return match.group(1).strip() if match and match.group(1) else None

def _extract_name(text):
    lines = [l.strip() for l in (text or "").splitlines() if l.strip()]
    email = _extract_first_match(r"([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})", text)
    for line in lines[:8]:
        words = line.replace("|", " ").split()
        if 2 <= len(words) <= 4 and all(w.replace("-", "").isalpha() for w in words):
            if email and line.lower() in email.lower():
                continue
            return line.title()
    return None

def _extract_phone(text):
    patterns = [
        r"(\+?\d[\d\s\-()]{7,}\d)",
        r"(?:tel|phone|mobile)\s*[:\-]?\s*(\+?\d[\d\s\-()]{7,}\d)"
    ]
    for pattern in patterns:
        value = _extract_first_match(pattern, text)
        if value:
            return _normalize_spaces(value)
    return None

def _extract_skills(text):
    cv_text = normalize_text(text)
    skills_library = [
        "Java", "Spring Boot", "Angular", "React", "Vue", "Python", "Docker", "Kubernetes",
        "AWS", "Azure", "SQL", "MongoDB", "PostgreSQL", "JavaScript", "TypeScript", "Node.js",
        "C#", "PHP", "Laravel", "Agile", "Scrum", "DevOps", "CI/CD", "Git", "Machine Learning",
        "Data Science", "NLP", "Spark", "Hadoop", "Leadership", "Communication", "Linux", "Selenium",
        "Flutter", "Kotlin", "Swift", "C++", "HTML", "CSS", "Figma",
        "AI", "IA", "Intelligence Artificielle", "Artificial Intelligence", "Deep Learning",
        "TensorFlow", "PyTorch", "Generated AI", "GenAI", "LLM", "Prompt Engineering",
        "OpenAI", "Scikit-Learn", "FastAPI", "Flask", "Solidity", "Blockchain", "NoSQL"
    ]
    found = []
    for skill in skills_library:
        if normalize_text(skill) in cv_text:
            found.append(skill)
    return found

def _extract_languages(text):
    language_map = {
        "francais": "Francais",
        "french": "Francais",
        "anglais": "Anglais",
        "english": "Anglais",
        "arabe": "Arabe",
        "arabic": "Arabe",
        "espagnol": "Espagnol",
        "spanish": "Espagnol",
        "allemand": "Allemand",
        "german": "Allemand",
        "italien": "Italien",
        "italian": "Italien"
    }
    normalized_text = normalize_text(text)
    found = sorted({label for key, label in language_map.items() if key in normalized_text})
    return found

def _extract_years_experience(text):
    years = [int(v) for v in re.findall(r"(\d{1,2})\s*(?:ans?|years?)", text or "", flags=re.IGNORECASE)]
    return max(years) if years else None

@app.route('/extract-profile', methods=['POST'])
def extract_profile():
    try:
        if 'cv' not in request.files:
            return jsonify({"error": "Fichier CV manquant (champ 'cv')."}), 400

        cv_file = request.files['cv']
        if not cv_file.filename:
            return jsonify({"error": "Nom de fichier CV invalide."}), 400

        if not cv_file.filename.lower().endswith('.pdf'):
            return jsonify({"error": "Format non supporte. Utilisez un fichier PDF."}), 400

        cv_text = extract_cv_text(cv_file)
        if not cv_text:
            return jsonify({"error": "Impossible d'extraire le texte du CV."}), 422

        email = _extract_first_match(r"([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})", cv_text)
        profile = {
            "nomComplet": _extract_name(cv_text),
            "email": email,
            "telephone": _extract_phone(cv_text),
            "adresse": _extract_first_match(r"(?:adresse|address)\s*[:\-]\s*([^\n]+)", cv_text),
            "anneesExperience": _extract_years_experience(cv_text),
            "skills": _extract_skills(cv_text),
            "languages": _extract_languages(cv_text)
        }

        missing_fields = [
            field for field in ["nomComplet", "email", "telephone"]
            if not profile.get(field)
        ]

        confidence = {
            "nomComplet": 0.75 if profile["nomComplet"] else 0.0,
            "email": 0.95 if profile["email"] else 0.0,
            "telephone": 0.85 if profile["telephone"] else 0.0,
            "skills": min(1.0, len(profile["skills"]) / 10.0),
            "languages": min(1.0, len(profile["languages"]) / 4.0),
            "anneesExperience": 0.8 if profile["anneesExperience"] is not None else 0.0
        }

        return jsonify({
            "profile": profile,
            "missingFields": missing_fields,
            "confidence": confidence
        })
    except ValueError as ve:
        return jsonify({"error": str(ve)}), 400
    except Exception as e:
        print(f"❌ ERREUR EXTRACTION PROFIL: {str(e)}")
        return jsonify({"error": "Erreur interne lors de l'extraction du CV."}), 500

# --- REAL ENGLISH ANALYSIS ENGINE (Loaded from JSON) ---
PROFESSIONAL_VOCABULARY = ENGLISH_REFERENCE

def cap_score(val):
    return max(0.0, min(100.0, val))

@app.route('/analyze-speech', methods=['POST'])
def analyze_speech():
    try:
        data = request.json
        text = data.get('text', '')
        if not text.strip():
            return jsonify({"score": 0.0, "feedback": "Silence détecté ou transcription vide."})

        # 1. Analyse de la Richesse (Vocabulaire unique)
        words = text.lower().split()
        unique_words = set(words)
        richness_score = min(100, (len(unique_words) / 20) * 100) if words else 0

        # 2. Analyse Sémantique (TF-IDF vs Ideal Corpus)
        user_vec = vectorizer.transform([text])
        ideal_vec = vectorizer.transform([" ".join(IDEAL_BUSINESS_CORPUS)])
        similarity = cosine_similarity(user_vec, ideal_vec)[0][0] * 100

        # 3. Détection de Vocabulaire Professionnel (Le "5000+ words" logic)
        prof_score = 0
        for cat in PROFESSIONAL_VOCABULARY.values():
            for word in cat:
                if word in text.lower():
                    prof_score += 5
        prof_score = min(100, prof_score)

        # 4. Calcul du Score Final
        # 40% Similitude sémantique, 40% Richesse Vocabulaire, 20% Mots Professionnels
        final_score = (similarity * 0.4) + (richness_score * 0.4) + (prof_score * 0.2)
        final_score = cap_score(final_score) # Cap at 100

        return jsonify({
            "score": round(final_score, 1),
            "wordCount": len(words),
            "uniqueCount": len(unique_words),
            "feedback": "Analyse NLP complète."
        })
    except Exception as e:
        print(f"❌ ERREUR ANALYSE SPEECH: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/chat-coach', methods=['POST'])
def chat_coach():
    try:
        data = request.json or {}
        user_message_raw = data.get('message', '')
        candidat_name = data.get('fullname', 'Candidat')
        offre_title = data.get('offreTitle', 'un poste')
        missing_skills = data.get('missingSkills', [])
        extracted_skills = data.get('extractedSkills', [])
        history = data.get('history', [])
        is_interview_mode = data.get('isInterviewMode', False)

        if not user_message_raw.strip():
            return jsonify({"reply": "Comment puis-je vous aider aujourd'hui ?"}), 400

        # Preparation du contexte enrichi pour Gemini
        system_instruction = f"""
        Tu es le 'Coach Virtuel Anti-Biais' de l'entreprise RH_RSE. 
        Ton rôle est d'accompagner les candidats de manière bienveillante, inclusive et professionnelle.

        IDENTITÉ ET VALEURS :
        - Entreprise : RH_RSE (Recrutement Humain et Responsabilité Sociétale des Entreprises).
        - Vision : {KNOWLEDGE_BASE.get('rse_policy', {}).get('vision')}
        - Valeurs : {KNOWLEDGE_BASE.get('company_values', [])}
        - FAQ : {KNOWLEDGE_BASE.get('faq', {})}

        MISSIONS :
        1. ANALYSE : Aide le candidat sur ses points forts et ses axes d'amélioration ({missing_skills}).
        2. PRÉPARATION : Conseils sur la méthode STAR et les soft-skills.
        3. RÉSEAUTAGE : Recommande un ambassadeur précis parmi cette liste : {AMBASSADEURS} si le candidat veut parler à l'équipe.
        4. FORMATION : Propose un cours spécifique de cette liste : {TRAINING_LIBRARY} si des compétences manquent.

        MODE INTERVIEW : {"ACTIVÉ - Pose une question d'entretien technique ou de soft-skill et évalue la réponse." if is_interview_mode else "DÉSACTIVÉ"}

        DONNÉES DU CANDIDAT :
        - Nom : {candidat_name}
        - Poste : {offre_title}
        - Compétences CV : {extracted_skills}

        CONSIGNES :
        - Ne dis JAMAIS que tu es un modèle de langage. Tu ES le Coach.
        - Utilise des emojis. Réponds en Markdown. Soyez encourageant.
        """

        # Conversion de l'historique (Exclure le message actuel s'il est déjà dans l'historique Angular)
        processed_history = history
        if history and history[-1]['sender'] == 'user' and history[-1]['text'] == user_message_raw:
            processed_history = history[:-1]

        chat_history = []
        for msg in processed_history:
            role = "user" if msg['sender'] == 'user' else "model"
            chat_history.append(types.Content(role=role, parts=[types.Part.from_text(text=msg['text'])]))

        # Stratégie de repli automatique
        reply = None
        last_err = ""

        for model_id in MODELS_PRIORITY:
            try:
                # Création de la session avec le modèle actuel
                chat_session = client.chats.create(
                    model=model_id,
                    config=types.GenerateContentConfig(system_instruction=system_instruction),
                    history=chat_history
                )
                response = chat_session.send_message(user_message_raw)
                reply = response.text
                if reply: 
                    print(f"✅ Chatbot : Succès avec {model_id}")
                    break
            except Exception as e:
                last_err = str(e)
                print(f"⚠️ Chatbot : Échec avec {model_id} (Quota ou indisponibilité)")
                continue

        if not reply:
            print(f"❌ AUCUN MODÈLE N'A RÉPONDU (Quota épuisé). Passage en mode local.")
            # Fallback sur une réponse basée sur les connaissances locales
            intent = "amelioration" # Par défaut
            reply = "Désolé, je rencontre une forte affluence. En attendant, je peux vous dire que chez RH_RSE, nous valorisons l'inclusion et le talent. N'hésitez pas à consulter nos offres ou à revenir vers moi dans quelques instants !"
            
            # Tentative de réponse plus précise via les fichiers JSON locaux
            for key in KNOWLEDGE_BASE.get('faq', {}):
                if key.lower() in user_message_raw.lower():
                    reply = KNOWLEDGE_BASE['faq'][key]
                    break

        return jsonify({"reply": reply})

    except Exception as e:
        print(f"❌ ERREUR CRITIQUE CHATBOT IA:")
        traceback.print_exc()
        return jsonify({"reply": "Désolé, j'ai rencontré une petite erreur technique. Pouvez-vous reformuler ?"}), 500

# --- NEW ENDPOINT: ADVANCED AI BIAS DETECTION ---
@app.route('/analyze-bias-ai', methods=['POST'])
def analyze_bias_ai():
    try:
        data = request.json
        description = data.get('description', '')
        
        if not description or len(description) < 10:
            return jsonify({"score": 100, "suggestions": []})

        prompt = f"""
        En tant qu'expert en Inclusion et Diversité (RSE) dans le recrutement, analyse la description de poste suivante pour détecter des biais subtils (genre, âge, culture, origine, etc.).
        
        Description : "{description}"
        
        Réponds uniquement en format JSON avec cette structure :
        {{
            "inclusionScore": (nombre entre 0 et 100),
            "biasesFound": ["liste de biais détectés ou expressions problématiques"],
            "suggestions": [
                {{
                    "problem": "explication du biais",
                    "suggestion": "version plus inclusive"
                }}
            ],
            "conclusion": "un bref résumé encourageant"
        }}
        """

        # Liste de modèles à tester par ordre de priorité
        response = None
        last_error = ""

        for model_id in MODELS_PRIORITY:
            try:
                response = client.models.generate_content(
                    model=model_id,
                    contents=prompt
                )
                if response:
                    print(f"✅ Biais AI : Succès avec {model_id}")
                    break
            except Exception as e:
                last_error = str(e)
                print(f"⚠️ Biais AI : Échec avec {model_id}")
                continue

        if not response:
            return jsonify({"error": f"Tous les modèles Gemini sont saturés ou restreints pour votre clé. Erreur : {last_error}"}), 500

        # Extraire le JSON de la réponse (pour éviter les backticks ```json ... ```)
        text_resp = response.text
        json_match = re.search(r'\{.*\}', text_resp, re.DOTALL)
        if json_match:
            result = json.loads(json_match.group())
            return jsonify(result)
        else:
            return jsonify({"error": "Format IA invalide"}), 500

    except Exception as e:
        print(f"❌ ERREUR BIAS AI: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    try:
        print("Modèles disponibles pour votre clé :")
        for m in client.models.list():
            print(f"  - {m.name}")
    except Exception as e:
        print(f"Impossible de lister les modèles : {e}")

    print("Modèle NLP et Intelligence Coach RSE activés sur le port 5000...")
    app.run(port=5000, debug=True)
