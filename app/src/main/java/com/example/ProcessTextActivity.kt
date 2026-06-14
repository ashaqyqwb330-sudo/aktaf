package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.database.ClipItem
import com.example.database.EktefaaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        if (selectedText.isNotBlank()) {
            val category = classifyText(selectedText)
            val context = this
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = EktefaaDatabase.getDatabase(context)
                    db.dao().insertClipItem(
                        ClipItem(
                            content = selectedText.trim(),
                            category = category
                        )
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "تم الحفظ في الحافظة الذكية (تصنيف: $category)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "خطأ في الحفظ: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Return the text back to the host application if it is editable
        if (!isReadOnly && selectedText.isNotEmpty()) {
            val resultIntent = Intent().apply {
                putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
            }
            setResult(RESULT_OK, resultIntent)
        }
        finish()
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
}
