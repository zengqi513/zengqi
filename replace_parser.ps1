param(
    [string]$FilePath = "C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\util\DouyinPdfParser.kt"
)

$content = Get-Content $FilePath -Raw

# Find the marker "交易解析（逐行扫描队列模式）"
$marker1 = @"
    // ─── 交易解析（逐行扫描队列模式）───
"@

$startIdx = $content.IndexOf($marker1)
if ($startIdx -eq -1) {
    Write-Host "ERROR: 找不到起点标记"
    exit 1
}

$newCode = @"
    // ─── 交易解析（按时间切割交易块）───
    // 参考用户给的 Java 实现思路：以完整日期时间为分隔符，
    // 将 OCR 文本切割成块，每个块内独立判断类型。
    // 这样彻底避免列优先布局中的跨列误配问题。

    private val fullDateRx = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    private val shortDateRx = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")
    private val amountRx = Regex("\\d+\\.\\d{2}")

    internal fun parseTransactions(lines: List<String>): List<ParsedEntry> {
        val fullText = lines.joinToString("\n")

        val dateMatches = fullDateRx.findAll(fullText).map { it.value }.filter { !it.endsWith("00:00:00") }.toList()
        val blocks = fullDateRx.split(fullText).filter { it.isNotBlank() }

        if (dateMatches.isEmpty() || blocks.isEmpty()) return emptyList()

        val pairCount = minOf(dateMatches.size, blocks.size)
        val result = mutableListOf<ParsedEntry>()

        for (idx in 0 until pairCount) {
            val dateStr = dateMatches[idx]
            val blockContent = blocks[idx]

            val am = amountRx.find(blockContent) ?: continue
            val amount = am.value.toDoubleOrNull() ?: continue
            if (amount <= 0 || amount >= 999999) continue

            val isRefund = "退款" in blockContent
            val finalAmount = if (isRefund) amount else -amount
            val type = if (isRefund) "收入" else "支出"

            result.add(ParsedEntry(dateStr, finalAmount, type, "抖音电商"))
        }

        val seen = mutableSetOf<String>()
        val unique = mutableListOf<ParsedEntry>()
        for (p in result) {
            val key = "${p.date}|${String.format("%.2f", Math.abs(p.amount))}"
            if (key in seen) continue
            seen.add(key)
            unique.add(p)
        }

        Log.i(TAG, "去重前: " + result.size + " 去重后: " + unique.size)
        Log.i(TAG, "最终识别: " + unique.size + " 条")
        unique.forEach { Log.i(TAG, "  ${it.date} | ${String.format("%.2f", it.amount)} | ${it.type} | ${it.merchant}") }
        return unique
    }
"@

$newContent = $content.Substring(0, $startIdx) + $newCode + "`n}"

# Verify brace balance
$openCount = 0; $closeCount = 0
foreach ($c in $newContent.ToCharArray()) {
    if ($c -eq '{') { $openCount++ }
    if ($c -eq '}') { $closeCount++ }
}
Write-Host "Brace balance: open=$openCount close=$closeCount diff=$($openCount - $closeCount)"

# Quick validation - find class boundaries
$objectStart = $newContent.IndexOf("object DouyinPdfParser {")
$objectEnd = $newContent.LastIndexOf("}")
if ($objectStart -ge 0) {
    $inner = $newContent.Substring($objectStart, $objectEnd - $objectStart + 1)
    $io = 0; $ic = 0
    foreach ($c in $inner.ToCharArray()) {
        if ($c -eq '{') { $io++ }
        if ($c -eq '}') { $ic++ }
    }
    Write-Host "Object braces: open=$io close=$ic (expected: open=close+1)"
}

Set-Content -Path $FilePath -Value $newContent -Encoding UTF8 -NoNewline
Write-Host "Written: $($newContent.Length) bytes"
