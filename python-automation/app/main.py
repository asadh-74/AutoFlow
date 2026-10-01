from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
from datetime import datetime, timezone
from io import BytesIO
import pandas as pd

app = FastAPI(title="AutoFlow Python Automation Service", version="1.0.0")

@app.get("/health")
def health():
    return {"status":"ok","service":"autoflow-python-service","time":datetime.now(timezone.utc).isoformat()}

@app.post("/reports/daily")
def daily_report():
    return {
        "title":"Daily Operations Summary",
        "generatedAt":datetime.now(timezone.utc).isoformat(),
        "kpis":{"workflowRuns":1248,"successRate":99.4,"customers":486,"hoursAutomated":214},
        "summary":"Automation health is stable and the majority of workflows completed successfully."
    }

@app.post("/automation/clean-csv")
async def clean_csv(file: UploadFile = File(...)):
    raw=await file.read()
    df=pd.read_csv(BytesIO(raw))
    before=len(df)
    df=df.drop_duplicates().dropna(how="all")
    df.columns=[str(c).strip().lower().replace(" ","_") for c in df.columns]
    return {"filename":file.filename,"rowsBefore":before,"rowsAfter":len(df),"columns":list(df.columns)}

class Message(BaseModel):
    customer:str
    email:str
    action:str="follow-up"

@app.post("/automation/prepare-message")
def prepare_message(item: Message):
    return {
        "subject":f"AutoFlow: {item.action.title()} for {item.customer}",
        "recipient":item.email,
        "body":f"Hello {item.customer}, this automated message was prepared by AutoFlow for: {item.action}."
    }
