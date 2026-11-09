"""Extract a native installer in the workspace and test its bundled Java/JDBC.

This never installs into Program Files or /opt and does not modify user settings.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('platform', choices=['windows', 'linux'])
args = parser.parse_args()
root = Path('build/desktop-packages').resolve()
output = root / args.platform
extracted = output / 'extracted'
if extracted.exists():
    shutil.rmtree(extracted)
extracted.mkdir(parents=True)
if args.platform == 'windows':
    installer, = (output / 'msi').glob('*.msi')
    log = output / 'msi-extract.log'
    result = subprocess.run(['msiexec.exe', '/a', str(installer), '/qn', '/norestart',
                             f'TARGETDIR={extracted}', '/l*v', str(log)])
    if result.returncode not in (0, 3010):
        raise RuntimeError(f'MSI extraction failed ({result.returncode}); see {log}')
    java, = extracted.rglob('runtime/bin/java.exe')
else:
    installer, = (output / 'deb').glob('*.deb')
    name = subprocess.check_output(['dpkg-deb', '--field', str(installer), 'Package'], text=True).strip()
    assert name == 'yonaka-reader', name
    subprocess.run(['dpkg-deb', '--extract', str(installer), str(extracted)], check=True)
    java, = extracted.rglob('lib/runtime/bin/java')
    desktop_files = list(extracted.rglob('*.desktop'))
    assert desktop_files, 'Missing desktop launcher'
    assert any('MimeType=' in p.read_text() for p in desktop_files), 'Missing file associations'
jar, = extracted.rglob('mangaReader-*.jar')
assert java.is_file() and jar.is_file()
temp = output / 'extracted-smoke-temp'
temp.mkdir(exist_ok=True)
subprocess.run([str(java), '-Djava.awt.headless=true', f'-Dorg.sqlite.tmpdir={temp}',
                '-cp', os.pathsep.join([str(jar), str(root / 'smoke-classes')]),
                'org.dustyroom.persistence.PackagedStoreSmoke'], check=True)
print(f'PASS: {installer.name}, extracted runtime, packaged SQLite')
