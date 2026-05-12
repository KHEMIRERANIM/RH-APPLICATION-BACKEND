import traceback
from google import genai

API_KEY = "AIzaSyATWH3G3mB8NyyfDSpGii5jc7UKst_4iM4"

try:
    print("Testing Gemini API without 'models/' prefix...")
    client = genai.Client(api_key=API_KEY)
    response = client.models.generate_content(
        model='gemini-1.5-flash',
        contents='Dis bonjour très brièvement.'
    )
    print("SUCCESS!")
    print("Response:", response.text)
except Exception as e:
    print("\n========== ERROR ==========")
    print(str(e))
    print("===========================\n")
