package com.autobookkeeper.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ─── 默认分类数据 ───
@Suppress("MatchingDeclarationName")
object DefaultCategories {
    val categories = listOf(
        // 支出分类 - 一级（步长10，拖动排序不冲突）
        CategoryData(name = "餐饮", icon = "🍜", type = "expense", parentName = null, sortOrder = 10),
        CategoryData(name = "医疗", icon = "🏥", type = "expense", parentName = null, sortOrder = 20),
        CategoryData(name = "交通", icon = "🚗", type = "expense", parentName = null, sortOrder = 30),
        CategoryData(name = "住房", icon = "🏠", type = "expense", parentName = null, sortOrder = 40),
        CategoryData(name = "购物", icon = "🛍️", type = "expense", parentName = null, sortOrder = 50),
        CategoryData(name = "金融", icon = "💰", type = "expense", parentName = null, sortOrder = 60),
        CategoryData(name = "通信", icon = "📡", type = "expense", parentName = null, sortOrder = 55),
        CategoryData(name = "厨具", icon = "🍳", type = "expense", parentName = null, sortOrder = 43),
        CategoryData(name = "其他", icon = "📌", type = "expense", parentName = null, sortOrder = 100),
        // 支出分类 - 二级
        CategoryData(name = "早餐", icon = "🥣", type = "expense", parentName = "餐饮", sortOrder = 10),
        CategoryData(name = "午餐", icon = "🍱", type = "expense", parentName = "餐饮", sortOrder = 20),
        CategoryData(name = "晚餐", icon = "🍲", type = "expense", parentName = "餐饮", sortOrder = 30),
        CategoryData(name = "零食", icon = "🍪", type = "expense", parentName = "餐饮", sortOrder = 40),
        CategoryData(name = "水果", icon = "🍎", type = "expense", parentName = "餐饮", sortOrder = 50),
        // 购物→移除数码
        CategoryData(name = "服饰", icon = "👔", type = "expense", parentName = "购物", sortOrder = 10),
        CategoryData(name = "日用", icon = "🧴", type = "expense", parentName = "购物", sortOrder = 30),
        CategoryData(name = "美容", icon = "💄", type = "expense", parentName = "购物", sortOrder = 40),
        CategoryData(name = "淘宝", icon = "🛒", type = "expense", parentName = "购物", sortOrder = 10),
        CategoryData(name = "拼多多", icon = "📦", type = "expense", parentName = "购物", sortOrder = 20),
        CategoryData(name = "京东", icon = "🐶", type = "expense", parentName = "购物", sortOrder = 30),
        CategoryData(name = "美团", icon = "🍔", type = "expense", parentName = "购物", sortOrder = 40),
        CategoryData(name = "抖音购物", icon = "🎬", type = "expense", parentName = "购物", sortOrder = 50),
        CategoryData(name = "快手购物", icon = "📺", type = "expense", parentName = "购物", sortOrder = 60),
        CategoryData(name = "超市", icon = "🏪", type = "expense", parentName = "购物", sortOrder = 70),
        // 通信
        CategoryData(name = "话费充值", icon = "📲", type = "expense", parentName = "通信", sortOrder = 10),
        CategoryData(name = "宽带", icon = "📶", type = "expense", parentName = "通信", sortOrder = 20),
        CategoryData(name = "API", icon = "🖥️", type = "expense", parentName = "通信", sortOrder = 30),
        // 交通-子分类
        CategoryData(name = "公交", icon = "🚌", type = "expense", parentName = "交通", sortOrder = 10),
        CategoryData(name = "地铁", icon = "🚇", type = "expense", parentName = "交通", sortOrder = 20),
        CategoryData(name = "打车", icon = "🚕", type = "expense", parentName = "交通", sortOrder = 30),
        CategoryData(name = "火车", icon = "🚄", type = "expense", parentName = "交通", sortOrder = 40),
        CategoryData(name = "飞机", icon = "✈️", type = "expense", parentName = "交通", sortOrder = 50),
        // 住房-子分类
        CategoryData(name = "租金", icon = "🏘️", type = "expense", parentName = "住房", sortOrder = 10),
        CategoryData(name = "水费", icon = "💧", type = "expense", parentName = "住房", sortOrder = 20),
        CategoryData(name = "电费", icon = "⚡", type = "expense", parentName = "住房", sortOrder = 30),
        CategoryData(name = "物业费", icon = "🏢", type = "expense", parentName = "住房", sortOrder = 40),
        CategoryData(name = "燃气费", icon = "🔥", type = "expense", parentName = "住房", sortOrder = 50),
        // 金融-子分类（主类目改为金融）
        CategoryData(name = "转账", icon = "💸", type = "expense", parentName = "金融", sortOrder = 10),
        CategoryData(name = "花呗", icon = "花呗", type = "expense", parentName = "金融", sortOrder = 20),
        CategoryData(name = "借呗", icon = "借呗", type = "expense", parentName = "金融", sortOrder = 21),
        CategoryData(name = "微粒贷", icon = "微粒贷", type = "expense", parentName = "金融", sortOrder = 22),
        CategoryData(name = "网商贷", icon = "网商贷", type = "expense", parentName = "金融", sortOrder = 23),
        CategoryData(name = "放心借", icon = "放心借", type = "expense", parentName = "金融", sortOrder = 24),
        // 收入分类（步长10）
        CategoryData(name = "工资", icon = "💰", type = "income", parentName = null, sortOrder = 10),
        CategoryData(name = "兼职", icon = "💼", type = "income", parentName = null, sortOrder = 20),
        CategoryData(name = "理财", icon = "📈", type = "income", parentName = null, sortOrder = 30),
        CategoryData(name = "红包", icon = "🧧", type = "income", parentName = null, sortOrder = 40),
        CategoryData(name = "转账", icon = "💸", type = "income", parentName = null, sortOrder = 50),
        CategoryData(name = "退款", icon = "🔙", type = "income", parentName = null, sortOrder = 60),
        CategoryData(name = "其他收入", icon = "💵", type = "income", parentName = null, sortOrder = 70)
    )
}// ─── 分类实体 ───
@Entity(tableName = "categories")
data class CategoryData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val type: String,           // "expense" 或 "income"
    val parentName: String?,   // 父分类名称，null 表示顶级分类
    val isCustom: Boolean = false,
    val sortOrder: Int = 0,
    val isHidden: Boolean = false  // 是否隐藏（仅对一级分类有效）
)

