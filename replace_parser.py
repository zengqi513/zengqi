with open(r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\util\DouyinPdfParser.kt', 'r', encoding='utf-8') as f:
    content = f.read()

marker = '    // ─── 交易解析（全局最近邻配对）───'
idx = content.find(marker)
print(f'Marker at: {idx}')
if idx < 0:
    print('ERROR: marker not found')
    exit(1)

new_code = r'''    // ─── 交易解析（全局最近邻配对 — 参考用户 Java 实现）───
    // 核心思路：
    // 1. 逐行判断：isDate 还是 isAmount（跳过类型词行如"支出/收入/退款/其他"）
    // 2. 每个金额行，往前找最近的日期行 → 日期行→金额行 必在同行
    // 3. 在 [日期行, 金额行] 区间内搜索"退款/收入/支出"判定类型
    // 4. 类型决定金额正负：收入→正，支出→负

    private val datePattern = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    private val amountPattern = Regex("\\d+\.\d{2}")

    internal fun parseTransactions(fullText: String): List<ParsedEntry> {
        val lines = fullText.split("\n")
        Log.i(TAG, "总行数: ${lines.size}")

        // 收集所有日期行信息：(lineIndex, dateString)
        val dateLines = mutableListOf<Pair<Int, String>>()
        for ((i, line) in lines.withIndex()) {
            val m = datePattern.find(line) ?: continue
            val ds = m.value
            // 过滤表头假日期（交易时间段起止行）
            if (ds.endsWith(" 00:00:00")) continue
            dateLines.add(i to ds)
        }

        Log.i(TAG, "识别到 ${dateLines.size} 个日期行")

        // 全局配对
        val usedDateIdxs = mutableSetOf<Int>()
        val result = mutableListOf<ParsedEntry>()

        for ((amtLineIdx, line) in lines.withIndex()) {
            val trimmed = line.trim()
            // 跳过纯类型词行（这些行不是金额行）
            if (trimmed == "支出" || trimmed == "收入" || trimmed == "退款" || trimmed == "其他") continue
            // 跳过含日期的行（不是纯金额行）
            if (datePattern.find(line) != null) continue

            val m = amountPattern.find(line) ?: continue
            val amount = m.value.toDoubleOrNull() ?: continue
            if (amount <= 0 || amount >= 999999) continue

            // 往前找最近的未匹配日期
            var bestDateIdx = -1
            for (j in amtLineIdx - 1 downTo 0) {
                if (dateLines.any { it.first == j }) {
                    bestDateIdx = j
                    break
                }
            }
            if (bestDateIdx < 0) continue

            usedDateIdxs.add(bestDateIdx)
            val dateStr = dateLines.first { it.first == bestDateIdx }.second

            // 在 [日期行, 金额行] 区间内搜索类型关键词
            var type = "支出"
            for (j in bestDateIdx..amtLineIdx) {
                val lj = lines.getOrNull(j)?.trim() ?: continue
                when {
                    "退款" in lj -> { type = "收入"; break }
                    "收入" in lj -> type = "收入"
                    "支出" in lj -> type = "支出"
                }
            }

            val finalAmount = if (type == "收入") Math.abs(amount) else -Math.abs(amount)
            result.add(ParsedEntry(dateStr, finalAmount, type, "抖音电商"))
        }

        // 去重（相同日期+金额）
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<ParsedEntry>()
        for (p in result) {
            val key = "${p.date}|${String.format("%.2f", Math.abs(p.amount))}"
            if (key in seen) continue
            seen.add(key)
            unique.add(p)
        }

        Log.i(TAG, "配对结果: ${result.size} 条，去重后: ${unique.size} 条")
        unique.forEach { Log.i(TAG, "  ${it.date} | ${String.format("%.2f", it.amount)} | ${it.type} | ${it.merchant}") }
        return unique
    }

    private fun classifyDouyinEntry(merchant: String, amount: Double): Pair<String, String> {
        if (amount > 0) return "退款" to "\uD83D\uDD19"
        val lower = merchant.lowercase()
        if (lower.contains("超市") || lower.contains("便利") || lower.contains("朴朴")) {
            return "超市" to "\uD83C\uDFEA"
        }
        if (lower.contains("餐") || lower.contains("饭") || lower.contains("面") || lower.contains("饺")) {
            return "餐饮" to "\uD83C\uDF5C"
        }
        if (lower.contains("车") || lower.contains("油") || lower.contains("地铁") || lower.contains("公交")) {
            return "交通" to "\uD83D\uDE97"
        }
        return "购物" to "\uD83D\uDED2"
    }

    private fun parseDateToMs(dateStr: String): Long? {
        val cleaned = dateStr.replace("/", "-")
        for (fmt in listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA),
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA),
            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        )) { try { return fmt.parse(cleaned)?.time } catch (_: Exception) {} }
        return null
    }
'''

new_content = content[:idx] + new_code + '\n}'

oc = new_content.count('{')
cc = new_content.count('}')
print(f'Braces: open={oc} close={cc} diff={oc - cc}')

with open(r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\util\DouyinPdfParser.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)

print(f'Written: {len(new_content)} bytes')