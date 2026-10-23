from fastapi.testclient import TestClient
from app.main import app
client=TestClient(app)

def test_report_reflects_supplied_api_snapshot():
    r=client.post('/reports/daily',json={'totalRuns':7,'customers':3,'successRate':80.0,'averageDurationMs':25})
    assert r.status_code==200
    assert r.json()['kpis']['totalRuns']==7
    assert '7 recorded runs' in r.json()['summary']

def test_csv_download_is_actually_cleaned():
    r=client.post('/automation/clean-csv',files={'file':('demo.csv',b'Full Name,Value\nDemo,1\nDemo,1\nOther,2\n','text/csv')})
    assert r.status_code==200
    assert r.json()['rowsBefore']==3 and r.json()['rowsAfter']==2
    assert r.json()['cleanedCsv']=='full_name,value\nDemo,1\nOther,2\n'

def test_cors_for_hosted_frontend():
    r=client.options('/reports/daily',headers={'Origin':'https://autoflow-frontend-syr5.onrender.com','Access-Control-Request-Method':'POST'})
    assert r.headers['access-control-allow-origin']=='https://autoflow-frontend-syr5.onrender.com'

def test_lead_agent_returns_reviewable_recommendation(monkeypatch):
    monkeypatch.delenv('GEMINI_API_KEY', raising=False)
    r=client.post('/agent/lead-review',json={
        'name':'Jordan Avery','company':'Northstar Studio',
        'message':'We urgently need pricing for an automation demo this week.'
    })
    assert r.status_code==200
    body=r.json()
    assert body['provider']=='rules_demo'
    assert body['status']=='awaiting_human_approval'
    assert body['human_approval_required'] is True
    assert body['action_executed'] is False
    assert body['recommendation']['intent']=='sales_enquiry'
    assert body['recommendation']['priority']=='high'
    assert len(body['agent_trace'])==3

def test_lead_agent_rejects_invalid_payload():
    r=client.post('/agent/lead-review',json={'name':'','message':'x'})
    assert r.status_code==422