// ─── 交易记录实体 ───
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryName: String,     // 改用分类名称
    val categoryIcon: String,     // 分类图标
    val source: Source,
    val note: String = "",
    val rawNotification: String = "",
    val date: Long = System.currentTimeMillis(),  // 用户可自定义日期
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val orderNo: String? = null,              // 订单号/交易单号（用于去重）
    val merchantRaw: String? = null,          // 原始商户名（保留用于调试）
    val fingerprintStrong: String? = null,    // 强指纹：sha256(orderNo+amount+direction)
    val fingerprintMedium: String? = null,    // 中指纹：sha256(amount+direction+merchant+date)
    val isDuplicate: Boolean = false,         // 是否被标记为重复
    val duplicateOf: Long? = null             // 指向被重复的记录ID
)

// ─── 来源枚举 ───
enum class Source(val label: String, val packageName: String? = null) {
    MANUAL("手动"),
    WECHAT("微信", "com.tencent.mm"),
    ALIPAY("支付宝", "com.eg.android.AlipayGphone"),
    TAOBAO("淘宝", "com.taobao.taobao"),
    TAOBAO_FLASH("淘宝闪购", "com.taobao.taobao"),
    PDD("拼多多", "com.xunmeng.pinduoduo"),
    JD("京东", "com.jingdong.app.mall"),
    DOUYIN("抖音", "com.ss.android.ugc.aweme"),
    KUAISHOU("快手", "com.kuaishou.nebula"),
    MEITUAN("美团", "com.sankuai.meituan"),
    DINGDONG("叮咚买菜", "com.yaya.zone"),
    PUPU("朴朴超市", "com.pupu.store"),
    // 银行
    BANK_ICBC("工商银行", "com.icbc"),
    BANK_CCB("建设银行", "com.ccb.pb"),
    BANK_CMB("招商银行", "cmb.pb"),
    BANK_BOCOM("交通银行", "com.bankcomm.Bankcomm"),
    BANK_ABC("农业银行", "com.chinamworld.mbank"),
    BOC("中国银行", "com.boc.bocnet"),
    BANK_SPDB("浦发银行", "cn.com.spdb.mobilebank.per"),
    BANK_CITI("花旗银行", "com.citibank.mobile"),
    BANK_CEB("光大银行", "com.ceb.mobilebank"),
    BANK_CMBC("民生银行", "com.cmbc.mbank"),
    BANK_CITIC("中信银行", "com.citic.mobilebank"),
    BANK_HXB("华夏银行", "com.hxb.mobilebank"),
    BANK_PAB("平安银行", "com.pingan.bank"),
    // 银联 / 数字人民币
    UNIONPAY("银联", "com.unionpay"),
    DCEP("数字人民币", "cn.gov.pbc.dcep")
}

