from google import genai
client = genai.Client(api_key="AIzaSyATWH3G3mB8NyyfDSpGii5jc7UKst_4iM4")
for m in client.models.list():
    print(m.name)
