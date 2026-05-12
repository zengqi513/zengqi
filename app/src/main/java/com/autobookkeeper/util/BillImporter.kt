package com.autobookkeeper.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.autobookkeeper.data.CategoryData
import com.autobookkeeper.data.Source
import com.autobookkeeper.data.Transaction
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Cell
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * 账单导入器 —— 支持微信、支付宝、京东、抖音 导出的 CSV 账单
 *
 * 各平台导出入口：
 *   - 微信：我 → 服务 → 钱包 → 账单 → 常见问题 → 下载账单
 *   - 支付宝：我的 → 账单 → 右上角··· → 开具交易流水证明（用于个人对账，CSV）
 *   - 京东：我的 → 客户服务 → 发票服务
 *   - 抖音：我 → 抖音商城 → 查看全部订单 → 下载账单
 *
 * 核心设计：不依赖固定列索引。先找表头行，通过列名匹配映射各字段位置。
 */
object BillImporter {

    private const val TAG = "BillImporter"

    /** 文件类型枚举 */
    private enum class FileType { XLSX, CSV, UNKNOWN }

    /**
     * 检测文件类型：通过文件头 Magic Number + 扩展名 + 内容嗅探三重判断
     * - xlsx: PK\x03\x04 (ZIP格式，Office Open XML)
     * - xls: \xD0\xCF\x11\xE0 (OLE2 Compound Document)
     * - csv: 非二进制，可读文本，包含逗号分隔特征
     */
    private fun detectFileType(context: Context, uri: Uri): FileType {
        // 1. 尝试通过扩展名判断（多种方式获取文件名）
        var ext = ""
        try {
            // 方式1: lastPathSegment
            val fileName = uri.lastPathSegment ?: ""
            ext = fileName.substringAfterLast('.', "").lowercase()

            // 方式2: 通过 ContentResolver 查询显示名称
            if (ext.isEmpty()) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            val displayName = cursor.getString(nameIdx) ?: ""
                            ext = displayName.substringAfterLast('.', "").lowercase()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "获取文件名扩展名失败", e)
        }

        // 2. 读取文件头进行 Magic Number 验证
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(8)
                val read = stream.read(header)
                if (read < 4) return FileType.UNKNOWN

