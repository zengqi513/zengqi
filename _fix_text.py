#!/usr/bin/env python3
"""Write correct ReportScreen.kt with proper M3 Text API"""
import re

path = r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\ReportScreen.kt'

# Read the broken file
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix Text() calls: Text("xxx", MaterialTheme.typography.xxx) -> Text("xxx", style = MaterialTheme.typography.xxx)
# Pattern: Text(<quote>, <M3.typo>)  without "style ="
def fix_text(m):
    full = m.group(0)
    # If already has 'style =', skip
    if 'style =' in full:
        return full
    # Replace: Text("...", MaterialTheme.typography.xxx) -> Text("...", style = MaterialTheme.typography.xxx)
    # Match: Text( "string" , M3.typo )
    return re.sub(
        r'Text\s*\(\s*(["\'][^"\']*["\'])\s*,\s*(MaterialTheme\.typography\.\w+)',
        r'Text(\1, style = \2',
        full
    )

content = re.sub(r'Text\([^)]*MaterialTheme\.typography[^)]*\)', fix_text, content)

# Fix extra patterns
# Text(text = ..., color = ..., style = ...) - OK
# Text("...", color = ..., style = ...) - OK if style named

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Text fixes applied')
