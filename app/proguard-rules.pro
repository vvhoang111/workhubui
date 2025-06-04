# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in @(link:proguard-android-optimize.txt);
# you can go in there and edit the flags to if you want to change the
# global defaults.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name of your WebView client.
#-keepclassmembers class fqcn.of.your.webview.client {
#    public void *(android.webkit.WebView);
#}

# Add rules for Firebase
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }

# Add rules for Room and SQLCipher
-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** { *; }
-keep class androidx.room.RoomDatabase { *; }
-keep class androidx.room.Room.** { *; }
-keep class androidx.room.util.** { *; }
-keep class androidx.room.InvalidationTracker { *; }
-keep class androidx.room.RoomWarnings { *; }
-keep class androidx.room.DatabaseConfiguration { *; }
-keep class androidx.room.QueryInterceptorProgram { *; }
-keep class androidx.room.TransactionExecutor { *; }
-keep class androidx.room.ColumnInfo { *; }
-keep class androidx.room.PrimaryKey { *; }
-keep class androidx.room.Entity { *; }
-keep class androidx.room.Dao { *; }
-keep class androidx.room.Insert { *; }
-keep class androidx.room.Query { *; }
-keep class androidx.room.Update { *; }
-keep class androidx.room.Delete { *; }
-keep class androidx.room.TypeConverter { *; }

# Add rules for Security Crypto
-keep class androidx.security.crypto.** { *; }

# Add rules for OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }

# Add rules for Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# Add rules for Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keep public class * extends androidx.compose.ui.tooling.preview.PreviewParameterProvider { *; }

# Add rules for your data classes and entities
-keep class com.workhubui.model.** { *; }
-keep class com.workhubui.data.local.entity.** { *; }

# For ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}