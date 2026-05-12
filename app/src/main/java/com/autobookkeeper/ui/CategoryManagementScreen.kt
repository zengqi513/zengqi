package com.autobookkeeper.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.autobookkeeper.data.CategoryData
import com.autobookkeeper.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object IconLibrary {
    // 平台品牌图标（支持图片资源或文字+品牌色）
    data class BrandIcon(
        val text: String,
        val color: Color,
        val drawableRes: Int? = null  // 图片资源ID，优先使用
    )

    // 品牌图标资源映射（需要在使用时通过 Context 获取 drawable）
    val brandIconResources = mapOf(
        "支付宝" to "ic_logo_alipay",
        "微信" to "ic_logo_wechat",
        "微信支付" to "ic_logo_wechat_pay",
        "淘宝" to "ic_logo_taobao",
        "京东" to "ic_logo_jingdong",
        "拼多多" to "ic_logo_pdd",
        "抖音" to "ic_logo_douyin",
        "美团" to "ic_logo_meituan",
        "放心借" to "ic_logo_fangxinjie",
        "快手" to "ic_logo_kuaishou",
        "网商贷" to "ic_logo_wangshangdai",
        "微粒贷" to "ic_logo_weilidai",
        "借呗" to "ic_logo_jiebei",
        "花呗" to "ic_logo_huabei"
    )

    val brandIcons = mapOf(
        // 电商平台 - 使用本地图片资源
        "淘宝" to BrandIcon("Taobao", Color(0xFFFF5000)),
        "天猫" to BrandIcon("Tmall", Color(0xFFFF0036)),
        "京东" to BrandIcon("JD", Color(0xFFE4393C)),
        "拼多多" to BrandIcon("PDD", Color(0xFFE02E24)),
        "唯品会" to BrandIcon("VIP", Color(0xFFF10180)),
        "苏宁" to BrandIcon("Suning", Color(0xFFFF6600)),
        "抖音" to BrandIcon("抖音", Color(0xFF000000)),
        "快手" to BrandIcon("快手", Color(0xFFFE2C55)),
        "小红书" to BrandIcon("小红书", Color(0xFFFF2442)),
        "得物" to BrandIcon("得物", Color(0xFF00C8B1)),
        "闲鱼" to BrandIcon("闲鱼", Color(0xFFFFE100)),

        // 外卖/生鲜 - 官方LOGO配色
        "美团" to BrandIcon("美团", Color(0xFFFFD100)),        // 美团黄
        "饿了么" to BrandIcon("饿了么", Color(0xFF0085FF)),    // 饿了么蓝 #0085FF
        "朴朴" to BrandIcon("朴朴", Color(0xFF00B386)),        // 朴朴绿 #00B386
        "叮咚" to BrandIcon("叮咚", Color(0xFF00C853)),        // 叮咚绿 #00C853
        "盒马" to BrandIcon("盒马", Color(0xFF00C8B1)),        // 盒马青 #00C8B1
        "每日优鲜" to BrandIcon("优鲜", Color(0xFF00C853)),
        "永辉" to BrandIcon("永辉", Color(0xFF00A651)),
        "山姆" to BrandIcon("山姆", Color(0xFF0055AA)),
        "Costco" to BrandIcon("Costco", Color(0xFFE31837)),

        // 支付/金融 - 官方LOGO配色
        // ⚠ 以下已有图片资源，BrandIcon 仅作回退
        "微信" to BrandIcon("WeChat", Color(0xFF07C160)),      // 微信绿 #07C160
        "支付宝" to BrandIcon("支付", Color(0xFF1677FF)),       // 支付宝蓝 #1677FF
        // "花呗" to BrandIcon("花呗", Color(0xFF1677FF)),  -- 已使用图片资源
        "白条" to BrandIcon("白条", Color(0xFFE4393C)),
        "信用卡" to BrandIcon("信用", Color(0xFFFF6B00)),
        "银行" to BrandIcon("银行", Color(0xFF4A90D9)),
        "工行" to BrandIcon("ICBC", Color(0xFFC41E3A)),        // 工行红 #C41E3A
        "建行" to BrandIcon("CCB", Color(0xFF0066B3)),         // 建行蓝 #0066B3
        "招行" to BrandIcon("CMB", Color(0xFFE60012)),         // 招行红 #E60012
        "中行" to BrandIcon("BOC", Color(0xFFB81C22)),         // 中行红 #B81C22
        "农行" to BrandIcon("ABC", Color(0xFF008C4F)),         // 农行绿 #008C4F

        // 借贷平台
        "网商贷" to BrandIcon("网商", Color(0xFFFF6A00)),
        "借呗" to BrandIcon("借呗", Color(0xFF1677FF)),
        "京东白条" to BrandIcon("白条", Color(0xFFE4393C)),
        "京东金条" to BrandIcon("金条", Color(0xFFFFD700)),
        "放心借" to BrandIcon("放心", Color(0xFF00C8B1)),
        "微粒贷" to BrandIcon("微粒", Color(0xFF07C160)),

        // 出行
        "滴滴" to BrandIcon("滴滴", Color(0xFFFF6B00)),
        "高德" to BrandIcon("高德", Color(0xFF4285F4)),
        "百度" to BrandIcon("百度", Color(0xFF2932E1)),
        "携程" to BrandIcon("携程", Color(0xFF2577E3)),
        "飞猪" to BrandIcon("飞猪", Color(0xFFFF6A00)),
        "去哪儿" to BrandIcon("去哪", Color(0xFF00BFFF)),
        "12306" to BrandIcon("铁路", Color(0xFFFF0000)),
        "哈啰" to BrandIcon("哈啰", Color(0xFF00BFFF)),

        // 视频/娱乐
        "爱奇艺" to BrandIcon("爱奇", Color(0xFF00BE06)),
        "腾讯视频" to BrandIcon("腾讯", Color(0xFFFF6B00)),
        "优酷" to BrandIcon("优酷", Color(0xFF1E90FF)),
        "B站" to BrandIcon("Bili", Color(0xFFFB7299)),
        "芒果" to BrandIcon("芒果", Color(0xFFFF6A00)),
        "网易云" to BrandIcon("网易", Color(0xFFDD001B)),
        "QQ音乐" to BrandIcon("QQ", Color(0xFF31C27C)),
        "喜马拉雅" to BrandIcon("喜马", Color(0xFFFF6B00)),

        // 社交
        "QQ" to BrandIcon("QQ", Color(0xFF12B7F5)),
        "微博" to BrandIcon("微博", Color(0xFFE6162D)),
        "知乎" to BrandIcon("知乎", Color(0xFF0084FF)),

        // 办公/工具
        "钉钉" to BrandIcon("钉钉", Color(0xFF3370FF)),
        "飞书" to BrandIcon("飞书", Color(0xFF3370FF)),
        "企业微信" to BrandIcon("企微", Color(0xFF2BAD31)),
        "WPS" to BrandIcon("WPS", Color(0xFFFF6A00)),
        "百度网盘" to BrandIcon("网盘", Color(0xFF2932E1)),

        // 快递/物流
        "顺丰" to BrandIcon("顺丰", Color(0xFF000000)),
        "中通" to BrandIcon("中通", Color(0xFF0066CC)),
        "圆通" to BrandIcon("圆通", Color(0xFFFF6A00)),
        "韵达" to BrandIcon("韵达", Color(0xFF0099FF)),
        "菜鸟" to BrandIcon("菜鸟", Color(0xFFFF6A00)),

        // 教育
        "作业帮" to BrandIcon("作业", Color(0xFFFF6B00)),
        "猿辅导" to BrandIcon("猿辅", Color(0xFF00C8B1)),
        "学而思" to BrandIcon("学而", Color(0xFFFF6B00)),
        "新东方" to BrandIcon("新东", Color(0xFF00B386)),

        // 医疗/健康
        "平安好医生" to BrandIcon("平安", Color(0xFFFF6B00)),
        "丁香医生" to BrandIcon("丁香", Color(0xFF00B386)),
        "美团买药" to BrandIcon("买药", Color(0xFFFF6B00)),
        "叮当快药" to BrandIcon("叮当", Color(0xFFFF6B00)),

        // 游戏
        "Steam" to BrandIcon("STEAM", Color(0xFF1B2838)),
        "腾讯游戏" to BrandIcon("腾讯", Color(0xFF00A1D6)),
        "网易游戏" to BrandIcon("网易", Color(0xFFFF6B00)),
        "米哈游" to BrandIcon("米哈", Color(0xFF4A90E2)),

        // 云服务
        "阿里云" to BrandIcon("阿里", Color(0xFFFF6A00)),
        "腾讯云" to BrandIcon("腾讯", Color(0xFF00A1D6)),
        "华为云" to BrandIcon("华为", Color(0xFFFF0000)),
        "AWS" to BrandIcon("AWS", Color(0xFFFF9900)),

        // 其他常用
        "苹果" to BrandIcon("Apple", Color(0xFF555555)),
        "华为" to BrandIcon("HUAWEI", Color(0xFFFF0000)),
        "小米" to BrandIcon("MI", Color(0xFFFF6A00)),
        "OPPO" to BrandIcon("OPPO", Color(0xFF1BAA52)),
        "vivo" to BrandIcon("vivo", Color(0xFF415FFF)),
        "星巴克" to BrandIcon("星巴", Color(0xFF00704A)),
        "瑞幸" to BrandIcon("瑞幸", Color(0xFF0022AB)),
        "喜茶" to BrandIcon("喜茶", Color(0xFF000000)),
        "奈雪" to BrandIcon("奈雪", Color(0xFFFF6B9D)),
        "蜜雪冰城" to BrandIcon("蜜雪", Color(0xFFFF0000)),
        "肯德基" to BrandIcon("KFC", Color(0xFFA3080C)),
        "麦当劳" to BrandIcon("M", Color(0xFFFFC72C)),
        "汉堡王" to BrandIcon("BK", Color(0xFFFF8732)),
        "必胜客" to BrandIcon("必胜", Color(0xFF00A651)),
        "海底捞" to BrandIcon("海底", Color(0xFFE60012)),
    )

    // 按分类组织的图标
    data class IconCategory(val name: String, val icons: List<String>)

    val iconCategories = listOf(
        // ===== 餐饮美食 =====
        IconCategory("餐饮", listOf(
            "🍜", "🍚", "🍽️", "🥣", "🍱", "🍲", "🥡", "🍪", "🥤", "☕", "🧋", "🍕", "🍔", "🍰", "🥟", "🍳",
            "🥗", "🥩", "🍗", "🍖", "🦐", "🦀", "🐟", "🍣", "🍙", "🍘", "🍢", "🍡", "🍧", "🍨", "🍦", "🍫",
            "🍵", "🍺", "🍷", "🥂", "🍾", "🥛", "🧃", "🧊"
        )),

        // ===== 服饰购物 =====
        IconCategory("购物", listOf(
            "🛒", "🛍️", "👔", "👕", "👖", "🧥", "👗", "👘", "🥻", "🩱", "👙", "👚", "👛", "👜", "👝", "🎒",
            "👞", "👟", "🥾", "🥿", "👠", "👡", "👢", "🧦", "🧤", "🧣", "🎩", "🧢", "👒", "🎓", "👑", "🕶️",
            "💄", "💍", "⌚", "📿", "🧴", "🧼", "🧽"
        )),

        // ===== 数码电器 =====
        IconCategory("数码", listOf(
            "📱", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "🖲️", "🕹️", "💽", "💾", "💿", "📀", "📷", "📸",
            "📹", "🎥", "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️", "🎛️", "🧭", "⏰", "⏲️",
            "🔊", "🔉", "🔇", "🎧", "🎤", "📡", "🔋", "🔌", "💡", "🔦", "🕯️"
        )),

        // ===== 交通出行 =====
        IconCategory("出行", listOf(
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🚚", "🚛", "🚜", "🛵", "🏍️", "🛺",
            "🚲", "🛴", "🚏", "🛣️", "🛤️", "🚇", "🚊", "🚉", "🚈", "🚂", "🚆", "🚅", "🚄", "🚃", "🚋", "🚞",
            "✈️", "🛫", "🛬", "🛩️", "💺", "🚁", "🚟", "🚠", "🚡", "🚀", "🛸", "⛵", "🚤", "🛥️", "🛳️",
            "⛽", "🅿️", "🚥", "🚦", "🛑", "🚧", "⚓"
        )),

        // ===== 医疗健康 =====
        IconCategory("医疗", listOf(
            "🏥", "💊", "🩺", "🦷", "💉", "🩹", "🩼", "🩻", "🧬", "❤️", "🧡", "💛", "💚", "💙", "💜",
            "🧘", "🧘‍♂️", "🧘‍♀️", "🏃", "🏃‍♂️", "🏃‍♀️", "🚶", "🚶‍♂️", "🚶‍♀️", "💪",
            "🧠", "🫀", "🫁", "👀", "👁️", "👂", "👃", "🩸", "🦠", "🧫", "🧪", "🌡️", "🧴"
        )),

        // ===== 保险金融 =====
        IconCategory("金融", listOf(
            "🛡️", "🔒", "🔓", "🔏", "🔐", "🔑", "🗝️", "💰", "💴", "💵", "💶", "💷", "💸", "💳", "🧾", "💹",
            "💱", "💲", "🏦", "🏧", "💼", "📈", "📉", "📊", "🏆", "🥇", "🥈", "🥉", "🏅"
        )),

        // ===== 房屋家居 =====
        IconCategory("家居", listOf(
            "🏠", "🏡", "🏘️", "🏚️", "🏗️", "🏭", "🏢", "🏬", "🏣", "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫",
            "🛋️", "🛏️", "🚪", "🪟", "🪑", "🚽", "🪠", "🚿", "🛁", "🪒", "🧴", "🧷", "🧹", "🧺", "🧻", "🧼",
            "🧽", "🧯", "🔧", "🪛", "🔩", "⚙️", "🗜️", "⚖️", "💧", "⚡", "🔥", "💨", "🌐", "🔌", "🔋", "💡"
        )),

        // ===== 通信网络 =====
        IconCategory("通信", listOf(
            "📲", "📳", "📴", "📶", "📸", "📹", "📺", "📻", "📼", "📽️", "🎙️", "🎚️", "🎛️", "📡", "🔉", "🔊",
            "📢", "📣", "📯", "🔔", "🔕", "🎵", "🎶", "🎤", "🎧"
        )),

        // ===== 娱乐休闲 =====
        IconCategory("娱乐", listOf(
            "🎮", "🎯", "🎰", "🎲", "🎳", "🎪", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸",
            "🪕", "🎻", "🎭", "📚", "📖", "📕", "📗", "📘", "📙", "📓", "📒", "📃", "📜", "📄", "📰", "🗞️",
            "📑", "🔖", "🏷️", "🎓", "🎒", "📝", "✏️", "✒️", "🖋️", "🖊️", "🖌️", "🖍️", "💼", "📁", "📂",
            "🗂️", "📅", "📆", "🗒️", "🗓️", "📇", "📈", "📉", "📊", "📋", "📌", "📍", "📎", "🖇️", "📏", "📐",
            "✂️", "🗃️", "🗄️", "🗑️", "🖼️", "🧵", "🪡", "🧶", "🪢", "🛍️"
        )),

        // ===== 运动健身 =====
        IconCategory("运动", listOf(
            "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍",
            "🏏", "🥅", "⛳", "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌", "🎿",
            "⛷️", "🏂", "🪂", "🏋️", "🏋️‍♂️", "🏋️‍♀️", "🤼", "🤼‍♂️", "🤼‍♀️", "🤸", "🤸‍♂️", "🤸‍♀️", "⛹️", "⛹️‍♂️", "⛹️‍♀️", "🤺",
            "🤾", "🤾‍♂️", "🤾‍♀️", "🏌️", "🏌️‍♂️", "🏌️‍♀️", "🏇", "🧘", "🧘‍♂️", "🧘‍♀️", "🏄", "🏄‍♂️", "🏄‍♀️", "🏊", "🏊‍♂️", "🏊‍♀️",
            "🤽", "🤽‍♂️", "🤽‍♀️", "🚣", "🚣‍♂️", "🚣‍♀️", "🧗", "🧗‍♂️", "🧗‍♀️", "🚵", "🚵‍♂️", "🚵‍♀️", "🚴", "🚴‍♂️", "🚴‍♀️", "🏆"
        )),

        // ===== 旅游出行 =====
        IconCategory("旅游", listOf(
            "🗺️", "🗾", "🧭", "🏔️", "⛰️", "🌋", "🗻", "🏕️", "🏖️", "🏜️", "🏝️", "🏞️", "🏟️", "🏛️", "🏗️",
            "🪨", "🪵", "🛖", "🏘️", "🏚️", "🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫",
            "🏬", "🏭", "🏯", "🏰", "💒", "🗼", "🗽", "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋", "⛲", "⛺", "🌁",
            "🌃", "🏙️", "🌄", "🌅", "🌆", "🌇", "🌉", "♨️", "🎠", "🎡", "🎢", "💈", "🎪"
        )),

        // ===== 宠物植物 =====
        IconCategory("宠物", listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵",
            "🐔", "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌",
            "🐞", "🐜", "🦗", "🕷️", "🕸️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀",
            "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏",
            "🐪", "🐫", "🦒", "🦘", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐩",
            "🦮", "🐕‍🦺", "🐈", "🐈‍⬛", "🐓", "🦃", "🦚", "🦜", "🦢", "🦩", "🕊️", "🐇", "🦝", "🦨", "🦡", "🦦",
            "🦥", "🐁", "🐀", "🐿️", "🦔", "🐾", "🌵", "🎄", "🌲", "🌳", "🌴", "🌱", "🌿", "☘️", "🍀", "🎍",
            "🎋", "🍃", "🍂", "🍁", "🍄", "🐚", "🌾", "💐", "🌷", "🌹", "🥀", "🌺", "🌸", "🌼", "🌻", "🌞"
        )),

        // ===== 母婴育儿 =====
        IconCategory("母婴", listOf(
            "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩", "🧓", "👴", "👵",
            "🍼", "🧃", "🧂", "🥄", "🍴", "🍽️", "🥣", "🥡", "🥢", "🧊", "🧋", "🧉"
        )),

        // ===== 常用符号 =====
        IconCategory("常用", listOf(
            "📌", "⭐", "❤️", "🔔", "📅", "⏰", "🌟", "✨", "🎁", "🎈", "🔐", "💎", "🔮", "🧸",
            "🧹", "🧺", "🧻", "🧼", "🧽", "⚰️", "🗿", "🏧",
            "⚠️", "⛔", "🚫", "✅", "✔️", "❌", "➰", "➿", "✳️", "✴️", "❇️"
        ))
    )

    // 获取所有图标（扁平列表，用于兼容旧代码）
    val icons: List<String> = iconCategories.flatMap { it.icons }
}

