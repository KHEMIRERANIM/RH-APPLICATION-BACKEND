from flask import Flask, request, jsonify
from flask_cors import CORS
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import random

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


# --- MOTEUR NLU (INTENTIONS) POUR LE CHATBOT COACH ---
COACH_INTENTS = {
    "salutation": ["bonjour", "salut", "hello", "coucou", "hey", "bonsoir"],
    "amelioration": ["comment m'améliorer", "je veux m'améliorer", "j'ai des lacunes", "compétences manquantes", "que dois-je apprendre", "comment progresser", "améliorer"],
    "culture_rse": ["c'est quoi la rse", "environnement", "écologie", "valeurs de l'entreprise", "culture d'entreprise", "inclusion", "diversité", "éthique"],
    "preparation_entretien": ["comment me préparer", "conseil pour l'entretien", "aide entretien", "que dire à l'entretien", "questions fréquentes", "stress"],
    "salaire": ["salaire", "rémunération", "combien je vais gagner", "smic", "argent", "paye"]
}

intent_labels = list(COACH_INTENTS.keys())
intent_corpus = [" ".join(COACH_INTENTS[label]) for label in intent_labels]

nlu_vectorizer = TfidfVectorizer()
nlu_vectorizer.fit(intent_corpus)
nlu_matrix = nlu_vectorizer.transform(intent_corpus)


# --- ANALYSE DE SENTIMENT (RSE / EMPATHIE) ---
NEGATIVE_WORDS = ["stress", "stressé", "stressée", "peur", "panique", "angoisse", "anxieux", "perdu", "hésite", "mal", "craint", "pression"]
POSITIVE_WORDS = ["confiant", "prêt", "motivé", "enthousiaste", "super", "bien", "hate"]

def analyze_sentiment(text):
    text_lower = text.lower()
    neg_score = sum(1 for word in NEGATIVE_WORDS if word in text_lower)
    pos_score = sum(1 for word in POSITIVE_WORDS if word in text_lower)
    if neg_score > 0 and neg_score > pos_score:
        return "negative"
    if pos_score > 0:
        return "positive"
    return "neutral"


INTERVIEW_QUESTIONS = [
    "Parlez-moi d'une situation où vous avez fait preuve de leadership pour résoudre un conflit.",
    "Comment gérez-vous une critique constructive sur votre code ou votre travail ?",
    "Décrivez un moment où vous avez dû vous adapter rapidement à un changement inattendu.",
    "Qu'est-ce qui vous motive à vous lever le matin pour venir travailler ?",
    "Comment assurez-vous l'inclusion et la bienveillance au sein de votre équipe ?"
]


@app.route('/analyze-speech', methods=['POST'])
def analyze_speech():
    data = request.json
    candidate_text = data.get('text', '').lower()
    
    if len(candidate_text.strip()) < 10:
        return jsonify({"score": 0.0, "feedback": "Texte trop court."})
    
    target_matrix = vectorizer.transform([" ".join(IDEAL_BUSINESS_CORPUS)])
    candidate_matrix = vectorizer.transform([candidate_text])
    
    similarity_score = cosine_similarity(candidate_matrix, target_matrix)[0][0]
    final_score = min(100.0, (similarity_score * 300.0))
    word_count = len(candidate_text.split())
    if word_count < 10: final_score = min(final_score, 20.0)

    return jsonify({
        "score": round(final_score, 1),
        "wordCount": word_count,
        "keywordsDetected": -1,
        "transcribedText": candidate_text
    })


@app.route('/chat-coach', methods=['POST'])
def chat_coach():
    data = request.json
    user_message = data.get('message', '').lower()
    candidat_name = data.get('fullname', 'Candidat')
    offre_title = data.get('offreTitle', 'ce poste')
    missing_skills = data.get('missingSkills', [])
    history = data.get('history', [])
    is_interview_mode = data.get('isInterviewMode', False)
    
    if not user_message.strip():
        return jsonify({"reply": "Je n'ai pas compris votre silence. Comment puis-je vous aider ?"})
    
    # 1. MODE SIMULATEUR D'ENTRETIEN
    if is_interview_mode:
        if user_message == "start_interview":
            question = random.choice(INTERVIEW_QUESTIONS)
            reply = f"Mettez-vous en condition, l'entretien commence. 1ère question : {question}"
            return jsonify({"reply": reply})
        else:
            # Évaluation ML de la réponse du candidat
            target_matrix = nlu_vectorizer.transform([" ".join(IDEAL_BUSINESS_CORPUS) + " responsable équipe solution innovation RSE agile"])
            candidate_matrix = nlu_vectorizer.transform([user_message])
            score = cosine_similarity(candidate_matrix, target_matrix)[0][0] * 100
            
            if len(user_message.split()) < 5:
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
    # Analyse de Sentiment
    sentiment = analyze_sentiment(user_message)
    if sentiment == "negative":
        empathic_prefix = "Respirez profondément, il est tout à fait normal de ressentir de l'appréhension. Nous valorisons l'authenticité et le droit à l'erreur chez RH_RSE. "
    elif sentiment == "positive":
        empathic_prefix = "J'adore votre enthousiasme ! C'est exactement cette énergie que nous recherchons. "
        
    # Analyse du contexte (si le message est très court et fait suite à un autre)
    if len(user_message.split()) < 3 and len(history) > 2:
        last_bot_msg = history[-1].get('text', '')
        if "entretien" in last_bot_msg.lower():
            reply = empathic_prefix + "Avez-vous une inquiétude spécifique concernant cet entretien ? Je peux vous coacher."
            return jsonify({"reply": reply})

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
        else:
            reply = empathic_prefix + "Je suis à votre écoute pour préparer l'entretien."
            
    print(f"💬 [CHATBOT] Sentiment: {sentiment} | Intent: {intent_labels[best_match_idx]} | Score: {best_score:.2f}")
    
    return jsonify({"reply": reply})

if __name__ == '__main__':
    print("🧠 Modèle NLP et Intelligence Coach RSE activés sur le port 5000...")
    app.run(port=5000, debug=True)
