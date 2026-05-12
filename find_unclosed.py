#!/usr/bin/env python3
# -*- coding: utf-8 -*-
with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8-sig') as f:
    content = f.read()

import re
# Remove string literals
content = re.sub(r'"[^"]*"', '', content)
# Remove line comments
content = re.sub(r'//.*', '', content)

lines = content.split('\n')
stack = []
for i, line in enumerate(lines, 1):
    for ch in line:
        if ch == '{':
            stack.append(i)
        elif ch == '}':
            if stack:
                stack.pop()

print(f'Unclosed blocks: {len(stack)}')
with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8-sig') as f:
    flines = f.readlines()
for line_no in stack:
    print(f'  Line {line_no}: {flines[line_no-1].rstrip()[:120]}')
