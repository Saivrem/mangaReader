"""Register existing macOS file types as alternate viewers, never as their owner."""
import plistlib
import sys
from pathlib import Path

bundle = Path(sys.argv[1])
plist = bundle / 'Contents' / 'Info.plist'
with plist.open('rb') as stream:
    info = plistlib.load(stream)
assert info['CFBundleIdentifier'] == 'org.dustyroom.mangareader'
assert info['CFBundleVersion'] == sys.argv[3]
info['CFBundleShortVersionString'] = sys.argv[2]
expected = {p.stem for p in (Path(__file__).parent / 'associations').glob('*.properties')}
standard_types = {'jpg': 'public.jpeg', 'jpeg': 'public.jpeg', 'png': 'public.png',
                  'gif': 'com.compuserve.gif', 'zip': 'public.zip-archive'}
# JDK 17 exports custom UTIs and marks the app as the default editor. These are
# existing system formats: use their system UTIs and a non-default Viewer role.
exported = info.pop('UTExportedTypeDeclarations', [])
extensions = {entry['UTTypeIdentifier']: entry['UTTypeTagSpecification']['public.filename-extension']
              for entry in exported}
actual = set()
for document in info.get('CFBundleDocumentTypes', []):
    suffixes = [ext for uti in document['LSItemContentTypes'] for ext in extensions[uti]]
    actual.update(suffixes)
    document['LSItemContentTypes'] = sorted({standard_types[ext] for ext in suffixes})
    document['CFBundleTypeExtensions'] = suffixes
    document['CFBundleTypeRole'] = 'Viewer'
    document['LSHandlerRank'] = 'Alternate'
    document.pop('LSIsAppleDefaultForType', None)
assert actual == expected, f'Incorrect file associations: {actual} != {expected}'
with plist.open('wb') as stream:
    plistlib.dump(info, stream, sort_keys=False)
