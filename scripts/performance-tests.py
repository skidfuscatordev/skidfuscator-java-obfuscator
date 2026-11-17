#!/usr/bin/env python3
"""Compare candidate and released tools on the same host; never run customer applications."""
import argparse, hashlib, json, os, pathlib, statistics, subprocess, tempfile, time, signal

ROOT = pathlib.Path(__file__).resolve().parents[1]


def measured(command, work, name, timeout=180):
    log = work / (name + '.log')
    start = time.monotonic(); usage = None
    with log.open('w') as output:
        process = subprocess.Popen(list(map(str,command)), cwd=work, stdout=output, stderr=subprocess.STDOUT,
                                   start_new_session=os.name=='posix')
        try:
            if hasattr(os,'wait4'):
                while True:
                    pid,status,usage = os.wait4(process.pid,os.WNOHANG)
                    if pid:
                        process.returncode = os.waitstatus_to_exitcode(status); break
                    if time.monotonic()-start > timeout: raise subprocess.TimeoutExpired(command,timeout)
                    time.sleep(0.01)
            else:
                process.wait(timeout=timeout)
        except BaseException:
            if os.name=='posix': os.killpg(process.pid,signal.SIGKILL)
            else: process.kill()
            process.wait(); raise
    if process.returncode:
        raise RuntimeError(log.read_text(errors='replace')[-6000:])
    return {'wallMillis': (time.monotonic()-start)*1000,
            'peakRssBytes': usage.ru_maxrss * (1 if os.uname().sysname=='Darwin' else 1024) if usage is not None else None}, log.read_text(errors='replace')


