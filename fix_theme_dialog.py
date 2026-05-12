import sys

with open(sys.argv[1], 'r', encoding='utf-8') as f:
    content = f.read()

# Find the start of the theme dialog section
old_start = "// ═══ 深色模式弹窗 ═══\n    if (showThemeDialog && userPreferences != null) {"

if old_start not in content:
    # Try with BOM
    old_start = "\ufeff// ═══ 深色模式弹窗 ═══\n    if (showThemeDialog && userPreferences != null) {"
    if old_start not in content:
        print("Could not find start marker", file=sys.stderr)
        # Show what we have near the end
        lines = content.split('\n')
        for i, line in enumerate(lines[-65:-30]):
            print(f"  {i}: {repr(line)}", file=sys.stderr)
        sys.exit(1)

new_content = """    // ═══ 主题弹窗（配色+深色模式）═══
    if (showThemeDialog && userPreferences != null) {
        var selTheme by remember(paletteName) { mutableStateOf(paletteName) }
        var selF by remember { mutableStateOf(selFollow) }
        var selD by remember { mutableStateOf(selDark) }
        val themeOptions = listOf(
            "WarmGreen" to "🍃 暖绿",
            "ForestGreen" to "🌲 深林",
            "IndigoBlue" to "💎 靛蓝",
            "RoseGold" to "🌸 玫瑰金"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("主题设置") },
            text = {
                Column {
                    Text("配色", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                    themeOptions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selTheme = key }
                                .background(
                                    if (selTheme == key) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            if (selTheme == key) {
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("深色模式", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selF = true; selD = false }
                            .background(
                                if (selF) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌓 跟随系统", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (selF) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selF = false; selD = false }
                            .background(
                                if (!selF && !selD) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☀️ 浅色", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (!selF && !selD) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selF = false; selD = true }
                            .background(
                                if (!selF && selD) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌙 深色", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (!selF && selD) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (selF) {
                            userPreferences.setFollowSystem(true)
                        } else {
                            userPreferences.setDarkMode(selD)
                        }
                        userPreferences.setThemePalette(selTheme)
                    }
                    showThemeDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("取消") }
            }
        )
    }
"""

# Find end of theme dialog (dismissButton)
end_marker = "dismissButton = {\n                TextButton(onClick = { showThemeDialog = false }) { Text("
if "取消" in content[content.find("dismissButton"):]:
    # Try to find the end
    start_idx = content.find(old_start)
    if start_idx < 0:
        # Try without BOM prefix
        old_start_no_bom = "// ═══ 深色模式弹窗 ═══\n    if (showThemeDialog && userPreferences != null) {"
        start_idx = content.find(old_start_no_bom)
    if start_idx >= 0:
        end_idx = content.rfind("\n}")
        if end_idx > start_idx:
            # Replace from old_start to end
            before = content[:start_idx]
            after = content[end_idx+2:]
            new_full = before + new_content + after
            with open(sys.argv[1], 'w', encoding='utf-8') as f:
                f.write(new_full)
            print("Done: replaced theme dialog successfully", file=sys.stderr)
        else:
            print("Could not find end of file", file=sys.stderr)
    else:
        print(f"Could not find start marker. Searching near end...", file=sys.stderr)
        # debug output
        last_part = content[-1500:]
        print(repr(last_part), file=sys.stderr)
