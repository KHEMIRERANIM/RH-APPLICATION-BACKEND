import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.multioutput import MultiOutputClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
import pickle
import json

df = pd.read_csv('dataset_allergenes.csv')
print(f"Dataset: {len(df)} exemples")

X = df['ingredients']
y = df[['gluten','lactose','oeufs','poisson','fruits_de_mer','vegetarien']]

vectorizer = TfidfVectorizer(ngram_range=(1,2), min_df=1)
X_vec = vectorizer.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(X_vec, y, test_size=0.2, random_state=42)

model = MultiOutputClassifier(RandomForestClassifier(n_estimators=100, random_state=42))
model.fit(X_train, y_train)

y_pred = model.predict(X_test)
print("\n=== Performance ===")
for i, col in enumerate(y.columns):
    acc = accuracy_score(y_test.iloc[:,i], y_pred[:,i])
    print(f"{col}: {acc*100:.1f}%")

with open('model_allergenes.pkl', 'wb') as f:
    pickle.dump(model, f)
with open('vectorizer.pkl', 'wb') as f:
    pickle.dump(vectorizer, f)
with open('labels.json', 'w') as f:
    json.dump(list(y.columns), f)

print("\nModele sauvegarde!")