def verify_mixed_modules(java, tools, work, home):
    # Two separately protected JARs, different seeds/tool versions, one application loader.
    for label, suffix, seed in [('baseline','A',11),('candidate','B',22)]:
        source=work/('Shared'+suffix+'.java')
        source.write_text('public class Shared'+suffix+' { public static boolean check(Object x) { return x instanceof CharSequence; } }')
        classes=work/('module-'+suffix); classes.mkdir()
        subprocess.run(['javac','--release','8','-d',str(classes),str(source)],check=True,capture_output=True)
        original=work/('input-'+suffix+'.jar'); output=work/('module-'+suffix+'.jar')
        subprocess.run(['jar','--create','--file',str(original),'-C',str(classes),'.'],check=True)
        measured([java,'-Xmx3g','-Duser.home='+home,'-jar',tools[label],'obfuscate',original,'--output',output,
                  '--profile','CONSERVATIVE','--seed',str(seed),'--offline'],work,'mixed-'+suffix)
    runner=work/'Combined.java'
    runner.write_text('public class Combined { public static void main(String[] args) { System.out.println(SharedA.check("a")+":"+SharedB.check("b")+":"+SharedA.check(null)+":"+SharedB.check(null)); } }')
    cp=os.pathsep.join(str(work/('module-'+suffix+'.jar')) for suffix in ('A','B'))
    subprocess.run(['javac','--release','8','-cp',cp,'-d',str(work/'runner'),str(runner)],check=True,capture_output=True)
    _, result=measured([java,'-Xverify:all','-cp',str(work/'runner')+os.pathsep+cp,'Combined'],work,'mixed-run')
    if result.strip()!='true:true:false:false': raise AssertionError('Mixed-version SDKs changed behavior')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tool', type=pathlib.Path, default=ROOT/'dev.skidfuscator.client.standalone/build/libs/client-standalone-all.jar')
    parser.add_argument('--baseline', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, default=ROOT/'build/reports/performance.json')
    parser.add_argument('--samples', type=int, default=3)
    parser.add_argument('--require-rss', action='store_true', help='Fail if process RSS cannot be measured')
    parser.add_argument('--jfr', action='store_true', help='Retain JFR profiles for both tools beside the report')
    args = parser.parse_args()
    if args.samples < 3: parser.error('At least three samples are required')
    tools = {'baseline': args.baseline.resolve(), 'candidate': args.tool.resolve()}
    args.output = args.output.resolve(); args.output.parent.mkdir(parents=True, exist_ok=True)
    home = os.environ.get('SKIDFUSCATOR_USER_HOME', os.environ['HOME'])
    java = pathlib.Path(os.environ['JAVA_HOME'])/'bin/java'
    report = {'schemaVersion': 1, 'status': 'running', 'profile': 'CONSERVATIVE',
              'samples': args.samples, 'tools': {k: hashlib.sha256(v.read_bytes()).hexdigest() for k,v in tools.items()},
              'method': 'same-host interleaved trials; one warm-up protection per tool; 120 classes; warmed Java loop',
              'limits': {'protectionRatio': 1.5, 'protectionAllowanceMillis': 500,
                         'runtimeRatio': 1.5, 'runtimeAllowanceMillis': 20, 'rssRatio': 1.25, 'rssAllowanceBytes': 32*1024*1024}}
    args.output.write_text(json.dumps(report, indent=2)+'\n')
    try:
        with tempfile.TemporaryDirectory(prefix='skid-performance-') as directory:
            work = pathlib.Path(directory); source = work/'source/bench'; source.mkdir(parents=True)
            for index in range(120):
                methods = ''.join('public int m%d(int x){return (x*%d)^%d;}' % (m,index+3,m*97+index) for m in range(8))
                (source/('F%d.java'%index)).write_text('package bench; public class F%d {%s}' % (index,methods))
            (source/'Probe.java').write_text('''package bench;
public class Probe {
  static volatile long sink;
  static long exercise(int count) {
    String[] values = {"I","i","other","\\u0131",null}; long result=0;
    for(int i=0;i<count;i++) {
      String value=values[i%values.length];
      if("I".equals(value)) result+=3;
      if("I".equalsIgnoreCase(value)) result+=7;
      if(value instanceof CharSequence) result++;
    }
    return result;
  }
  public static void main(String[] args) {
    for(int i=0;i<3;i++) sink=exercise(120000);
    long start=System.nanoTime(); long result=0;
    for(int i=0;i<3;i++) result+=exercise(600000);
    sink=result;
    System.out.println("RESULT:"+result+":"+((System.nanoTime()-start)/1000000.0/3));
  }
}''')
            subprocess.run(['javac','--release','8','-encoding','UTF-8','-d',str(work/'classes'),*map(str,source.glob('*.java'))],check=True,capture_output=True)
            original=work/'input.jar'
            subprocess.run(['jar','--create','--file',str(original),'--main-class','bench.Probe','-C',str(work/'classes'),'.'],check=True)
            config=work/'benchmark.hocon'; config.write_text('stringEncryption.exempt=["class{^bench/Probe$}"]\n')
            _, reference=measured([java,'-Xverify:all','-jar',original],work,'original')
            expected=next(line.split(':')[1] for line in reference.splitlines() if line.startswith('RESULT:'))
            records={label:[] for label in tools}
            for trial in range(args.samples+1):
                for label in (list(tools) if trial%2==0 else list(reversed(tools))):
                    output=work/(label+'.jar'); name=label+'-'+str(trial)
                    command=[java,'-Xmx3g','-Duser.home='+home]
                    if args.jfr: command+=['-XX:StartFlightRecording=settings=profile,filename='+str(args.output.parent/(name+'.jfr'))]
                    command+=['-jar',tools[label],'obfuscate',original,'--output',output,'--profile','CONSERVATIVE',
                              '--seed','42','--offline','--non-interactive','--config',config]
                    protection,_=measured(command,work,name)
                    run_report=json.loads(pathlib.Path(str(output)+'.report.json').read_text())
                    if run_report['status']!='succeeded': raise AssertionError('Protection did not complete')
                    execution,text=measured([java,'-Xverify:all','-jar',output],work,name+'-runtime')
                    result=next(line.split(':') for line in text.splitlines() if line.startswith('RESULT:'))
                    if result[1]!=expected: raise AssertionError('Protected benchmark behavior changed')
                    if trial:
                        records[label].append({'protection':protection,'execution':execution,'loopMillis':float(result[2]),
                                               'jarBytes':output.stat().st_size,'phases':run_report.get('phases',[])})
            report['trials']=records
            medians={label:{'protectionMillis':statistics.median(s['protection']['wallMillis'] for s in runs),
                            'loopMillis':statistics.median(s['loopMillis'] for s in runs),
                            'executionMillis':statistics.median(s['execution']['wallMillis'] for s in runs),
                            'peakRssBytes':statistics.median(s['protection']['peakRssBytes'] for s in runs) if runs[0]['protection']['peakRssBytes'] is not None else None}
                     for label,runs in records.items()}
            report['medians']=medians
            old,new=medians['baseline'],medians['candidate']; limits=report['limits']
            if args.require_rss and (new['peakRssBytes'] is None or old['peakRssBytes'] is None):
                raise RuntimeError('Required process RSS measurement is unavailable')
            regressions=[]
            if new['protectionMillis']>old['protectionMillis']*limits['protectionRatio']+limits['protectionAllowanceMillis']: regressions.append('protection time')
            if new['loopMillis']>old['loopMillis']*limits['runtimeRatio']+limits['runtimeAllowanceMillis']: regressions.append('steady-state runtime')
            if new['peakRssBytes'] is not None and new['peakRssBytes']>old['peakRssBytes']*limits['rssRatio']+limits['rssAllowanceBytes']: regressions.append('protection RSS')
            report['regressions']=regressions
            if regressions: raise AssertionError('Performance regression: '+', '.join(regressions))
            verify_mixed_modules(java,tools,work,home)
            report['mixedModuleCheck']='passed'
            report['status']='succeeded'
            print(json.dumps({'status':'succeeded','medians':medians},indent=2))
    except Exception as failure:
        report['status']='failed'; report['error']=str(failure); raise
    finally:
        args.output.write_text(json.dumps(report,indent=2)+'\n')


if __name__=='__main__': main()
