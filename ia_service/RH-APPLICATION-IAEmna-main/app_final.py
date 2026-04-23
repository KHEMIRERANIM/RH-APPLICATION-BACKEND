from flask import Flask, request, jsonify
from flask_cors import CORS
import pickle
import json

app = Flask(__name__)
CORS(app)

with open('model_allergenes.pkl', 'rb') as f:
    model = pickle.load(f)
with open('vectorizer.pkl', 'rb') as f:
    vectorizer = pickle.load(f)
with open('labels.json', 'r') as f:
    labels = json.load(f)

def analyser(texte):
    texte = texte.lower()
    X = vectorizer.transform([texte])
    prediction = model.predict(X)[0]
    allergenes = []
    if prediction[labels.index('gluten')] == 1:
        allergenes.append('gluten')
    if prediction[labels.index('lactose')] == 1:
        allergenes.append('lactose')
    if prediction[labels.index('oeufs')] == 1:
        allergenes.append('oeufs')
    if prediction[labels.index('poisson')] == 1:
        allergenes.append('poisson')
    if prediction[labels.index('fruits_de_mer')] == 1:
        allergenes.append('fruits de mer')
    est_vegetarien = bool(prediction[labels.index('vegetarien')] == 1)
    est_sans_gluten = 'gluten' not in allergenes
    est_sans_lactose = 'lactose' not in allergenes
    tags = []
    if est_vegetarien: tags.append('vegetarien')
    if est_sans_gluten: tags.append('sans_gluten')
    if est_sans_lactose: tags.append('sans_lactose')
    niveau = 'eleve' if len(allergenes) >= 3 else 'moyen' if len(allergenes) >= 1 else 'faible'
    return {'allergenes': allergenes, 'ingredients_detectes': texte.split(','), 'tags_suggeres': tags, 'est_vegetarien': est_vegetarien, 'est_sans_gluten': est_sans_gluten, 'est_sans_lactose': est_sans_lactose, 'niveau_risque': niveau}

@app.route('/api/ia/analyser-ingredients', methods=['POST'])
def analyser_ingredients():
    data = request.get_json()
    ingredients = data.get('ingredients', '')
    if not ingredients:
        return jsonify({'error': 'ingredients manquants'}), 400
    return jsonify(analyser(ingredients))

@app.route('/api/ia/verifier-allergie', methods=['POST'])
def verifier_allergie():
    data = request.get_json()
    allergies_employe = [a.lower() for a in data.get('allergies', [])]
    plats = data.get('plats', [])
    resultats = []
    for plat in plats:
        texte = plat.get('ingredients', '') + ' ' + plat.get('nom', '') + ' ' + plat.get('description', '')
        analyse = analyser(texte)
        conflits = [a for a in allergies_employe if any(a in al for al in analyse['allergenes'])]
        resultats.append({'platId': plat.get('platId'), 'nom': plat.get('nom'), 'sur': len(conflits) == 0, 'conflits': conflits, 'allergenes_presents': analyse['allergenes'], 'tags_suggeres': analyse['tags_suggeres'], 'niveau_risque': analyse['niveau_risque']})
    return jsonify(resultats)

