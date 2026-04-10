from flask import Flask, request, jsonify
from flask_cors import CORS
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

app = Flask(__name__)
CORS(app) # Autorise les requêtes depuis localhost:4200 (Angular)

# VRAI DATASET (CORPUS D'ENTRAINEMENT) : Ce que cherche l'entreprise
# C'est contre ce corpus que le Machine Learning (TF-IDF) va comparer le discours du candidat !
IDEAL_BUSINESS_CORPUS = [
    "I have extensive experience in software development and managing agile projects.",
    "My skills include designing strong architectures, leading technical teams, and deploying cloud systems.",
    "I am highly motivated to learn new technologies, solve complex data problems, and contribute to the company's growth.",
    "Professional communication, teamwork, and a passion for continuous learning in computer science."
]

# Initialisation du véritable Modèle Machine Learning (TF-IDF)
vectorizer = TfidfVectorizer(stop_words='english')
# Le modèle "apprend" le vocabulaire parfait de l'entreprise
vectorizer.fit(IDEAL_BUSINESS_CORPUS)

@app.route('/analyze-speech', methods=['POST'])
def analyze_speech():
    data = request.json
    candidate_text = data.get('text', '').lower()
    
    if len(candidate_text.strip()) < 10:
        return jsonify({"score": 0.0, "feedback": "Texte trop court pour le Modèle ML."})
    
    # 1. On vectorise le texte de référence (le Corpus idéal)
    target_matrix = vectorizer.transform([" ".join(IDEAL_BUSINESS_CORPUS)])
    
    # 2. On transforme le texte prononcé par le candidat en Vecteur Mathématique Math/ML
    candidate_matrix = vectorizer.transform([candidate_text])
    
    # 3. Calcul absolu de la Distance Cosinus (Machine Learning - Unsupervised Distance)
    similarity_array = cosine_similarity(candidate_matrix, target_matrix)
    similarity_score = similarity_array[0][0] # entre 0.0 et 1.0
    
    # Le score est ramené sur 100%. 
    # (Un score ML de 0.25+ en TF-IDF classique est en réalité excellent car les textes sont courts)
    final_score = min(100.0, (similarity_score * 300.0))
    
    # Validation croisée de fluidité
    word_count = len(candidate_text.split())
    if word_count < 10:
        final_score = min(final_score, 20.0)

    print(f"==============================")
    print(f"🤖 [MODELE ML] Texte du Candidat : '{candidate_text}'")
    print(f"🤖 [MODELE ML] Distance Cosinus Brute (TF-IDF) : {similarity_score:.4f}")
    print(f"🤖 [MODELE ML] Évaluation Finale / 100 : {final_score:.1f}%")
    print(f"==============================")
        
    return jsonify({
        "score": round(final_score, 1),
        "wordCount": word_count,
        "keywordsDetected": -1, # Géré par le modèle maintenant
        "transcribedText": candidate_text
    })

if __name__ == '__main__':
    print("🧠 Modèle Machine Learning SciKit-Learn activé sur le port 5000...")
    app.run(port=5000, debug=True)