// ─── DAO ───
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getByDateRangeSync(start: Long, end: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE amount < 0 AND date BETWEEN :start AND :end")
    suspend fun getMonthlyExpense(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE amount > 0 AND date BETWEEN :start AND :end")
    suspend fun getMonthlyIncome(start: Long, end: Long): Double

    @Query("""
        SELECT categoryIcon as icon, categoryName as name, COALESCE(SUM(ABS(amount)), 0) as total
        FROM transactions
        WHERE amount < 0 AND date BETWEEN :start AND :end
        GROUP BY categoryName
        ORDER BY total DESC
    """)
    suspend fun getCategoryStats(start: Long, end: Long): List<CategoryStat>

    @Query("""
        SELECT categoryIcon as icon, categoryName as name, COALESCE(SUM(ABS(amount)), 0) as total
        FROM transactions
        WHERE amount > 0 AND date BETWEEN :start AND :end
        GROUP BY categoryName
        ORDER BY total DESC
    """)
    suspend fun getIncomeCategoryStats(start: Long, end: Long): List<CategoryStat>

    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :start AND :end")
    suspend fun getTransactionCount(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE amount > 0 AND date BETWEEN :start AND :end")
    suspend fun getIncomeTransactionCount(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE amount < 0 AND date BETWEEN :start AND :end")
    suspend fun getExpenseTransactionCount(start: Long, end: Long): Int

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllSync(): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    // 按月+分类统计支出（用于预算进度）
    @Query("SELECT COALESCE(SUM(ABS(amount)), 0) FROM transactions WHERE amount < 0 AND date BETWEEN :start AND :end AND categoryName = :categoryName")
    suspend fun getCategoryExpense(start: Long, end: Long, categoryName: String): Double

    // ═══════════════════════════════════════════════
    //  去重相关查询
    // ═══════════════════════════════════════════════

    /** 通过强指纹查找已存在记录（P0：订单号完全匹配） */
    @Query("SELECT * FROM transactions WHERE fingerprintStrong = :fingerprint LIMIT 1")
    suspend fun findByStrongFingerprint(fingerprint: String): Transaction?

    /** 通过中指纹查找疑似重复记录（P1：金额+商户+日期） */
    @Query("SELECT * FROM transactions WHERE fingerprintMedium = :fingerprint LIMIT 1")
    suspend fun findByMediumFingerprint(fingerprint: String): Transaction?

    /** 查找时间窗口内的相似记录（P2：金额+方向+时间窗口） */
    @Query("""
        SELECT * FROM transactions 
        WHERE ABS(amount - :amount) < 0.01 
        AND ((amount < 0 AND :amount < 0) OR (amount > 0 AND :amount > 0))
        AND date BETWEEN :startTime AND :endTime
        AND isDuplicate = 0
        LIMIT 5
    """)
    suspend fun findSimilarInTimeWindow(
        amount: Double,
        startTime: Long,
        endTime: Long
    ): List<Transaction>

    /** 标记记录为重复 */
    @Query("UPDATE transactions SET isDuplicate = 1, duplicateOf = :originalId WHERE id = :id")
    suspend fun markAsDuplicate(id: Long, originalId: Long)

    /** 获取所有被标记为重复的记录 */
    @Query("SELECT * FROM transactions WHERE isDuplicate = 1 ORDER BY date DESC")
    fun getDuplicates(): Flow<List<Transaction>>

    /** 获取所有非重复记录 */
    @Query("SELECT * FROM transactions WHERE isDuplicate = 0 ORDER BY date DESC")
    fun getAllNonDuplicates(): Flow<List<Transaction>>

    /** 获取指定时间范围内的非重复记录 */
    @Query("SELECT * FROM transactions WHERE isDuplicate = 0 AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getNonDuplicatesByDateRange(start: Long, end: Long): Flow<List<Transaction>>
    @Query("UPDATE transactions SET categoryName = :categoryName, categoryIcon = :categoryIcon, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun batchUpdateCategory(ids: List<Long>, categoryName: String, categoryIcon: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET date = :newDate, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun batchUpdateDate(ids: List<Long>, newDate: Long, updatedAt: Long = System.currentTimeMillis())

}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder, id")
    fun getByType(type: String): Flow<List<CategoryData>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder, id")
    suspend fun getCategoriesByType(type: String): List<CategoryData>

    @Query("SELECT * FROM categories WHERE parentName = :parentName ORDER BY sortOrder, id")
    fun getSubCategories(parentName: String): Flow<List<CategoryData>>

    @Query("SELECT * FROM categories WHERE parentName IS NULL AND type = :type ORDER BY sortOrder, id")
    fun getParentCategories(type: String): Flow<List<CategoryData>>

    @Query("SELECT * FROM categories ORDER BY type, sortOrder, id")
    fun getAll(): Flow<List<CategoryData>>

    @Query("SELECT * FROM categories ORDER BY type, sortOrder, id")
    suspend fun getAllSync(): List<CategoryData>

    @Insert
    suspend fun insert(category: CategoryData): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryData>)

    @Update
    suspend fun update(category: CategoryData)

    @Delete
    suspend fun delete(category: CategoryData)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
}

// 分类统计结果
data class CategoryStat(
    val icon: String,
    val name: String,
    val total: Double
)

// ─── Database ───
@Database(entities = [Transaction::class, CategoryData::class, Budget::class, ManualDebt::class], version = 18, exportSchema = false)
@TypeConverters(BudgetConverters::class)
@Suppress("MatchingDeclarationName")
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun manualDebtDao(): ManualDebtDao
}

// 数据库迁移
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
    }
}