@app.route('/api/ia/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'message': 'IA ML actif'})

@app.route('/api/ia/analyser-avis', methods=['POST'])
def analyser_avis():
    data = request.get_json()
    commentaire = data.get('commentaire', '').lower()
    note = data.get('note', 3)
    mots_positifs = ['bon','bien','excellent','super','genial','parfait','delicieux','savoureux','genereux','frais','rapide','chaud','bonne','tres bon','j aime','recommande']
    mots_negatifs = ['mauvais','froid','trop','peu','petit','decevant','mediocre','pas bon','bof','nul','long','attente','dur','sec','insipide','fade']
    score = sum(1 for m in mots_positifs if m in commentaire) - sum(1 for m in mots_negatifs if m in commentaire)
    if note >= 4: score += 1
    elif note <= 2: score -= 1
    if score > 0:
        sentiment, message, emoji = 'positif', 'Merci pour votre retour positif !', 'smile'
    elif score < 0:
        sentiment, message, emoji = 'negatif', 'Merci pour votre retour. Nous allons nous ameliorer.', 'sad'
    else:
        sentiment, message, emoji = 'neutre', 'Merci pour votre avis !', 'neutral'
    points_cles = []
    if 'quantit' in commentaire: points_cles.append('quantite')
    if 'prix' in commentaire or 'cher' in commentaire: points_cles.append('prix')
    if 'rapide' in commentaire or 'lent' in commentaire or 'long' in commentaire: points_cles.append('delai')
    if 'chaud' in commentaire or 'froid' in commentaire: points_cles.append('temperature')
    if 'gout' in commentaire or 'saveur' in commentaire or 'delici' in commentaire: points_cles.append('gout')
    return jsonify({'sentiment': sentiment, 'message': message, 'emoji': emoji, 'score': score, 'points_cles': points_cles})

@app.route('/api/ia/analyser-nutrition', methods=['POST'])
def analyser_nutrition():
    data = request.get_json()
    texte = data.get('ingredients', '').lower() + ' ' + data.get('nom', '').lower()
    NUTRIMENTS = {'poulet': {'cal':165,'prot':31,'gluc':0,'lip':4,'sucre':0,'fibre':0},'boeuf': {'cal':250,'prot':26,'gluc':0,'lip':15,'sucre':0,'fibre':0},'agneau': {'cal':294,'prot':25,'gluc':0,'lip':21,'sucre':0,'fibre':0},'saumon': {'cal':208,'prot':20,'gluc':0,'lip':13,'sucre':0,'fibre':0},'thon': {'cal':132,'prot':29,'gluc':0,'lip':1,'sucre':0,'fibre':0},'crevette': {'cal':85,'prot':18,'gluc':1,'lip':1,'sucre':0,'fibre':0},'oeuf': {'cal':155,'prot':13,'gluc':1,'lip':11,'sucre':1,'fibre':0},'fromage': {'cal':402,'prot':25,'gluc':1,'lip':33,'sucre':1,'fibre':0},'lait': {'cal':61,'prot':3,'gluc':5,'lip':3,'sucre':5,'fibre':0},'beurre': {'cal':717,'prot':1,'gluc':0,'lip':81,'sucre':0,'fibre':0},'huile': {'cal':884,'prot':0,'gluc':0,'lip':100,'sucre':0,'fibre':0},'couscous': {'cal':112,'prot':4,'gluc':23,'lip':1,'sucre':0,'fibre':1},'riz': {'cal':130,'prot':3,'gluc':28,'lip':0,'sucre':0,'fibre':0},'pates': {'cal':131,'prot':5,'gluc':25,'lip':1,'sucre':1,'fibre':2},'pain': {'cal':265,'prot':9,'gluc':49,'lip':3,'sucre':5,'fibre':3},'farine': {'cal':364,'prot':10,'gluc':76,'lip':1,'sucre':0,'fibre':3},'lentille': {'cal':116,'prot':9,'gluc':20,'lip':0,'sucre':2,'fibre':8},'tomate': {'cal':18,'prot':1,'gluc':4,'lip':0,'sucre':3,'fibre':1},'salade': {'cal':15,'prot':1,'gluc':2,'lip':0,'sucre':1,'fibre':2},'carotte': {'cal':41,'prot':1,'gluc':10,'lip':0,'sucre':5,'fibre':3},'gateau': {'cal':350,'prot':5,'gluc':55,'lip':12,'sucre':35,'fibre':1},'chocolat': {'cal':546,'prot':5,'gluc':60,'lip':31,'sucre':48,'fibre':7},'cafe': {'cal':2,'prot':0,'gluc':0,'lip':0,'sucre':0,'fibre':0}}
    DIFFICILES_PMR = ['crevette','homard','crabe','moule','os','cote','entier','grille','brochette']
    FACILES_PMR = ['soupe','puree','veloute','yaourt','jus','cafe','riz','couscous']
    cal=prot=gluc=lip=sucre=fibre=0
    trouves=[]
    for aliment, n in NUTRIMENTS.items():
        if aliment in texte:
            cal+=n['cal']; prot+=n['prot']; gluc+=n['gluc']; lip+=n['lip']; sucre+=n['sucre']; fibre+=n['fibre']
            trouves.append(aliment)
    if cal==0: cal=250
    difficile = any(d in texte for d in DIFFICILES_PMR)
    facile = any(f in texte for f in FACILES_PMR)
    niveau = 'leger' if cal<300 else 'moyen' if cal<600 else 'copieux'
    total_macro = prot+gluc+lip if (prot+gluc+lip)>0 else 1
    return jsonify({'calories':cal,'proteines':prot,'glucides':gluc,'lipides':lip,'sucres':sucre,'fibres':fibre,'pct_proteines':round(prot/total_macro*100),'pct_glucides':round(gluc/total_macro*100),'pct_lipides':round(lip/total_macro*100),'niveau_calories':niveau,'pmr_adapte':not difficile,'pmr_raison':'Contient des aliments difficiles' if difficile else 'Facile a consommer' if facile else 'Adapte','ingredients_trouves':trouves})

if __name__ == '__main__':
    print('IA ML Service demarre sur http://localhost:5000')
    app.run(debug=True, port=5000)