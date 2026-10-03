# Preserve Firebase & Google Play Services classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Preserve Data Models (Room entities & Firebase JSON DTOs)
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Jackson/Moshi/Gson serializers
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
}

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
