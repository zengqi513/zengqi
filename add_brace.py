#!/usr/bin/env python3
import sys

with open(sys.argv[1], 'r', encoding='utf-8-sig') as f:
    lines = f.readlines()

# Add closing brace before the last newlines
# Find the last non-empty line
last_content = -1
for i in range(len(lines)-1, -1, -1):
    if lines[i].strip():
        last_content = i
        break

print(f'Last content line: {last_content+1}: {lines[last_content].rstrip()}')
lines.insert(last_content+1, '}\n')

with open(sys.argv[1], 'w', encoding='utf-8-sig') as f:
    f.writelines(lines)
print('Added }')
