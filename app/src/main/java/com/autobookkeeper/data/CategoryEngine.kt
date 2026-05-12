package com.autobookkeeper.data

/**
 * 统一分类引擎
 * 
 * 同时用于：
 * 1. PaymentNotificationListener - 通知自动记账分类
 * 2. BillImporter - 账单导入分类
 * 
 * 确保两个入口的分类逻辑完全一致
 */
object CategoryEngine {

    // ═══════════════════════════════════════════════════
    // 关键词定义（统一维护）
    // ═══════════════════════════════════════════════════

    /** 支出指示词 */
    val EXPENSE_INDICATORS = listOf(
        "支出", "消费", "扣款", "取款", "取现", "付款", "支付", "实付"
    )

    /** 收入指示词 */
    val INCOME_INDICATORS = listOf(
        "收入", "入账", "到账", "收款", "存入", "汇入", "转入"
    )

    /** 转账支出关键词 */
    val TRANSFER_OUT_KEYWORDS = listOf(
        "转账", "转给", "转款", "转出", "跨行转账", "手机转账", "网银转账"
    )

    /** 退款关键词 */
    val REFUND_KEYWORDS = listOf(
        "退款", "退款成功", "已退款", "退费", "返还"
    )

    /** 红包收入关键词 */
    val RED_IN_KEYWORDS = listOf(
        "红包", "收到红包", "红包收入"
    )

    /** 转账收入关键词 */
    val TRANSFER_IN_KEYWORDS = listOf(
        "转入", "收到转账", "转账收入"
    )

    /** 工资关键词 */
    val SALARY_KEYWORDS = listOf(
        "工资", "薪资", "薪水", "工资收入", "代发工资"
    )

    /** 理财投资关键词 */
    val INVEST_KEYWORDS = listOf(
        "理财", "基金", "股票", "债券", "余额宝", "余利宝"
    )

    /** 理财收益指示词 */
    val INVEST_INCOME_INDICATORS = listOf(
        "收益", "分红", "利息", "收益到账"
    )

    /** 充值到账关键词 */
    val RECHARGE_IN_KEYWORDS = listOf(
        "充值", "话费充值", "流量充值"
    )

    // ═══════════════════════════════════════════════════
    // 分类关键词映射
    // ═══════════════════════════════════════════════════

    data class CategoryRule(
        val keywords: List<String>,
        val catName: String,
        val catIcon: String,
        val isExpense: Boolean = true
    )

