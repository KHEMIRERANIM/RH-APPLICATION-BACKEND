# main_api.py
from fastapi import FastAPI
from transformers import Trainer, TrainingArguments

app = FastAPI()

@app.post("/api/training/retrain")
async def retrain_model(samples: List[dict], epochs: int = 3):
    # Convertir samples en Dataset
    dataset = prepare_dataset(samples)

    # Fine-tuning
    training_args = TrainingArguments(
        output_dir="./results",
        num_train_epochs=epochs,
        per_device_train_batch_size=8,
        learning_rate=2e-5,
        warmup_steps=500,
        weight_decay=0.01,
        logging_dir="./logs",
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=dataset,
    )

    trainer.train()

    # Sauvegarder le nouveau modèle
    model.save_pretrained("./models/latest")

    return {"status": "retrained", "samples_used": len(samples)}