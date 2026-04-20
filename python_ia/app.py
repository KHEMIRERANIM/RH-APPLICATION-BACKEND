from flask import Flask, request, jsonify
from flask_cors import CORS
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import random
import json
import os
import re
import io
import urllib.request
from PyPDF2 import PdfReader

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

app = Flask(__name__)
CORS(app) # Autorise les requêtes depuis localhost:4200 (Angular)
MAX_CV_BYTES = 5 * 1024 * 1024

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
    "trouver_offre": ["quelles sont les offres", "offre pour moi", "quel poste", "trouver un travail", "opportunités", "matcher", "compatibilité", "qui marche avec moi", "conseiller offre"]
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
    positive_words = ['bien', 'super', 'génial', 'content', 'heureux', 'passion', 'motivé', 'excellent', 'merci', 'top', 'love', 'great', 'happy']
    negative_words = ['stress', 'peur', 'difficile', 'nul', 'triste', 'mauvais', 'problème', 'inquiétude', 'fatigué', 'hard', 'bad', 'sad']
    
    text = text.lower()
    pos_score = sum(1 for word in positive_words if word in text)
    neg_score = sum(1 for word in negative_words if word in text)
    
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
        "Flutter", "Kotlin", "Swift", "C++", "HTML", "CSS", "Figma"
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

def cap_score(val):
    return max(0.0, min(100.0, val))

