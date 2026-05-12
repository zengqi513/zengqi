#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import re

with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8-sig') as f:
    content = f.read()

# Simple approach: just count braces line by line ignoring strings
lines = content.split('\n')
stack = []
open_line = None

for i, line in enumerate(lines, 1):
    stripped = line
    # remove strings
    stripped = re.sub(r'".*?"', '', stripped)
    stripped = re.sub(r"'.*?'", '', stripped)
    stripped = re.sub(r'//.*', '', stripped)
    
    for ch in stripped:
        if ch == '{':
            stack.append(i)
        elif ch == '}':
            if stack:
                stack.pop()

with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8-sig') as f:
    flines = f.readlines()

print(f'Unmatched opens: {len(stack)}')
for ln in stack:
    print(f'  Line {ln}: {flines[ln-1].rstrip()[:100]}')
