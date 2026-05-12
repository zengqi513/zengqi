with open("E:/AutoBookkeeper/app/src/main/java/com/autobookkeeper/ui/MainScreen.kt", "r", encoding="utf-8-sig") as f:
    c = f.read()
for imp in ["MutableInteractionSource", "IntOffset", "roundToInt", "pointerInput", "detectTapGestures"]:
    present = "YES" if imp in c else "NO"
    print(f"{imp}: {present}")
