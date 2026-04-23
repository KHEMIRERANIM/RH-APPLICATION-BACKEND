from flask import Flask, request, jsonify
from flask_cors import CORS
import pickle
import json

app = Flask(__name__)
CORS(app)

with open("model_allergenes.pkl","rb") as f2:
    model=pickle.load(f2)
with open("vectorizer.pkl","rb") as f2:
    vectorizer=pickle.load(f2)
with open("labels.json","r") as f2:
    labels=json.load(f2)

def analyser(t):
    t=t.lower()
    X=vectorizer.transform([t])
    p=model.predict(X)[0]
    al=[]
    if p[labels.index("gluten")]==1:al.append("gluten")
    if p[labels.index("lactose")]==1:al.append("lactose")
    if p[labels.index("oeufs")]==1:al.append("oeufs")
    if p[labels.index("poisson")]==1:al.append("poisson")
    if p[labels.index("fruits_de_mer")]==1:al.append("fruits de mer")
    veg=bool(p[labels.index("vegetarien")]==1)
    tags=[]
    if veg:tags.append("vegetarien")
    if "gluten" not in al:tags.append("sans_gluten")
    if "lactose" not in al:tags.append("sans_lactose")
    niv="eleve" if len(al)>=3 else "moyen" if len(al)>=1 else "faible"
    return{"allergenes":al,"ingredients_detectes":t.split(","),"tags_suggeres":tags,"est_vegetarien":veg,"est_sans_gluten":"gluten" not in al,"est_sans_lactose":"lactose" not in al,"niveau_risque":niv}

@app.route("/api/ia/analyser-ingredients",methods=["POST"])
def analyser_ingredients():
    data=request.get_json()
    ing=data.get("ingredients","")
    if not ing:return jsonify({"error":"ingredients manquants"}),400
    return jsonify(analyser(ing))

@app.route("/api/ia/verifier-allergie",methods=["POST"])
def verifier_allergie():
    data=request.get_json()
    all_emp=[a.lower() for a in data.get("allergies",[])]
    plats=data.get("plats",[])
    res=[]
    for plat in plats:
        txt=plat.get("ingredients","")+" "+plat.get("nom","")+" "+plat.get("description","")
        an=analyser(txt)
        conf=[a for a in all_emp if any(a in al for al in an["allergenes"])]
        res.append({"platId":plat.get("platId"),"nom":plat.get("nom"),"sur":len(conf)==0,"conflits":conf,"allergenes_presents":an["allergenes"],"tags_suggeres":an["tags_suggeres"],"niveau_risque":an["niveau_risque"]})
    return jsonify(res)

@app.route("/api/ia/health",methods=["GET"])
def health():
    return jsonify({"status":"ok","message":"IA ML actif"})

@app.route("/api/ia/analyser-avis",methods=["POST"])
def analyser_avis():
    data=request.get_json()
    com=data.get("commentaire","").lower()
    note=data.get("note",3)
    pos=["bon","bien","excellent","super","parfait","delicieux","savoureux","frais","rapide","recommande"]
    neg=["mauvais","froid","decevant","mediocre","nul","long","sec","insipide","fade"]
    score=sum(1 for m in pos if m in com)-sum(1 for m in neg if m in com)
    if note>=4:score+=1
    elif note<=2:score-=1
    if score>0:sent,msg,em="positif","Merci pour votre retour positif !","smile"
    elif score<0:sent,msg,em="negatif","Merci pour votre retour. Nous allons nous ameliorer.","sad"
    else:sent,msg,em="neutre","Merci pour votre avis !","neutral"
    pts=[]
    if "quantit" in com:pts.append("quantite")
    if "prix" in com or "cher" in com:pts.append("prix")
    if "rapide" in com or "long" in com:pts.append("delai")
    if "chaud" in com or "froid" in com:pts.append("temperature")
    if "gout" in com or "delici" in com:pts.append("gout")
    return jsonify({"sentiment":sent,"message":msg,"emoji":em,"score":score,"points_cles":pts})

@app.route("/api/ia/analyser-nutrition",methods=["POST"])
def analyser_nutrition():
    data=request.get_json()
    txt=data.get("ingredients","").lower()+" "+data.get("nom","").lower()
    NUT={"poulet":{"cal":165,"prot":31,"gluc":0,"lip":4,"sucre":0,"fibre":0},"boeuf":{"cal":250,"prot":26,"gluc":0,"lip":15,"sucre":0,"fibre":0},"saumon":{"cal":208,"prot":20,"gluc":0,"lip":13,"sucre":0,"fibre":0},"thon":{"cal":132,"prot":29,"gluc":0,"lip":1,"sucre":0,"fibre":0},"oeuf":{"cal":155,"prot":13,"gluc":1,"lip":11,"sucre":1,"fibre":0},"fromage":{"cal":402,"prot":25,"gluc":1,"lip":33,"sucre":1,"fibre":0},"lait":{"cal":61,"prot":3,"gluc":5,"lip":3,"sucre":5,"fibre":0},"beurre":{"cal":717,"prot":1,"gluc":0,"lip":81,"sucre":0,"fibre":0},"couscous":{"cal":112,"prot":4,"gluc":23,"lip":1,"sucre":0,"fibre":1},"riz":{"cal":130,"prot":3,"gluc":28,"lip":0,"sucre":0,"fibre":0},"pates":{"cal":131,"prot":5,"gluc":25,"lip":1,"sucre":1,"fibre":2},"pain":{"cal":265,"prot":9,"gluc":49,"lip":3,"sucre":5,"fibre":3},"farine":{"cal":364,"prot":10,"gluc":76,"lip":1,"sucre":0,"fibre":3},"tomate":{"cal":18,"prot":1,"gluc":4,"lip":0,"sucre":3,"fibre":1},"gateau":{"cal":350,"prot":5,"gluc":55,"lip":12,"sucre":35,"fibre":1},"chocolat":{"cal":546,"prot":5,"gluc":60,"lip":31,"sucre":48,"fibre":7},"cafe":{"cal":2,"prot":0,"gluc":0,"lip":0,"sucre":0,"fibre":0}}
    DIF=["crevette","homard","crabe","moule","os","grille","brochette"]
    FAC=["soupe","puree","yaourt","jus","cafe","riz","couscous"]
    cal=prot=gluc=lip=sucre=fibre=0;tr=[]
    for a,n in NUT.items():
        if a in txt:cal+=n["cal"];prot+=n["prot"];gluc+=n["gluc"];lip+=n["lip"];sucre+=n["sucre"];fibre+=n["fibre"];tr.append(a)
    if cal==0:cal=250
    dif=any(d in txt for d in DIF);fac=any(f in txt for f in FAC)
    niv="leger" if cal<300 else "moyen" if cal<600 else "copieux"
    tot=prot+gluc+lip if prot+gluc+lip>0 else 1
    return jsonify({"calories":cal,"proteines":prot,"glucides":gluc,"lipides":lip,"sucres":sucre,"fibres":fibre,"pct_proteines":round(prot/tot*100),"pct_glucides":round(gluc/tot*100),"pct_lipides":round(lip/tot*100),"niveau_calories":niv,"pmr_adapte":not dif,"pmr_raison":"Difficile a manipuler" if dif else "Facile a consommer" if fac else "Adapte","ingredients_trouves":tr})

if __name__=="__main__":
    print("IA ML Service demarre")
    app.run(debug=True,port=5000)
