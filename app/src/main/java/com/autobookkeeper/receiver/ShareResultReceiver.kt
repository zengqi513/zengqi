package com.autobookkeeper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.autobookkeeper.backup.BackupHelper
import com.autobookkeeper.util.CsvExportHelper

/**
 * 接收分享完成/取消的回调
 */
class ShareResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CsvExportHelper.ACTION_SHARE_COMPLETED -> {
                Toast.makeText(context, "导出完成", Toast.LENGTH_SHORT).show()
            }
            BackupHelper.ACTION_BACKUP_COMPLETED -> {
                Toast.makeText(context, "备份完成", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