object IconMatcher {
    // 优先匹配品牌
    fun matchIcon(name: String): String {
        // 1. 先尝试匹配品牌（返回品牌名称，UI层会特殊渲染）
        val brandMatch = IconLibrary.brandIcons.keys.find { brand ->
            name.contains(brand, ignoreCase = true)
        }
        if (brandMatch != null) return brandMatch

        // 2. 再匹配普通图标
        val iconMap = mapOf(
            // ===== 餐饮美食 =====
            "餐" to "🍜", "饭" to "🍚", "食" to "🍽️", "早" to "🥣", "午" to "🍱", "晚" to "🍲",
            "外" to "🥡", "外卖" to "🥡", "零食" to "🍪", "饮" to "🥤", "料" to "🧋", "咖啡" to "☕",
            "奶茶" to "🧋", "茶" to "🍵", "酒" to "🍺", "面" to "🍜", "米" to "🍚", "肉" to "🥩",
            "菜" to "🥗", "果" to "🍎", "蛋" to "🥚", "糕" to "🍰", "甜" to "🍨", "冰" to "🍦",
            "烧烤" to "🍖", "火锅" to "🍲", "寿司" to "🍣", "汉堡" to "🍔", "披萨" to "🍕",
            "鸡" to "🍗", "鱼" to "🐟", "虾" to "🦐", "蟹" to "🦀", "贝" to "🦪",

            // ===== 服饰购物 =====
            "购" to "🛒", "买" to "🛍️", "衣" to "👔", "服" to "👗", "装" to "🧥", "鞋" to "👟",
            "靴" to "🥾", "包" to "👜", "袋" to "🛍️", "帽" to "🧢", "巾" to "🧣", "袜" to "🧦",
            "手套" to "🧤", "首饰" to "💍", "戒" to "💍", "链" to "📿", "表" to "⌚", "镜" to "🕶️",
            "妆" to "💄", "肤" to "🧴", "香" to "🧴", "洗" to "🧼", "护" to "🧴",

            // ===== 数码电器 =====
            "数" to "📱", "码" to "💻", "电" to "📺", "脑" to "💻", "机" to "📱", "手" to "📱",
            "平板" to "📱", "笔记本" to "💻", "相机" to "📷", "摄" to "📹", "耳机" to "🎧",
            "键盘" to "⌨️", "鼠标" to "🖱️", "打印" to "🖨️", "路由" to "📡", "音响" to "🔊",
            "电视" to "📺", "冰箱" to "❄️", "空调" to "❄️", "洗衣" to "🧺", "家电" to "📺",

            // ===== 交通出行 =====
            "车" to "🚗", "汽" to "🚗", "轿" to "🚗", "SUV" to "🚙", "越野" to "🚙",
            "打" to "🚕", "租" to "🚙", "公" to "🚌", "交" to "🚌", "巴" to "🚌",
            "地" to "🚇", "铁" to "🚇", "轨" to "🚈", "火" to "🚄", "高" to "🚄", "动" to "🚅",
            "飞" to "✈️", "航" to "✈️", "船" to "🚢", "轮" to "🛳️",
            "骑" to "🚲", "单" to "🚲", "摩" to "🏍️", "电驴" to "🛵", "滑板" to "🛹",
            "油" to "⛽", "停" to "🅿️", "高速" to "🛣️", "过路" to "🛣️", "保养" to "🔧",

            // ===== 医疗健康 =====
            "医" to "🏥", "院" to "🏥", "诊" to "🩺", "所" to "🏥", "药" to "💊", "房" to "💊",
            "病" to "🤒", "痛" to "🤕", "伤" to "🩹", "牙" to "🦷", "眼" to "👁️", "皮" to "🧴",
            "检" to "🩺", "查" to "🔍", "体" to "💪", "康" to "❤️", "复" to "🧘", "养" to "🧘",
            "针" to "💉", "疫" to "💉", "苗" to "💉", "血" to "🩸", "验" to "🧪", "护" to "👨‍⚕️",

            // ===== 保险金融 =====
            "保" to "🛡️", "险" to "🛡️", "人寿" to "👤", "健康" to "❤️", "医疗" to "🏥",
            "意外" to "⚠️", "车险" to "🚗", "财产" to "🏠", "养老" to "👴", "社保" to "🏛️",
            "公积" to "🏦", "金" to "💰", "银" to "🏦", "卡" to "💳", "信" to "💳",
            "存" to "🏦", "取" to "💵", "转" to "🔄", "汇" to "💱", "费" to "🧾", "税" to "💸",
            "工资" to "💰", "薪" to "💵", "奖" to "🏆", "红" to "🧧", "利" to "💰",
            "理" to "📈", "投" to "📊", "基" to "📈", "股" to "📉", "期" to "📅",
            "兼" to "💼", "副" to "💼", "退" to "↩️", "赔" to "💸", "偿" to "💸",

            // ===== 借贷还款 =====
            "贷" to "📄", "借" to "📄", "款" to "💵", "还" to "↩️", "欠" to "📝",
            "网商贷" to "网商", "借呗" to "借呗", "花呗" to "花呗", "白条" to "白条",
            "金条" to "金条", "放心借" to "放心", "微粒贷" to "微粒", "分期" to "📅",
            "利息" to "📈", "本金" to "💰", "逾期" to "⚠️", "罚息" to "💸",

            // ===== 房屋家居 =====
            "房" to "🏠", "屋" to "🏠", "家" to "🏠", "住" to "🏠", "宅" to "🏠",
            "押" to "🔐", "物业" to "🏢", "水" to "💧", "电" to "⚡", "气" to "🔥",
            "煤" to "🔥", "暖" to "🔥", "网" to "🌐", "宽" to "🌐", "修" to "🔧", "装" to "🛠️",
            "具" to "🛋️", "床" to "🛏️", "桌" to "🪑", "椅" to "🪑", "窗" to "🪟", "门" to "🚪",
            "厨" to "🍳", "卫" to "🚽", "浴" to "🛁", "清" to "🧹", "洁" to "🧼", "扫" to "🧹",

            // ===== 通信网络 =====
            "话" to "📞", "讯" to "📡", "邮" to "📧", "件" to "📧",
            "短" to "💬", "络" to "🌐", "流" to "📶", "量" to "📊", "号" to "#️⃣",
            "WiFi" to "📶", "5G" to "📶", "4G" to "📶",

            // ===== 娱乐休闲 =====
            "娱" to "🎮", "乐" to "🎵", "玩" to "🎯", "游" to "🎮", "戏" to "🎮", "影" to "🎬",
            "视" to "📺", "剧" to "📺", "片" to "🎞️", "票" to "🎫", "唱" to "🎤", "歌" to "🎤",
            "KTV" to "🎤", "会" to "🎉", "聚" to "🥳", "派" to "🎉", "吧" to "🍻",
            "书" to "📚", "读" to "📖", "报" to "📰", "志" to "📰", "画" to "🎨", "展" to "🖼️",
            "旅" to "✈️", "游" to "🗺️", "景" to "🏞️", "区" to "🏰", "门" to "🎫",

            // ===== 运动健身 =====
            "运" to "⚽", "动" to "🏃", "健" to "💪", "身" to "🏋️", "体" to "🏃", "跑" to "🏃",
            "走" to "🚶", "步" to "👟", "泳" to "🏊", "球" to "⚽", "篮" to "🏀",
            "足" to "⚽", "羽" to "🏸", "乒" to "🏓", "网" to "🎾", "台" to "🎱", "高" to "⛳",
            "瑜" to "🧘", "伽" to "🧘", "拳" to "🥊", "武" to "🥋", "舞" to "💃",
            "员" to "👤", "卡" to "💳", "年" to "📅", "季" to "🗓️", "月" to "🌙",

            // ===== 宠物植物 =====
            "宠" to "🐕", "物" to "🐈", "狗" to "🐕", "犬" to "🐕", "猫" to "🐈", "咪" to "🐱",
            "鸟" to "🐦", "鱼" to "🐟", "龟" to "🐢", "兔" to "🐰", "鼠" to "🐭", "仓" to "🐹",
            "粮" to "🥫", "食" to "🍖", "罐" to "🥫", "零" to "🍪", "具" to "🎾",
            "花" to "🌸", "草" to "🌿", "树" to "🌳", "盆" to "🪴", "园" to "🌻", "艺" to "🌷",

            // ===== 母婴育儿 =====
            "孕" to "🤰", "婴" to "👶", "儿" to "🍼", "宝" to "👶", "孩" to "🧒", "童" to "🧸",
            "奶" to "🍼", "粉" to "🥛", "尿" to "👶", "裤" to "👖", "衣" to "👶",
            "教" to "📚", "育" to "🎓", "托" to "🏫", "幼" to "🏫", "园" to "🏫", "学" to "🎒",

            // ===== 其他通用 =====
            "礼" to "🎁", "物" to "🎁", "捐" to "❤️", "赠" to "🎁", "善" to "💝", "爱" to "❤️",
            "婚" to "💒", "丧" to "⚰️", "葬" to "⚰️", "祭" to "🕯️", "佛" to "🙏", "寺" to "⛩️",
            "其" to "📌", "他" to "📌", "杂" to "📦", "项" to "📋", "未" to "❓", "分" to "📂",
            "自" to "🤖", "定" to "⚙️", "义" to "📝", "新" to "✨", "增" to "➕", "建" to "🏗️"
        )

        for ((key, icon) in iconMap) {
            if (name.contains(key)) return icon
        }
        return "📌"
    }

