from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app) # Autorise les requêtes depuis localhost:4200 (Angular)

@app.route('/analyze-speech', methods=['POST'])
def analyze_speech():
    data = request.json
    text = data.get('text', '').lower()
    
    if not text.strip():
        return jsonify({"score": 0.0, "feedback": "Aucune parole détectée en anglais."})
    
    words = text.split()
    length = len(words)
    
    # NLP: Lexique métier technique et RH
    business_keywords = [
        'experience', 'develop', 'software', 'manage', 'project', 'team',
        'skills', 'passionate', 'design', 'architecture', 'application',
        'engineer', 'leadership', 'agile', 'scrum', 'data', 'cloud', 'system',
        'motivation', 'learn', 'grow', 'hobby', 'hobbies', 'interested', 'technology',
        'hello', 'name', 'work', 'job', 'excited', 'opportunity'
    ]
    
    # Calcule la "densité sémantique" du CV
    keyword_count = sum(1 for word in words if any(kw in word for kw in business_keywords))
    
    score = min(100.0, (length * 2.0) + (keyword_count * 12.0))
    if length < 5:
        score = min(score, 20.0)
        
    print(f"[IA] Texte analysé : '{text}'")
    print(f"[IA] Mots techniques détectés : {keyword_count}")
    print(f"[IA] Score final : {score}%")
        
    return jsonify({
        "score": round(score, 1),
        "wordCount": length,
        "keywordsDetected": keyword_count,
        "transcribedText": text
    })

if __name__ == '__main__':
    print("🤖 Serveur IA Python en écoute sur le port 5000...")
    app.run(port=5000, debug=True)
