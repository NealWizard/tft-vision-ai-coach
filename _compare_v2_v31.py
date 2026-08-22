# -*- coding: utf-8 -*-
import json, re, pathlib

base = pathlib.Path(r'C:\Users\ASUS\Desktop\TFT')

def load(name):
    text = (base / name).read_text(encoding='utf-8')
    return json.loads(re.search(r'const TASKS = (\[.*?\]);', text, re.DOTALL).group(1))

v31 = load('TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html')
v2 = load('TFT_Vision_AI_Coach_Complete_Roadmap_v2.html')

p1_31 = [t for t in v31 if t['phase'] == 'P1']
p1_v2 = [t for t in v2 if t['phase'] == 'P1']

# V2 P1-001~005
old = {t['id']: t for t in p1_v2}

mapping = {
    'P1-001': 'P1-DATA-SourceAdapter-001',
    'P1-002': 'P1-DATA-Snapshot-001',
    'P1-003': 'P1-DATA-Riot-001',
    'P1-004': 'P1-DATA-Stats-001',
    'P1-005': 'P1-DATA-Stats-002',
}

out = {
    'v31_p1_tasks': p1_31,
    'v2_p1_count': len(p1_v2),
    'v31_p1_count': len(p1_31),
    'mapping_compare': {},
}

for old_id, new_id in mapping.items():
    o = old.get(old_id, {})
    n = next((t for t in p1_31 if t['id'] == new_id), {})
    out['mapping_compare'][old_id] = {
        'new_id': new_id,
        'v2': {k: o.get(k) for k in ['title', 'requirement', 'acceptance', 'dependency', 'story_points']},
        'v31': {k: n.get(k) for k in ['title', 'requirement', 'acceptance', 'dependencies', 'sp']},
    }

# V2 tasks not in V3.1 canonical naming
v2_ids = {t['id'] for t in p1_v2}
v31_ids = {t['id'] for t in p1_31}

# count v2 P1 including RAG/LLM
v2_p1_all = [t['id'] for t in p1_v2]

out['v2_p1_ids'] = v2_p1_all
out['v31_p1_ids'] = [t['id'] for t in p1_31]

(base / '_analysis_output.json').write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding='utf-8')
print('done', len(p1_31))
