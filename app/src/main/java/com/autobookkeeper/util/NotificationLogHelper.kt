package com.autobookkeeper.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 通知日志记录器 —— 存储最近30条通知到 SharedPreferences
 * 用于诊断通知监听是否正常工作
 */
object NotificationLogHelper {
    private const val PREFS_NAME = "notif_logs"
    private const val KEY_LOGS = "logs"
    private const val MAX = 30

    data class LogEntry(
        val time: Long,
        val pkg: String,
        val source: String,
        val title: String,
        val text: String,
        val result: String  // "已记录" / "已排除" / "无金额" / "空文本" / "分组摘要"
    )

    fun log(context: Context, pkg: String, source: String, title: String, text: String, result: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val entries = load(prefs).toMutableList()
            entries.add(0, LogEntry(System.currentTimeMillis(), pkg, source, title.take(50), text.take(200), result))
            while (entries.size > MAX) entries.removeLast()
            save(prefs, entries)
        } catch (_: Exception) { }
    }

    fun getLogs(context: Context): List<LogEntry> {
        return try {
            load(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
        } catch (_: Exception) { emptyList() }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_LOGS).apply()
    }

    private fun load(prefs: SharedPreferences): List<LogEntry> {
        val json = prefs.getString(KEY_LOGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val o = arr.getJSONObject(i)
                    LogEntry(
                        o.getLong("t"), o.getString("p"), o.getString("s"),
                        o.getString("ti"), o.getString("tx"), o.getString("r")
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun save(prefs: SharedPreferences, entries: List<LogEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            arr.put(JSONObject().apply {
                put("t", e.time); put("p", e.pkg); put("s", e.source)
                put("ti", e.title); put("tx", e.text); put("r", e.result)
            })
        }
        prefs.edit().putString(KEY_LOGS, arr.toString()).apply()
    }
}
