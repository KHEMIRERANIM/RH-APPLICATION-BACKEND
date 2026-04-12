from flask import Flask, request, jsonify
from flask_cors import CORS
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import random
import urllib.request
import json

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

# ... (Analyse de sentiment et questions d'entretien restent identiques) ...
# [Note: I am keeping lines 42-89 from the original file]

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
                reply = empathic_prefix + "Notre groupe prône l'inclusion sociale (Anti-biais cognitifs), le bien-être au travail et une empreinte carbone maîtrisée."
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
                
        return jsonify({"reply": reply})

    except Exception as e:
        print(f"❌ ERREUR CRITIQUE CHATBOT: {str(e)}")
        return jsonify({"reply": "Désolé, j'ai rencontré une erreur interne. Réessayez dans un instant."}), 500

if __name__ == '__main__':
    print("🧠 Modèle NLP et Intelligence Coach RSE activés sur le port 5000...")
    app.run(port=5000, debug=True)