// 修复 sortOrder：为每个分类设置唯一的排序值
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 为支出一级分类设置 sortOrder
        val expenseParents = listOf("餐饮" to 1, "消费" to 2, "医疗" to 3, "交通" to 4, "住房" to 5, "购物" to 6, "金融" to 7, "其他" to 8)
        for ((name, order) in expenseParents) {
            database.execSQL("UPDATE categories SET sortOrder = $order WHERE name = '$name' AND type = 'expense' AND parentName IS NULL")
        }
        // 为收入分类设置 sortOrder
        val incomeParents = listOf("工资" to 1, "兼职" to 2, "理财" to 3, "红包" to 4, "其他收入" to 5)
        for ((name, order) in incomeParents) {
            database.execSQL("UPDATE categories SET sortOrder = $order WHERE name = '$name' AND type = 'income' AND parentName IS NULL")
        }
        // 为子分类设置 sortOrder（按 id 顺序）
        database.execSQL("""
            UPDATE categories SET sortOrder = (
                SELECT COUNT(*) FROM categories c2 
                WHERE c2.parentName = categories.parentName 
                AND c2.id <= categories.id
            ) WHERE parentName IS NOT NULL
        """)
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增"转账"支出分类（sortOrder=7），"其他"改为8
        database.execSQL("UPDATE categories SET sortOrder = 8 WHERE name = '其他' AND type = 'expense' AND parentName IS NULL")
        database.execSQL("INSERT INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('转账', '💸', 'expense', NULL, 0, 7, 0)")
        // 新增收入分类
        database.execSQL("UPDATE categories SET sortOrder = 7 WHERE name = '其他收入' AND type = 'income' AND parentName IS NULL")
        database.execSQL("INSERT INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('转账', '💸', 'income', NULL, 0, 5, 0)")
        database.execSQL("INSERT INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('退款', '🔙', 'income', NULL, 0, 6, 0)")
    }
}