    /**
     * 检查是否是品牌图标
     */
    fun isBrandIcon(icon: String): Boolean {
        return IconLibrary.brandIcons.containsKey(icon)
    }

    /**
     * 获取品牌图标信息
     */
    fun getBrandIcon(icon: String): IconLibrary.BrandIcon? {
        return IconLibrary.brandIcons[icon]
    }
}

/**
 * 通用图标显示组件
 * 支持：1) 本地图片资源 2) 品牌文字图标 3) 普通emoji图标
 */
@Composable
fun CategoryIcon(
    icon: String,
    size: Int = 24,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val brandIcon = IconLibrary.brandIcons[icon]
    val drawableName = IconLibrary.brandIconResources[icon]

    // 1. 优先尝试加载本地图片资源
    if (drawableName != null) {
        val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        if (resId != 0) {
            Box(
                modifier = modifier.size(size.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = resId),
                    contentDescription = icon,
                    modifier = Modifier.fillMaxSize()
                )
            }
            return
        }
    }

    // 2. 品牌图标 - 使用品牌色背景+文字
    if (brandIcon != null) {
        Box(
            modifier = modifier
                .size(size.dp)
                .background(brandIcon.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(0.5.dp, brandIcon.color.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = brandIcon.text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (size * 0.35).sp
                ),
                fontWeight = FontWeight.Bold,
                color = brandIcon.color,
                maxLines = 1
            )
        }
    } else {
        // 3. 普通emoji图标
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = size.sp
            ),
            modifier = modifier
        )
    }
}

