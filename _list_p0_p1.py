import json, re
html = open(r'C:\Users\ASUS\Desktop\TFT\TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html', encoding='utf-8').read()
m = re.search(r'const TASKS = (\[.*?\]);', html, re.S)
tasks = json.loads(m.group(1))
for phase in ['P0', 'P1']:
    print(f'\n=== {phase} ===')
    for t in sorted([x for x in tasks if x['phase']==phase], key=lambda x: x['id']):
        print(f"{t['id']}: {t['title']} [{t['status']}]")
