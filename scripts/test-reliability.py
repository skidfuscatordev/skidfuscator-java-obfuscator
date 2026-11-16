#!/usr/bin/env python3
import hashlib,json,os,pathlib,subprocess,tempfile,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
jar=root/'dev.skidfuscator.client.standalone/build/libs/client-standalone-all.jar'
java=os.environ.get('JAVA_BIN','java');home=os.environ.get('SKIDFUSCATOR_USER_HOME',os.environ['HOME'])
with tempfile.TemporaryDirectory() as directory:
    work=pathlib.Path(directory)
    source=work/'Probe.java';source.write_text('public class Probe { public static void main(String[] a) { System.out.println("probe:"+(3*7)); } }')
    subprocess.run(['javac','--release','17','-d',str(work/'classes'),str(source)],check=True)
    original=work/'input.jar'
    subprocess.run(['jar','--create','--file',str(original),'--main-class','Probe','-C',str(work/'classes'),'.'],check=True)
    def protect(output,*extra,expected=0):
        log=work/'last.log'
        with log.open('w') as handle:
            process=subprocess.run([java,'-Xmx2g','-Duser.home='+home,'-jar',str(jar),'obfuscate',str(original),
                '--output',str(output),'--offline','--non-interactive','--seed','123456',*extra],cwd=work,stdout=handle,stderr=subprocess.STDOUT,timeout=180)
        assert process.returncode==expected,log.read_text()[-6000:]
        return json.loads(pathlib.Path(str(output)+'.report.json').read_text())
    planned=work/'planned.jar'
    report=protect(planned,'--dry-run');assert not planned.exists() and report['status']=='planned'
    assert report['seed']==123456 and report['elements']
    for profile in ('CONSERVATIVE','BALANCED','STRONG'):
        protected=work/(profile+'.jar');report=protect(protected,'--profile',profile)
        assert report['status']=='succeeded' and report['outputSha256']==hashlib.sha256(protected.read_bytes()).hexdigest()
        assert report['runtime'] and all(len(module['sha256'])==64 for module in report['runtime'])
        observed=subprocess.check_output([java,'-Xverify:all','-jar',str(protected)],text=True).strip();assert observed=='probe:21'
        second=work/(profile+'-second.jar');protect(second,'--profile',profile)
        if protected.read_bytes()!=second.read_bytes():
            import shutil
            debug=root/'build/reports/reproduction';debug.mkdir(parents=True,exist_ok=True)
            for artifact in (original,protected,second): shutil.copyfile(artifact,debug/artifact.name)
        assert protected.read_bytes()==second.read_bytes(),'Same seed and inputs did not produce identical output: '+profile
    previous=protected.read_bytes()
    report=protect(protected,'--max-output-bytes','1',expected=1)
    assert report['status']=='failed' and protected.read_bytes()==previous
    report=protect(protected,'--max-duration-ms','1',expected=1)
    assert report['status']=='failed' and protected.read_bytes()==previous
    config=work/'bad.hocon';config.write_text('stringEncrypton.enabled=true')
    report=protect(protected,'--config',str(config),expected=1);assert report['status']=='failed'
    import io
    saved=original.read_bytes()
    with zipfile.ZipFile(io.BytesIO(saved)) as base, zipfile.ZipFile(original,'w') as multi:
        for entry in base.infolist():multi.writestr(entry.filename,base.read(entry.filename))
        multi.writestr('META-INF/versions/17/Probe.class',base.read('Probe.class'))
    report=protect(protected,expected=1)
    assert report['status']=='failed' and protected.read_bytes()==previous
    print('Reliability: dry-run, runtime provenance, repeatable seed in all profiles, output/time budgets and invalid config passed')
