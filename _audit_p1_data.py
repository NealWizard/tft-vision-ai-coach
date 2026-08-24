import json, re
html = open(r'C:\Users\ASUS\Desktop\TFT\TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html', encoding='utf-8').read()
m = re.search(r'const TASKS = (\[.*?\]);', html, re.S)
tasks = json.loads(m.group(1))
p1 = [t for t in tasks if t['phase'] == 'P1' and t['epic'] == 'DATA']
for t in sorted(p1, key=lambda x: x['id']):
    deps = ','.join(t.get('dependencies') or [])
    print(f"{t['id']}: {t['status']} | deps={deps} | {t['title']}")
