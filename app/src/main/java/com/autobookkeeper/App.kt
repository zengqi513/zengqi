package com.autobookkeeper

import android.app.Application
import androidx.room.Room
import com.autobookkeeper.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "autobookkeeper.db")
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    val userPreferences: UserPreferences by lazy {
        UserPreferences(this)
    }

    /** 便捷访问 TransactionDao */
    val transactionDao by lazy { database.transactionDao() }

    /** 便捷访问 CategoryDao */
    val categoryDao by lazy { database.categoryDao() }

    val budgetDao by lazy { database.budgetDao() }
    val manualDebtDao by lazy { database.manualDebtDao() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 初始化 PdfBox Android（用于 PDF 渲染和 OCR）
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
        initDefaultCategories()
    }

    private fun initDefaultCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val count = database.categoryDao().getCount()
            if (count == 0) {
                database.categoryDao().insertAll(DefaultCategories.categories)
            }
        }
    }
}
