#!/usr/bin/env python3
"""Fix ReportScreen.kt Text() calls and other issues"""
import re

path = r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\ReportScreen.kt'

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: Text("...", MaterialTheme.typography.xxx) -> Text(text = "...", style = MaterialTheme.typography.xxx)
# But don't double-fix if already has style =
def fix_text_call(match):
    full = match.group(0)
    if 'style =' in full:
        return full
    # Extract the string and the typography
    # Pattern: Text("...", MaterialTheme.typography.xxx)
    m = re.search(r'Text\s*\(\s*([^,]+)\s*,\s*(MaterialTheme\.typography\.\w+)', full)
    if m:
        text_part = m.group(1)
        typo_part = m.group(2)
        # Replace with named parameters
        return full.replace(f'{text_part}, {typo_part}', f'text = {text_part}, style = {typo_part}')
    return full

# Apply fix to all Text calls containing MaterialTheme.typography
content = re.sub(r'Text\([^)]*MaterialTheme\.typography[^)]*\)', fix_text_call, content)

# Fix 2: Text("...", color = ..., MaterialTheme.typography.xxx) -> add style =
content = re.sub(
    r'Text\s*\(\s*([^,]+)\s*,\s*(color\s*=\s*[^,]+)\s*,\s*(MaterialTheme\.typography\.\w+)',
    r'Text(text = \1, \2, style = \3',
    content
)

# Fix 3: Text("...", fontWeight = ..., MaterialTheme.typography.xxx) -> add style =  
content = re.sub(
    r'Text\s*\(\s*([^,]+)\s*,\s*(fontWeight\s*=\s*[^,]+)\s*,\s*(MaterialTheme\.typography\.\w+)',
    r'Text(text = \1, \2, style = \3',
    content
)

# Fix 4: Text with multiple params before typography
# Text("...", color = ..., fontWeight = ..., MaterialTheme.typography.xxx)
content = re.sub(
    r'Text\s*\(\s*([^,]+)\s*,\s*((?:\w+\s*=\s*[^,]+,\s*)+)(MaterialTheme\.typography\.\w+)',
    r'Text(text = \1, \2style = \3',
    content
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Text fixes applied")