/**
 * v6→v7：所有分类sortOrder改为步长10，确保拖动排序不回弹
 * 同一(类型, parentName)分组内按原sortOrder重排为10,20,30...
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 支出一级分类
        val expenseParents = listOf("餐饮" to 10, "消费" to 20, "医疗" to 30, "交通" to 40,
            "房屋水电" to 50, "购物" to 60, "转账" to 70, "其他" to 80)
        for ((name, order) in expenseParents) {
            database.execSQL("UPDATE categories SET sortOrder = $order WHERE name = '$name' AND type = 'expense' AND parentName IS NULL")
        }
        // 收入一级分类
        val incomeParents = listOf("工资" to 10, "兼职" to 20, "理财" to 30, "红包" to 40,
            "转账" to 50, "退款" to 60, "其他收入" to 70)
        for ((name, order) in incomeParents) {
            database.execSQL("UPDATE categories SET sortOrder = $order WHERE name = '$name' AND type = 'income' AND parentName IS NULL")
        }
        // 二级分类也改为步长10
        val subCategories = listOf(
            "餐饮" to listOf("早餐" to 10, "午餐" to 20, "晚餐" to 30, "零食" to 40),
            "消费" to listOf("服饰" to 10, "数码" to 20, "日用" to 30),
            "交通" to listOf("公交" to 10, "地铁" to 20, "打车" to 30),
            "房屋水电" to listOf("房租" to 10, "水费" to 20, "电费" to 30),
            "购物" to listOf("淘宝" to 10, "拼多多" to 20, "京东" to 30, "美团" to 40, "抖音购物" to 50, "快手购物" to 60)
        )
        for ((parent, subs) in subCategories) {
            for ((name, order) in subs) {
                database.execSQL("UPDATE categories SET sortOrder = $order WHERE name = '$name' AND parentName = '$parent'")
            }
        }
    }
}

/**
 * v7→v8：在"消费"下添加子分类"话费充值"和"美容"
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 检查是否已存在这些分类（避免重复插入）
        val phoneRechargeExists = database.query("SELECT 1 FROM categories WHERE name = '话费充值' AND type = 'expense'").count > 0
        val beautyExists = database.query("SELECT 1 FROM categories WHERE name = '美容' AND type = 'expense'").count > 0

        if (!phoneRechargeExists) {
            database.execSQL("INSERT INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('话费充值', '📱', 'expense', '消费', 0, 40, 0)")
        }
        if (!beautyExists) {
            database.execSQL("INSERT INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('美容', '💄', 'expense', '消费', 0, 50, 0)")
        }
    }
}

/**
 * v8→v9：移除重复的"消费"分类，保留"购物"分类及其子分类
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 检查是否存在重复的消费分类
        val duplicateConsumption = database.query("SELECT 1 FROM categories WHERE name = '消费' AND type = 'expense' AND parentName IS NULL").count > 0
        
        if (duplicateConsumption) {
            // 移除重复的"消费"一级分类
            database.execSQL("DELETE FROM categories WHERE name = '消费' AND type = 'expense' AND parentName IS NULL")
            
            // 重新分配子分类的parentName为"购物"
            database.execSQL("UPDATE categories SET parentName = '购物' WHERE parentName = '消费'")
            
            // 重新排序子分类
            database.execSQL("UPDATE categories SET sortOrder = 10 WHERE name = '服饰' AND parentName = '购物'")
            database.execSQL("UPDATE categories SET sortOrder = 20 WHERE name = '数码' AND parentName = '购物'")
            database.execSQL("UPDATE categories SET sortOrder = 30 WHERE name = '日用' AND parentName = '购物'")
            database.execSQL("UPDATE categories SET sortOrder = 40 WHERE name = '话费充值' AND parentName = '购物'")
            database.execSQL("UPDATE categories SET sortOrder = 50 WHERE name = '美容' AND parentName = '购物'")
        }
    }
}


val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增通信→API子分类
        database.execSQL("INSERT OR IGNORE INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('API', '🖥️', 'expense', '通信', 0, 30, 0)")
    }
}

/**
 * v10→v11：新增厨具、超市一级分类
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增厨具
        database.execSQL("INSERT OR IGNORE INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('厨具', '🍳', 'expense', NULL, 0, 43, 0)")
        // 新增超市
        database.execSQL("INSERT OR IGNORE INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('超市', '🛪', 'expense', NULL, 0, 47, 0)")
    }
}

/**
 * v9→v10：新增"通信"一级分类（支出），将"话费充值"从"购物"移至"通信"，新增"宽带"子分类
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增"通信"一级分类（sortOrder=55，插在购物50和转账60之间）
        database.execSQL("INSERT OR IGNORE INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('通信', '📡', 'expense', NULL, 0, 55, 0)")
        
        // 将"话费充值"从购物移入通信
        database.execSQL("UPDATE categories SET parentName = '通信', sortOrder = 10 WHERE name = '话费充值' AND type = 'expense'")
        
        // 新增"宽带"子分类
        val broadbandExists = database.query("SELECT 1 FROM categories WHERE name = '宽带' AND type = 'expense'").count > 0
        if (!broadbandExists) {
            database.execSQL("INSERT INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('宽带', '📶', 'expense', '通信', 0, 20, 0)")
        }
        
        // 更新"其他"的sortOrder从70改为100，腾出空间
        database.execSQL("UPDATE categories SET sortOrder = 100 WHERE name = '其他' AND type = 'expense' AND parentName IS NULL")
        
        // 重新排"购物"剩余子分类的sortOrder（话费充值已移走）
        database.execSQL("UPDATE categories SET sortOrder = 40 WHERE name = '美容' AND parentName = '购物'")
    }
}

// ─── Budget TypeConverters ───
class BudgetConverters {
    @androidx.room.TypeConverter
    fun fromBudgetList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
    @androidx.room.TypeConverter
    fun toBudgetList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}



// v12→v13：新增 budgets 表
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS "budgets" (
            "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            "yearMonth" TEXT NOT NULL,
            "amount" REAL NOT NULL,
            "categoryName" TEXT,
            "enabled" INTEGER NOT NULL DEFAULT 1,
            "createdAt" INTEGER NOT NULL,
            "updatedAt" INTEGER NOT NULL
        )""")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS \"index_budgets_yearMonth_categoryName\" ON \"budgets\" (\"yearMonth\", \"categoryName\")")
    }
}

// v13→v14：新增去重相关字段
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增订单号字段
        database.execSQL("ALTER TABLE transactions ADD COLUMN orderNo TEXT")
        // 新增原始商户名字段
        database.execSQL("ALTER TABLE transactions ADD COLUMN merchantRaw TEXT")
        // 新增强指纹字段（订单号+金额+方向）
        database.execSQL("ALTER TABLE transactions ADD COLUMN fingerprintStrong TEXT")
        // 新增中指纹字段（金额+方向+商户+日期）
        database.execSQL("ALTER TABLE transactions ADD COLUMN fingerprintMedium TEXT")
        // 新增是否重复标记
        database.execSQL("ALTER TABLE transactions ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0")
        // 新增指向原记录的ID
        database.execSQL("ALTER TABLE transactions ADD COLUMN duplicateOf INTEGER")
        
        // 创建索引加速去重查询
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_fingerprintStrong ON transactions(fingerprintStrong)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_fingerprintMedium ON transactions(fingerprintMedium)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isDuplicate ON transactions(isDuplicate)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_amount_date ON transactions(amount, date)")
    }
}

// v14→v15：新增预算提醒表
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS "budget_alerts" (
            "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            "categoryName" TEXT,
            "alertType" TEXT NOT NULL,
            "message" TEXT NOT NULL,
            "date" INTEGER NOT NULL,
            "isRead" INTEGER NOT NULL DEFAULT 0
        )""")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_budget_alerts_isRead ON budget_alerts(isRead)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_budget_alerts_date ON budget_alerts(date)")
    }
}

/**
 * v15→v16：超市从一级分类移入购物子分类；水果新增为餐饮子分类
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 超市：从一级(parentName IS NULL)改为购物子分类
        database.execSQL("UPDATE categories SET parentName = '购物', sortOrder = 70 WHERE name = '超市' AND type = 'expense'")
        // 水果：新增为餐饮子分类（若不存在）
        database.execSQL("INSERT OR IGNORE INTO categories (name, icon, type, parentName, isCustom, sortOrder, isHidden) VALUES ('水果', '🍎', 'expense', '餐饮', 0, 50, 0)")
    }
}


val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET name = '住房' WHERE name = '房屋水电' AND type = 'expense'")
        database.execSQL("UPDATE categories SET name = '金融' WHERE name = '转账' AND type = 'expense' AND parentName IS NULL")
        database.execSQL("UPDATE categories SET parentName = '住房' WHERE parentName = '房屋水电'")
        database.execSQL("UPDATE categories SET parentName = '金融' WHERE parentName = '转账'")
        database.execSQL("DELETE FROM categories WHERE name = '数码' AND parentName = '购物'")
        database.execSQL("UPDATE categories SET name = '租金' WHERE name = '房租' AND parentName = '住房'")
        database.execSQL("DELETE FROM categories WHERE name = '燃气' AND parentName = '住房'")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""CREATE TABLE IF NOT EXISTS `manual_debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `totalAmount` REAL NOT NULL, `monthlyPayment` REAL NOT NULL, `interestRate` REAL NOT NULL DEFAULT 0, `notes` TEXT NOT NULL DEFAULT '', `sortOrder` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL)""")
    }
}

