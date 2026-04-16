from flask import Flask, request, jsonify
from flask_cors import CORS
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import random
import json
import os

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

app = Flask(__name__)
CORS(app) # Autorise les requêtes depuis localhost:4200 (Angular)

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
