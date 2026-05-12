# AutoBookkeeper 重构 v69 变更总结

## 需求
砍掉所有财务自动分析类目，只保留：
1. 月度整体预算
2. 分项生活预算
3. 个人负债账单（手动录入）

## 变更文件

### 删除
- `FinancialHealthScreen.kt` — 财务分析页面（含 MonthlyOverviewCard、DebtHealthBar、BudgetTemplateSection、SavingsGoalCard 等600+行）
- `FinancialHealthViewModel.kt` — 财务分析 ViewModel（300+行）
- `ReportScreen.kt` — 报表页面
- `BudgetDailyCalculator.kt` — 每日预算计算器

### 精简
- `FinancialHealth.kt` → 只保留 `DebtAnalysis` 数据类和 `AlertType`/`AlertTypeConverter`（遗留迁移兼容）
- `Budget.kt` → 删除 `CategoryBudgetDailyStatus`、`BudgetAlert`、`AlertType`、`SpecialBudgetPlan`、`BudgetAlertDao`
- `MainActivity.kt` → 移除 financial_health/report 路由、精简导航（home/add/edit/budget/data/settings/category）
- `MainScreen.kt` → 移除 FinancialHealth/Report 回调参数
- `BudgetViewModel.kt` → 精简约60%，删除每日预算、模板套用、计划生成；新增手动负债管理

### 新增
- `ManualDebt.kt` — 手动录入负债 Room 实体 + DAO（`manual_debts` 表）
- 数据库迁移 `MIGRATION_16_17`

### 重写
- `BudgetScreen.kt` → 完全重写为极简版（约400行）：总预算卡片 + 分项预算列表 + 负债管理折叠卡片
- `BudgetViewModel.kt` → 精简版：预算状态计算 + 手动负债 CRUD

## 新预算管理界面结构
1. **月份导航**（🔄 左右切换）
2. **月度总预算** — 卡片显示总额/已支出/进度条
3. **分项生活预算** — 每个分类一行（额度/已用/进度条/暂停/编辑/删除）
4. **"添加预算分类"** — 未设置预算的分类快捷添加
5. **个人负债账单**（可折叠展开） — 负债总额/月还款汇总 + 逐条明细 + 添加按钮
6. **编辑弹窗** — 统一编辑预算/负债金额

## 用户操作
安装 APK 后：
- 预算页所有功能正常使用
- 手动添加负债：展开"个人负债账单" → 点"添加负债" → 填名称/总额/月还/利率
- 所有历史数据保留（数据库迁移 v16→v17）
