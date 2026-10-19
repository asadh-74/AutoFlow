import os
import pytest
from fastapi.testclient import TestClient
from app.main import app
@pytest.fixture
def client(tmp_path,monkeypatch):
 monkeypatch.setenv('DOCFLOW_DB',str(tmp_path/'docs.db'))
 return TestClient(app)
def upload(c,broken=False):
 raw=c.get('/docflow/sample.pdf'+('?broken=true' if broken else '')).content
 return c.post('/api/docflow/documents',files={'file':('sample.pdf',raw,'application/pdf')})
def test_duplicate_validation_approval_export_and_isolation(client):
 d=upload(client,True).json()['document'];assert any('Total does not' in s for s in d['issues'])
 assert upload(client,True).json()['duplicate']
 body={**d['fields'],'reviewer':'Test reviewer','approve':True}
 assert client.post('/api/docflow/documents/'+d['id']+'/review',json=body).status_code==422
 assert len(client.get('/api/docflow/export.csv').text.splitlines())==1
 body['total']='1050.00'
 assert client.post('/api/docflow/documents/'+d['id']+'/review',json=body).json()['status']=='approved'
 assert '1050.00' in client.get('/api/docflow/export.csv').text
 other=TestClient(app)
 assert other.get('/api/docflow/documents').json()==[]
 assert other.post('/api/docflow/documents/'+d['id']+'/review',json=body).status_code==404
def test_reject_bad_files(client):
 assert client.post('/api/docflow/documents',files={'file':('bad.pdf',b'not PDF')}).status_code==422
 assert client.post('/api/docflow/documents',files={'file':('huge.pdf',b'%PDF-'+b'x'*2097152)}).status_code==413
def test_sample_extraction(client):
 d=upload(client).json()['document'];assert d['fields']['vendor']=='Northstar Studio';assert not d['issues'];assert d['status']=='needs-review'
