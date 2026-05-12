#!/usr/bin/env python3
# -*- coding: utf-8 -*-
with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

opens = []
issues = []
for i, line in enumerate(lines, 1):
    stripped = line.rstrip()
    for j, ch in enumerate(stripped):
        if ch == '{':
            opens.append((i, j, stripped))
        elif ch == '}':
            if not opens:
                issues.append(f"Line {i}: extra closing brace (no matching open)")
            else:
                opens.pop()

print(f"Total lines: {len(lines)}")
print(f"Unmatched opens: {len(opens)}")
for line_no, col, text in opens:
    print(f"  Open at line {line_no}: {text}")
for issue in issues:
    print(issue)
