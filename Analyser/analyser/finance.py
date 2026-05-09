from transformers import pipeline

pipe = pipeline(
    "sentiment-analysis",
    model="ProsusAI/finbert"
)

print(
    pipe("world war three start")
)