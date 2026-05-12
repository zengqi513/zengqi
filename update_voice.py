# -*- coding: utf-8 -*-
import sys
sys.path.insert(0, r'D:\新建文件夹\QClaw\resources\openclaw\config\skills\qclaw-text-file\scripts')
from write_file import main as write_main

with open(r'E:\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\VoiceRecordScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace parseVoiceText function
old_func = '''// 解析语音文本
private fun parseVoiceText(text: String): ParsedTransaction {
    // 提取金额
    val amountRegex = Regex("""(\\d+(?:\\.\\d{1,2})?)\\s*[元块圆]""")
    val amountMatch = amountRegex.find(text)
    val amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    
    // 判断收支类型
    val isExpense = !text.contains(Regex("""收入|到账|收款|工资|收益""", RegexOption.IGNORE_CASE))
    
    // 提取分类和商户
    val (categoryName, categoryIcon, merchantRaw) = extractCategoryAndMerchant(text, isExpense)
    
    return ParsedTransaction(
        amount = amount,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        merchantRaw = merchantRaw,
        isExpense = isExpense
    )
}'''

new_func = '''// 解析语音文本 - 使用增强版自然语言理解
private fun parseVoiceText(text: String): ParsedTransaction {
    // 使用增强版解析器
    val result = VoiceTransactionParser.parse(text)
    
    // 转换为 ParsedTransaction
    val isExpense = result.transactionType == VoiceTransactionParser.TransactionType.EXPENSE ||
                    result.transactionType == VoiceTransactionParser.TransactionType.UNKNOWN
    
    // 根据分类获取图标
    val categoryIcon = getCategoryIcon(result.category, isExpense)
    
    return ParsedTransaction(
        amount = result.amount ?: 0.0,
        categoryName = result.category,
        categoryIcon = categoryIcon,
        merchantRaw = result.merchant,
        isExpense = isExpense
    )
}

// 根据分类获取图标
private fun getCategoryIcon(category: String, isExpense: Boolean): String {
    val expenseIcons = mapOf(
        "餐饮" to "🍜",
        "交通" to "🚗",
        "购物" to "🛍️",
        "娱乐" to "🎬",
        "住房" to "🏠",
        "医疗" to "🏥",
        "教育" to "📚",
        "通讯" to "📱",
        "人情" to "🎁",
        "宠物" to "🐱",
        "金融" to "💳",
        "其他" to "📌"
    )
    
    val incomeIcons = mapOf(
        "收入" to "💰",
        "工资" to "💵",
        "奖金" to "🎁",
        "投资" to "📈",
        "其他收入" to "💵"
    )
    
    return if (isExpense) {
        expenseIcons[category] ?: "📌"
    } else {
        incomeIcons[category] ?: "💰"
    }
}'''

content = content.replace(old_func, new_func)

# Write the file
import sys
sys.argv = ['write_file.py', '--path', r'E:\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\VoiceRecordScreen.kt', '--content', content, '--platform', 'windows']
write_main()
print('Updated parseVoiceText')
