package com.autobookkeeper

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autobookkeeper.data.UserPreferences
import com.autobookkeeper.ui.AddEditScreen
import com.autobookkeeper.ui.AutoBookkeeperTheme
import com.autobookkeeper.ui.CategoryManagementScreen
import com.autobookkeeper.ui.BudgetScreen
import com.autobookkeeper.ui.DataManagementScreen
import com.autobookkeeper.ui.DuplicateMergeScreen
import com.autobookkeeper.ui.MainScreen
import com.autobookkeeper.ui.ReportScreen
import com.autobookkeeper.ui.SettingsScreen
import com.autobookkeeper.ui.VoiceRecordScreen
import com.autobookkeeper.ui.VoiceRecognitionCallbackHolder
import com.autobookkeeper.service.ServiceWatchdog
import com.autobookkeeper.viewmodel.BudgetViewModel
import com.autobookkeeper.viewmodel.TransactionViewModel
import com.autobookkeeper.speech.XunfeiSpeechRecognizer
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    private lateinit var transactionViewModel: TransactionViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化讯飞语音识别 SDK
        XunfeiSpeechRecognizer.initialize(this)
        
        // 启动服务看门狗，确保通知监听服务稳定运行
        // 延迟启动，避免与应用启动冲突
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        // 请求电池优化白名单，确保通知监听服务稳定运行
        if (!ServiceWatchdog.isIgnoringBatteryOptimizations(this)) {
            ServiceWatchdog.requestBatteryOptimizationWhitelist(this)
        }

            ServiceWatchdog.start(this)
        }, 5000)
        
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val userPreferences = remember { UserPreferences(context) }
            val followSystem by userPreferences.followSystem.collectAsState(initial = true)
            val darkModeSetting by userPreferences.darkMode.collectAsState(initial = false)
            val paletteName by userPreferences.themePalette.collectAsState(initial = "WarmGreen")
            val isDark = if (followSystem) isSystemInDarkTheme() else darkModeSetting

            AutoBookkeeperTheme(darkTheme = isDark, paletteName = paletteName) {
                val navController = rememberNavController()
                val txnViewModel: TransactionViewModel = viewModel()
                transactionViewModel = txnViewModel
                MainApp(
                    navController = navController, 
                    userPreferences = userPreferences, 
                    transactionViewModel = txnViewModel,
                    onStartVoiceRecognition = { startVoiceRecognition() }
                )
            }
        }
    }
    
    fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出记账内容，例如：今天午餐花了35块")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "无法启动语音识别：${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!matches.isNullOrEmpty()) {
                        VoiceRecognitionCallbackHolder.callback?.onResult(matches[0])
                    } else {
                        VoiceRecognitionCallbackHolder.callback?.onError("未能识别语音内容")
                    }
                }
                RESULT_CANCELED -> {
                    VoiceRecognitionCallbackHolder.callback?.onError("语音识别已取消")
                }
                else -> {
                    VoiceRecognitionCallbackHolder.callback?.onError("语音识别失败，请重试")
                }
            }
        }
    }
    
    companion object {
        const val VOICE_REQUEST_CODE = 1001
    }
}

@Composable
fun MainApp(
    navController: NavHostController,
    userPreferences: UserPreferences,
    transactionViewModel: TransactionViewModel = viewModel(),
    onStartVoiceRecognition: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val budgetViewModel: BudgetViewModel = viewModel()

    androidx.compose.material3.Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                MainScreen(
                    viewModel = transactionViewModel,
                    userPreferences = userPreferences,
                    onAddClick = { navController.navigate("add") },
                    onEditClick = { id -> navController.navigate("edit/$id") },
                    onSettingsClick = { navController.navigate("settings") },
                    onDataClick = { navController.navigate("data") },
                    onCategoryClick = { navController.navigate("category") },
                    onReportClick = { navController.navigate("report") },
                    onBudgetClick = { navController.navigate("budget") },
                    onAiClick = { navController.navigate("ai") },
                    onVoiceRecordClick = { navController.navigate("voice_record") },
                    onBack = null
                )
            }
            composable("voice_record") {
                VoiceRecordScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() },
                    onSaveComplete = { navController.popBackStack() }
                )
            }
            composable("ai") {
                com.autobookkeeper.ui.AnalysisScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("report") {
                ReportScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add") {
                AddEditScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() },
                    onSaveComplete = { }
                )
            }
            composable("edit/{transactionId}") { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
                AddEditScreen(
                    viewModel = transactionViewModel,
                    editId = editId,
                    onBack = { navController.popBackStack() },
                    onSaveComplete = { }
                )
            }
            composable("budget") {
                BudgetScreen(
                    budgetViewModel = budgetViewModel,
                    transactionViewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("data") {
                DataManagementScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() },
                    onImportComplete = {
                        transactionViewModel.forceRefresh()
                        budgetViewModel.refresh()
                    },
                    onNavigateToDuplicateMerge = { navController.navigate("duplicate_merge") }
                )
            }
            composable("duplicate_merge") {
                DuplicateMergeScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    userPreferences = userPreferences,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("category") {
                CategoryManagementScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
