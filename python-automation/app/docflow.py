"""Session-scoped invoice review workspace. Text extraction is deterministic, never invented AI output."""
import csv, hashlib, io, json, os, re, secrets, sqlite3, threading, time, uuid
from contextlib import contextmanager
from datetime import date, datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from fastapi import APIRouter, Request, Response, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field
from pypdf import PdfReader
router=APIRouter()
lock=threading.RLock()
STATIC=Path(__file__).parent/'static'
@contextmanager
def db():
 c=sqlite3.connect(os.getenv('DOCFLOW_DB','/tmp/docflow.sqlite3'),timeout=10)
 try:
  c.execute('CREATE TABLE IF NOT EXISTS documents (id TEXT PRIMARY KEY,sid TEXT,digest TEXT,created REAL,data TEXT,UNIQUE(sid,digest))')
  c.execute('DELETE FROM documents WHERE created<?',(time.time()-86400,))
  yield c
  c.commit()
 finally:c.close()
def session(req,res):
 sid=req.cookies.get('docflow_session','')
 if not re.fullmatch('[a-f0-9]{48}',sid):
  sid=secrets.token_hex(24);res.set_cookie('docflow_session',sid,httponly=True,samesite='lax',secure=bool(os.getenv('RENDER')),max_age=86400)
 return sid
def stamp():return datetime.now(timezone.utc).isoformat()
def issues(f):
 out=[]
 for k in ('vendor','invoice_number','date','currency','subtotal','tax','discount','total'):
  if not str(f.get(k,'')).strip():out.append(f'Missing {k}')
 try:date.fromisoformat(f.get('date',''))
 except ValueError:out.append('Date must use YYYY-MM-DD')
 if f.get('currency') not in ('USD','PKR','EUR','GBP','AED'):out.append('Select a supported currency')
 numbers={}
 for k in ('subtotal','tax','discount','total'):
  try:
   n=Decimal(str(f.get(k,'')))
   if not n.is_finite() or n<0 or n>Decimal('1000000000') or n.as_tuple().exponent < -2:raise ValueError()
   numbers[k]=n
  except (InvalidOperation,ValueError):out.append(f'{k}: use a nonnegative amount with at most two decimals')
 if len(numbers)==4 and abs(numbers['subtotal']+numbers['tax']-numbers['discount']-numbers['total'])>Decimal('0.01'):out.append('Total does not equal subtotal + tax − discount')
 return out
def extract(text):
 f={k:'' for k in ('vendor','invoice_number','date','currency','subtotal','tax','discount','total')}
 labels={'vendor':r'(?:vendor|supplier|merchant)','invoice_number':r'(?:invoice\s*(?:number|no\.?|#)|receipt\s*(?:number|no\.?|#))','date':r'(?:invoice date|date)','currency':r'currency','subtotal':r'subtotal','tax':r'(?:tax|vat)','discount':r'discount','total':r'(?:grand total|total|amount due)'}
 for k,label in labels.items():
  m=re.search(r'^\s*'+label+r'\s*[:#]\s*(.+?)\s*$',text,re.I|re.M)
  if m:f[k]=m.group(1).strip()[:200]
 for k in ('subtotal','tax','discount','total'):
  if f[k]:f[k]=re.sub(r'^(?:USD|PKR|EUR|GBP|AED|[$€£])\s*','',f[k],flags=re.I).replace(',','').strip()
 f['currency']=f['currency'].upper()
 return f
class Review(BaseModel):
 vendor:str=Field(max_length=200);invoice_number:str=Field(max_length=100);date:str=Field(max_length=30);currency:str=Field(max_length=10)
 subtotal:str=Field(max_length=30);tax:str=Field(max_length=30);discount:str=Field(max_length=30);total:str=Field(max_length=30)
 reviewer:str=Field(min_length=2,max_length=80);approve:bool=False
