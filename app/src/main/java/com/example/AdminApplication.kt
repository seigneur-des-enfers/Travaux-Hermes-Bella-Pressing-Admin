package com.example

import android.app.Application
import android.util.Log
import com.example.di.Graph
import com.google.firebase.FirebaseApp

class AdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Erreur Firebase", e)
        }
        Graph.provide(this) // APRÈS Firebase
    }
}