                when {
                    // xlsx: ZIP格式 (PK\x03\x04 或 PK\x05\x06)
                    header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                            (header[2] == 0x03.toByte() || header[2] == 0x05.toByte()) &&
                            (header[3] == 0x04.toByte() || header[3] == 0x06.toByte()) -> {
                        FileType.XLSX
                    }
                    // xls: OLE2 Compound Document
                    header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() &&
                            header[2] == 0x11.toByte() && header[3] == 0xE0.toByte() -> {
                        FileType.XLSX
                    }
                    // csv/txt: 文本文件（可打印字符为主）
                    else -> {
                        // 如果扩展名明确是 csv/txt，直接判定为 CSV
                        if (ext == "csv" || ext == "txt") {
                            FileType.CSV
                        } else {
                            // 尝试读取更多内容判断是否为文本（CSV特征：逗号分隔）
                            val sample = String(header, Charsets.UTF_8)
                            val printableCount = sample.count { it.isLetterOrDigit() || it in ",;-_.\t\n\r '" }
                            if (printableCount >= read * 0.7) {
                                FileType.CSV
                            } else {
                                FileType.UNKNOWN
                            }
                        }
                    }
                }
            } ?: FileType.UNKNOWN
        } catch (e: Exception) {
            Log.e(TAG, "文件类型检测失败", e)
            // 降级：仅通过扩展名判断
            when (ext) {
                "xlsx", "xls" -> FileType.XLSX
                "csv", "txt" -> FileType.CSV
                else -> FileType.UNKNOWN
            }
        }
    }

    /** BillEntry — 解析后的单条交易数据 */
    data class BillEntry(
        val amount: Double,
        val source: Source,
        val categoryName: String,
        val categoryIcon: String,
        val note: String,
        val date: Long,
        val platformNotes: String = "",
        val orderNo: String? = null  // 订单号/交易单号（用于去重）
    )

    data class ImportResult(
        val totalLines: Int = 0,
        val imported: Int = 0,
        val skipped: Int = 0,
        val errors: Int = 0,
        val suspicious: Int = 0,  // 疑似重复（需用户确认）
        val sourceCounts: Map<Source, Int> = emptyMap(),
        val errorMessages: List<String> = emptyList()
    )

    /** 列名映射 */
    private data class ColumnMap(
        val dateIdx: Int = -1,
        val amountIdx: Int = -1,
        val typeIdx: Int = -1,       // 交易类型（微信）/ 收/支
        val counterpartyIdx: Int = -1, // 交易对方/商户
        val goodsIdx: Int = -1,       // 商品名称
        val noteIdx: Int = -1,        // 备注
        val statusIdx: Int = -1,      // 交易状态（微信）
        val orderNoIdx: Int = -1      // 交易单号/订单号（用于去重）
    )

    suspend fun importCsv(
        context: Context,
        uri: Uri,
        @Suppress("UNUSED_PARAMETER") existingCategories: List<CategoryData>
    ): ImportResult {
        val entries = mutableListOf<BillEntry>()
        var parseErrors = 0
        val errorMsgs = mutableListOf<String>()

        // 判断文件类型：先读取文件头进行 Magic Number 检测
        val fileType = detectFileType(context, uri)

        val rawLines: MutableList<String>
        val xlsxRows: MutableList<List<String>>

        try {
            when (fileType) {
                FileType.XLSX -> {
                    // Excel 格式：使用 Apache POI 解析
                    val rows = mutableListOf<List<String>>()
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        try {
                            val workbook = WorkbookFactory.create(stream)
                            val sheet = workbook.getSheetAt(0)
                            for (row in sheet) {
                                val cells = mutableListOf<String>()
                                for (cell in row) {
                                    cells.add(getCellStringValue(cell))
                                }
                                rows.add(cells)
                            }
                            workbook.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "Excel解析失败", e)
                            return ImportResult(
                                errors = 1,
                                errorMessages = listOf("Excel文件解析失败，请检查文件是否损坏: ${e.message}")
                            )
                        }
                    } ?: return ImportResult(errors = 1, errorMessages = listOf("无法打开文件"))
                    rawLines = mutableListOf()
                    xlsxRows = rows
                }

                FileType.CSV -> {
                    // CSV 格式：按文本解析
                    val lines = mutableListOf<String>()
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.buffered().use { buffered ->
                            val reader = InputStreamReader(buffered, detectCharset(buffered))
                            val br = BufferedReader(reader)
                            var line: String?
                            while (br.readLine().also { line = it } != null) {
                                lines.add(line!!)
                            }
                        }
                    } ?: return ImportResult(errors = 1, errorMessages = listOf("无法打开文件"))
                    rawLines = lines
                    xlsxRows = mutableListOf()
                }

                FileType.UNKNOWN -> {
                    return ImportResult(
                        errors = 1,
                        errorMessages = listOf("文件格式不支持，请选择 .csv 或 .xlsx 格式的账单文件")
                    )
                }
            }

            if ((fileType == FileType.XLSX && xlsxRows.isEmpty()) || (fileType == FileType.CSV && rawLines.isEmpty())) {
                return ImportResult(errors = 1, errorMessages = listOf("文件为空或未找到数据"))
            }

            // 第一步：检测平台和表头位置
            val detection = if (fileType == FileType.XLSX) {
                detectAndFindHeaderXlsx(xlsxRows)
            } else {
                detectAndFindHeader(rawLines)
            }

            if (detection.platform == "unknown" && detection.columnMap.dateIdx < 0) {
                val errMsg = if (detection.headerRaw.isNotBlank() && detection.headerRaw.contains("未找到表头")) {
                    detection.headerRaw
                } else {
                    "未找到交易数据表头，请确认是微信/支付宝/京东/抖音导出的账单文件"
                }
                return ImportResult(errors = 1, errorMessages = listOf(errMsg))
            }

            Log.i(TAG, "平台检测: ${detection.platform}, 列映射=${detection.columnMap}, 数据起始行=${detection.dataStart}")

            // 第二步：解析所有数据行
            if (fileType == FileType.XLSX) {
                for (idx in detection.dataStart until xlsxRows.size) {
                    val row = xlsxRows[idx]
                    if (row.isEmpty() || row.all { it.isBlank() }) continue

                    try {
                        val entry = parseRowXlsx(row, detection)
                        if (entry != null) {
                            val isDup = entries.any { e ->
                                abs(e.amount - entry.amount) < 0.01 &&
                                        e.source == entry.source &&
                                        abs(e.date - entry.date) < 5000
                            }
                            if (!isDup) entries.add(entry)
                        }
                    } catch (e: Exception) {
                        parseErrors++
                        if (parseErrors <= 5) {
                            errorMsgs.add("第${idx + 1}行解析失败: ${e.message}")
                        }
                        Log.v(TAG, "跳过行 #$idx: ${row.take(5)}", e)
                    }
                }
            } else {
                for (idx in detection.dataStart until rawLines.size) {
                    val rawLine = rawLines[idx]
                    if (rawLine.isBlank()) continue

                    try {
                        val entry = parseRow(rawLine, detection)
                        if (entry != null) {
                            val isDup = entries.any { e ->
                                abs(e.amount - entry.amount) < 0.01 &&
                                        e.source == entry.source &&
                                        abs(e.date - entry.date) < 5000
                            }
                            if (!isDup) entries.add(entry)
                        } else {
                            val fallbackLineLower = rawLine.lowercase()
                            if (!(fallbackLineLower.contains("不计") && (fallbackLineLower.contains("余利宝") || fallbackLineLower.contains("余额宝")))) {
                                try {
                                    val allFields = parseCsvLine(rawLine)
                                    val bruteAmount = allFields.firstNotNullOfOrNull { parseAmount(it) }
                                    val bruteDate = allFields.firstNotNullOfOrNull { parseDate(it) }
                                    if (bruteAmount != null && bruteDate != null) {
                                        entries.add(BillEntry(
                                            amount = -abs(bruteAmount),
                                            source = Source.MANUAL,
                                            categoryName = "其他",
                                            categoryIcon = "\uD83D\uDCCC",
                                            note = allFields.firstOrNull { f -> f.length in 3..40 && !f.contains("-") && !f.contains(":") && parseAmount(f) == null && parseDate(f) == null }?.take(20) ?: "",
                                            date = bruteDate
                                        ))
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        parseErrors++
                        if (parseErrors <= 5) {
                            errorMsgs.add("第${idx + 1}行解析失败: ${e.message}")
                        }
                        Log.v(TAG, "跳过行 #$idx: ${rawLine.take(60)}", e)
                    }
                }
            }

            if (entries.isEmpty() && parseErrors == 0) {
                return ImportResult(errors = 1, errorMessages = listOf("未找到有效交易记录，请检查文件格式"))
            }

            if (entries.isEmpty() && errorMsgs.isNotEmpty()) {
                return ImportResult(errors = parseErrors, errorMessages = errorMsgs)
            }

        } catch (e: Exception) {
            Log.e(TAG, "导入失败", e)
            return ImportResult(errors = parseErrors + 1, errorMessages = listOf("导入失败: ${e.message ?: "未知错误"}"))
        }

        Log.i(TAG, "解析完毕: 成功${entries.size}条, 失败${parseErrors}条")
        Log.i(TAG, "导入条目: " + entries.size + ", 来源: " + entries.groupBy { it.source }.mapValues { it.value.size })

        val result = classifyAndImport(context, entries)
        val totalLines = when {
            xlsxRows.isNotEmpty() -> xlsxRows.size
            rawLines.isNotEmpty() -> rawLines.size
            else -> 0
        }
        return result.copy(
            totalLines = totalLines,
            errors = parseErrors,
            errorMessages = if (errorMsgs.isNotEmpty()) errorMsgs else result.errorMessages
        )
    }

    // ═══════════════════════════════════════════════
    //  第一步：检测 + 建列映射
    // ═══════════════════════════════════════════════

    private data class DetectionResult(
        val platform: String,     // "wechat" / "alipay" / "jd" / "douyin" / "unknown"
        val columnMap: ColumnMap,
        val dataStart: Int,        // 第一条数据行的索引
        val headerRaw: String = "", // 原始表头行（调试用）
        val isExpensePositive: Boolean = false  // 金额正数表示支出？（默认正数表示收入）
    )

    /** 读取 Excel 单元格字符串值 */
    private fun getCellStringValue(cell: Cell): String {
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue ?: ""
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue?.time?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(it)) } ?: ""
                } else {
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                }
            }
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                try { cell.stringCellValue ?: "" }
                catch (_: Exception) {
                    try { cell.numericCellValue.toString() }
                    catch (_: Exception) { "" }
                }
            }
            else -> ""
        }
    }

    /** xlsx 表头检测 */
    private fun detectAndFindHeaderXlsx(rows: List<List<String>>): DetectionResult {
        val allText = rows.flatten().joinToString("\n").lowercase()

        val isWeChat = allText.contains("微信支付") && (
            allText.contains("交易明细") || allText.contains("账单") ||
            allText.contains("明细") || allText.contains("流水")
        )
        val isAlipayAll = allText.contains("支付宝") && !allText.contains("微信支付")

        // 收集前20行用于错误提示
        val previewLines = rows.take(20).mapIndexed { i, row ->
            "行${i + 1}: ${row.joinToString(", ").take(100)}"
        }

        // 查找表头行 —— 找包含关键列名的那一行
        // 关键字段：时间 + 金额 + (交易对方/商品/收/支/交易类型 至少一个)
        var headerRow = -1
        var headerFields = emptyList<String>()
        for ((i, row) in rows.withIndex()) {
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            val lowerFields = row.map { it.lowercase().trim() }
            val lowerJoined = lowerFields.joinToString(" ")

            // 检查是否包含时间列（最核心）
            val hasDate = lowerFields.any { it.contains("时间") || it.contains("日期") || it.contains("date") || it.contains("time") }
            // 检查是否包含金额列
            val hasAmount = lowerFields.any { it.contains("金额") || it.contains("元") || it.contains("amount") || it.contains("money") }
            // 检查是否包含交易对方/商品/类型等辅助列
            val hasBizField = lowerFields.any {
                it.contains("交易") || it.contains("对方") || it.contains("商户") ||
                it.contains("商品") || it.contains("名称") || it.contains("类型") ||
                it.contains("收/支") || it.contains("收支") || it.contains("状态")
            }

            // 表头判定：必须有时间+金额，且至少有1个业务字段
            // 放宽条件：只要有时间+金额，且该行看起来像列名（不含数字日期格式）
            if (hasDate && hasAmount && hasBizField) {
                headerRow = i
                headerFields = lowerFields
                break
            }
        }

        // 如果上面没找到，尝试更宽松的匹配（只要有时间+金额）
        if (headerRow < 0) {
            for ((i, row) in rows.withIndex()) {
                if (row.isEmpty() || row.all { it.isBlank() }) continue
                val lowerFields = row.map { it.lowercase().trim() }
                val hasDate = lowerFields.any { it.contains("时间") || it.contains("日期") }
                val hasAmount = lowerFields.any { it.contains("金额") || it.contains("元") }
                if (hasDate && hasAmount) {
                    headerRow = i
                    headerFields = lowerFields
                    break
                }
            }
        }

        if (headerRow < 0) {
            val preview = previewLines.joinToString("\n")
            return DetectionResult(
                "unknown",
                ColumnMap(),
                0,
                "未找到表头。文件前20行内容：\n$preview"
            )
        }

        val cm = buildColumnMap(headerFields)
        val dataStart = headerRow + 1

        val platform = when {
            isWeChat -> "wechat"
            isAlipayAll -> "alipay"
            else -> {
                if (headerFields.any { it.contains("交易类型") || it.contains("交易对方") }) "wechat"
                else if (headerFields.any { it.contains("交易号") || it.contains("商品名称") }) "alipay"
                else "unknown"
            }
        }

        val hasIncomeExpenseCol = headerFields.any { it.contains("收/支") || it.contains("收入/支出") || it.contains("收支") }
        val isExpensePositive = !hasIncomeExpenseCol && platform == "alipay"

        return DetectionResult(
            platform = platform,
            columnMap = cm,
            dataStart = dataStart,
            headerRaw = headerFields.joinToString(","),
            isExpensePositive = isExpensePositive
        )
    }

    private fun detectAndFindHeader(lines: List<String>): DetectionResult {
        val allText = lines.joinToString("\n").lowercase()

        val isWeChat = allText.contains("微信支付") && (
            allText.contains("交易明细") || allText.contains("账单") ||
            allText.contains("明细") || allText.contains("流水")
        )
        val isAlipayAll = allText.contains("支付宝") && !allText.contains("微信支付")

        // 收集前20行用于错误提示
        val previewLines = lines.take(20).mapIndexed { i, line ->
            "行${i + 1}: ${line.take(120)}"
        }

        // 查找表头行 —— 找包含关键列名的那一行
        var headerRow = -1
        var headerLine = ""
        for ((i, line) in lines.withIndex()) {
            if (line.isBlank()) continue
            val lower = line.lowercase()

            // 解析CSV字段进行更精确的匹配
            val fields = try { parseCsvLine(line).map { it.lowercase().trim() } } catch (_: Exception) { emptyList() }

            // 检查关键列
            val hasDate = fields.any { it.contains("时间") || it.contains("日期") || it.contains("date") || it.contains("time") }
            val hasAmount = fields.any { it.contains("金额") || it.contains("元") || it.contains("amount") || it.contains("money") }
            val hasBizField = fields.any {
                it.contains("交易") || it.contains("对方") || it.contains("商户") ||
                it.contains("商品") || it.contains("名称") || it.contains("类型") ||
                it.contains("收/支") || it.contains("收支") || it.contains("状态")
            }

            // 表头判定：必须有时间+金额+至少一个业务字段
            if (hasDate && hasAmount && hasBizField) {
                headerRow = i
                headerLine = line
                break
            }
        }

        // 如果上面没找到，尝试更宽松的匹配
        if (headerRow < 0) {
            for ((i, line) in lines.withIndex()) {
                if (line.isBlank()) continue
                val fields = try { parseCsvLine(line).map { it.lowercase().trim() } } catch (_: Exception) { emptyList() }
                val hasDate = fields.any { it.contains("时间") || it.contains("日期") }
                val hasAmount = fields.any { it.contains("金额") || it.contains("元") }
                if (hasDate && hasAmount) {
                    headerRow = i
                    headerLine = line
                    break
                }
            }
        }

        if (headerRow < 0) {
            val preview = previewLines.joinToString("\n")
            return DetectionResult(
                "unknown",
                ColumnMap(),
                0,
                "未找到表头。文件前20行内容：\n$preview"
            )
        }

        val fields = parseCsvLine(headerLine).map { it.lowercase().trim() }
        val cm = buildColumnMap(fields)

        val dataStart = headerRow + 1

        // 判断平台
        val platform = when {
            isWeChat -> "wechat"
            isAlipayAll -> "alipay"
            else -> {
                if (fields.any { it.contains("交易类型") || it.contains("交易对方") }) "wechat"
                else if (fields.any { it.contains("交易号") || it.contains("商品名称") }) "alipay"
                else "unknown"
            }
        }

        // 支出/收入方向判断
        val hasIncomeExpenseCol = fields.any { it.contains("收/支") || it.contains("收入/支出") || it.contains("收支") }
        val isExpensePositive = !hasIncomeExpenseCol && platform == "alipay"

        return DetectionResult(
            platform = platform,
            columnMap = cm,
            dataStart = dataStart,
            headerRaw = headerLine,
            isExpensePositive = isExpensePositive
        )
    }

    /** 基于列名构建字段索引映射 */
    private fun buildColumnMap(fields: List<String>): ColumnMap {
        var dateIdx = -1
        var amountIdx = -1
        var typeIdx = -1
        var counterpartyIdx = -1
        var goodsIdx = -1
        var noteIdx = -1
        var statusIdx = -1
        var orderNoIdx = -1

        // 统一匹配：精确+模糊合并，优先级从高到低
        for ((i, f) in fields.withIndex()) {
            when {
                // 时间列（最高优先级）
                f == "交易时间" || f == "日期" || f == "time" || f == "date" -> dateIdx = i
                // 金额列
                f == "金额" || f == "amount" || f == "money" ||
                f == "总价" || f == "价钱" || f == "金额(元)" || f == "交易金额" || f == "收支金额" -> amountIdx = i
                // 收/支列（优先于交易类型）
                f == "收/支" || f == "收入/支出" || f == "收付款" || f == "收支类型" -> typeIdx = i
                // 交易对方列（京东用"商户名称"）
                f == "交易对方" || f == "收款方" || f == "付款方" || f == "对方" ||
                f == "商户名称" || f == "商户" -> counterpartyIdx = i
                // 商品列（京东用"交易说明"）
                f == "商品名称" || f == "商品说明" || f == "商品" ||
                f == "名称" || f == "描述" || f == "商品/服务" ||
                f == "交易说明" || f == "说明" -> goodsIdx = i
                // 备注列
                f == "备注" || f == "note" -> noteIdx = i
                // 状态列
                f == "状态" || f == "交易状态" || f == "当前状态" || f == "status" -> statusIdx = i
                // 订单号列（京东用"交易订单号"）
                f == "交易单号" || f == "订单号" || f == "交易订单号" || f == "订单编号" ||
                f == "流水号" || f == "交易号" -> orderNoIdx = i
            }
        }

        // 第二遍：模糊匹配（只补缺，不覆盖第一遍的结果）
        if (dateIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("时间") || f.contains("日期") || f.contains("date") || f.contains("time")) {
                    dateIdx = i
                    break
                }
            }
        }
        if (amountIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("金额") || f.contains("amount") || f.contains("money") ||
                    f.contains("总价") || f.contains("价钱")) {
                    amountIdx = i
                    break
                }
            }
        }
        if (typeIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("收/支") || f.contains("收入/支出") || f.contains("收付款") || f.contains("收支")) {
                    typeIdx = i
                    break
                } else if (f.contains("交易类型") || f.contains("类型") || f == "type") {
                    typeIdx = i
                }
            }
        }
        if (counterpartyIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("交易对方") || f.contains("收款方") || f.contains("付款方") ||
                    f.contains("对方") || f.contains("商户") || f.contains("交易人")) {
                    counterpartyIdx = i
                    break
                }
            }
        }
        if (goodsIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("商品") || f.contains("名称") || f.contains("描述") ||
                    f.contains("说明") || f.contains("服务")) {
                    goodsIdx = i
                    break
                }
            }
        }
        if (statusIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("状态") || f.contains("status")) {
                    statusIdx = i
                    break
                }
            }
        }
        if (noteIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("备注") || f.contains("note")) {
                    noteIdx = i
                    break
                }
            }
        }

        // 订单号/交易单号列（如果前面精确匹配未找到，进行模糊匹配）
        if (orderNoIdx < 0) {
            for ((i, f) in fields.withIndex()) {
                if (f.contains("单号") || f.contains("交易号") || f.contains("流水") ||
                    f.contains("订单") || f.contains("orderno") || f.contains("trade")) {
                    orderNoIdx = i
                    break
                }
            }
        }

        return ColumnMap(dateIdx, amountIdx, typeIdx, counterpartyIdx, goodsIdx, noteIdx, statusIdx, orderNoIdx)
    }

    // ═══════════════════════════════════════════════
    //  第二步：解析单行
    // ═══════════════════════════════════════════════

    private fun parseRow(line: String, detection: DetectionResult): BillEntry? {
        val fields = parseCsvLine(line)
        val cm = detection.columnMap

        if (fields.isEmpty()) {
            Log.v(TAG, "[parseRow] Empty fields")
            return null
        }

        // 1) 解析金额
        val amountRaw = if (cm.amountIdx >= 0 && cm.amountIdx < fields.size) {
            fields[cm.amountIdx]
        } else {
            fields.firstOrNull { parseAmount(it) != null } ?: run {
                Log.v(TAG, "[parseRow] No amount found in: ${line.take(80)}")
                return null
            }
        }

        val amountVal = parseAmount(amountRaw) ?: run {
            Log.v(TAG, "[parseRow] Amount parse failed: '$amountRaw' in: ${line.take(80)}")
            return null
        }

        // 2) 解析日期
        val dateRaw = if (cm.dateIdx >= 0 && cm.dateIdx < fields.size) {
            fields[cm.dateIdx]
        } else ""
        var dateVal = parseDate(dateRaw)

        if (dateVal == null) {
            for (f in fields) {
                val d = parseDate(f)
                if (d != null) {
                    dateVal = d
                    break
                }
            }
        }
        if (dateVal == null) {
            Log.v(TAG, "[parseRow] Date parse failed: dateIdx=${cm.dateIdx}, dateRaw='$dateRaw' in: ${line.take(80)}")
            return null
        }

        // 3) 判断收支方向
        // 先把 status 读出来（后面会根据 status 做不计收支的覆盖判断）
        val status = if (cm.statusIdx >= 0 && cm.statusIdx < fields.size) {
            fields[cm.statusIdx]
        } else ""

        // 微信：排除明确失败的交易（退款成功/已退款不排除，由后续逻辑处理）
        if (detection.platform == "wechat" && status.isNotBlank()) {
            val s = status.lowercase()
            if (s.contains("失败") || s.contains("取消") || s.contains("已关闭")) {
                Log.v(TAG, "[parseRow] Skipped failed txn: status='$status'")
                return null
            }
        }

        // 支付宝/微信：不计收支且商品含余额宝/余利宝 → 跳过
        // 暴力扫描：不计收支 + 余额宝/余利宝 → 跳过（不依赖列名映射）
        val lineLower = line.lowercase()
        if (lineLower.contains("不计") && (lineLower.contains("余利宝") || lineLower.contains("余额宝"))) {
            Log.v(TAG, "[parseRow] Skipped 余额宝/余利宝 不计收支: $line")
            return null
        }

        val isExpense = if (detection.isExpensePositive) {
            // 支付宝：金额列带符号，负数即为支出
            amountVal < 0
        } else {
            if (cm.typeIdx >= 0 && cm.typeIdx < fields.size) {
                val t = fields[cm.typeIdx].lowercase()
                val s = status.lowercase()
                // 支付宝/微信「收/支」列：
                // - 不计收支+退款成功→false(收入)
                // - 不计收支+还款成功→true(支出)
                // - 其他不计收支/中性→跳过
                when {
                    t.contains("不计") && s.contains("退款") -> false
                    t.contains("不计") && s.contains("还款") -> true
                    t.contains("不计") || t.contains("中性") -> {
                        // 不计收支但商品说明含转账到银行卡→作为支出导入
                        val g = if (cm.goodsIdx >= 0 && cm.goodsIdx < fields.size) fields[cm.goodsIdx] else ""
                        if (g.contains("转账到银行卡") || g.contains("转到银行卡")) {
                            true
                        } else if (g.contains("余利宝") || g.contains("余额宝")) {
                            // 支付宝转入到余利宝/余额宝自动转入 -> 跳过
                            return@parseRow null
                        } else {
                            return@parseRow null
                        }
                    }
                    t.contains("收入") || t.contains("收款") || t.contains("到账") -> false
                    t.contains("支") || t.contains("付") || t.contains("消费") -> true
                    else -> true
                }
            } else {
                amountVal < 0
            }
        }
        val signedAmount = if (isExpense) -abs(amountVal) else abs(amountVal)

        // 4) 交易对方/商品名
        val counterparty = if (cm.counterpartyIdx >= 0 && cm.counterpartyIdx < fields.size) {
            fields[cm.counterpartyIdx]
        } else ""
        val goodsName = if (cm.goodsIdx >= 0 && cm.goodsIdx < fields.size) {
            fields[cm.goodsIdx]
        } else ""
        val notes = if (cm.noteIdx >= 0 && cm.noteIdx < fields.size) {
            fields[cm.noteIdx]
        } else ""

        // 5) 交易类型（微信）
        val txnType = if (cm.typeIdx >= 0 && cm.typeIdx < fields.size) {
            fields[cm.typeIdx]
        } else ""

        // 6) 推断来源和分类
        val source = inferSourceFromBill(counterparty, goodsName, txnType)

        // ── 根据交易状态/商品说明强制覆盖分类 ──
        val forcedCategory = when {
            // 退款成功
            !isExpense && status.lowercase().contains("退款") -> "退款" to "🔙"
            // 还款成功
            isExpense && status.lowercase().contains("还款") -> "还款" to "💳"
            // 支付宝转入到余利宝（支出）→ 转账
            isExpense && goodsName.contains("余利宝") -> "转账" to "💸"
            // 转账到银行卡
            goodsName.lowercase().contains("转账到银行卡") || 
            goodsName.contains("转到银行卡") ||
            counterparty.lowercase().contains("转账到银行卡") -> "转账" to "💸"
            else -> null
        }
        val finalCatName: String
        val finalCatIcon: String
        if (forcedCategory != null) {
            finalCatName = forcedCategory.first
            finalCatIcon = forcedCategory.second
        } else {
            val cat = classifyByText(
                text = "$counterparty $goodsName $notes",
                source = source,
                isExpense = isExpense,
                counterparty = counterparty,
                goodsName = goodsName,
                txnType = txnType
            )
            finalCatName = cat.first
            finalCatIcon = cat.second
        }

        // 备注 — 商品说明完整明细+交易对方
        val note = listOfNotNull(
            goodsName.ifBlank { null },
            counterparty.take(20).ifBlank { null }
        ).filter { it.isNotBlank() }.joinToString(" - ").take(200)

        val platformNote = if (notes.isNotBlank()) notes.take(100) else
            if (txnType.isNotBlank() && detection.platform == "wechat") txnType.take(100) else ""

        // 提取订单号（用于去重）
        val orderNo = if (cm.orderNoIdx >= 0 && cm.orderNoIdx < fields.size) {
            fields[cm.orderNoIdx].trim()
        } else null

        return BillEntry(
            amount = signedAmount,
            source = source,
            categoryName = finalCatName,
            categoryIcon = finalCatIcon,
            note = note,
            date = dateVal,
            platformNotes = platformNote,
            orderNo = orderNo?.takeIf { it.length >= 4 }  // 只保留有效订单号
        )
    }

    /** xlsx 行解析（复用 parseRow 的核心逻辑） */
    private fun parseRowXlsx(row: List<String>, detection: DetectionResult): BillEntry? {
        val cm = detection.columnMap
        if (row.isEmpty()) {
            Log.v(TAG, "[parseRowXlsx] Empty row")
            return null
        }

        // 1) 解析金额
        val amountRaw = if (cm.amountIdx >= 0 && cm.amountIdx < row.size) {
            row[cm.amountIdx]
        } else {
            row.firstOrNull { parseAmount(it) != null } ?: run {
                Log.v(TAG, "[parseRowXlsx] No amount found in: ${row.take(5)}")
                return null
            }
        }

        val amountVal = parseAmount(amountRaw) ?: run {
            Log.v(TAG, "[parseRowXlsx] Amount parse failed: '$amountRaw' in: ${row.take(5)}")
            return null
        }

        // 2) 解析日期
        val dateRaw = if (cm.dateIdx >= 0 && cm.dateIdx < row.size) {
            row[cm.dateIdx]
        } else ""
        var dateVal = parseDate(dateRaw)

        if (dateVal == null) {
            for (f in row) {
                val d = parseDate(f)
                if (d != null) {
                    dateVal = d
                    break
                }
            }
        }
        if (dateVal == null) {
            Log.v(TAG, "[parseRowXlsx] Date parse failed: dateIdx=${cm.dateIdx}, dateRaw='$dateRaw' in: ${row.take(5)}")
            return null
        }

        // 3) 判断收支方向
        val status = if (cm.statusIdx >= 0 && cm.statusIdx < row.size) {
            row[cm.statusIdx]
        } else ""

        // 微信：排除明确失败的交易
        if (detection.platform == "wechat" && status.isNotBlank()) {
            val s = status.lowercase()
            if (s.contains("失败") || s.contains("取消") || s.contains("已关闭") || s.contains("交易关闭")) {
                Log.v(TAG, "[parseRowXlsx] Skipped failed txn: status='$status'")
                return null
            }
        }


        // 中性/不计收支跳过（但退款类保留为收入）
        val typeCol = if (cm.typeIdx >= 0 && cm.typeIdx < row.size) row[cm.typeIdx].lowercase() else ""
        if (typeCol == "/" || (typeCol.contains("中性") && typeCol.contains("不计"))) {
            val s = status.lowercase()
            if (!s.contains("退款") && !s.contains("还款")) {
                Log.v(TAG, "[parseRowXlsx] Skipped neutral txn: type='$typeCol', status='$status'")
                return null
            }
        }

        // 余额宝/余利宝不计收支跳过
        val rowLower = row.joinToString(" ").lowercase()
        if (rowLower.contains("不计") && (rowLower.contains("余利宝") || rowLower.contains("余额宝"))) {
            Log.v(TAG, "[parseRowXlsx] Skipped 余额宝/余利宝 不计收支")
            return null
        }

        val isExpense = if (detection.isExpensePositive) {
            amountVal < 0
        } else {
            if (cm.typeIdx >= 0 && cm.typeIdx < row.size) {
                val t = row[cm.typeIdx].lowercase()
                val s = status.lowercase()
                when {
                    t.contains("不计") && s.contains("退款") -> false
                    t.contains("不计") && s.contains("还款") -> true
                    t.contains("不计") || t.contains("中性") -> {
                        val g = if (cm.goodsIdx >= 0 && cm.goodsIdx < row.size) row[cm.goodsIdx] else ""
                        if (g.contains("转账到银行卡") || g.contains("转到银行卡")) {
                            true
                        } else if (g.contains("余利宝") || g.contains("余额宝")) {
                            return@parseRowXlsx null
                        } else {
                            return@parseRowXlsx null
                        }
                    }
                    t.contains("收入") || t.contains("收款") || t.contains("到账") -> false
                    t.contains("支") || t.contains("付") || t.contains("消费") -> true
                    else -> true
                }
            } else {
                amountVal < 0
            }
        }
        val signedAmount = if (isExpense) -abs(amountVal) else abs(amountVal)

        // 4) 交易对方/商品名
        val counterparty = if (cm.counterpartyIdx >= 0 && cm.counterpartyIdx < row.size) {
            row[cm.counterpartyIdx]
        } else ""
        val goodsName = if (cm.goodsIdx >= 0 && cm.goodsIdx < row.size) {
            row[cm.goodsIdx]
        } else ""
        val notes = if (cm.noteIdx >= 0 && cm.noteIdx < row.size) {
            row[cm.noteIdx]
        } else ""

        // 5) 交易类型（微信）
        val txnType = if (cm.typeIdx >= 0 && cm.typeIdx < row.size) {
            row[cm.typeIdx]
        } else ""

        // 6) 推断来源和分类
        val source = inferSourceFromBill(counterparty, goodsName, txnType)

        // ── 根据交易状态/商品说明强制覆盖分类 ──
        val forcedCategory = when {
            !isExpense && status.lowercase().contains("退款") -> "退款" to "🔙"
            isExpense && status.lowercase().contains("还款") -> "还款" to "💳"
            isExpense && goodsName.contains("余利宝") -> "转账" to "💸"
            goodsName.lowercase().contains("转账到银行卡") ||
            goodsName.contains("转到银行卡") ||
            counterparty.lowercase().contains("转账到银行卡") -> "转账" to "💸"
            else -> null
        }
        val finalCatName: String
        val finalCatIcon: String
        if (forcedCategory != null) {
            finalCatName = forcedCategory.first
            finalCatIcon = forcedCategory.second
        } else {
            val cat = classifyByText(
                text = "$counterparty $goodsName $notes",
                source = source,
                isExpense = isExpense,
                counterparty = counterparty,
                goodsName = goodsName,
                txnType = txnType
            )
            finalCatName = cat.first
            finalCatIcon = cat.second
        }

        val note = listOfNotNull(
            goodsName.ifBlank { null },
            counterparty.take(20).ifBlank { null }
        ).filter { it.isNotBlank() }.joinToString(" - ").take(200)

        val platformNote = if (notes.isNotBlank()) notes.take(100) else
            if (txnType.isNotBlank() && detection.platform == "wechat") txnType.take(100) else ""

        // 提取订单号（用于去重）
        val orderNo = if (cm.orderNoIdx >= 0 && cm.orderNoIdx < row.size) {
            row[cm.orderNoIdx].trim()
        } else null

        return BillEntry(
            amount = signedAmount,
            source = source,
            categoryName = finalCatName,
            categoryIcon = finalCatIcon,
            note = note,
            date = dateVal,
            platformNotes = platformNote,
            orderNo = orderNo?.takeIf { it.length >= 4 }  // 只保留有效订单号
        )
    }

    // ═══════════════════════════════════════════════
    //  工具函数
    // ═══════════════════════════════════════════════

    /** 智能金额解析：支持多种格式 */
    private fun parseAmount(str: String): Double? {
        if (str.isBlank()) return null
        val cleaned = str.trim()
            .replace("¥", "").replace("￥", "").replace(",", "")
            .replace("+", "").replace(" ", "").replace("\u00A0", "")
            .replace("（", "(").replace("）", ")").trim()
        // 去掉结尾的非数字（如"元"）
        val numeric = cleaned.replace(Regex("[^\\d.\\-]$"), "")
            .replace(Regex("[^\\d.\\-]$"), "")   // 再清一遍
        return numeric.toDoubleOrNull()?.let { value ->
            val absVal = abs(value)
            if (absVal in 0.01..9_999_999.0) value else null
        }
    }

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA),
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA),
        SimpleDateFormat("yyyy/MM/dd", Locale.CHINA),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA),
        SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    )

    private fun parseDate(str: String): Long? {
        val cleaned = str.trim().replace("T", " ").replace("'", "")
        for (fmt in dateFormats) {
            try {
                return fmt.parse(cleaned)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    /** CSV 行解析（支持引号包裹的字段） */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && !inQuotes -> inQuotes = true
                ch == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        fields.add(current.toString().trim())
        return fields
    }

    // ═══════════════════════════════════════════════
    //  来源 & 分类推断
    // ═══════════════════════════════════════════════

    private fun inferSourceFromBill(counterparty: String, goodsName: String, txnType: String): Source {
        val full = "$counterparty $goodsName $txnType".lowercase()

        // 先匹配具体商户/品牌（必须精确一些的字段，避免误匹配）
        if (full.contains("拼多多") || full.contains("pdd")) return Source.PDD
        if (full.contains("淘宝") || full.contains("天猫") || full.contains("taobao")) return Source.TAOBAO
        if (full.contains("京东") || full.contains("jd.")) return Source.JD
        if (full.contains("美团") || full.contains("饿了么") || full.contains("meituan")) return Source.MEITUAN
        if (full.contains("抖音") || full.contains("douyin")) return Source.DOUYIN
        if (full.contains("快手") || full.contains("kuaishou")) return Source.KUAISHOU
        if (full.contains("叮咚") || full.contains("yaya.zone")) return Source.DINGDONG
        if (full.contains("朴朴")) return Source.PUPU
        if (full.contains("工商银行") || full.contains("icbc")) return Source.BANK_ICBC
        if (full.contains("建设银行") || full.contains("ccb")) return Source.BANK_CCB
        if (full.contains("招商银行") || full.contains("cmb")) return Source.BANK_CMB

        // 再检测具体超市/药店等（通过这些可以推断来源，但又不能直接返回 MANUAL）
        if (full.contains("惠顺多") || full.contains("永旺") || full.contains("华润万家") ||
            full.contains("沃尔玛") || full.contains("大润发") || full.contains("百佳") ||
            full.contains("盒马") || full.contains("山姆") || full.contains("钱大妈")) return Source.MANUAL

        // 平台名放最后兜底（支付宝/微信等可能只是对方列签名，不应覆盖具体商户）
        // 只有确实没匹配到任何具体商户时才回退到平台源
        if (full.contains("支付宝") || full.contains("alipay")) return Source.ALIPAY
        if (full.contains("微信支付") || full.contains("微信")) return Source.WECHAT

        return Source.MANUAL
    }

    /** 从交易文本中推断 API/云服务来源 */
    private fun inferApiSource(counterparty: String, goodsName: String, txnType: String): String {
        val full = "$counterparty $goodsName $txnType".lowercase()
        if (full.contains("深度求索") || full.contains("deepseek")) return "API"
        if (full.contains("阿里云") || full.contains("aliyun")) return "API"
        if (full.contains("腾讯云") || full.contains("华为云") || full.contains("azure")) return "API"
        if (full.contains("api服务") || full.contains("云服务") || full.contains("云服务器")) return "API"
        if (full.contains("服务器") || full.contains("vps") || full.contains("域名") || full.contains("cdn") || full.contains("oss")) return "API"
        return ""
    }

    private data class CategoryRule(
        val keywords: List<String>,
        val catName: String,
        val catIcon: String,
        val isExpense: Boolean = true
    )

    private val categoryRules = listOf(
        // ═══ 食 ═══
        CategoryRule(listOf("外卖","吃饭","午餐","晚餐","早餐","早饭","午饭","晚饭","快餐","食堂","餐厅","饭店","美食","小吃","奶茶","咖啡","星巴克","瑞幸","肯德基","麦当劳","饿了么","美团外卖"), "餐饮", "🍜"),
        CategoryRule(listOf("面包","蛋糕","甜品","冰淇淋","巧克力","薯片"), "零食", "🍪"),
        CategoryRule(listOf("水果","香蕉","苹果","橙子","葡萄","西瓜","草莓","芒果","榴莲","梨","桃","荔枝"), "餐饮", "🍎"),
        CategoryRule(listOf("零食","饮料","牛奶","酸奶","饼干"), "零食", "🍪"),
        CategoryRule(listOf("包子","豆浆","油条","粥","肠粉","饺子","馄饨","面条","米粉","米线"), "餐饮", "🍜"),
        CategoryRule(listOf("火锅","烧烤","麻辣烫","串串","麻辣香锅","冒菜"), "餐饮", "🍜"),
        CategoryRule(listOf("生鲜","买菜"), "购物", "🏪"),
        CategoryRule(listOf("超市","盒马","山姆","沃尔玛","家乐福","大润发","永旺","华润万家","钱大妈","肉联帮","便利店","7-11","美宜佳"), "购物", "🏪"),
        // ═══ 衣 ═══
        CategoryRule(listOf("衣服","上衣","裤子","裙子","外套","卫衣","毛衣","羽绒服","衬衫"), "服饰", "👔"),
        CategoryRule(listOf("鞋","运动鞋","皮鞋","靴子","球鞋"), "服饰", "👔"),
        CategoryRule(listOf("包包","书包","背包"), "服饰", "👔"),
        // ═══ 住 ═══
        CategoryRule(listOf("房租","水费","电费","燃气","煤气","物业","暖气"), "住房", "🏠"),
        // 新增：物业费、燃气费
        CategoryRule(listOf("物业费","管理费"), "住房", "🏢"),
        CategoryRule(listOf("燃气费","天然气"), "住房", "🔥"),
        CategoryRule(listOf("宽带","光纤"), "通信", "📡"),
        // ═══ 行 ═══
        CategoryRule(listOf("公交","地铁","打车","滴滴","出租车","网约车","顺风车","高德"), "交通", "🚗"),
        CategoryRule(listOf("加油","加油站","充电","充电桩","停车费","高速","ETC"), "交通", "🚗"),
        CategoryRule(listOf("高铁","火车","动车","长途","大巴","客运"), "交通", "🚄"),
        CategoryRule(listOf("机票","飞机","航班"), "交通", "✈️"),
        // ═══ 玩 ═══
        CategoryRule(listOf("电影","影院","演出","演唱会","KTV"), "消费", "🛒"),
        CategoryRule(listOf("游戏","steam","点卡"), "消费", "🛒"),
        CategoryRule(listOf("会员","vip","订阅"), "消费", "🛒"),
        CategoryRule(listOf("旅游","旅行","酒店","民宿","门票","景区","度假"), "消费", "🛒"),
        CategoryRule(listOf("健身","运动","瑜伽","游泳","跑步"), "消费", "🛒"),
        CategoryRule(listOf("学习","教育","书籍","文具","培训","课程"), "消费", "🛒"),
        // ═══ 日用/美容/数码 ═══
        CategoryRule(listOf("日用","日用品","洗衣","洗护","纸巾","垃圾袋","牙刷","牙膏","洗发水","沐浴露"), "日用", "🧴"),
        CategoryRule(listOf("化妆品","护肤","面膜","口红","美容","美发","理发","剪发"), "美容", "💄"),
        // 数码从购物子类移除，改为映射到通信-API或直接到通信
        CategoryRule(listOf("手机","电脑","平板","耳机","充电器","数据线","充电宝","数码","电子","显示器","鼠标","键盘"), "购物", "📱"),
        // ═══ 医疗 ═══
        CategoryRule(listOf("医院","药店","诊所","看病","买药","体检","医疗","药房","处方药","口罩"), "医疗", "🏥"),
        // ═══ 通信 ═══
        // 运营商自动分类到话费充值
        CategoryRule(listOf("广东联通","中国移动","中国电信","话费","流量包","联通","移动","电信","交费","手机充值"), "通信", "📡"),
        // API类统一分配
        CategoryRule(listOf("API","api","deepseek","阿里云","腾讯云","华为云","azure","aws","cloudflare","api服务","服务器","云服务","vps","域名","主机","cdn","oss"), "通信", "🖥️"),
        // ═══ 金融 ═══
        CategoryRule(listOf("花呗"), "金融", "花呗"),
        CategoryRule(listOf("借呗"), "金融", "借呗"),
        CategoryRule(listOf("微粒贷"), "金融", "微粒贷"),
        CategoryRule(listOf("网商贷"), "金融", "网商贷"),
        CategoryRule(listOf("放心借"), "金融", "放心借"),
        // ═══ 电商 ═══
        CategoryRule(listOf("淘宝","天猫"), "购物", "🛒"),
        CategoryRule(listOf("京东"), "购物", "📱"),
        CategoryRule(listOf("拼多多"), "购物", "📦"),
        // ═══ 资金 ═══
        CategoryRule(listOf("红包","工资","转账","理财","收入"), "金融", "💸", false),
        CategoryRule(listOf("退款","退费","返利"), "退款", "🔙", false),
        // ═══ 其他 ═══
        CategoryRule(listOf("快递","邮政","打印"), "其他", "📌"),
        CategoryRule(listOf("厨具","炒锅","铁锅"), "厨具", "🍳"),
        CategoryRule(listOf("签证","证件","手续费","服务费"), "其他", "📌"),
        CategoryRule(listOf("住房公积金","社保","医保","保险"), "其他", "📌"),
        CategoryRule(listOf("捐款","捐赠","公益"), "其他", "📌")
    )
        private fun classifyByText(
        text: String,
        source: Source,
        isExpense: Boolean,
        counterparty: String = "",
        goodsName: String = "",
        txnType: String = ""
    ): Pair<String, String> {
        val lower = text.lowercase()
        // 非支出时先匹配退款/返利等关键词，不匹配则用来源名
        if (!isExpense) {
            for (rule in categoryRules) {
                if (!rule.isExpense && rule.keywords.any { lower.contains(it) }) {
                    return rule.catName to rule.catIcon
                }
            }
            val srcName = when (source) {
                Source.ALIPAY -> "支付宝"
                Source.WECHAT -> "微信支付"
                Source.PDD -> "拼多多"
                Source.JD -> "京东"
                Source.TAOBAO -> "淘宝"
                Source.DOUYIN -> "抖音"
                Source.KUAISHOU -> "快手"
                Source.MEITUAN -> "美团"
                Source.DINGDONG -> "叮咚"
                Source.PUPU -> "朴朴"
                Source.BANK_ICBC -> "工商银行"
                Source.BANK_CCB -> "建设银行"
                Source.BANK_CMB -> "招商银行"
                else -> "其他"
            }
            return srcName to "💰"
        }
        // API 类服务优先检测（阿里云、深度求索等）
        val apiSrc = inferApiSource(counterparty, goodsName, txnType)
        if (apiSrc.isNotBlank()) {
            return apiSrc to "💻"
        }

        for (rule in categoryRules) {
            if (rule.keywords.any { lower.contains(it) }) {
                return rule.catName to rule.catIcon
            }
        }

        // txnType 兜底：关键词没匹配到时，检查交易分类
        if (txnType.isNotBlank()) {
            val txnLower = txnType.lowercase()
            for (rule in categoryRules) {
                if (rule.keywords.any { txnLower.contains(it) }) {
                    return rule.catName to rule.catIcon
                }
            }
        }

        return when (source) {
            Source.WECHAT -> "微信支付" to "💳"
            Source.ALIPAY -> "支付宝" to "💳"
            Source.TAOBAO -> "购物" to "🛒"
            Source.PDD -> "购物" to "📦"
            Source.JD -> "购物" to "📱"
            Source.DOUYIN -> "购物" to "🎵"
            Source.KUAISHOU -> "购物" to "📺"
            Source.MEITUAN -> "餐饮" to "🍔"
            Source.DINGDONG -> "购物" to "🛒"
            Source.PUPU -> "购物" to "🛒"
            // 完全没匹配到任何信息 → 用对方名/商品名第一段作为分类
            Source.MANUAL -> {
                val fallback = text.split(" ", ",", "，", "、").firstOrNull {
                    it.isNotBlank() && it.length >= 2 && !it.contains("支付宝") && !it.contains("微信")
                }?.take(6) ?: "其他"
                fallback to "📌"
            }
            Source.BANK_ICBC, Source.BANK_CCB, Source.BANK_CMB,
            Source.BANK_BOCOM, Source.BANK_ABC, Source.BOC -> "转账" to "💸"
            else -> "其他" to "📌"
        }
    }

    // ═══════════════════════════════════════════════
    //  批量导入数据库
    // ═══════════════════════════════════════════════

    /** 批量导入数据库（公开给其他解析器调用，如抖音PDF解析器） */
    internal suspend fun classifyAndImport(
        context: Context,
        entries: List<BillEntry>
    ): ImportResult {
        val app = context.applicationContext as com.autobookkeeper.App
        val db = app.database
        val dao = db.transactionDao()

        var imported = 0
        var skipped = 0
        var suspicious = 0
        val sourceCounts = mutableMapOf<Source, Int>()

        // 构建 Transaction 对象列表
        val transactions = entries.mapNotNull { entry ->
            Transaction(
                amount = entry.amount,
                categoryName = entry.categoryName,
                categoryIcon = entry.categoryIcon,
                source = entry.source,
                note = entry.note.take(100),
                rawNotification = "[导入账单] ${entry.platformNotes}".take(500),
                date = entry.date,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                orderNo = entry.orderNo
            )
        }

        // 使用 DuplicateFilter 进行批量去重
        val dedupResult = DuplicateFilter.batchDeduplicate(dao, transactions)
        
        // 处理保留的记录
        val keptRecords = dedupResult.kept
        if (keptRecords.isNotEmpty()) {
            // 标准化记录（生成指纹等）
            val normalizedRecords = keptRecords.map { 
                DuplicateFilter.normalizeTransaction(it) 
            }
            
            try {
                dao.insertAll(normalizedRecords)
                imported += normalizedRecords.size
                
                // 统计来源
                normalizedRecords.forEach { tx ->
                    sourceCounts[tx.source] = (sourceCounts[tx.source] ?: 0) + 1
                }
            } catch (e: Exception) {
                Log.e(TAG, "批量插入失败", e)
                // 降级：逐条插入
                for (txn in normalizedRecords) {
                    try { 
                        dao.insert(txn)
                        imported++
                        sourceCounts[txn.source] = (sourceCounts[txn.source] ?: 0) + 1
                    } catch (_: Exception) { 
                        skipped++ 
                    }
                }
            }
        }
        
        // 统计被过滤的记录
        dedupResult.filtered.forEach { filtered ->
            if (filtered.autoDiscarded) {
                skipped++
            } else if (filtered.result.isSuspicious) {
                suspicious++
            }
        }

        Log.d(TAG, "导入完成: 导入=$imported, 跳过=$skipped, 疑似重复=$suspicious")
        return ImportResult(
            imported = imported, 
            skipped = skipped, 
            suspicious = suspicious,
            sourceCounts = sourceCounts.toMap()
        )
    }

    private fun detectCharset(stream: java.io.InputStream): String {
        return try {
            stream.mark(4)
            val b0 = stream.read()
            val b1 = stream.read()
            val b2 = stream.read()
            stream.reset()
            when {
                b0 == 0xEF && b1 == 0xBB && b2 == 0xBF -> "UTF-8"
                b0 == 0xFF && b1 == 0xFE -> "UTF-16LE"
                b0 == 0xFE && b1 == 0xFF -> "UTF-16BE"
                else -> {
                    // 中文 Windows 平台导出的 CSV 无 BOM 时多数为 GBK 编码
                    // （支付宝、微信等国内支付平台均默认 GBK）
                    "GBK"
                }
            }
        } catch (_: Exception) { "GBK" }
    }

}
