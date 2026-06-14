package com.example.service

import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.database.ClipItem
import com.example.database.EktefaaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClipboardMonitorService : Service() {
    private var isMonitoring = false
    private var lastClipedText = ""
    private var clipboardManager: ClipboardManager? = null

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        processLatestClip()
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        startMonitoring()
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "clipboard_monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(channelId) == null) {
                val chan = NotificationChannel(
                    channelId,
                    "مراقبة الحافظة الذكية",
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(chan)
            }
        }

        // Action to stop service nicely from notification button
        val stopIntent = Intent(this, ClipboardMonitorService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            120,
            stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            121,
            mainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("الحافظة العائلية الذكية")
            .setContentText("المراقبة الخلفية نشطة لتلقي النصوص وتصنيفها...")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف المراقبة", stopPendingIntent)
            .setOngoing(true)
            .build()

        startForeground(9921, notification)
    }

    private fun startMonitoring() {
        if (!isMonitoring) {
            try {
                clipboardManager?.addPrimaryClipChangedListener(listener)
                isMonitoring = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopMonitoring() {
        if (isMonitoring) {
            try {
                clipboardManager?.removePrimaryClipChangedListener(listener)
                isMonitoring = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processLatestClip() {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotBlank() && text != lastClipedText) {
                lastClipedText = text
                val cat = classifyText(text)
                saveToDatabase(text, cat)
            }
        }
    }

    private fun saveToDatabase(content: String, category: String) {
        val context = this
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = EktefaaDatabase.getDatabase(context)
                db.dao().insertClipItem(ClipItem(content = content, category = category))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun classifyText(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("خطب") || t.contains("خطبة") || t.contains("الجمعة") -> "خطب"
            t.contains("اللهم") || t.contains("ربنا") || t.contains("أدع") || t.contains("دعاء") || t.contains("يارب") -> "أدعية"
            t.contains("حلال") || t.contains("حرام") || t.contains("فتوى") || t.contains("فقه") || t.contains("حكم") || t.contains("يجوز") -> "فقه"
            t.contains("نصيحة") || t.contains("أنصح") || t.contains("ينصح") || t.contains("حكمة") -> "نصائح"
            t.contains("تربية") || t.contains("الطفل") || t.contains("أولاد") || t.contains("أبناء") -> "تربية"
            t.contains("وصفة") || t.contains("طريقة عمل") || t.contains("أكل") || t.contains("طبخ") -> "وصفات"
            else -> "عام"
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }
}
