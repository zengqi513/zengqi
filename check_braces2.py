#!/usr/bin/env python3
# -*- coding: utf-8 -*-
with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

stack = []
in_block_comment = False
for i, line in enumerate(lines, 1):
    j = 0
    while j < len(line):
        ch = line[j]
        # Block comments
        if in_block_comment:
            if ch == '*' and j+1 < len(line) and line[j+1] == '/':
                in_block_comment = False
                j += 2
                continue
            j += 1
            continue
        # Line comment start
        if ch == '/' and j+1 < len(line) and line[j+1] == '/':
            break
        # Block comment start
        if ch == '/' and j+1 < len(line) and line[j+1] == '*':
            in_block_comment = True
            j += 2
            continue
        # Strings (skip)
        if ch == '"' or ch == "'":
            quote = ch
            j += 1
            while j < len(line):
                if line[j] == '\\':
                    j += 2
                    continue
                if line[j] == quote:
                    break
                j += 1
            j += 1
            continue
        # Template string ${
        if ch == '$' and j+1 < len(line) and line[j+1] == '{':
            j += 2
            continue
        if ch == '{':
            stack.append((i, line.rstrip()))
        elif ch == '}':
            if not stack:
                print(f"EXTRA CLOSE at line {i}: {line.rstrip()}")
            else:
                stack.pop()
        j += 1

print(f"Unmatched opens: {len(stack)}")
for line_no, text in stack:
    print(f"  line {line_no}: {text}")
