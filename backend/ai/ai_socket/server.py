# How to start or create env:
# python3 -m venv venv - create
# source venv/bin/activate - start

import time
import requests
from openai import OpenAI
import os
from dotenv import  load_dotenv, find_dotenv
import socket
import json
import re
from pathlib import Path
HOST = '0.0.0.0'
PORT = 2828

url = 'http://localhost:8080/api/v1/groundtruth/saveAiResponse'


dotenvpath = find_dotenv()
load_dotenv(dotenvpath)


api_key = (
    os.getenv("api_key")
    or os.getenv("OPENROUTER_API_KEY")
    or os.getenv("OPENAI_API_KEY")
)
if not api_key:
    raise ValueError(
        "Missing OpenRouter/OpenAI key. "
        f"Create {dotenvpath} with api_key=your_key "
        "or set OPENROUTER_API_KEY / OPENAI_API_KEY in your environment."
    )


client = OpenAI(
  base_url="https://openrouter.ai/api/v1",
   api_key=api_key,
 )

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((HOST,PORT))

server.listen(5)

while True:

    communication_socket, address = server.accept()
    print(f"Connected to {address}")
    messageFromClient = communication_socket.recv(1024).decode('utf-8')
    messageFromClientToString = str(messageFromClient)
    print("Message from the client is: {0}".format(messageFromClientToString))
    print()

    completion = client.chat.completions.create(
        extra_headers={
        },
        extra_body={},
        model="deepseek/deepseek-r1-distill-llama-70b:free",
        messages=[
            {
            "role": "user",
            "content": 
            
            """ 
            MANDATE: You are only allowed to output a single raw JSON object matching the schema below. Under no circumstances output explanatory text, step-by-step reasoning, or any other content. If you cannot comply, output exactly {"error":"cannot_comply"}.

            Do NOT reveal chain-of-thought or internal steps. Do NOT print or summarize how you arrived at numbers. Do NOT include markdown, backticks, or code fences. Return raw JSON only.


            You are an objective evaluator. Input is a JSON object containing groundTruth, groundTruthFacts, keyEntities, userAnswer, and scoringTolerances/weights. Do NOT produce any text other than a single JSON object exactly matching the output schema below.

                Steps you must perform:
                1. Tokenize groundTruthFacts and keyEntities and attempt to match each to the userAnswer using synonyms and fuzzy matching as allowed by matchingTolerances.
                2. For every matched keyEntity/fact mark it PRESENT and check whether any attribute (color, number, relationship, location, time) is ACCURATE or INCORRECT.
                3. Detect OMISSIONS (expected facts not mentioned) and COMMISSIONS (facts stated in userAnswer that contradict the groundTruth or add verifiably false people/objects).
                4. Compute four sub-scores from 0.0–1.0: presenceScore, accuracyScore, omissionScore, commissionScore (where omission and commission are penalties so higher penalty lowers final).
                5. Combine sub-scores using scoringWeights to create a finalNormalizedScore in range 0.0–1.0. Map finalNormalizedScore to rememberScore = 1..10 by multiplying by 9 and adding 1, then rounding to nearest integer.
                6. aiResponse string must be friendly and lightly humorous: use warm, positive phrasing and a touch of tasteful humor (no sarcasm, insults, sensitive topics, or profanity). Keep it clear and factual, suitable for diverse audiences and, while details in the JSON explain specifics.
                7. Return exactly this JSON schema and nothing else. Just return this JSON schema and nothing else.
                8. Return ONLY the output JSON schema
                9. aiResponse should be mildly extended, friendly, lightly humorous, positive, and respectful verdict. Should be 5 sentences minimum. No sarcasm, no insults, no explaining internal scoring or evaluation steps
                10. presentEntities, missingEntities, incorrectDetails and confabulatedDetails MUST be strings that are themselves valid JSON arrays of strings. Example: \"[\\\"blue notebook\\\", \\\"Sunday afternoon\\\"]\" (including the outer quotes). They must start with '[' and end with ']'. If empty, return \"[]\" (a string containing empty array).\n4) 

                Output JSON schema:
                {
                "aiResponse": "<short verdict string>",
                "rememberScore": <integer 1-10>,
                "presentEntities": '["\"<example>\", \"<exmaple>\","]',
                "missingEntities": '["\"<example>\", \"<exmaple>\","]',
                "incorrectDetails": '["\"<example>\", \"<exmaple>\","]',
                "confabulatedDetails": '["\"<example>\", \"<exmaple>\","]',
                "presence": 0.0-1.0,
                "accuracy": 0.0-1.0,
                "omission": 0.0-1.0,
                "commission": 0.0-1.0
                "explanation": "<1-2 sentence explanation>",
                "usergroundTruthResponse": { "id": <userid, found in message from client> }
                }
              }
 Input = """ + messageFromClientToString
            }
        ]
    )
    print(completion.choices[0].message.content)

    
    raw = completion.choices[0].message.content

    raw2 = re.search(r"/```json([\s\S]*?)```/g", raw, re.MULTILINE)

    if raw2:
        clean_json = raw2.group(1)  # 🟢 Extracted pure JSON
    else:
        clean_json = raw  # In case the model didn't wrap JSON in backticks

    print(raw2, 'HOLA')

    match = re.search(r"```(?:json)?\s*(\{[\s\S]*?\})\s*```", clean_json, re.MULTILINE)

    if match:
        clean_json = match.group(1)  # 🟢 Extracted pure JSON
        print(clean_json)
    else:
        print(clean_json)

#     clean_json = {
#   "aiResponse": "Good recall, but some details were missing.",
#   "rememberScore": 7,
#   "presentEntities": '[\"Grandma\", \"recipe book\", \"kitchen\"]',
#   "missingEntities": '[\"blue notebook\", \"Sunday afternoon\"]',
#   "incorrectDetails": '[\"grandma cooked pasta (it was cookies)\"]',
#   "confabulatedDetails": '[\"grandma mentioned Paris\"]',
#   "presence": 0.85,
#   "accuracy": 0.72,
#   "omission": 0.28,
#   "commission": 0.18,
#   "explanation": "Most relevant entities were mentioned, but there were a few incorrect or invented details.",
#   "usergroundTruthResponse": { "id": 4 }
# }


    myobj = json.loads(clean_json)

    print("ai json -> ", myobj)

    response = requests.post(url, json=myobj)
    
    # print("ai json -> ", clean_json)

    # response = requests.post(url, json=clean_json)


    print()
    communication_socket.send("Got your message".encode('utf-8'))
    communication_socket.close()
    print()
    print(f"Connection with {address} ended!")