#!/usr/bin/env python3
"""Exercise signing and tamper rejection using an ephemeral TEST identity."""
import hashlib,os,pathlib,secrets,subprocess,tempfile,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as directory:
    work=pathlib.Path(directory); env=os.environ.copy()
    env.update(SIGNING_KEYSTORE=str(work/'test.p12'),SIGNING_ALIAS='fixture',SIGNING_STOREPASS=secrets.token_urlsafe(32))
    subprocess.run(['keytool','-genkeypair','-alias','fixture','-keyalg','RSA','-keysize','3072',
        '-dname','CN=Ephemeral TEST identity','-validity','2','-storetype','PKCS12',
        '-keystore',env['SIGNING_KEYSTORE'],'-storepass:env','SIGNING_STOREPASS'],env=env,check=True,capture_output=True)
    source=root/'dev.skidfuscator.client.standalone/build/libs/client-standalone-all.jar'
    alias=work/'input-link.jar'; alias.symlink_to(source)
    digest=hashlib.sha256(source.read_bytes()).digest()
    blocked=subprocess.run(['bash',str(root/'scripts/sign-artifact.sh'),str(source),str(alias)],env=env,capture_output=True)
    assert blocked.returncode==2 and hashlib.sha256(source.read_bytes()).digest()==digest
    signed=work/'signed.jar'
    subprocess.run(['bash',str(root/'scripts/sign-artifact.sh'),str(source),str(signed)],env=env,check=True,capture_output=True)
    tampered=work/'tampered.jar'
    with zipfile.ZipFile(signed) as original,zipfile.ZipFile(tampered,'w') as changed:
        modified=False
        for entry in original.infolist():
            content=original.read(entry.filename)
            if not modified and entry.filename.endswith('.class'):
                content+=b'\0'; modified=True
            changed.writestr(entry,content)
    assert modified
    result=subprocess.run(['jarsigner','-verify','-strict','-keystore',env['SIGNING_KEYSTORE'],
        '-storepass:env','SIGNING_STOREPASS',str(tampered)],env=env,capture_output=True)
    assert result.returncode!=0, 'Tampered artifact was accepted'
    print('Test identity: signature verified; tampered class rejected; no production identity generated')
