package com.example

import android.app.Application
import com.example.database.LibraryContentImporter
import com.example.database.EktefaaDatabase
import com.example.data.PrayersData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            LibraryContentImporter.importIfNeeded(this@App)
            try {
                com.example.database.AssetManager.importParallelIfNeeded(this@App)
            } catch (e: Exception) {
                android.util.Log.e("App", "Failed AssetManager preloading", e)
            }
            
            try {
                val db = EktefaaDatabase.getDatabase(this@App)
                if (db.prayerTextDao().getAllPrayers().isEmpty()) {
                    PrayersData.getAllPrayers().forEach { prayer ->
                        db.prayerTextDao().insertPrayer(prayer)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("App", "Failed to populate prayers database", e)
            }
        }
    }
}
