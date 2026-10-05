from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parent
MODE = 'gui'

def digest(root):
    h = hashlib.sha256()
    for p in sorted(root.rglob('*')):
        if p.is_file():
            h.update(p.relative_to(root).as_posix().encode())
            h.update(p.read_bytes())
    return h.hexdigest()

def main():
    ap = argparse.ArgumentParser(description='Standalone AgentCraft-derived pixel asset generator')
    ap.add_argument('--out', required=True, type=Path, help='New or empty output directory')
    ap.add_argument('--namespace', required=True)
    ap.add_argument('--verify', action='store_true', help='Compare output with two fresh-process builds')
    args = ap.parse_args()
    if not re.fullmatch(r'[a-z0-9_][a-z0-9_.-]*', args.namespace):
        ap.error('Invalid Minecraft namespace')
    out = args.out.resolve()
    if out.exists() and (not out.is_dir() or any(out.iterdir())):
        ap.error('Output must be new or empty; choose a versioned path')
    if out == ROOT or ROOT.is_relative_to(out):
        ap.error('Output must not contain the source directory')
    out.mkdir(parents=True, exist_ok=True)
    sys.path.insert(0, str(ROOT / 'gen'))
    import common
    common.OUT = out / 'assets' / args.namespace
    common.ART = out / 'review'
    if MODE == 'gui':
        import gui
        gui.build()
    elif MODE == 'blocks':
        import blocks, models
        blocks.build()
        models.build()
    elif MODE == 'skins':
        import skins, portraits
        skins.build()
        portraits.build()
    for path in common.OUT.rglob('*'):
        if path.suffix in ('.json', '.mcmeta'):
            def rewrite(value):
                if isinstance(value, str):
                    for old, new in [('agentcraft:', args.namespace + ':'),
                                     ('block.agentcraft.', 'block.' + args.namespace + '.'),
                                     ('item.agentcraft.', 'item.' + args.namespace + '.'),
                                     ('itemGroup.agentcraft', 'itemGroup.' + args.namespace),
                                     ('"agentcraft"', '"' + args.namespace + '"')]:
                        value = value.replace(old, new)
                    return value
                if isinstance(value, dict):
                    return {rewrite(k): rewrite(v) for k, v in value.items()}
                if isinstance(value, list): return [rewrite(v) for v in value]
                return value
            data = rewrite(json.loads(path.read_text(encoding='utf-8')))
            path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8', newline='\n')
    from PIL import Image
    report = {}
    for path in sorted(common.OUT.rglob('*.png')):
        with Image.open(path) as im:
            im.load()
            if im.mode != 'RGBA':
                raise ValueError(f'{path}: expected RGBA')
            colors = set(im.getdata())
            report[path.relative_to(out).as_posix()] = {
                'size': list(im.size), 'rgba_colors': len(colors),
                'visible_rgb_colors': len({p[:3] for p in colors if p[3]}),
                'alpha_values': sorted({p[3] for p in colors}),
            }
            meta_path = path.with_suffix('.png.mcmeta')
            if meta_path.exists():
                scaling = json.loads(meta_path.read_text())['gui']['scaling']
                b = scaling['border']
                if (scaling['width'], scaling['height']) != im.size:
                    raise ValueError(f'{path}: metadata dimensions differ')
                if b['left'] + b['right'] >= im.width or b['top'] + b['bottom'] >= im.height:
                    raise ValueError(f'{path}: nine-slice leaves no center')
    if MODE == 'blocks':
        def check_ref(value, folder):
            if not value.startswith(args.namespace + ':'):
                return
            rel = value.split(':', 1)[1]
            suffix = '.png' if folder == 'textures' else '.json'
            if not (common.OUT / folder / (rel + suffix)).is_file():
                raise ValueError(f'Missing {folder} reference: {value}')
        def walk(value):
            if isinstance(value, dict):
                for key, item in value.items():
                    if key in ('parent', 'model') and isinstance(item, str):
                        check_ref(item, 'models')
                    elif key == 'textures' and isinstance(item, dict):
                        for ref in item.values():
                            if isinstance(ref, str) and not ref.startswith('#'):
                                check_ref(ref, 'textures')
                    walk(item)
            elif isinstance(value, list):
                for item in value: walk(item)
        for path in common.OUT.rglob('*.json'):
            walk(json.loads(path.read_text(encoding='utf-8')))
    (out / 'asset-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(f'Validated {len(report)} PNG assets in {out}')
    if args.verify:
        baseline = digest(out)
        for seed in ('1', '987654'):
            with tempfile.TemporaryDirectory(prefix='agentcraft-assets-') as tmp:
                check = Path(tmp) / 'out'
                subprocess.run([sys.executable, str(ROOT / 'build.py'), '--out', str(check),
                                '--namespace', args.namespace], check=True,
                               env={**os.environ, 'PYTHONHASHSEED': seed})
                if digest(check) != baseline:
                    raise RuntimeError(f'Non-deterministic output with seed {seed}')
        print('Determinism verified across three fresh builds')

if __name__ == '__main__':
    main()
