package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object QuranDownloadTracker {
    var downloadingSurahId = mutableStateOf<Int?>(null)
    var progress = mutableStateOf(0f)
    var label = mutableStateOf("")
}

class QuranAudioDownloadService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val ACTION_START_DOWNLOAD = "com.example.service.action.START_DOWNLOAD"
        const val EXTRA_SURAH_ID = "EXTRA_SURAH_ID"
        const val EXTRA_TOTAL_VERSES = "EXTRA_TOTAL_VERSES"
        const val EXTRA_IS_TEACHER = "EXTRA_IS_TEACHER"
        private const val CHANNEL_ID = "quran_download_channel"
        private const val NOTIFICATION_ID = 404
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_DOWNLOAD) {
            val surahId = intent.getIntExtra(EXTRA_SURAH_ID, -1)
            val totalVerses = intent.getIntExtra(EXTRA_TOTAL_VERSES, -1)
            val isTeacher = intent.getBooleanExtra(EXTRA_IS_TEACHER, false)

            if (surahId != -1 && totalVerses != -1) {
                // If specialUse FGS type is used, startForeground is required within 10 seconds of onStartCommand for targeting high API levels
                startForeground(NOTIFICATION_ID, createNotification("تهيئة تحميل صوتيات المصحف...", 0))
                startDownloading(surahId, totalVerses, isTeacher)
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownloading(surahId: Int, totalVerses: Int, isTeacher: Boolean) {
        serviceScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    QuranDownloadTracker.downloadingSurahId.value = surahId
                    QuranDownloadTracker.progress.value = 0f
                    QuranDownloadTracker.label.value = "بدء التحميل..."
                }

                val subFolder = if (isTeacher) "teacher" else "murattal"
                val parentDir = File(filesDir, "quran_audio/$subFolder")
                if (!parentDir.exists()) parentDir.mkdirs()

                val pathName = if (isTeacher) "Minshawy_Teacher_128kbps" else "Minshawy_Murattal_128kbps"

                for (v in 1..totalVerses) {
                    val progressVal = v.toFloat() / totalVerses
                    val labelText = "تحميل آية $v من $totalVerses..."

                    withContext(Dispatchers.Main) {
                        QuranDownloadTracker.progress.value = progressVal
                        QuranDownloadTracker.label.value = labelText
                    }

                    updateNotification("جاري تحميل سورة $surahId - آية $v/$totalVerses", (progressVal * 100).toInt())

                    // Format URL
                    val sStr = String.format("%03d", surahId)
                    val vStr = String.format("%03d", v)
                    val urlString = "https://everyayah.com/data/$pathName/$sStr$vStr.mp3"
                    val localFile = File(parentDir, "${surahId}_${v}.mp3")

                    if (!localFile.exists() || localFile.length() < 1000) {
                        var success = false
                        var attempts = 0
                        while (!success && attempts < 3) {
                            try {
                                val bytes = URL(urlString).readBytes()
                                FileOutputStream(localFile).use { fos ->
                                    fos.write(bytes)
                                }
                                success = true
                            } catch (e: Exception) {
                                attempts++
                                delay(100)
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    QuranDownloadTracker.downloadingSurahId.value = null
                    QuranDownloadTracker.progress.value = 1f
                    QuranDownloadTracker.label.value = "تم التحميل بنجاح!"
                }

                updateNotification("اكتمل تحميل السورة رقم $surahId بنجاح! ✅", 100)
                delay(1500)
                stopForeground(true)
                stopSelf()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    QuranDownloadTracker.downloadingSurahId.value = null
                }
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun createNotification(text: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("تحميل سور وآيات المصحف الكريم معلم ومرتل")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(text, progress))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحميل صوتيات المصحف",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "خدمة خلفية لتحميل الآيات بالتفصيل"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
