"""Build the README's Minecraft pixel illustrations from the editable GUI kit."""
from pathlib import Path
import argparse
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / 'art/readme'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify', action='store_true', help='Verify fresh-process determinism')
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='qlxe-readme-art-') as temporary:
        output = Path(temporary) / 'out'
        command = [sys.executable, str(SOURCE / 'build.py'), '--out', str(output),
                   '--namespace', 'questlog_envelope']
        if args.verify:
            command.append('--verify')
        subprocess.run(command, check=True)
        images = ROOT / 'images'
        images.mkdir(exist_ok=True)
        sprites = output / 'assets/questlog_envelope/textures/gui/sprites/readme'
        for name in ('banner', 'features'):
            shutil.copyfile(sprites / (name + '.png'), images / (name + '.png'))
        shutil.copyfile(output / 'asset-report.json', SOURCE / 'asset-report.json')
    print('Exported docs/images/banner.png and docs/images/features.png')


if __name__ == '__main__':
    main()
