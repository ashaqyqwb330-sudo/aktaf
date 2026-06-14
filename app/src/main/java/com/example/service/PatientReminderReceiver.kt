package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class PatientReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("PATIENT_NAME") ?: "المريض"
        val medName = intent.getStringExtra("MEDICATION_NAME") ?: "الدواء"
        val dosage = intent.getStringExtra("MEDICATION_DOSAGE") ?: ""
        val age = intent.getIntExtra("PATIENT_AGE", 40)

        val serviceIntent = Intent(context, PatientReminderService::class.java).apply {
            putExtra("PATIENT_NAME", name)
            putExtra("MEDICATION_NAME", medName)
            putExtra("MEDICATION_DOSAGE", dosage)
            putExtra("PATIENT_AGE", age)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