@router.get('/docflow',include_in_schema=False)
def page():return FileResponse(STATIC/'docflow.html')
@router.get('/docflow/workspace.css',include_in_schema=False)
def css():return FileResponse(STATIC/'workspace.css',media_type='text/css')
@router.get('/docflow/app.js',include_in_schema=False)
def js():return FileResponse(STATIC/'docflow.js',media_type='application/javascript')
@router.post('/api/docflow/documents')
async def upload(req:Request,res:Response,file:UploadFile=File(...)):
 raw=await file.read(2*1024*1024+1)
 if len(raw)>2*1024*1024:raise HTTPException(413,'PDF must be below 2 MB')
 if not raw.startswith(b'%PDF-'):raise HTTPException(422,'Upload a valid PDF file')
 sid=session(req,res);digest=hashlib.sha256(raw).hexdigest()
 with lock,db() as c:
  prev=c.execute('SELECT data FROM documents WHERE sid=? AND digest=?',(sid,digest)).fetchone()
  if prev:return {'document':json.loads(prev[0]),'duplicate':True}
 try:
  pdf=PdfReader(io.BytesIO(raw),strict=False)
  if pdf.is_encrypted:raise ValueError('encrypted')
  if len(pdf.pages)>20:raise ValueError('too many pages')
  text='\n'.join((p.extract_text() or '') for p in pdf.pages)[:60000]
 except Exception:raise HTTPException(422,'PDF is unreadable, encrypted, or exceeds 20 pages. Use an unlocked text PDF.')
 fields=extract(text);errors=issues(fields)
 d={'id':str(uuid.uuid4()),'filename':Path(file.filename or 'invoice.pdf').name[:150],'created':stamp(),'fields':fields,'issues':errors,'status':'needs-review','extraction':'text-label extraction' if text.strip() else 'No embedded text: manual entry required (OCR not enabled)','text':text,'audit':[{'at':stamp(),'event':'uploaded-and-extracted'}]}
 with lock,db() as c:
  if c.execute('SELECT count(*) FROM documents').fetchone()[0]>=1000:raise HTTPException(429,'Demo storage capacity reached')
  try:c.execute('INSERT INTO documents VALUES(?,?,?,?,?)',(d['id'],sid,digest,time.time(),json.dumps(d)))
  except sqlite3.IntegrityError:
   previous=c.execute('SELECT data FROM documents WHERE sid=? AND digest=?',(sid,digest)).fetchone();return {'document':json.loads(previous[0]),'duplicate':True}
 return {'document':d,'duplicate':False}
@router.get('/api/docflow/documents')
def listing(req:Request,res:Response):
 sid=session(req,res)
 with db() as c:return [json.loads(r[0]) for r in c.execute('SELECT data FROM documents WHERE sid=? ORDER BY created DESC LIMIT 50',(sid,))]
@router.post('/api/docflow/documents/{id}/review')
def review(id:str,body:Review,req:Request,res:Response):
 sid=session(req,res)
 with lock,db() as c:
  row=c.execute('SELECT data FROM documents WHERE id=? AND sid=?',(id,sid)).fetchone()
  if not row:raise HTTPException(404,'Document not found in this session')
  d=json.loads(row[0]);f=body.model_dump(exclude={'approve','reviewer'});errors=issues(f)
  if body.approve and errors:raise HTTPException(422,{'message':'Correct these exceptions before approval','issues':errors})
  d.update(fields=f,issues=errors,status='approved' if body.approve else 'needs-review')
  d['audit'].append({'at':stamp(),'event':'approved' if body.approve else 'review-saved','reviewer':body.reviewer.strip()})
  d['audit']=d['audit'][-50:];c.execute('UPDATE documents SET data=? WHERE id=? AND sid=?',(json.dumps(d),id,sid))
 return d
@router.get('/api/docflow/export.csv')
def export(req:Request,res:Response):
 sid=session(req,res)
 with db() as c:docs=[json.loads(r[0]) for r in c.execute('SELECT data FROM documents WHERE sid=? ORDER BY created',(sid,))]
 output=io.StringIO();keys=['vendor','invoice_number','date','currency','subtotal','tax','discount','total'];w=csv.writer(output);w.writerow(keys+['status'])
 for d in docs:
  if d['status']=='approved':w.writerow([safe_cell(d['fields'].get(k,'')) for k in keys]+['approved'])
 return Response(output.getvalue(),media_type='text/csv',headers={'Content-Disposition':'attachment; filename="approved-invoices.csv"'})
def safe_cell(s):return "'"+s if str(s).lstrip().startswith(('=','+','-','@')) else s
@router.get('/docflow/sample.pdf')
def sample(broken:bool=False):
 lines=['NORTHSTAR STUDIO - FICTIONAL DEMO','Vendor: Northstar Studio','Invoice Number: NS-2026-104','Date: 2026-10-19','Currency: USD','Subtotal: 1000.00','Tax: 50.00','Discount: 0.00','Total: '+('999.00' if broken else '1050.00')]
 stream='BT /F1 14 Tf 60 760 Td 22 TL '+' '.join('('+s+') Tj T*' for s in lines)+' ET'
 objects=['<< /Type /Catalog /Pages 2 0 R >>','<< /Type /Pages /Kids [3 0 R] /Count 1 >>','<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>','<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',f'<< /Length {len(stream)} >>\nstream\n{stream}\nendstream']
 raw=b'%PDF-1.4\n';offsets=[0]
 for i,obj in enumerate(objects,1):offsets.append(len(raw));raw+=f'{i} 0 obj\n{obj}\nendobj\n'.encode()
 start=len(raw);raw+=('xref\n0 6\n0000000000 65535 f \n'+''.join(f'{n:010d} 00000 n \n' for n in offsets[1:])+f'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n{start}\n%%EOF').encode()
 return Response(raw,media_type='application/pdf',headers={'Content-Disposition':'attachment; filename="sample-invoice.pdf"'})
