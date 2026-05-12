import traceback
from google import genai

API_KEY = "AIzaSyATWH3G3mB8NyyfDSpGii5jc7UKst_4iM4"

try:
    print("Testing gemini-2.0-flash...")
    client = genai.Client(api_key=API_KEY)
    response = client.models.generate_content(
        model='models/gemini-2.0-flash',
        contents='Dis bonjour.'
    )
    print("SUCCESS!")
    print("Response:", response.text)
except Exception as e:
    print("\n========== ERROR 2.0 ==========")
    print(str(e))

try:
    print("\nTesting gemini-flash-latest...")
    response = client.models.generate_content(
        model='models/gemini-flash-latest',
        contents='Dis bonjour.'
    )
    print("SUCCESS!")
    print("Response:", response.text)
except Exception as e:
    print("\n========== ERROR LATEST ==========")
    print(str(e))
