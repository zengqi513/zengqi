#!/usr/bin/env python3
# -*- coding: utf-8 -*-
with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8-sig') as f:
    content = f.read()

import re
content = re.sub(r'"[^"]*"', '', content)
content = re.sub(r"'(not|doesn|ain|isn|wasn|weren|haven|hasn|hadn|won|wouldn|shan|shouldn|mayn|mightn|couldn)" r"t'", '', content)  # avoid removing apostrophes in contractions
content = re.sub(r"'.*?'", '', content)  # char literals
content = re.sub(r'//.*', '', content)

lines = content.split('\n')
stack = []  # list of lists - track full nesting

for i, line in enumerate(lines, 1):
    for ch in line:
        if ch == '{':
            stack.append(i)
        elif ch == '}':
            if stack:
                stack.pop()
            else:
                print(f'EXTRA close at line {i}')

print(f'Unclosed: {len(stack)}')
with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8-sig') as f:
    flines = f.readlines()
for line_no in stack:
    print(f'  Line {line_no}: {flines[line_no-1].rstrip()[:120]}')