@app.route('/chat-coach', methods=['POST'])
def chat_coach():
    try:
        data = request.json
        user_message_raw = data.get('message', '')
        user_message = normalize_text(user_message_raw)
        candidat_name = data.get('fullname', 'Candidat')
        offre_title = data.get('offreTitle', 'ce poste')
        missing_skills = data.get('missingSkills', [])
        extracted_skills = data.get('extractedSkills', [])
        history = data.get('history', [])
        is_interview_mode = data.get('isInterviewMode', False)
        
        if not user_message_raw.strip():
            return jsonify({"reply": "Je n'ai pas compris votre silence. Comment puis-je vous aider ?"})
        
        # 1. MODE SIMULATEUR D'ENTRETIEN
        if is_interview_mode:
            if user_message_raw.lower() == "start_interview":
                question = random.choice(INTERVIEW_QUESTIONS)
                reply = f"Mettez-vous en condition, l'entretien commence. 1ère question : {question}"
                return jsonify({"reply": reply})
            else:
                target_matrix = nlu_vectorizer.transform([normalize_text(" ".join(IDEAL_BUSINESS_CORPUS) + " responsable équipe solution innovation RSE agile")])
                candidate_matrix = nlu_vectorizer.transform([user_message])
                score = cosine_similarity(candidate_matrix, target_matrix)[0][0] * 100
                
                if len(user_message_raw.split()) < 5:
                    feedback = "Votre réponse est très courte. Pensez à l'étoffer avec des exemples concrets (Méthode STAR)."
                elif score > 20:
                    feedback = "Excellente réponse, vous montrez une belle profondeur de réflexion !"
                else:
                    feedback = "C'est un bon début, mais n'hésitez pas à relier votre réponse à nos valeurs ou vos expériences passées."
                    
                next_question = random.choice(INTERVIEW_QUESTIONS)
                reply = f"✅ Évaluation IA : {feedback} (Score Mots-Clés : {int(score)}/100)\n\nQuestion suivante : {next_question}"
                return jsonify({"reply": reply})

        # 2. MODE COACH CLASSIQUE (NLU + Mémoire Contextuelle)
        user_vec = nlu_vectorizer.transform([user_message])
        sim_scores = cosine_similarity(user_vec, nlu_matrix)[0]
        best_match_idx = sim_scores.argmax()
        best_score = sim_scores[best_match_idx]
        
        empathic_prefix = ""
        sentiment = analyze_sentiment(user_message_raw)
        if sentiment == "negative":
            empathic_prefix = "Respirez profondément, il est tout à fait normal de ressentir de l'appréhension. Nous valorisons l'authenticité et le droit à l'erreur chez RH_RSE. "
        elif sentiment == "positive":
            empathic_prefix = "J'adore votre enthousiasme ! C'est exactement cette énergie que nous recherchons. "
            
        if len(user_message_raw.split()) < 3 and len(history) > 2:
            last_bot_msg = history[-1].get('text', '')
            if "entretien" in last_bot_msg.lower():
                reply = empathic_prefix + "Avez-vous une inquiétude spécifique concernant cet entretien ? Je peux vous coacher."
                return jsonify({"reply": reply})

        # Seuil de déclenchement (0.1)
        if best_score < 0.1:
            # --- SEMANTIC KNOWLEDGE SEARCH (RAG LITE) ---
            found_answer = None
            query = user_message.lower()
            
            # Simple keyword matching for knowledge base
            for category, content in KNOWLEDGE_BASE.items():
                if isinstance(content, dict):
                    for key, val in content.items():
                        if key in query or any(word in query for word in key.split('_')):
                            found_answer = val
                            break
                elif isinstance(content, list):
                    if category in query:
                        found_answer = f"Nos valeurs sont : {', '.join(content)}."
                if found_answer: break
            
            if found_answer:
                reply = empathic_prefix + found_answer
            else:
                reply = empathic_prefix + "C'est une réflexion intéressante ! Toutefois, en tant que Coach RSE, je suis focalisé sur votre progression technique, nos valeurs d'entreprise et la préparation à l'entretien."
        else:
            intent = intent_labels[best_match_idx]
            if intent == "salutation":
                reply = empathic_prefix + f"Bonjour {candidat_name} ! Je suis votre Coach Virtuel Anti-Biais. Félicitations pour votre profil pour : {offre_title}. Comment voulez-vous orienter notre préparation ?"
            elif intent == "amelioration":
                if missing_skills and len(missing_skills) > 0:
                    reply = empathic_prefix + f"D'après l'IA, voici les compétences à consolider : {', '.join(missing_skills)}. Ne vous inquiétez pas la perfection n'existe pas, misez sur votre capacité d'apprentissage !"
                else:
                    reply = empathic_prefix + "Votre profil technique est impeccable. Nous examinerons surtout vos soft-skills (agilité, empathie, pédagogie)."
            elif intent == "culture_rse":
                reply = empathic_prefix + "Notre groupe RH_RSE prône l'inclusion radicale (Anti-biais cognitifs), le bien-être au travail via la flexibilité et une empreinte carbone maîtrisée. Nous sommes certifiés 'Entreprise Responsable' !"
            elif intent == "preparation_entretien":
                reply = empathic_prefix + "Pour préparer l'entretien : préparez vos réussites en méthode STAR (Situation, Tâche, Action, Résultat). Souvenez-vous, c'est aussi un échange humain, soyez vous-même !"
            elif intent == "salaire":
                reply = empathic_prefix + "La question financière est légitime. Toute grille salariale de RH_RSE est transparente et exempte de biais de genre. Elle sera abordée avec le recruteur."
            elif intent == "trouver_offre":
                try:
                    url = "http://localhost:8081/api/recrutement/offres"
                    req = urllib.request.Request(url)
                    response = urllib.request.urlopen(req)
                    offres = json.loads(response.read().decode('utf-8'))
                    
                    if not extracted_skills or len(extracted_skills) == 0:
                        reply = empathic_prefix + "Analyse de documents requise. Veuillez d'abord soumettre votre CV pour que je puisse matcher vos compétences !"
                    elif not offres:
                        reply = empathic_prefix + "Aucune offre n'est publiée pour le moment."
                    else:
                        skills_text = " ".join(extracted_skills)
                        offer_corpus = [ (off.get('titre', '') + " " + off.get('description', '') + " " + " ".join(off.get('competencesRequises', []))) for off in offres ]
                        offer_vec = TfidfVectorizer(stop_words='english')
                        offer_matrix = offer_vec.fit_transform(offer_corpus)
                        user_query_vec = offer_vec.transform([skills_text])
                        sims = cosine_similarity(user_query_vec, offer_matrix)[0]
                        idx = sims.argmax()
                        if sims[idx] > 0.05:
                            reply = empathic_prefix + f"L'offre '{offres[idx]['titre']}' est idéale pour vous (Matching IA : {int(sims[idx]*100)}%)."
                        else:
                            reply = empathic_prefix + "Je n'ai pas trouvé d'offre correspondant exactement à votre profil actuel."
                except Exception:
                    reply = "Service de base de données temporairement indisponible."
            else:
                reply = empathic_prefix + "Comment puis-je vous aider ?"
                
        # --- MENTORAT PROACTIF ANTI-BIAIS ---
        bias_keywords = ["femme", "homme", "vieux", "jeune", "origine", "nationalité", "religion"]
        if any(w in user_message.lower() for w in bias_keywords):
            reply = "💡 [Note RSE] : Je remarque une mention de critères d'identité. Rappelez-vous que chez RH_RSE, notre IA de matching ignore ces données pour se concentrer exclusivement sur vos compétences réelles. C'est notre garantie d'équité ! \n\n" + reply

        # --- RECOMMANDATION DE FORMATIONS (UPSKILLING) ---
        if "conseil" in user_message.lower() or "améliorer" in user_message.lower() or "apprendre" in user_message.lower():
            for skill in missing_skills:
                match = next((t for t in TRAINING_LIBRARY if t['skill'].lower() in skill.lower()), None)
                if match:
                    reply += f"\n\n🎓 [Action Formation] : Pour renforcer votre profil en {skill}, je vous suggère de suivre le module '{match['course_name']}' ({match['duration']})."
                    break

        return jsonify({"reply": reply})

    except Exception as e:
        print(f"❌ ERREUR CRITIQUE CHATBOT: {str(e)}")
        return jsonify({"reply": "Désolé, j'ai rencontré une erreur interne. Réessayez dans un instant."}), 500

if __name__ == '__main__':
    print("🧠 Modèle NLP et Intelligence Coach RSE activés sur le port 5000...")
    app.run(port=5000, debug=True)