    /** 分类规则列表（按优先级排序） */
    val CATEGORY_RULES = listOf(
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
        CategoryRule(listOf("手机","电脑","平板","耳机","充电器","数据线","充电宝","数码","电子","显示器","鼠标","键盘"), "购物", "📱"),
        // ═══ 医疗 ═══
        CategoryRule(listOf("医院","药店","诊所","看病","买药","体检","医疗","药房","处方药","口罩"), "医疗", "🏥"),
        // ═══ 通信 ═══
        CategoryRule(listOf("广东联通","中国移动","中国电信","话费","流量包","联通","移动","电信","交费","手机充值"), "通信", "📡"),
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

    // ═══════════════════════════════════════════════════
    // 商户/平台关键词映射
    // ═══════════════════════════════════════════════════

    /** 餐饮商户 */
    val FOOD_KEYWORDS = listOf(
        "餐厅", "饭店", "美食", "餐饮", "外卖", "快餐", "火锅", "烧烤", "奶茶", "咖啡",
        "星巴克", "瑞幸", "肯德基", "麦当劳", "必胜客", "海底捞", "喜茶", "奈雪",
        " bakery", "cafe", "restaurant", "美团外卖", "饿了么"
    )

    /** 交通商户 */
    val TRAFFIC_KEYWORDS = listOf(
        "滴滴", "出租车", "地铁", "公交", "加油", "停车", "高速", "ETC", "打车",
        "网约车", "顺风车", "代驾", "充电", "充电桩", "高铁", "火车", "机票", "航班"
    )

    /** 医疗商户 */
    val MEDICAL_KEYWORDS = listOf(
        "医院", "诊所", "药店", "药房", "体检", "医疗", "医药", "挂号", "诊疗"
    )

    /** 住房/水电商户 */
    val UTILITY_KEYWORDS = listOf(
        "水电", "燃气", "物业", "房租", "宽带", "暖气", "水务", "电力", "电网"
    )

    /** 购物商户 */
    val SHOPPING_KEYWORDS = listOf(
        "超市", "商场", "便利店", "百货", "购物", "零售", "山姆", "盒马", "沃尔玛",
        "家乐福", "大润发", "永辉", "华润万家", "7-11", "美宜佳", "全家", "罗森"
    )

    /** 娱乐商户 */
    val ENTERTAINMENT_KEYWORDS = listOf(
        "电影", "影院", "KTV", "游戏", "网吧", "娱乐", "休闲", "会所", "酒吧", "剧本杀"
    )

    // ═══════════════════════════════════════════════════
    // 核心分类方法
    // ═══════════════════════════════════════════════════

    /**
     * 根据文本内容分类交易
     * 
     * @param text 交易文本（通知内容或账单行）
     * @param source 交易来源
     * @param isExpense 是否为支出（null时自动判断）
     * @return 分类结果：分类名 + 图标
     */
    fun classify(
        text: String,
        source: Source,
        isExpense: Boolean? = null
    ): Pair<String, String> {
        val lower = text.lowercase()
        
        // 1. 自动判断收支方向（如果未提供）
        val actualIsExpense = isExpense ?: run {
            val hasExpense = EXPENSE_INDICATORS.any { lower.contains(it) }
            val hasIncome = INCOME_INDICATORS.any { lower.contains(it) }
            when {
                hasExpense && !hasIncome -> true
                hasIncome && !hasExpense -> false
                else -> true // 默认支出
            }
        }
        
        // 2. 特殊交易类型识别
        if (!actualIsExpense) {
            // 退款
            if (REFUND_KEYWORDS.any { lower.contains(it) }) {
                return "退款" to "🔙"
            }
            // 红包
            if (RED_IN_KEYWORDS.any { lower.contains(it) }) {
                return "红包" to "🧧"
            }
            // 工资
            if (SALARY_KEYWORDS.any { lower.contains(it) }) {
                return "工资" to "💼"
            }
            // 理财收益
            if (INVEST_KEYWORDS.any { lower.contains(it) } && 
                INVEST_INCOME_INDICATORS.any { lower.contains(it) }) {
                return "理财收益" to "📈"
            }
            // 转账收入
            if (TRANSFER_IN_KEYWORDS.any { lower.contains(it) }) {
                return "转账" to "💸"
            }
        } else {
            // 转账支出
            if (TRANSFER_OUT_KEYWORDS.any { lower.contains(it) }) {
                return "转账" to "💸"
            }
        }
        
        // 3. 按关键词匹配分类
        for (rule in CATEGORY_RULES) {
            if (rule.isExpense == actualIsExpense || !actualIsExpense) {
                if (rule.keywords.any { lower.contains(it) }) {
                    return rule.catName to rule.catIcon
                }
            }
        }
        
        // 4. 商户关键词匹配
        val merchantCategory = classifyByMerchant(text)
        if (merchantCategory != null) {
            return merchantCategory
        }
        
        // 5. 按来源兜底
        return classifyBySource(source, actualIsExpense)
    }

    /**
     * 根据商户关键词分类
     */
    fun classifyByMerchant(text: String): Pair<String, String>? {
        val lower = text.lowercase()
        
        // 餐饮
        if (FOOD_KEYWORDS.any { lower.contains(it) }) return "餐饮" to "🍜"
        // 交通
        if (TRAFFIC_KEYWORDS.any { lower.contains(it) }) return "交通" to "🚗"
        // 医疗
        if (MEDICAL_KEYWORDS.any { lower.contains(it) }) return "医疗" to "🏥"
        // 住房
        if (UTILITY_KEYWORDS.any { lower.contains(it) }) return "住房" to "🏠"
        // 购物
        if (SHOPPING_KEYWORDS.any { lower.contains(it) }) return "购物" to "🛒"
        // 娱乐
        if (ENTERTAINMENT_KEYWORDS.any { lower.contains(it) }) return "消费" to "🛒"
        
        return null
    }

    /**
     * 按来源兜底分类
     */
    fun classifyBySource(source: Source, isExpense: Boolean): Pair<String, String> {
        return when (source) {
            Source.WECHAT -> if (isExpense) "微信支付" to "💳" else "微信" to "💳"
            Source.ALIPAY -> if (isExpense) "支付宝" to "💳" else "支付宝" to "💳"
            Source.TAOBAO -> "购物" to "🛒"
            Source.PDD -> "购物" to "📦"
            Source.JD -> "购物" to "📱"
            Source.DOUYIN -> "购物" to "🎵"
            Source.KUAISHOU -> "购物" to "📺"
            Source.MEITUAN -> "餐饮" to "🍔"
            Source.DINGDONG -> "购物" to "🛒"
            Source.PUPU -> "购物" to "🛒"
            Source.BANK_ICBC, Source.BANK_CCB, Source.BANK_CMB,
            Source.BANK_BOCOM, Source.BANK_ABC, Source.BOC -> "转账" to "💸"
            else -> "其他" to "📌"
        }
    }

    /**
     * 判断是否为银行来源
     */
    fun isBankSource(source: Source): Boolean {
        return source.name.startsWith("BANK_") || 
               source in listOf(Source.UNIONPAY, Source.DCEP)
    }

    /**
     * 推断实际来源（处理微信内嵌其他平台的情况）
     */
    fun inferActualSource(text: String, pkgSource: Source): Source {
        if (pkgSource != Source.WECHAT) return pkgSource
        
        val lower = text.lowercase()
        return when {
            lower.contains("美团") || lower.contains("大众点评") -> Source.MEITUAN
            lower.contains("拼多多") || lower.contains("pinduoduo") -> Source.PDD
            lower.contains("京东") || lower.contains("jd") -> Source.JD
            lower.contains("淘宝") || lower.contains("天猫") -> Source.TAOBAO
            lower.contains("快手") -> Source.KUAISHOU
            lower.contains("抖音") || lower.contains("douyin") -> Source.DOUYIN
            else -> pkgSource
        }
    }
}
