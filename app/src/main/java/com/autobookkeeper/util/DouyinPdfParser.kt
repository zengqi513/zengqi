package com.autobookkeeper.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.autobookkeeper.data.Source
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 抖音支付账单 PDF 导入器（PdfBox 表格解析方案）
 *
 * 核心策略：
 * 1. PdfBox PDFTextStripper 提取完整文本（正确处理中文CID编码）
 * 2. 按行分割，每行7列：日期、收支类型、金额、付款方式、交易单号、交易对方、商家单号
 * 3. 直接解析，无需OCR
 */
object DouyinPdfParser {

    private const val TAG = "DouyinPdfParser"

    data class ParsedEntry(
        val date: String,
        val amount: Double,
        val type: String,
        val payMethod: String
    )

    /** 主入口 */
    suspend fun importPdf(
        context: Context,
        uri: Uri,
        @Suppress("UNUSED_PARAMETER") existingCategories: List<com.autobookkeeper.data.CategoryData>
    ): BillImporter.ImportResult {
        return try {
            val entries = withContext(Dispatchers.IO) {
                parsePdf(context, uri)
            }

            if (entries.isEmpty()) {
                return BillImporter.ImportResult(
                    errors = 1,
                    errorMessages = listOf("未识别到交易记录，请确认PDF为抖音支付账单")
                )
            }

            Log.i(TAG, "解析到 ${entries.size} 条交易")
            entries.forEach {
                Log.i(TAG, "  ${it.date} | ${it.type} | ${it.amount} | ${it.payMethod}")
            }

            val billEntries = entries.map { entry ->
                val (catName, catIcon) = classifyDouyinEntry(entry.payMethod, entry.amount)
                BillImporter.BillEntry(
                    amount = entry.amount,
                    source = Source.DOUYIN,
                    categoryName = catName,
                    categoryIcon = catIcon,
                    note = entry.payMethod.take(30).ifBlank { "Douyin" },
                    date = parseDateToMs(entry.date) ?: System.currentTimeMillis(),
                    platformNotes = "Douyin ${entry.date}"
                )
            }

            BillImporter.classifyAndImport(context, billEntries)
        } catch (e: Exception) {
            Log.e(TAG, "Douyin PDF import error", e)
            BillImporter.ImportResult(
                errors = 1,
                errorMessages = listOf("Import failed: ${e.message ?: "unknown"}")
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  核心：PdfBox 解析
    // ═══════════════════════════════════════════════════════════════

    private fun parsePdf(context: Context, uri: Uri): List<ParsedEntry> {
        // 将 URI 复制到临时文件
        val tempFile = File(context.cacheDir, "douyin_temp.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 用 PdfBox 提取完整文本
        val fullText = extractTextWithPdfBox(tempFile)
        Log.i(TAG, "PdfBox提取文本长度: ${fullText.length}")
        Log.i(TAG, "前500字符:\n${fullText.take(500)}")

        // 解析交易行
        return parseTransactions(fullText)
    }

    private fun extractTextWithPdfBox(tempFile: File): String {
        return PDDocument.load(tempFile).use { document ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.wordSeparator = " | "
            stripper.getText(document)
        }
    }

    // ─── 交易解析 ───

    private fun parseTransactions(fullText: String): List<ParsedEntry> {
        val lines = fullText.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }

        val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
        val amountRegex = Regex("\\d+\\.\\d{2}")

        val entries = mutableListOf<ParsedEntry>()
        val seen = mutableSetOf<String>()

        for (line in lines) {
            // 找日期
            val dateMatch = dateRegex.find(line) ?: continue
            val dateStr = dateMatch.value
            if (dateStr.endsWith("00:00:00")) continue

            // 找金额
            val amounts = amountRegex.findAll(line).map { it.value.toDoubleOrNull() }.filterNotNull().toList()
            if (amounts.isEmpty()) continue
            val amount = amounts.firstOrNull { it > 0 && it < 999999 } ?: continue

            // 判断类型
            val type = when {
                line.contains("退款") -> "收入"
                line.contains("收入") -> "收入"
                line.contains("支出") -> "支出"
                else -> "支出"
            }

            // 提取商户名（在日期和金额之后的文本）
            val merchant = extractMerchant(line, dateStr, amount)

            val entry = ParsedEntry(
                date = dateStr,
                amount = if (type == "收入") Math.abs(amount) else -Math.abs(amount),
                type = type,
                payMethod = merchant
            )

            // 去重
            val key = "${entry.date}|${String.format("%.2f", Math.abs(entry.amount))}"
            if (seen.add(key)) {
                entries.add(entry)
            }
        }

        return entries
    }

    private fun extractMerchant(line: String, date: String, amount: Double): String {
        // 按 | 分割列
        val columns = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }

        // 抖音账单列顺序：日期 | 收支类型 | 金额 | 付款方式 | 交易单号 | 交易对方 | 商家单号
        // 交易对方在第6列（索引5）
        if (columns.size >= 6) {
            val merchant = columns[5].trim()
            if (merchant.isNotEmpty() &&
                !merchant.contains("宗剑锋") &&
                !merchant.contains("编号") &&
                !merchant.contains("导出时间") &&
                !merchant.contains("支付交易流水") &&
                !merchant.startsWith("210") &&
                !merchant.startsWith("ONP") &&
                !merchant.startsWith("200") &&
                !merchant.startsWith("0443")
            ) {
                return merchant
            }
        }

        // 回退：按空格分割，找最可能是商户名的词
        var cleaned = line
            .replace(date, "")
            .replace(String.format("%.2f", amount), "")
            .replace(Regex("\\d{4}-\\d{2}-\\d{2}"), "")
            .replace(Regex("\\d{2}:\\d{2}:\\d{2}"), "")
            .replace("支出", "")
            .replace("收入", "")
            .replace("退款", "")
            .replace("其他", "")
            .replace("抖音支付", "")
            .replace("全部", "")
            .replace("|", " ")
            .trim()

        val words = cleaned.split(Regex("\\s+")).filter { word ->
            word.isNotEmpty() &&
            word.length >= 2 &&
            word.length <= 30 &&
            !word.matches(Regex("\\d+")) &&
            !word.startsWith("210") &&
            !word.startsWith("ONP") &&
            !word.startsWith("200") &&
            !word.startsWith("0443") &&
            !word.startsWith("2026") &&
            !word.contains("宗剑锋") &&
            !word.contains("编号") &&
            !word.contains("导出时间") &&
            !word.contains("支付交易流水") &&
            word != "抖音月付" &&
            word != "退款" &&
            !word.contains("银行") &&
            !word.contains("借记卡")
        }

        return words.firstOrNull() ?: "抖音支付"
    }

    // ─── 分类映射 ───

    private fun classifyDouyinEntry(payMethod: String, amount: Double): Pair<String, String> {
        if (amount > 0) return "退款" to "\uD83D\uDD19"
        val lower = payMethod.lowercase()
        return when {
            lower.contains("朴朴") || lower.contains("超市") || lower.contains("便利") -> "超市" to "\uD83C\uDFEA"
            lower.contains("抖音电商") || lower.contains("抖音") -> "抖音购物" to "\uD83D\uDED2"
            lower.contains("餐") || lower.contains("饭") || lower.contains("面") -> "餐饮" to "\uD83C\uDF5C"
            lower.contains("车") || lower.contains("油") || lower.contains("地铁") -> "交通" to "\uD83D\uDE97"
            else -> "抖音购物" to "\uD83D\uDED2"
        }
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
}
