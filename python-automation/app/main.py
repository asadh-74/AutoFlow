from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from datetime import datetime, timezone
from io import BytesIO
import pandas as pd

app = FastAPI(title="AutoFlow Python Automation Service", version="1.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["https://autoflow-frontend-syr5.onrender.com", "http://localhost:5173"], allow_methods=["GET", "POST"], allow_headers=["Content-Type"])

@app.get("/health")
def health():
    return {"status":"ok","service":"autoflow-python-service","time":datetime.now(timezone.utc).isoformat()}

class ReportSnapshot(BaseModel):
    totalRuns: int = 0
    customers: int = 0
    successRate: float = 0
    averageDurationMs: float = 0

@app.post("/reports/daily")
def daily_report(snapshot: ReportSnapshot):
    return {
        "title": "Operations Snapshot",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "source": "Java dashboard snapshot supplied by the caller; not an independently audited report",
        "kpis": snapshot.model_dump(),
        "summary": f"{snapshot.totalRuns} recorded runs and {snapshot.customers} customers. Recent success rate: {snapshot.successRate:.1f}%.",
    }

@app.post("/automation/clean-csv")
async def clean_csv(file: UploadFile = File(...)):
    raw=await file.read(2 * 1024 * 1024 + 1)
    if len(raw)>2 * 1024 * 1024:
        raise HTTPException(413, "CSV must be smaller than 2 MB")
    try:
        df=pd.read_csv(BytesIO(raw))
    except (ValueError, UnicodeDecodeError, pd.errors.ParserError):
        raise HTTPException(400, "Upload a valid UTF-8 CSV")
    before=len(df)
    df=df.drop_duplicates().dropna(how="all")
    df.columns=[str(c).strip().lower().replace(" ","_") for c in df.columns]
    return {"filename":file.filename,"rowsBefore":before,"rowsAfter":len(df),"columns":list(df.columns),"cleanedCsv":df.to_csv(index=False)}

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
