# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Keep Compose metadata
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
