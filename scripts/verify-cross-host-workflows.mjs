#!/usr/bin/env node
// Acceptance after the browser walkthrough. Credentials come from env.
import assert from 'node:assert/strict';
const origin=process.env.KNOXX_VERIFY_ORIGIN;
const source=process.env.AXXIUM_VERIFY_SOURCE_ORIGIN;
const email=process.env.KNOXX_VERIFY_EMAIL,password=process.env.KNOXX_VERIFY_PASSWORD;
if(!origin||!source||!email||!password)throw Error('Set KNOXX_VERIFY_ORIGIN, AXXIUM_VERIFY_SOURCE_ORIGIN, KNOXX_VERIFY_EMAIL and KNOXX_VERIFY_PASSWORD');
for(const value of [origin,source])assert.equal(new URL(value).protocol,'https:');
const checks=[];
async function check(name,fn){await fn();checks.push({name,status:'passed'});}
await check('source identity endpoint unavailable',async()=>{const r=await fetch(source+'/health');assert.ok(r.status>=500);});
let cookie;
await check('fresh recipient password sign-in',async()=>{
 const r=await fetch(origin+'/api/auth/local/login',{method:'POST',headers:{'content-type':'application/json',origin},body:JSON.stringify({email,password})});
 assert.equal(r.status,200);cookie=r.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ');assert.ok(cookie);
});
async function api(path){const r=await fetch(origin+path,{headers:{cookie}});assert.equal(r.status,200);return r.json();}
await check('recipient session remains valid',async()=>{await api('/api/auth/me');});
await check('anonymous CMS read refused',async()=>{const r=await fetch(origin+'/api/cms/documents');assert.ok([401,403].includes(r.status));});
let documents;
await check('saved CMS content reloads from recipient',async()=>{
 documents=(await api('/api/cms/documents')).documents;assert.ok(documents.length>0);
 const doc=documents.find(d=>d.title===process.env.KNOXX_VERIFY_DOCUMENT_TITLE)||documents[0];
 const reloaded=await api('/api/cms/documents/'+encodeURIComponent(doc.doc_id));assert.equal(reloaded.content,doc.content);assert.ok(doc.content.length>0);
});
await check('unknown local document refused',async()=>{const r=await fetch(origin+'/api/cms/documents/absent-verification-document',{headers:{cookie}});assert.equal(r.status,404);});
await check('document content cannot grant public visibility',async()=>{
 const r=await fetch(origin+'/api/cms/documents',{method:'POST',headers:{cookie,origin,'content-type':'application/json'},body:JSON.stringify({title:'Rejected acceptance input',content:'Never persisted',visibility:'public'})});assert.equal(r.status,400);
});
const reviews=await api('/api/publications/translations/reviews');
console.log(JSON.stringify({origin,source,email,verifiedAt:new Date().toISOString(),checks,documentCount:documents.length,translationReviews:reviews},null,2));
