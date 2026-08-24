# -*- coding: utf-8 -*-
import json, re, pathlib
from collections import defaultdict, deque

base = pathlib.Path(r'C:\Users\ASUS\Desktop\TFT')

def load_tasks(name):
    text = (base / name).read_text(encoding='utf-8')
    m = re.search(r'const TASKS = (\[.*?\]);', text, re.DOTALL)
    return json.loads(m.group(1)) if m else []

v31_all = load_tasks('TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html')
v2_all = load_tasks('TFT_Vision_AI_Coach_Complete_Roadmap_v2.html')
p1 = [t for t in v31_all if t.get('phase') == 'P1']
p1_v2 = [t for t in v2_all if t.get('phase') == 'P1']

# save full P1
(base / '_p1_v31_full.json').write_text(json.dumps(p1, ensure_ascii=False, indent=2), encoding='utf-8')

# topo sort within P1 (only P1 deps)
ids = {t['id'] for t in p1}
indeg = {i: 0 for i in ids}
adj = defaultdict(list)
for t in p1:
    for d in t.get('dependencies', []):
        if d in ids:
            adj[d].append(t['id'])
            indeg[t['id']] += 1
q = deque(sorted(i for i in ids if indeg[i] == 0))
order = []
while q:
    n = q.popleft()
    order.append(n)
    for c in sorted(adj[n]):
        indeg[c] -= 1
        if indeg[c] == 0:
            q.append(c)

# group by epic
groups = defaultdict(list)
for t in p1:
    groups[t['epic']].append(t['id'])

# V2 P1 mapping old ids
old_p1 = {t['id']: t for t in p1_v2 if t['id'].startswith('P1-') and len(t['id']) <= 6}

print('=== P1 V3.1 COUNT ===', len(p1))
print('=== TOPO ORDER ===')
for i in order:
    print(i)
print('=== GROUPS BY EPIC ===')
for ep, ids_list in sorted(groups.items()):
    print(ep, len(ids_list), ids_list)
print('=== V2 OLD P1 IDs ===', sorted(old_p1.keys()))
print('=== HAS tech_route in P1 ===', sum(1 for t in p1 if t.get('tech_route')))
print('=== SAMPLE KEYS ===', list(p1[0].keys()) if p1 else [])
