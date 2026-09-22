package com.example.ui

import android.content.Context
import coil.ImageLoader
import coil.request.CachePolicy

/**
 * Configuration optimisée de Coil pour l'application Admin et Livreur.
 * Implémente une politique de mise en cache agressive (mémoire et disque)
 * pour économiser le forfait de données mobiles des livreurs et accélérer l'affichage.
 */
object CoilImageConfig {

    @Volatile
    private var instance: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .respectCacheHeaders(false) // Favorise l'utilisation du cache même avec des headers stricts
                .crossfade(true)
                .build().also { instance = it }
        }
    }
}
