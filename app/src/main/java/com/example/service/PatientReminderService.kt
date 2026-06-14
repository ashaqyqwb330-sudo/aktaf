package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.HealthHubActivity
import com.example.R
import java.util.*

class PatientReminderService : Service() {
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var reminderCount = 0
    private var patientName = ""
    private var medicationName = ""
    private var medicationDosage = ""
    private var patientAge = 0
    private var isTtsInitialized = false

    // Queue-based audio state variables
    private val audioQueue = java.util.concurrent.ConcurrentLinkedQueue<String>()
    private var isPlayingAudio = false
    private var currentRingtone: android.media.Ringtone? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
                isTtsInitialized = true
            }
        }
        createNotificationChannel()
    }

    companion object {
        const val ACTION_CLEANUP_AUDIO = "com.example.ACTION_CLEANUP_AUDIO"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLEANUP_AUDIO) {
            handler.removeCallbacksAndMessages(null)
            try {
                currentRingtone?.stop()
            } catch (e: Exception) {}
            tts?.stop()
            audioQueue.clear()
            isPlayingAudio = false
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        patientName = intent?.getStringExtra("PATIENT_NAME") ?: "المريض"
        medicationName = intent?.getStringExtra("MEDICATION_NAME") ?: "الدواء"
        medicationDosage = intent?.getStringExtra("MEDICATION_DOSAGE") ?: ""
        patientAge = intent?.getIntExtra("PATIENT_AGE", 40) ?: 40

        startForeground(200, buildNotification("جاري متابعة $patientName"))
        startReminderLoop()

        return START_STICKY
    }

    private fun startReminderLoop() {
        val runnable = object : Runnable {
            override fun run() {
                reminderCount++
                if (reminderCount > 3) {
                    stopSelf()
                    return
                }

                // اختيار العبارة المناسبة حسب العمر والوقت
                val greeting = getTimeBasedGreeting()
                val message = buildReminderMessage(greeting)

                // وضع الرنين والرسالة الصوتية بالدور لضمان عدم التداخل
                enqueueAlert(message)

                // تحديث الإشعار
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(200, buildNotification("تذكير $reminderCount: $medicationName"))

                // إعادة التشغيل بعد دقيقة إذا لم يؤكد
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(runnable)
    }

    private fun enqueueAlert(message: String) {
        audioQueue.add(message)
        processQueue()
    }

    private fun processQueue() {
        if (isPlayingAudio || audioQueue.isEmpty()) return
        isPlayingAudio = true
        val message = audioQueue.poll() ?: return

        // 1. تشغيل جرس إنذار قصير وتنبيه مريح أولاً
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            currentRingtone = RingtoneManager.getRingtone(this@PatientReminderService, soundUri)
            currentRingtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // انتظر 2 ثانية، أوقف الرنين ثم تحدّث
        handler.postDelayed({
            try {
                currentRingtone?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (isTtsInitialized && tts != null) {
                val params = android.os.Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "reminder_tts")
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        handler.post {
                            isPlayingAudio = false
                            processQueue()
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        handler.post {
                            isPlayingAudio = false
                            processQueue()
                        }
                    }
                })
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "reminder_tts")
            } else {
                handler.postDelayed({
                    isPlayingAudio = false
                    processQueue()
                }, 4000)
            }
        }, 2200)
    }

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..11 -> "صباح الخير"
            hour in 12..17 -> "مساء الخير"
            else -> "مساء الخير"
        }
    }

    private fun buildReminderMessage(greeting: String): String {
        // اختيار العبارة حسب العمر
        val style = when {
            patientAge < 6 -> "أهلين بالصغير"
            patientAge < 18 -> "هلا بالشاطر"
            patientAge < 60 -> "السلام عليكم يا بطل"
            else -> "السلام عليكم ورحمة الله يا حبيبنا"
        }

        val phrases = listOf(
            "$greeting. $style $patientName. حان وقت تناول $medicationName. الجرعة: $medicationDosage. شفاك الله وعافاك.",
            "$greeting. $patientName، تذكر $medicationName الآن. $medicationDosage. صحتك تهمنا.",
            "$greeting. $patientName، لا تنسى $medicationName. $medicationDosage. بارك الله فيك.",
            "$greeting. $patientName، $medicationName في انتظارك. $medicationDosage. حفظك الله."
        )

        return phrases[reminderCount % phrases.size]
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, HealthHubActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, "patient_reminder")
            .setContentTitle("⏰ منبه الأدوية الذكي لموقع عائلتي")
            .setContentText("$text - الدواء القادم: $medicationName ($medicationDosage)")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setColor(0xFFD4AF37.toInt()) // Golden Accent Color
            .setColorized(true)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "patient_reminder",
                "تنبيهات الأدوية",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            currentRingtone?.stop()
        } catch (e: Exception) {}
        tts?.stop()
        tts?.shutdown()
        audioQueue.clear()
        isPlayingAudio = false
        super.onDestroy()
    }
}
