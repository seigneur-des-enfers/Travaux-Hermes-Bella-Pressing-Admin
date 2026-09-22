cat << 'INNER_EOF' > app/src/main/java/com/example/di/Graph.kt
package com.example.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.FirestoreSyncManager
import com.google.firebase.FirebaseApp

object Graph {
    lateinit var db: AppDatabase
        private set
    
    val repository by lazy {
        AppRepository(db.appDao())
    }

    val firestoreSyncManager by lazy {
        FirestoreSyncManager(repository)
    }

    fun provide(context: Context) {
        db = Room.databaseBuilder(context, AppDatabase::class.java, "bella_pressing.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        try {
            FirebaseApp.initializeApp(context)
        } catch (e: Throwable) {
            Log.e("Graph", "Firebase init failed", e)
        }
        try {
            firestoreSyncManager.startSync()
        } catch (e: Throwable) {
            Log.e("Graph", "Firestore sync start failed", e)
        }
    }
}
INNER_EOF
