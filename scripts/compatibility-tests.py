#!/usr/bin/env python3
"""Compile with the target JDK, protect with JDK 17+, and verify on the target JVM."""
import argparse,json,os,pathlib,shutil,subprocess,tempfile,zipfile
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('target_home',type=pathlib.Path);p.add_argument('release',type=int);args=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]; tool=pathlib.Path(os.environ['JAVA_HOME'])/'bin/java'
home=os.environ.get('SKIDFUSCATOR_USER_HOME',os.environ['HOME'])
reports=root/'build/reports'/('compatibility-java-'+str(args.release)); reports.mkdir(parents=True,exist_ok=True)
verified=[]
with tempfile.TemporaryDirectory() as directory:
    work=pathlib.Path(directory);record=args.release>=17
    source='''import java.io.*; import java.util.function.IntUnaryOperator;
public class VersionProbe {
    public static void main(String[] args) throws Exception {
        IntUnaryOperator function=x->x*7+3;
        int value=0;try { value=21/0; } catch(ArithmeticException e) { value=7; } finally { value++; }
        synchronized(VersionProbe.class) { value+=function.applyAsInt(5); }
        System.out.println("RESULT:"+value+%s+":"+fixtures.StringComparisonCases.run());
    }
    %s
}''' % ('new Pair(3,4).sum()' if record else '7',
         'public record Pair(int x,int y) implements Serializable { public int sum(){return x+y;} }' if record else '')
    path=work/'VersionProbe.java';path.write_text(source)
    (work/'classes').mkdir()
    compiler=args.target_home/'bin/javac'
    flags=['-source','8','-target','8'] if args.release==8 else ['--release',str(args.release)]
    comparisons=root/'dev.skidfuscator.integration/src/main/java/fixtures/StringComparisonCases.java'
    compiled=subprocess.run([str(compiler),*flags,'-encoding','UTF-8','-d',str(work/'classes'),str(path),str(comparisons)],capture_output=True)
    assert compiled.returncode==0,compiled.stderr.decode(errors='replace')
    original=work/'input.jar'
    subprocess.run(['jar','--create','--file',str(original),'--main-class','VersionProbe','-C',str(work/'classes'),'.'],check=True)
    java=args.target_home/'bin/java'
    expected=subprocess.check_output([str(java),'-Xmx256m','-Xverify:all','-jar',str(original)],text=True)
    runtime=args.target_home/'jmods'
    if args.release>8 and not runtime.is_dir(): runtime=args.target_home/'lib/modules'
    if args.release==8:
        runtime=args.target_home/'jre/lib/rt.jar'
        if not runtime.exists():runtime=args.target_home/'lib/rt.jar'
    config=work/'comparisons.hocon'
    config.write_text('stringEncryption.exempt=["class{^fixtures/StringComparisonCases$}"]\n')
    for profile in ('CONSERVATIVE','BALANCED','STRONG'):
        output=work/(profile+'.jar')
        log=work/'protection.log'
        with log.open('w') as stream:
            result=subprocess.run([str(tool),'-Xmx3g','-Duser.home='+home,'-jar',str(root/'dev.skidfuscator.client.standalone/build/libs/client-standalone-all.jar'),
                'obfuscate',str(original),'--runtime',str(runtime),'--output',str(output),'--profile',profile,'--seed','42','--offline','--config',str(config)],
                cwd=work,stdout=stream,stderr=subprocess.STDOUT,timeout=180)
        shutil.copyfile(log,reports/(profile+'-protection.log'))
        run_report=pathlib.Path(str(output)+'.report.json')
        if run_report.is_file(): shutil.copyfile(run_report,reports/(profile+'.report.json'))
        assert result.returncode==0,log.read_text()[-6000:]
        report=json.loads(pathlib.Path(str(output)+'.report.json').read_text())
        assert any(t['success']>0 for t in report['transformers']), 'No transformations executed'
        for name in ('String Equals Hash','String EqIgCase Hash'):
            assert any(t['name']==name and t['success']>0 for t in report['transformers']),name+' did not execute'
        actual=subprocess.check_output([str(java),'-Xmx256m','-Xverify:all','-jar',str(output)],text=True)
        assert actual==expected
        with zipfile.ZipFile(original) as before,zipfile.ZipFile(output) as after:
            for name in before.namelist():
                if name.endswith('.class'):
                    assert before.read(name)[6:8]==after.read(name)[6:8],name
        print(f'Java {args.release}, {profile}: behavior and original class-file versions verified')
        verified.append(profile)
    (reports/'result.json').write_text(json.dumps({'schemaVersion':1,'javaRelease':args.release,
        'status':'succeeded','verifiedProfiles':verified},indent=2)+'\n')
