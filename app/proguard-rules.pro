# Garder tous les modèles de données Firestore et Room
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Préserver les annotations Firebase et Android
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Sanctuariser les classes Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class com.google.firebase.firestore.** { *; }

# Préserver les constructeurs sans argument pour la réflexion de désérialisation
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    <init>();
}

# Garder toutes les classes annotées @Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
