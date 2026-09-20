#!/usr/bin/env python3
"""Fail closed unless four disjoint batches prove 1,000 test-killed source mutations."""
import json,re,sys
from pathlib import Path

def require(condition,message):
 if not condition:raise ValueError(message)
def verify(sha,directory):
 require(bool(re.fullmatch('[0-9a-f]{40}',sha)),'Immutable commit required')
 files=list(Path(directory).glob('*.json'));require(len(files)==4,'Exactly four completed batches required')
 seen=set();indexes=set()
 for path in files:
  b=json.loads(path.read_text())
  require(b['source_sha']==sha and b['status']=='passed' and b['baseline']=='passed','Invalid or stale batch')
  i=b['batch_index'];require(type(i) is int and i in range(4) and i not in indexes,'Invalid batch index');indexes.add(i)
  require(len(b['mutants'])>=250,'Insufficient mutations in batch')
  for m in b['mutants']:
   key=m['fingerprint'];require(bool(re.fullmatch('[0-9a-f]{64}',key)) and key not in seen,'Duplicate or invalid mutation')
   require(m['status']=='killed' and m['phase']=='test' and m['result']['timed-out?'] is False,'Mutation was not killed by completed tests')
   require(m['result']['assertions']>0,'No executed assertions');seen.add(key)
 return len(seen)
if __name__=='__main__':
 sha,directory=sys.argv[1:];count=verify(sha,directory)
 print(f'Qualified {sha}: {count} distinct test-killed source mutations across four batches')
