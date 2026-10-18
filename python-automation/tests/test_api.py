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
