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
    
    lateinit var appContext: Context
        private set

    val repository by lazy {
        AppRepository(db.appDao(), db.pendingActionDao())
    }

    val adminOrderRepository by lazy {
        com.example.data.AdminOrderRepository(db.orderDao(), db.pendingActionDao(), appContext)
    }

    val firestoreSyncManager by lazy {
        FirestoreSyncManager(repository, db.pendingActionDao())
    }

    fun provide(context: Context) {
        appContext = context.applicationContext
        try {
            context.deleteDatabase("bella_pressing.db")
            context.deleteDatabase("bella_pressing_v26.db")
            context.deleteDatabase("bella_pressing_v20.db")
        } catch (_: Throwable) {}
        db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "bella_pressing_v27.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
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
