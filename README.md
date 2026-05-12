# AutoBookkeeper - 自动记账 Android App

## 功能概览

### 手动记账
- 支持收入/支出切换
- 6 大分类：🍜 餐饮、🛒 消费、🏥 医疗、🚗 交通、🏠 房屋水电、📌 其他
- 8 种来源：手动、微信、支付宝、淘宝、拼多多、抖音、快手、朴朴超市
- 备注功能
- 可编辑、可删除

### 自动记账（核心功能）
通过 Android NotificationListenerService 监听支付 App 的推送通知，自动解析金额并创建记账记录。

**支持自动捕获的 App：**
| App | 包名 | 捕获方式 |
|-----|------|---------|
| 微信 | com.tencent.mm | 微信支付凭证通知 |
| 支付宝 | com.eg.android.AlipayGphone | 付款/收款通知 |
| 淘宝 | com.taobao.taobao | 订单付款通知 |
| 拼多多 | com.xunmeng.pinduoduo | 订单付款通知 |
| 抖音 | com.ss.android.ugc.aweme | 消费/收入通知 |
| 快手 | com.smile.gifmaker | 消费/收入通知 |
| 朴朴超市 | com.pupu.cart | 订单付款通知 |

### 月度统计
- 收入/支出/结余汇总
- 分类占比可视化
- 月份切换查看历史

## 技术栈
- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite 本地数据库)
- Navigation Compose
- NotificationListenerService
- minSdk 26 (Android 8.0+)

## 构建方式

1. 用 Android Studio 打开项目根目录
2. 等待 Gradle Sync 完成
3. 连接手机或模拟器，点击 Run

```bash
# 或命令行构建
./gradlew assembleDebug
```

## 首次使用

1. 安装 App 后，进入「设置」页面
2. 点击「前往开启」通知监听权限
3. 在系统设置中找到「自动记账」并开启通知访问权限
4. 返回 App，正常使用即可

## 自动记账原理

```
用户在支付App完成支付
        ↓
支付App发送推送通知
        ↓
NotificationListenerService 捕获通知
        ↓
解析通知文本（正则匹配金额）
        ↓
推断收支类型 + 分类
        ↓
写入 Room 数据库
        ↓
主页面 Flow 自动刷新
```

## 注意事项

- 自动记账依赖 App 发出的推送通知，如果通知被关闭或被系统拦截则无法捕获
- 金额解析基于正则匹配，部分特殊格式的通知可能解析失败，可手动编辑修正
- 分类推断基于关键词匹配，不保证 100% 准确，建议定期检查
- 所有数据存储在本地，不会上传任何服务器

## 项目结构

```
app/src/main/java/com/autobookkeeper/
├── App.kt                          # Application
├── MainActivity.kt                 # 入口 + Navigation
├── data/
│   └── Transaction.kt              # Entity + DAO + Database
├── service/
│   └── PaymentNotificationListener.kt  # 通知监听服务
├── ui/
│   ├── Theme.kt                    # Material 3 主题
│   ├── MainScreen.kt               # 主页面（列表+统计）
│   ├── AddEditScreen.kt            # 新增/编辑页面
│   └── SettingsScreen.kt           # 设置页面
└── viewmodel/
    └── TransactionViewModel.kt     # ViewModel
```
