package com.example.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AdminOrderRepository
import com.example.data.FirestoreSyncManager
import com.google.firebase.FirebaseApp
import kotlin.concurrent.thread

object Graph {
    lateinit var db: AppDatabase
        private set
    
    lateinit var appContext: Context
        private set

    val repository: AppRepository by lazy {
        AppRepository(db.appDao(), db.pendingActionDao())
    }

    val adminOrderRepository: AdminOrderRepository by lazy {
        AdminOrderRepository(db.orderDao(), db.pendingActionDao(), appContext)
    }

    val firestoreSyncManager: FirestoreSyncManager by lazy {
        FirestoreSyncManager(repository, db.pendingActionDao())
    }

    fun provide(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx

        try {
            appCtx.deleteDatabase("bella_pressing.db")
            appCtx.deleteDatabase("bella_pressing_v26.db")
            appCtx.deleteDatabase("bella_pressing_v20.db")
        } catch (_: Throwable) {}

        db = Room.databaseBuilder(appCtx, AppDatabase::class.java, "bella_pressing_v27.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

        try {
            FirebaseApp.initializeApp(appCtx)
        } catch (e: Throwable) {
            Log.e("Graph", "Firebase init failed", e)
        }

        // Lancement asynchrone sécurisé du sync sans bloquer l'initialisation principale
        thread {
            try {
                firestoreSyncManager.startSync()
            } catch (e: Throwable) {
                Log.e("Graph", "Firestore sync start failed", e)
            }
        }
    }
}
