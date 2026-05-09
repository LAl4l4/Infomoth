from transformers import AutoTokenizer, AutoModelForSequenceClassification



model_name = "ProsusAI/finbert"


tokenizer = AutoTokenizer.from_pretrained(
    model_name,
    cache_dir="./models"
)

model = AutoModelForSequenceClassification.from_pretrained(
    model_name,
    cache_dir="./models"
)

print("done")