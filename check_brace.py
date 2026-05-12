#!/usr/bin/env python3
import re, sys

f = sys.argv[1]
with open(f, 'r', encoding='utf-8-sig') as fh:
    content = fh.read()

lines = content.split('\n')
stack = []
for i, line in enumerate(lines, 1):
    sline = re.sub(r'"[^"]*"', '', line)
    sline = re.sub(r"'[^']*'", '', sline)
    sline = re.sub(r'//.*', '', sline)
    for ch in sline:
        if ch == '{':
            stack.append(i)
        elif ch == '}':
            if stack:
                stack.pop()
print(f'Unmatched: {len(stack)}')
