#!/usr/bin/env python3
"""Measure decompiler output and runtime costs. No metric proves reverse-engineering resistance."""
import argparse,hashlib,json,os,pathlib,statistics,subprocess,tempfile,time,zipfile

def command(args,timeout=120):
    started=time.monotonic()
    result=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=timeout)
    return result, (time.monotonic()-started)*1000

def measure(jar,main,cp,cfr):
    timings=[]
    for _ in range(5):
        result,millis=command(['java','-Xverify:all','-cp',str(jar)+os.pathsep+cp,main])
        if result.returncode: raise RuntimeError('Application benchmark failed: '+result.stderr.decode(errors='replace')[-2000:])
        timings.append(millis)
    with tempfile.TemporaryDirectory() as directory:
        result,_=command(['java','-jar',cfr,str(jar),'--outputdir',directory,'--silent','true',*(['--extraclasspath',cp] if cp else [])])
        files=list(pathlib.Path(directory).rglob('*.java'))
        decompilation={'tool':'CFR 0.152','exitCode':result.returncode,'javaFiles':len(files),
                       'sourceBytes':sum(p.stat().st_size for p in files)}
    return {'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'jarBytes':jar.stat().st_size,
            'startupMillis':{'median':statistics.median(timings),'max':max(timings),'samples':timings},
            'decompilation':decompilation}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('original',type=pathlib.Path);p.add_argument('protected',type=pathlib.Path)
    p.add_argument('main');p.add_argument('cfr');p.add_argument('report',type=pathlib.Path)
    p.add_argument('--classpath',default='');args=p.parse_args()
    before=measure(args.original,args.main,args.classpath,args.cfr)
    after=measure(args.protected,args.main,args.classpath,args.cfr)
    args.report.parent.mkdir(parents=True,exist_ok=True)
    args.report.write_text(json.dumps({'schemaVersion':1,'original':before,'protected':after,
        'sizeRatio':after['jarBytes']/before['jarBytes'],
        'startupRatio':after['startupMillis']['median']/before['startupMillis']['median'],
        'interpretation':'Decompiler completion and source size are observations, not a security score.'},indent=2)+'\n')

if __name__=='__main__': main()
