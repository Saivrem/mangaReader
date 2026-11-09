"""Real install/uninstall smoke test, restricted to disposable GitHub-hosted runners.

Never run this on a developer's workstation: it intentionally registers the app
with Windows Installer / APT and verifies removal of the installed files.
"""
import argparse
import os
from pathlib import Path
import subprocess
import uuid


def run(command, allowed=(0,)):
    print('Running:', ' '.join(str(arg) for arg in command), flush=True)
    result = subprocess.run([str(arg) for arg in command], check=False, timeout=300)
    if result.returncode not in allowed:
        raise RuntimeError(f'{command[0]} failed with exit code {result.returncode}')


def windows_registration():
    import winreg
    matches = []
    for hive in (winreg.HKEY_CURRENT_USER, winreg.HKEY_LOCAL_MACHINE):
        for view in (winreg.KEY_WOW64_64KEY, winreg.KEY_WOW64_32KEY):
            try:
                root = winreg.OpenKey(hive, r'Software\Microsoft\Windows\CurrentVersion\Uninstall',
                                      0, winreg.KEY_READ | view)
            except FileNotFoundError:
                continue
            with root:
                for index in range(winreg.QueryInfoKey(root)[0]):
                    name = winreg.EnumKey(root, index)
                    with winreg.OpenKey(root, name) as key:
                        try:
                            display = winreg.QueryValueEx(key, 'DisplayName')[0]
                            uninstall = winreg.QueryValueEx(key, 'UninstallString')[0]
                        except FileNotFoundError:
                            continue
                        if display == 'Yonaka Reader':
                            assert uninstall, 'Missing Windows uninstall command'
                            matches.append(name)
    return matches


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('platform', choices=['windows', 'linux'])
    args = parser.parse_args()
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted':
        raise SystemExit('This test is only allowed on a disposable GitHub-hosted runner.')
    output = Path('build/desktop-packages').resolve() / args.platform
    unique = 'yonaka-uninstall-test-' + uuid.uuid4().hex
    book = output / (unique + '.txt')
    data_root = (Path(os.environ['APPDATA']) / 'MangaReader' if args.platform == 'windows'
                 else Path(os.environ.get('XDG_DATA_HOME') or str(Path.home() / '.local/share')) / 'manga-reader')
    data_root.mkdir(parents=True, exist_ok=True)
    state = data_root / (unique + '.txt')
    for path in (book, state):
        with path.open('x') as stream:
            stream.write(unique)
    try:
        if args.platform == 'windows':
            installer, = (output / 'msi').glob('*.msi')
            assert not windows_registration(), 'Refusing to replace an existing installation'
            target = output / 'installed app'
            assert not target.exists(), 'Installation target must be new'
            try:
                run(['msiexec.exe', '/i', installer, '/qn', '/norestart', f'INSTALLDIR={target}',
                     '/l*v', output / 'msi-install.log'], allowed=(0, 3010))
                assert windows_registration(), 'Application not registered for Windows uninstall'
                assert (target / 'Yonaka Reader.exe').is_file(), 'Installed launcher missing'
                assert (target / 'runtime/bin/java.exe').is_file(), 'Installed runtime missing'
            finally:
                run(['msiexec.exe', '/x', installer, '/qn', '/norestart',
                     '/l*v', output / 'msi-uninstall.log'], allowed=(0, 3010))
            assert not windows_registration(), 'Uninstall registration left behind'
            assert not (target / 'Yonaka Reader.exe').exists(), 'Launcher left behind'
            assert not any(p.is_file() for p in target.rglob('*')), 'Installed files left behind'
        else:
            installer, = (output / 'deb').glob('*.deb')
            name = subprocess.check_output(['dpkg-deb', '--field', str(installer), 'Package'], text=True).strip()
            assert name == 'yonaka-reader', name
            status = subprocess.run(['dpkg-query', '-W', '-f=${Status}', name], capture_output=True, text=True)
            assert 'install ok installed' not in status.stdout, 'Refusing to replace an existing installation'
            installed_files = []
            try:
                run(['sudo', 'apt-get', 'install', '-y', str(installer)])
                paths = subprocess.check_output(['dpkg-query', '-L', name], text=True).splitlines()
                installed_files = [Path(p) for p in paths if Path(p).is_file() or Path(p).is_symlink()]
                assert any(p.name == 'Yonaka Reader' for p in installed_files), 'Installed launcher missing'
                assert any(p.suffix == '.desktop' for p in installed_files), 'Desktop entry missing'
            finally:
                run(['sudo', 'apt-get', 'remove', '-y', name])
            status = subprocess.run(['dpkg-query', '-W', '-f=${Status}', name], capture_output=True, text=True)
            assert 'install ok installed' not in status.stdout, 'Package still installed'
            assert not any(p.exists() or p.is_symlink() for p in installed_files), 'Installed files left behind'
        for path in (book, state):
            assert path.read_text() == unique, 'Uninstall modified user books or reader data'
        print('PASS: real install/uninstall; binaries removed, user data and books retained')
    finally:
        # Only remove the uniquely named sentinels created by this run.
        book.unlink(missing_ok=True)
        state.unlink(missing_ok=True)


if __name__ == '__main__':
    main()