data class DragState(
    val draggingIndex: Int = -1,
    val dragOffsetY: Float = 0f,
    val dragStartY: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    var isExpense by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryData?>(null) }
    var showVisibilityDialog by remember { mutableStateOf(false) }

    var expandedParents by remember { mutableStateOf(setOf<String>()) }
    var deleteTarget by remember { mutableStateOf<CategoryData?>(null) }

    val allCategories = if (isExpense) expenseCategories else incomeCategories
    val parentCategories = allCategories.filter { it.parentName == null && !it.isHidden }.sortedBy { it.sortOrder }

    val listState = rememberLazyListState()
    @Suppress("UNUSED_VARIABLE") val _scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val itemHeightPx = with(density) { 72.dp.toPx() }
    val itemSpacingPx = with(density) { 12.dp.toPx() }
    @Suppress("UNUSED_VARIABLE") val _totalItemHeight = itemHeightPx + itemSpacingPx

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 显示设置：仅保留图标
                    IconButton(onClick = { showVisibilityDialog = true }) {
                        Icon(Icons.Default.Visibility, contentDescription = "显示设置")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加分类")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            TabRow(selectedTabIndex = if (isExpense) 0 else 1) {
                Tab(
                    selected = isExpense,
                    onClick = {
                        isExpense = true
                        expandedParents = setOf()
                    },
                    text = { Text("支出分类") }
                )
                Tab(
                    selected = !isExpense,
                    onClick = {
                        isExpense = false
                        expandedParents = setOf()
                    },
                    text = { Text("收入分类") }
                )
            }

            // 简化的排序提示
            Text(
                "💡 长按分类选择，点击排序按钮调整顺序",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            if (parentCategories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("所有分类已隐藏", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showVisibilityDialog = true }) {
                            Text("去设置显示")
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(parentCategories) { index, parent ->
                            val subCats = allCategories.filter { it.parentName == parent.name }
                            val isExpanded = expandedParents.contains(parent.name)
                            val canMoveUp = index > 0
                            val canMoveDown = index < parentCategories.size - 1

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ParentCategoryItem(
                                    category = parent,
                                    isExpanded = isExpanded,
                                    subCount = subCats.size,
                                    canMoveUp = canMoveUp,
                                    canMoveDown = canMoveDown,
                                    onToggleExpand = {
                                        expandedParents = if (isExpanded) expandedParents - parent.name else expandedParents + parent.name
                                    },
                                    onEdit = { editingCategory = parent },
                                    onDelete = { deleteTarget = parent },
                                    onAddSub = {
                                        editingCategory = CategoryData(
                                            name = "", icon = "",
                                            type = if (isExpense) "expense" else "income",
                                            parentName = parent.name
                                        )
                                    },
                                    onMoveUp = {
                                        if (canMoveUp) {
                                            viewModel.moveCategoryToPosition(parent, index, index - 1)
                                        }
                                    },
                                    onMoveDown = {
                                        if (canMoveDown) {
                                            viewModel.moveCategoryToPosition(parent, index, index + 1)
                                        }
                                    }
                                )
                                if (isExpanded) {
                                    subCats.forEach { sub ->
                                        SubCategoryItem(
                                            category = sub,
                                            onEdit = { editingCategory = sub },
                                            onDelete = { viewModel.deleteCategory(sub.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingCategory != null) {
        CategoryEditDialog(
            category = editingCategory,
            parentCategories = parentCategories,
            isExpense = isExpense,
            onDismiss = { showAddDialog = false; editingCategory = null },
            onSave = { name, icon, type, parentName ->
                val finalIcon = if (icon.isBlank()) IconMatcher.matchIcon(name) else icon
                viewModel.addCategory(name, finalIcon, type, parentName)
                showAddDialog = false; editingCategory = null
            },
            onUpdate = { category ->
                val finalIcon = if (category.icon.isBlank()) IconMatcher.matchIcon(category.name) else category.icon
                viewModel.updateCategory(category.copy(icon = finalIcon))
                showAddDialog = false; editingCategory = null
            }
        )
    }

    if (showVisibilityDialog) {
        CategoryVisibilityDialog(viewModel = viewModel, isExpense = isExpense, onDismiss = { showVisibilityDialog = false })
    }

    if (deleteTarget != null) {
        val target = deleteTarget!!
        val subCats = allCategories.filter { it.parentName == target.name }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除分类") },
            text = {
                Column {
                    Text("确定要删除分类「${target.icon} ${target.name}」吗？", style = MaterialTheme.typography.bodyMedium)
                    if (subCats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("该分类下有 ${subCats.size} 个子分类，将一并删除：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        subCats.forEach { sub ->
                            Text("  · ${sub.icon} ${sub.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 先删子分类，再删父分类
                        subCats.forEach { viewModel.deleteCategory(it.id) }
                        viewModel.deleteCategory(target.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryVisibilityDialog(
    viewModel: TransactionViewModel,
    isExpense: Boolean,
    onDismiss: () -> Unit
) {
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val allCategories = if (isExpense) expenseCategories else incomeCategories
    val parentCategories = allCategories.filter { it.parentName == null }.sortedBy { it.sortOrder }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("分类显示设置")
            }
        },
        text = {
            Column {
                Text(
                    "控制哪些一级分类在分类管理和记账页面显示，隐藏后交易记录不受影响",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(parentCategories) { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${category.icon} ${category.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                color = if (category.isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = !category.isHidden,
                                onCheckedChange = { visible -> viewModel.updateCategory(category.copy(isHidden = !visible)) },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ParentCategoryItem(
    category: CategoryData,
    isExpanded: Boolean,
    @Suppress("UNUSED_PARAMETER") subCount: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSub: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    // 一级分类删除对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除分类" ) },
            text = { Text("确定要删除 ${category.name} 分类吗？\n\n删除此一级分类将同时删除其所有子分类。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        showConfirmDeleteDialog = true
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) { Text("取消") }
            }
        )
    }

    // 二次确认删除对话框
    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("确认删除" ) },
            text = { Text("⚠️ 删除 ${category.name} 分类后无法恢复！\n\n点击\"确认删除\"继续，或点击\"取消\"中止。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDeleteDialog = false
                    }
                ) { Text("确认删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDeleteDialog = false }
                ) { Text("取消") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 展开/折叠按钮（点击展开/折叠）
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 分类图标+名称（点击展开/折叠）
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(icon = category.icon, size = 24)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 排序按钮组（紧凑布局）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 上移按钮
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // 下移按钮
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // 更多操作按钮（菜单）
            IconButton(
                onClick = { showSortDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "更多操作",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 更多操作菜单对话框
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("分类操作") },
            text = {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // 添加子分类
                    TextButton(
                        onClick = {
                            onAddSub()
                            showSortDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("添加子分类")
                        }
                    }

                    // 编辑分类
                    TextButton(
                        onClick = {
                            onEdit()
                            showSortDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("编辑分类")
                        }
                    }

                    // 删除分类
                    TextButton(
                        onClick = {
                            showSortDialog = false
                            showDeleteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("删除分类")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("关闭") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryItem(
    category: CategoryData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
            .clickable { onEdit() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧色条（更低调，仅作为视觉分隔）
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(icon = category.icon, size = 20)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.DeleteOutline, "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
    // 分割线
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp, end = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: CategoryData?,
    parentCategories: List<CategoryData>,
    isExpense: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, type: String, parentName: String?) -> Unit,
    onUpdate: (CategoryData) -> Unit
) {
    val isEditing = category != null && category.id != 0L
    val isAddingSub = category != null && category.parentName != null && category.id == 0L

    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "") }
    var selectedParent by remember { mutableStateOf(
        if (isAddingSub) parentCategories.find { it.name == category?.parentName } else null
    ) }
    var showIconPicker by remember { mutableStateOf(false) }

    val autoIcon = if (icon.isBlank() && name.isNotBlank()) IconMatcher.matchIcon(name) else icon

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(when {
                isEditing -> "编辑分类"
                isAddingSub -> "添加子分类"
                else -> "添加总分类"
            })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("分类名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = icon.ifBlank { autoIcon }, onValueChange = { icon = it }, label = { Text("图标") }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clickable { showIconPicker = true }) {
                        CategoryIcon(icon = icon.ifBlank { autoIcon }, size = 32)
                    }
                }
                OutlinedButton(onClick = { showIconPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从图标库选择")
                }
                if (showIconPicker) {
                    IconPickerDialog(
                        onDismiss = { showIconPicker = false },
                        onSelect = { selectedIcon ->
                            icon = selectedIcon
                            showIconPicker = false
                        }
                    )
                }
                if (!isEditing && !isAddingSub) {
                    Text("父分类（可选）", style = MaterialTheme.typography.bodySmall)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedParent?.name ?: "无（作为总分类）",
                            onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(),
                            leadingIcon = selectedParent?.let { { CategoryIcon(icon = it.icon, size = 20) } }
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("无（作为总分类）") },
                                onClick = { selectedParent = null; expanded = false }
                            )
                            parentCategories.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(parent.name) },
                                    leadingIcon = { CategoryIcon(icon = parent.icon, size = 20) },
                                    onClick = { selectedParent = parent; expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                if (isEditing && category != null) {
                    onUpdate(category.copy(name = name, icon = icon.ifBlank { autoIcon }))
                } else {
                    onSave(name, icon.ifBlank { autoIcon }, if (isExpense) "expense" else "income", selectedParent?.name)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("品牌" to 0, "分类" to 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标") },
        text = {
            Column(modifier = Modifier.height(400.dp)) {
                // Tab 切换
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, (title, _) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> BrandIconGrid(onSelect = onSelect)
                    1 -> CategoryIconGrid(onSelect = onSelect)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun BrandIconGrid(onSelect: (String) -> Unit) {
    val brands = IconLibrary.brandIcons.entries.toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(brands.size) { index ->
            val (name, brandIcon) = brands[index]
            BrandIconItem(
                name = name,
                brandIcon = brandIcon,
                onClick = { onSelect(name) }
            )
        }
    }
}

@Composable
private fun BrandIconItem(
    name: String,
    brandIcon: IconLibrary.BrandIcon,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val drawableName = IconLibrary.brandIconResources[name]

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(brandIcon.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, brandIcon.color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (drawableName != null) {
                val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                if (resId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = resId),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Text(
                        text = brandIcon.text,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = brandIcon.color,
                        maxLines = 1
                    )
                }
            } else {
                Text(
                    text = brandIcon.text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = brandIcon.color,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryIconGrid(onSelect: (String) -> Unit) {
    var selectedCategory by remember { mutableStateOf(0) }
    val categories = IconLibrary.iconCategories

    Column(modifier = Modifier.fillMaxSize()) {
        // 分类选择器（横向滚动）
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            edgePadding = 0.dp
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategory == index,
                    onClick = { selectedCategory = index },
                    text = { Text(category.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 图标网格
        val currentIcons = categories.getOrNull(selectedCategory)?.icons ?: emptyList()
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentIcons.size) { index ->
                val emoji = currentIcons[index]
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onSelect(emoji) }
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}
