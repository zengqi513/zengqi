#!/usr/bin/env python3
"""Fix MainActivity.kt to use Compose"""
path = r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\MainActivity.kt'

code = '''package com.autobookkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autobookkeeper.ui.AddEditScreen
import com.autobookkeeper.ui.BudgetScreen
import com.autobookkeeper.ui.DataManagementScreen
import com.autobookkeeper.ui.MainScreen
import com.autobookkeeper.ui.ReportScreen
import com.autobookkeeper.ui.Theme_AutoBookkeeper
import com.autobookkeeper.ui.SettingsScreen
import com.autobookkeeper.viewmodel.BudgetViewModel
import com.autobookkeeper.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme_AutoBookkeeper {
                val navController = rememberNavController()
                val transactionViewModel: TransactionViewModel = viewModel()
                val budgetViewModel: BudgetViewModel = viewModel()

                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        if (currentRoute in listOf("home", "report", "budget", "data", "settings")) {
                            NavigationBar {
                                bottomNavItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            MainScreen(
                                viewModel = transactionViewModel,
                                onAddClick = { navController.navigate("add") }
                            )
                        }
                        composable("add") { AddEditScreen(viewModel = transactionViewModel, onBack = { navController.popBackStack() }) }
                        composable("edit/{txnId}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("txnId")?.toLongOrNull() ?: 0L
                            AddEditScreen(viewModel = transactionViewModel, transactionId = id, onBack = { navController.popBackStack() })
                        }
                        composable("report") { ReportScreen(viewModel = transactionViewModel, onBack = { navController.popBackStack() }) }
                        composable("budget") { BudgetScreen(viewModel = budgetViewModel, onBack = { navController.popBackStack() }) }
                        composable("data") { DataManagementScreen(viewModel = transactionViewModel, onBack = { navController.popBackStack() }) }
                        composable("settings") { SettingsScreen(viewModel = transactionViewModel, onBack = { navController.popBackStack() }) }
                    }
                }
            }
        }
    }
}

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("home", "账本", Icons.Filled.Home),
    BottomNavItem("report", "报表", Icons.Filled.DateRange),
    BottomNavItem("budget", "预算", Icons.Filled.AttachMoney),
    BottomNavItem("data", "数据", Icons.Filled.Storage),
    BottomNavItem("settings", "设置", Icons.Filled.Settings)
)
'''

with open(path, 'w', encoding='utf-8') as f:
    f.write(code)
print('MainActivity OK')
