# predict.py - Version corrigée
import joblib
import pandas as pd
import json
from datetime import datetime, timedelta
import os
import requests
import sys

API_KEY = "399cd3be53e3b265a97cadad4a8c8a41"
CITY = "Tunis"
URL = f"http://api.openweathermap.org/data/2.5/forecast?q={CITY}&appid={API_KEY}&units=metric&lang=fr"

script_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(script_dir)

print("🔮 Chargement du modèle...", file=sys.stderr)

model = joblib.load('bus_prediction_model (1).pkl')
historical_avg = joblib.load('historical_averages.pkl')
metadata = joblib.load('model_metadata (1).pkl')
weather_mapping = metadata['weather_mapping']

print("✅ Modèle et moyennes chargés!", file=sys.stderr)

def get_season(month):
    if month in [12, 1, 2]: return 0
    if month in [3, 4, 5]: return 1
    if month in [6, 7, 8]: return 2
    return 3

def get_weather_forecast():
    try:
        response = requests.get(URL)
        data = response.json()
        forecast_by_date = {}
        for item in data['list']:
            date = item['dt_txt'].split(' ')[0]
            if date not in forecast_by_date:
                weather_desc = item['weather'][0]['description'].lower()
                if 'soleil' in weather_desc or 'dégagé' in weather_desc or 'clair' in weather_desc:
                    weather = 'Soleil'
                    code = 0
                elif 'nuage' in weather_desc:
                    weather = 'Nuageux'
                    code = 1
                elif 'pluie légère' in weather_desc or 'bruine' in weather_desc:
                    weather = 'Pluie legere'
                    code = 2
                elif 'pluie' in weather_desc:
                    weather = 'Pluie'
                    code = 3
                else:
                    weather = 'Soleil'
                    code = 0
                forecast_by_date[date] = {
                    'weather': weather,
                    'code': code,
                    'temp': round(item['main']['temp'], 1)
                }
        return forecast_by_date
    except Exception as e:
        print(f"⚠️ Erreur API météo: {e}", file=sys.stderr)
        return {}

def get_today_weather():
    try:
        url_current = f"http://api.openweathermap.org/data/2.5/weather?q={CITY}&appid={API_KEY}&units=metric&lang=fr"
        response = requests.get(url_current)
        data = response.json()
        weather_desc = data['weather'][0]['description'].lower()
        if 'soleil' in weather_desc or 'dégagé' in weather_desc:
            weather = 'Ensoleillé'
        elif 'nuage' in weather_desc:
            weather = 'Nuageux'
        elif 'pluie' in weather_desc:
            weather = 'Pluie'
        else:
            weather = 'Ensoleillé'
        return {
            'weather': weather,
            'temp': round(data['main']['temp'], 1),
            'humidity': data['main']['humidity'],
            'wind': round(data['wind']['speed'], 1),
            'location': CITY
        }
    except Exception as e:
        return {'weather': 'Ensoleillé', 'temp': 24, 'humidity': 65, 'wind': 12, 'location': CITY}

print("🌤️ Récupération de la météo...", file=sys.stderr)
weather_forecast = get_weather_forecast()
today_weather = get_today_weather()
print(f"✅ Météo récupérée pour {len(weather_forecast)} jours", file=sys.stderr)

today = datetime.now()
jours_semaine = ['LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE']
today_index = today.weekday()
days = jours_semaine[today_index:] + jours_semaine[:today_index]

print(f"📅 Aujourd'hui: {days[0]} {today.strftime('%d/%m/%Y')}", file=sys.stderr)

print("🔮 Calcul des prédictions...", file=sys.stderr)

predictions = []
total = 0

for i, day in enumerate(days):
    pred_date = today + timedelta(days=i)
    date_str = pred_date.strftime('%Y-%m-%d')
    date_affichage = pred_date.strftime('%d/%m/%Y')
    day_of_week = pred_date.weekday()

    same_day_avg = historical_avg[day_of_week]

    if date_str in weather_forecast:
        weather_code = weather_forecast[date_str]['code']
        weather_name = weather_forecast[date_str]['weather']
        temp = weather_forecast[date_str]['temp']
    else:
        weather_code = 0
        weather_name = 'Soleil'
        temp = 20

    is_holiday = 0

    features = {
        'year': pred_date.year,
        'month': pred_date.month,
        'day': pred_date.day,
        'day_of_week': day_of_week,
        'week_of_year': pred_date.isocalendar().week,
        'weather_code': weather_code,
        'temp_avg': temp,
        'is_weekend': 1 if day_of_week >= 5 else 0,
        'is_holiday_binary': is_holiday,
        'season': get_season(pred_date.month),
        'ma_same_day': same_day_avg,
        'trend': 0
    }

    X_pred = pd.DataFrame([features])
    pred = model.predict(X_pred)[0]
    pred = max(0, min(160, int(pred)))

    total += pred
    predictions.append({
        'jour': day,
        'date': date_affichage,
        'full_date': date_str,
        'predicted': pred,
        'meteo': weather_name,
        'temp': temp
    })

    print(f"   {day} {date_affichage}: {pred} employés ({weather_name}, {temp}°C)", file=sys.stderr)

result = {
    'today': {
        'date': today.strftime('%Y-%m-%d'),
        'weather': today_weather['weather'],
        'temp': today_weather['temp'],
        'humidity': today_weather['humidity'],
        'wind': today_weather['wind'],
        'location': today_weather['location']
    },
    'predictions': predictions,
    'total': total,
    'average': round(total / 7)
}

print("\n✅ Prédictions terminées!", file=sys.stderr)
print(json.dumps(result))