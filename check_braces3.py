#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import re

with open('E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt', 'r', encoding='utf-8-sig') as f:
    content = f.read()

# Remove string literals (replace with spaces to maintain line numbers)
# Handle """ strings too
def remove_strings(text):
    # raw strings and triple-quoted strings
    text = re.sub(r'"""[\s\S]*?"""', lambda m: '\n' * m.group(0).count('\n') + ' ' * (len(m.group(0).split('\n')[-1])), text)
    text = re.sub(r"'''[\s\S]*?'''", lambda m: '\n' * m.group(0).count('\n') + ' ' * (len(m.group(0).split('\n')[-1])), text)
    # single-line strings
    result = []
    in_str = False
    str_char = None
    i = 0
    while i < len(text):
        ch = text[i]
        if not in_str:
            if ch in ('"', "'"):
                in_str = True
                str_char = ch
            result.append(ch)
        else:
            if ch == '\\':
                result.append(' ')
                i += 1
                if i < len(text):
                    result.append(' ')
                i += 1
                continue
            elif ch == str_char:
                in_str = False
                result.append(ch)
            else:
                result.append(' ')
            i += 1
            continue
        i += 1
    return ''.join(result)

content = remove_strings(content)

# Also remove line comments
lines = content.split('\n')
for i in range(len(lines)):
    idx = lines[i].find('//')
    if idx >= 0:
        lines[i] = lines[i][:idx]

content = '\n'.join(lines)

# Now count braces accurately
stack = []
lines = content.split('\n')
for i, line in enumerate(lines, 1):
    for j, ch in enumerate(line):
        if ch == '{':
            stack.append((i, line.rstrip()))
        elif ch == '}':
            if stack:
                stack.pop()

print(f"Unmatched opens: {len(stack)}")
for line_no, text in stack:
    print(f"  line {line_no}: {text}")
