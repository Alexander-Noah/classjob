"""Render one default branch per MyBatis SELECT for EXPLAIN on MySQL."""
from pathlib import Path
import xml.etree.ElementTree as ET
import re
root = Path(__file__).resolve().parents[2]
statements = []
for file in sorted((root / 'src/main/resources/mapper').glob('*.xml')):
    mapper = ET.parse(file).getroot()
    fragments = {e.get('id'): e for e in mapper.findall('sql')}
    # 渲染当前操作。
    # 入参：e（e）。
    # 返回：由当前表达式或分支生成的处理结果。
    def render(e):
        if e.tag == 'include':
            return render(fragments[e.get('refid')])
        if e.tag == 'if':
            return ''
        if e.tag == 'foreach':
            return e.get('open','') + 'NULL' + e.get('close','')
        if e.tag == 'choose':
            child = e.find('otherwise')
            if child is None:
                child = e.find('when')
            return render(child) if child is not None else ''
        s = (e.text or '') + ''.join(render(c) + (c.tail or '') for c in e)
        if e.tag == 'where':
            s = re.sub(r'^\s*(and|or)\b', '', s, flags=re.I).strip()
            return ' WHERE ' + s if s else ''
        return ' ' + s + ' '
    for element in mapper.findall('select'):
        sql = render(element)
        sql = re.sub(r'#\{[^}]*\}', 'NULL', sql)
        if '${' in sql:
            raise ValueError(f'Unresolved SQL substitution: {file.name}')
        sql = re.sub(r'limit\s+NULL\s+offset\s+NULL', 'limit 1 offset 0', sql, flags=re.I)
        sql = re.sub(r'limit\s+NULL', 'limit 1', sql, flags=re.I)
        statements.append((f'{file.name}:{element.get("id")}', sql.strip()))
import json
(Path(__file__).parent / 'mapper-select-checks.json').write_text(json.dumps(statements, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Prepared {len(statements)} SELECT plans (default dynamic branches).')
